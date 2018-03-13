package com.backyardbrains.usb;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import com.backyardbrains.audio.Filters;
import com.backyardbrains.data.processing.DataProcessor;
import com.backyardbrains.utils.SampleStreamUtils;
import com.backyardbrains.utils.SpikerBoxHardwareType;
import java.util.Arrays;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.LOGW;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
class SampleStreamProcessor implements DataProcessor {

    private static final String TAG = makeLogTag(SampleStreamProcessor.class);

    private static final int CLEANER = 0xFF;
    private static final int REMOVER = 0x7F;

    private static final int DEFAULT_CHANNEL_COUNT = 1;
    private static final int CHANNEL_INDEX = 0;

    // Responsible for detecting and processing escape sequences within incoming data
    private final EscapeSequence escapeSequence = new EscapeSequence();

    // Most recent unfinished frame
    private Frame unfinishedFrame;
    // Most recent unfinished sample
    private Sample unfinishedSample;
    // Holds currently processed channel
    private int currentChannel = 0;
    // Additional filtering that should be applied
    private Filters filters;
    // Number of channels
    private int channelCount = DEFAULT_CHANNEL_COUNT;
    // Whether channel count has changed during processing of the latest chunk of incoming data
    private boolean channelCountChanged;
    // Average signal which we use to avoid signal offset
    private double average;
    // Collection of events found within one sample batch
    private SparseArray<String> events;

    /**
     * Listens for responses sent by connected device as a response to custom messages sent by the application.
     */
    interface SampleStreamListener {
        /**
         * Triggered when SpikerBox sends hardware type message as a result of inquiry.
         */
        void onSpikerBoxHardwareTypeDetected(@SpikerBoxHardwareType int hardwareType);

        /**
         * Triggered when SpikerBox sends max sample rate and number of channels message as a result of inquiry.
         */
        void onMaxSampleRateAndNumOfChannelsReply(int maxSampleRate, int channelCount);
    }

    private SampleStreamListener listener;

    SampleStreamProcessor(@Nullable SampleStreamListener listener, @Nullable Filters filters) {
        this.listener = listener;
        this.filters = filters;
    }

    @NonNull @Override public short[] process(@NonNull byte[] data, @NonNull SparseArray<String> events) {
        if (data.length > 0) return processIncomingData(data, events);

        return new short[0];
    }

    /**
     * Set number of channels in the sample stream.
     */
    void setChannelCount(int channelCount) {
        this.channelCount = channelCount;

        channelCountChanged = true;
    }

