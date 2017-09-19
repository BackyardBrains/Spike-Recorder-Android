package com.backyardbrains.audio;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.data.DataProcessor;
import com.backyardbrains.utils.UsbUtils;
import java.util.Arrays;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class SampleStreamProcessor implements DataProcessor {

    private static final String TAG = makeLogTag(SampleStreamProcessor.class);

    private static final int SAMPLE_COUNT = (int) (UsbUtils.SAMPLE_RATE * 26.46); // 6s
    private static final int CLEANER = 0xFF;
    private static final int REMOVER = 0x7F;

    // Responsible for detecting and processing escape sequences within incoming data
    private final EscapeSequence escapeSequence = new EscapeSequence();

    // Buffer that holds most recent 680 ms of audio
    private RingBuffer buffer;
    private int lastIncomingBufferSize;
    private Sample unfinishedSample;

    private int printLineCounter;

    public SampleStreamProcessor() {
        // init buffers
        reset();
    }

    @Nullable @Override public short[] process(@NonNull byte[] data) {
        if (data.length >= 1) {
            processIncomingData(data);

            //LOGD(TAG, "6. USB - BEFORE returning processed data");
            return buffer.getArray();
        }

        return new short[0];
    }

    private void reset() {
        LOGD(TAG, "reset()");
        buffer = new RingBuffer(SAMPLE_COUNT);
    }

    private void processIncomingData(@NonNull byte[] data) {
        //LOGD(TAG, "4. USB - BEFORE processing");
        //long start = System.currentTimeMillis();
        //LOGD(TAG, "==========================================");
        //LOGD(TAG, "START - " + samplesForCalculation.size());

        // reset buffers if size  of buffer changed
        //if (sb.capacity() != lastIncomingBufferSize) {
        //    reset();
        //    lastIncomingBufferSize = sb.capacity();
        //}
        //LOGD(TAG, "1. AFTER resetting buffers:" + (System.currentTimeMillis() - start));

        // initialize incoming array
        //byte[] incomingAsArray = new byte[sb.capacity()];
        //sb.get(incomingAsArray, 0, incomingAsArray.length);

        // Max number of samples can be number of incoming bytes divided by 2 +1
        short[] samples = new short[data.length / 2 + 1];
        short[] orgSamples = new short[data.length / 2 + 1];
        int sampleCounter = 0;
        int lsb, msb; // less significant and most significant bytes
        for (byte b : data) {
            // 1. check if we are inside escape sequence or not
            // test the next byte to see if the sequence is valid
            if (!escapeSequence.test(b)) {
                byte[] sequence = escapeSequence.getSequence();
                for (byte b1 : sequence) {
                    // check if we have unfinished frame
                    if (unfinishedSample != null) {
                        lsb = b1 & CLEANER;

                        // if less significant byte is also grater then 127 make it most significant
                        if (lsb > 127) {
                            //LOGD(TAG, "LSB > 127! DROP!");
                            unfinishedSample = new Sample(lsb);
                            continue;
                        }

                        unfinishedSample.setLsb(lsb);

                        int smpl = unfinishedSample.getSample();
                        orgSamples[sampleCounter] = (short) smpl;
                        //samples[sampleCounter++] = (short) printLineCounter++;
                        //if (printLineCounter == 1024) printLineCounter = 0;
                        samples[sampleCounter++] = (short) normalize(smpl);
                        //LOGD(TAG, "ADDED NEW SAMPLE " + (sampleCounter - 1) + " org: " + smpl + ", norm: " + samples[
                        //    sampleCounter - 1]);

                        unfinishedSample = null;
                    } else {
                        msb = b1 & CLEANER;
                        if (msb > 127) {
                            unfinishedSample = new Sample(msb);
                        } else {
                            //LOGD(TAG, "MSB < 127! DROP!");
                        }
                    }
                }

                escapeSequence.reset();
            } else {
                if (escapeSequence.isCompleted()) {
                    // TODO: 7/29/2017 Process message
                    // clear the escape sequence instance so we can start detecting the next one
                    escapeSequence.reset();
                }
            }
        }

        short[] smpls = Arrays.copyOfRange(orgSamples, 0, sampleCounter);
        //LOGD(TAG, printLineCounter++ + ". " + smpls.length + " >> " + Arrays.toString(smpls));

        buffer.add(Arrays.copyOfRange(samples, 0, sampleCounter));

        //LOGD(TAG, "5. USB - AFTER processing");
    }

    private int normalize(int sample) {
        return (sample - 512) * 30;
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
            new byte[] { (byte) 0xFF, (byte) 0xFF, 0x01, 0x01, (byte) 0x80, (byte) 0xFF };
        private final byte[] MESSAGE_END_SEQUENCE = new byte[] {
            (byte) 0xFF, (byte) 0xFF, 0x01, 0x01, (byte) 0x81, (byte) 0xFF
        };
        private final int MAX_MESSAGE_LENGTH = 5000;

        int index = 0;
        int tmpIndex = 0;
        boolean started;
        boolean ended;
        byte[] start = new byte[MESSAGE_START_SEQUENCE.length];
        byte[] end = new byte[MESSAGE_END_SEQUENCE.length];
        byte[] sequence = new byte[MAX_MESSAGE_LENGTH];

        EscapeSequence() {
            reset();
        }

        /**
         * Tests whether specified {@code b} byte, when appended to already passed bytes, makes a valid escape sequence
         * and it returns {@code true} if it does, {@code false} otherwise.
         */
        boolean test(byte b) {
            boolean result = false;

            sequence[index++] = b;

            if (!started && MESSAGE_START_SEQUENCE[tmpIndex] == b) {
                start[tmpIndex++] = b;
                result = true;
            }
            if (Arrays.equals(MESSAGE_START_SEQUENCE, start)) {
                LOGD(TAG, "Escape sequence started!");

                started = true;
                tmpIndex = 0; // reset index, we need it for end of sequence
                result = true;
            }
            if (started && !ended && MESSAGE_END_SEQUENCE[tmpIndex] == b) {
                end[tmpIndex++] = b;
                result = true;
            }
            if (Arrays.equals(MESSAGE_END_SEQUENCE, end)) {
                LOGD(TAG, "Escape sequence ended! Message: " + new String(sequence));

                ended = true;
                result = true;
            }

            return result;
        }

        /**
         * Resets the instance and prepares it for detection of next escape sequence.
         */
        void reset() {
            index = 0;
            tmpIndex = 0;
            started = false;
            ended = false;
            start = new byte[MESSAGE_START_SEQUENCE.length];
            end = new byte[MESSAGE_END_SEQUENCE.length];
            sequence = new byte[MAX_MESSAGE_LENGTH];
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
            final int from = MESSAGE_START_SEQUENCE.length - 1;
            final int to = MESSAGE_END_SEQUENCE.length;

            if (sequence.length <= MESSAGE_START_SEQUENCE.length + MESSAGE_END_SEQUENCE.length) return new byte[0];

            return Arrays.copyOfRange(sequence, from, to);
        }
    }

    // da li moze da stigne samo pola frame-a? - DA
    // da li moze poruka da stigne usred frame-a? - DA
    // kada naidjem na nelogicnost (npr. dva byte-a > 127) da li dropujem samo prvi ili oba? - SAMO PRVI
    // da li moze da stigne samo deo escape sequence-a? - DA
}
