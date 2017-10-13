package com.backyardbrains.audio;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.data.DataProcessor;
import com.backyardbrains.utils.SpikerShieldBoardType;
import com.backyardbrains.utils.UsbUtils;
import java.util.Arrays;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGW;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class SampleStreamProcessor implements DataProcessor {

    private static final String TAG = makeLogTag(SampleStreamProcessor.class);

    private static final int CLEANER = 0xFF;
    private static final int REMOVER = 0x7F;

    // Responsible for detecting and processing escape sequences within incoming data
    private final EscapeSequence escapeSequence = new EscapeSequence();

    // Buffer that holds most recent 680 ms of audio
    private Sample unfinishedSample;
    // Additional filtering that should be applied
    private Filters filters;

    /**
     * Listens for responses sent by connected device as a response to custom messages sent by the application.
     */
    interface SampleStreamListener {
        /**
         * Triggered when SpikerShield sends board type message as a result of inquiry.
         */
        void onBoardTypeDetected(@SpikerShieldBoardType int boardType);
    }

    private SampleStreamListener listener;

    public SampleStreamProcessor(@Nullable SampleStreamListener listener, @Nullable Filters filters) {
        this.listener = listener;
        this.filters = filters;
    }

    @Override public short[] process(@NonNull byte[] data) {
        if (data.length > 0) return processIncomingData(data);

        return new short[0];
    }

    private short[] processIncomingData(@NonNull byte[] data) {
        // Max number of samples can be number of incoming bytes divided by 2 +1
        short[] samples = new short[data.length / 2 + 1];
        int sampleCounter = 0;
        int lsb, msb; // less significant and most significant bytes

        //LOGD(TAG, "START processing new batch of " + data.length + " bytes!");

        for (byte b : data) {
            // 1. check if we are inside escape sequence or not
            // test the next byte to see if the sequence is valid
            if (!escapeSequence.test(b)) {
                //LOGD(TAG, "Escape sequence test failed!!");
                byte[] sequence = escapeSequence.getSequence();
                for (byte b1 : sequence) {
                    // check if we have unfinished frame
                    if (unfinishedSample != null) {
                        lsb = b1 & CLEANER;

                        // if less significant byte is also grater then 127 make it most significant
                        if (lsb > 127) {
                            LOGW(TAG, "LSB > 127! DROP!");
                            unfinishedSample = new Sample(lsb);
                            continue;
                        }

                        unfinishedSample.setLsb(lsb);

                        samples[sampleCounter] = (short) normalize(unfinishedSample.getSample());
                        // apply additional filtering if necessary
                        if (filters != null) samples[sampleCounter] = filters.apply(samples[sampleCounter]);
                        sampleCounter++;

                        unfinishedSample = null;
                    } else {
                        msb = b1 & CLEANER;
                        if (msb > 127) {
                            unfinishedSample = new Sample(msb);
                        } else {
                            LOGW(TAG, "MSB < 127! DROP!");
                        }
                    }
                }

                escapeSequence.reset();
            } else {
                if (escapeSequence.isCompleted()) {
                    // let's process incoming message
                    processEscapeSequenceMessage(escapeSequence.getMessage());
                    // clear the escape sequence instance so we can start detecting the next one
                    LOGD(TAG, "Escape sequence is completed!");
                    escapeSequence.reset();
                }
            }
        }

        //LOGD(TAG, "END processing new batch of bytes!");

        //buffer.add(Arrays.copyOfRange(samples, 0, sampleCounter));
        return Arrays.copyOfRange(samples, 0, sampleCounter);

        //LOGD(TAG, "5. USB - AFTER processing");
    }

    private int normalize(int sample) {
        return (sample - 512) * 30;
    }

    // Processes escape sequence message and triggers appropriate listener
    private void processEscapeSequenceMessage(byte[] messageBytes) {
        final String message = new String(messageBytes);
        // check if it's board type message
        if (listener != null) {
            if (UsbUtils.isBoardTypeMsg(message)) listener.onBoardTypeDetected(UsbUtils.getBoardType(message));
        }
    }

    private class Frame {
        Sample[] samples;
    }

    private class Sample {

        int lsb, msb; // less and most significant bytes

        Sample(int msb) {
            this.msb = msb;
        }

        void setLsb(int lsb) {
            this.lsb = lsb;
        }

        int getSample() {
            msb = msb & REMOVER;
            msb = msb << 7;
            lsb = lsb & REMOVER;
            return msb | lsb;
        }
    }

    /**
     *
     */
    private class EscapeSequence {

        private final String TAG = makeLogTag(EscapeSequence.class);

        private final byte[] MESSAGE_START_SEQUENCE =
            new byte[] { (byte) 255, (byte) 255, (byte) 1, (byte) 1, (byte) 128, (byte) 255 };
        private final byte[] MESSAGE_END_SEQUENCE = new byte[] {
            (byte) 255, (byte) 255, (byte) 1, (byte) 1, (byte) 129, (byte) 255
        };
        // message cannot be longer than 64 bytes so sequence is sequence start + 64 + sequence end
        private final int MAX_SEQUENCE_LENGTH = MESSAGE_START_SEQUENCE.length + 64 + MESSAGE_END_SEQUENCE.length;

        int index = 0;
        int tmpIndex = 0;
        boolean started;
        boolean ended;
        byte[] start = new byte[MESSAGE_START_SEQUENCE.length];
        byte[] end = new byte[MESSAGE_END_SEQUENCE.length];
        byte[] sequence = new byte[MAX_SEQUENCE_LENGTH];

        EscapeSequence() {
            reset();
        }

        /**
         * Tests whether specified {@code b} byte, when appended to already passed bytes, makes a valid escape sequence
         * and it returns {@code true} if it does, {@code false} otherwise.
         */
        boolean test(byte b) {
            // if message is longer then 64 bytes reset
            if (index >= MAX_SEQUENCE_LENGTH) {
                LOGD(TAG, "Escape message longer than 64 bytes. RESET!!!");
                return false;
            }

            // and next byte to sequence
            sequence[index] = b;

            // detect sequence start
            if (!started) {
                if (MESSAGE_START_SEQUENCE[tmpIndex] == b && index < MESSAGE_START_SEQUENCE.length) {
                    LOGD(TAG, "Detected " + tmpIndex + " byte of escape sequence START!");

                    start[tmpIndex++] = b;

                    if (Arrays.equals(MESSAGE_START_SEQUENCE, start)) {
                        LOGD(TAG, "Escape sequence started!");

                        started = true;
                        tmpIndex = 0; // reset index, we need it for sequence end
                    }

                    index++;

                    return true;
                } else {
                    index++;

                    return false;
                }
            } else if (!ended) {
                // populate sequence end test array with last 6 bytes
                System.arraycopy(sequence, index - MESSAGE_END_SEQUENCE.length + 1, end, 0,
                    MESSAGE_END_SEQUENCE.length);

                if (Arrays.equals(MESSAGE_END_SEQUENCE, end)) {
                    LOGD(TAG, "Escape sequence ended!");

                    ended = true;
                }

                index++;

                return true;
            }

            index++;

            return false;
        }

        /**
         * Resets the instance and prepares it for new detection of next escape sequence.
         */
        void reset() {
            index = 0;
            tmpIndex = 0;
            started = false;
            ended = false;
            start = new byte[MESSAGE_START_SEQUENCE.length];
            end = new byte[MESSAGE_END_SEQUENCE.length];
            sequence = new byte[MAX_SEQUENCE_LENGTH];
        }

        /**
         * Whether escape sequence is complete. This means that it contained start and end sequences and the message
         * in between.
         */
        boolean isCompleted() {
            return started && ended;
        }

        /**
         * Returns raw sequence of bytes passed to {@link EscapeSequence}. Raw sequence should be queried if the
         * sequence wasn't completed ({@link #isCompleted()} will return {@code false}). In this case caller should
         * retrieve the raw sequence and process it as sample data.
         */
        byte[] getSequence() {
            return Arrays.copyOfRange(sequence, 0, index);
        }

        /**
         * Returns message sent by the USB device (excluding start and end of escape sequence). Message should be
         * queried only if the sequence was complete which can be checked by calling {@link #isCompleted()}. If message
         * was not complete, caller should call {@link #getSequence()} and process the sequence as sample data.
         *
         * @see #getSequence()
         */
        byte[] getMessage() {
            // don't returns start and end of sequence
            final int from = MESSAGE_START_SEQUENCE.length;
            final int to = index - MESSAGE_END_SEQUENCE.length;

            if (index <= MESSAGE_START_SEQUENCE.length + MESSAGE_END_SEQUENCE.length) return new byte[0];

            return Arrays.copyOfRange(sequence, from, to);
        }
    }

    // Can we get only part of frame in one batch? - YES
    // Can message arrive in the middle of frame? - YES
    // If there is an inconsistency in frame (i.e. two bytes > 127) do we drop only first or both? - ONLY FIRST
    // Can we get only part of escape sequence in one batch? - YES
}