    @NonNull private short[] processIncomingData(@NonNull byte[] data, @NonNull SparseArray<String> events) {
        this.events = events;

        // if channel count has changed during processing  previous data chunk we should disregard
        if (channelCountChanged) {
            unfinishedFrame = null;
            unfinishedSample = null;
            currentChannel = 0;

            channelCountChanged = false;
        }

        final int tmpChannelCount = channelCount;
        short[][] channels = new short[tmpChannelCount][];
        for (int i = 0; i < channels.length; i++) {
            // max number of samples can be number of incoming bytes divided by 2
            channels[i] = new short[(int) (data.length * .5)];
        }
        // array of sample counters, one for every channel
        int[] sampleCounters = new int[tmpChannelCount];
        int channelCounter = currentChannel;
        double avg = average;
        int lsb, msb; // less significant and most significant bytes
        short sample;

        for (byte b : data) {
            // 1. check if we are inside escape sequence or not
            // test the next byte to see if the sequence is valid
            if (!escapeSequence.test(b)) {
                //LOGD(TAG, "Escape sequence test failed!!");
                byte[] sequence = escapeSequence.getSequence();
                for (byte b1 : sequence) {
                    // check if we have unfinished frame
                    if (unfinishedFrame != null) {
                        // check if we have unfinished sample
                        if (unfinishedSample != null) {
                            lsb = b1 & CLEANER;

                            // if less significant byte is also grater then 127 drop whole frame
                            if (lsb > 127) {
                                LOGW(TAG, "LSB > 127! DROP WHOLE FRAME!");
                                unfinishedFrame = null;
                                unfinishedSample = null;
                                channelCounter = 0;
                                continue;
                            }

                            //LOGD(TAG, " --> LSB " + (channelCounter + 1) + ". CHANNEL");
                            unfinishedSample.setLsb(lsb);
                            if (!unfinishedFrame.isFull()) unfinishedFrame.incSampleCount();

                            sample = (short) normalize(unfinishedSample.getSample());
                            // calculate average sample
                            avg = 0.0001 * sample + 0.9999 * avg;
                            // use average to remove offset
                            sample = (short) (sample - avg);
                            // apply additional filtering if necessary
                            if (filters != null) sample = filters.apply(sample);

                            channels[channelCounter][sampleCounters[channelCounter]] = sample;

                            sampleCounters[channelCounter]++;

                            unfinishedSample = null;
                            if (unfinishedFrame.isFull()) {
                                //LOGD(TAG, " --> FRAME END");
                                unfinishedFrame = null;
                            }
                        } else {
                            msb = b1 & CLEANER;
                            // we already started the frame so if msb is greater then 127 drop whole frame
                            if (msb > 127) {
                                LOGW(TAG, "MSB > 127 WITHIN THE FRAME! DROP WHOLE FRAME!");
                                unfinishedFrame = null;
                                unfinishedSample = null;
                                channelCounter = 0;
                            } else {
                                channelCounter++;

                                //LOGD(TAG, " --> MSB " + (channelCounter + 1) + ". CHANNEL");
                                unfinishedSample = new Sample(msb);
                            }
                        }
                    } else {
                        msb = b1 & CLEANER;
                        if (msb > 127) {
                            channelCounter = 0;

                            //LOGD(TAG, " --> FRAME START");
                            unfinishedFrame = new Frame(tmpChannelCount);
                            //LOGD(TAG, " --> MSB " + (channelCounter + 1) + ". CHANNEL");
                            unfinishedSample = new Sample(msb);
                        } else {
                            LOGW(TAG, "MSB < 128 AT FRAME START! DROP!");
                        }
                    }
                }

                escapeSequence.reset();
            } else {
                if (escapeSequence.isCompleted()) {
                    // let's process incoming message
                    processEscapeSequenceMessage(escapeSequence.getMessage(), sampleCounters[CHANNEL_INDEX]);
                    // clear the escape sequence instance so we can start detecting the next one
                    LOGD(TAG, "Escape sequence is completed");
                    escapeSequence.reset();
                }
            }
        }

        //LOGD(TAG, "CHANNEL #1 SAMPLES COUNT: " + sampleCounters[0] + "/" + channels[0].length);
        //LOGD(TAG, "CHANNEL #2 SAMPLES COUNT: " + sampleCounters[1] + "/" + channels[1].length);

        currentChannel = channelCounter;
        average = avg;

        if (sampleCounters[CHANNEL_INDEX] == 0) {
            events.clear();
            return new short[0];
        }

        return Arrays.copyOfRange(channels[CHANNEL_INDEX], 0, sampleCounters[CHANNEL_INDEX]);
    }

    private int normalize(int sample) {
        return (sample - 512) * 30;
    }

    // Processes escape sequence message and triggers appropriate listener
    private void processEscapeSequenceMessage(byte[] messageBytes, int sampleIndex) {
        final String message = new String(messageBytes);
        LOGD(TAG, "ESCAPE MESSAGE: " + message);
        // check if it's board type message
        if (listener != null) {
            if (SampleStreamUtils.isHardwareTypeMsg(message)) {
                listener.onSpikerBoxHardwareTypeDetected(SampleStreamUtils.getBoardType(message));
            } else if (SampleStreamUtils.isSampleRateAndNumOfChannelsMsg(message)) {
                listener.onMaxSampleRateAndNumOfChannelsReply(SampleStreamUtils.getMaxSampleRate(message),
                    SampleStreamUtils.getChannelCount(message));
            } else if (SampleStreamUtils.isEventMsg(message)) {
                events.put(sampleIndex, SampleStreamUtils.getEventNumber(message));
            }
        }
    }

    /**
     * Represents single frame of a sample stream sent by BYB hardware.
     */
    private class Frame {

        int channelCount;
        int counter;

        Frame(int channelCount) {
            this.channelCount = channelCount;
        }

        boolean isFull() {
            return counter >= channelCount;
        }

        void incSampleCount() {
            counter++;
        }
    }

    /**
     * Represents single sample of a sample stream sent by BYB hardware.
     */
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
     * Represents an escape sequence that holds different messages by sent by BYB hardware.
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
}