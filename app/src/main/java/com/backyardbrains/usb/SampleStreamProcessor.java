package com.backyardbrains.usb;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.backyardbrains.audio.Filters;
import com.backyardbrains.data.processing.DataProcessor;
import com.backyardbrains.utils.SampleStreamUtils;
import com.backyardbrains.utils.SpikerBoxHardwareType;
import java.util.Arrays;

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

    // Array of bytes which represent start of an escape sequence
    private final byte[] ESCAPE_SEQUENCE_START =
        new byte[] { (byte) 255, (byte) 255, (byte) 1, (byte) 1, (byte) 128, (byte) 255 };
    // Array of bytes which represent end of an escape sequence
    private final byte[] ESCAPE_SEQUENCE_END = new byte[] {
        (byte) 255, (byte) 255, (byte) 1, (byte) 1, (byte) 129, (byte) 255
    };
    // Message cannot be longer than 64 bytes
    private final int EVENT_MESSAGE_LENGTH = 64;
    // Escape sequence cannot be longer than sequence start + 64 + sequence end
    private final int MAX_SEQUENCE_LENGTH =
        ESCAPE_SEQUENCE_START.length + EVENT_MESSAGE_LENGTH + ESCAPE_SEQUENCE_END.length;

    // Responsible for detecting and processing escape sequences within incoming data
    // Whether we are inside an escape sequence or not
    private boolean insideEscapeSequence;
    // Index of the byte within start or end of the escape sequence
    private int tmpIndex = 0;
    // Holds currently processed escape sequence
    private byte[] escapeSequence = new byte[MAX_SEQUENCE_LENGTH];
    // Index of the byte within currently processed escape sequence
    private int escapeSequenceIndex = 0;
    // Holds currently processed event message
    private byte[] eventMessage = new byte[EVENT_MESSAGE_LENGTH];
    // Index of the byte within currently processed event message
    private int eventMessageIndex = 0;
    // Whether new frame is started being processed
    private boolean frameStarted = false;
    // Whether new sample is started being processed
    private boolean sampleStarted = false;
    // Holds currently processed channel
    private int currentChannel;
    // Most significant and least significant bytes
    private int msb;
    // Additional filtering that should be applied
    private Filters filters;
    // Number of channels
    private int channelCount = DEFAULT_CHANNEL_COUNT;
    // Whether channel count has changed during processing of the latest chunk of incoming data
    private boolean channelCountChanged;
    // Average signal which we use to avoid signal offset
    private double average;
    // Array of sample counters, one for every channel
    private int[] sampleCounters = new int[DEFAULT_CHANNEL_COUNT];
    // Holds samples from all channels processed in a single batch
    private short[][] channels = new short[DEFAULT_CHANNEL_COUNT][];
    // Holds events processed in a single batch
    private String[] events = new String[0];

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

    @NonNull @Override public SamplesWithMarkers process(@NonNull byte[] data) {
        if (data.length > 0) return processIncomingData(data);

        return new SamplesWithMarkers();
    }

    /**
     * Set number of channels in the sample stream.
     */
    void setChannelCount(int channelCount) {
        this.channelCount = channelCount;

        channelCountChanged = true;
    }

    //private short prevSample = 0;

    @NonNull private SamplesWithMarkers processIncomingData(@NonNull byte[] data) {
        //long start = System.currentTimeMillis();
        //LOGD(TAG, ".........................................");
        //LOGD(TAG, "START - "/* + data.length*/);

        // if channel count has changed during processing  previous data chunk we should disregard
        if (channelCountChanged) {
            frameStarted = false;
            sampleStarted = false;
            currentChannel = 0;

            channels = new short[channelCount][];
            sampleCounters = new int[channelCount];

            channelCountChanged = false;
        }

        int lsb;
        short sample;
        byte b;

        // max number of samples can be number of incoming bytes divided by 2
        int maxSampleCount = (int) (data.length * .5 + 1);
        // init samples (by channels)
        for (int i = 0; i < channelCount; i++) {
            channels[i] = new short[maxSampleCount];
        }
        // init samples
        events = new String[maxSampleCount];

        // init sample counter for all channels
        Arrays.fill(sampleCounters, 0);

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < data.length; i++) {
            // and next byte to custom message sent by SpikerBox
            escapeSequence[escapeSequenceIndex++] = data[i];

            if (insideEscapeSequence) { // we are inside escape sequence
                if (eventMessageIndex >= EVENT_MESSAGE_LENGTH) { // event message shouldn't be longer then 64 bytes
                    // let's process incoming message
                    processEscapeSequenceMessage(Arrays.copyOfRange(eventMessage, 0, eventMessageIndex),
                        sampleCounters[CHANNEL_INDEX] == 0 ? 0 : sampleCounters[CHANNEL_INDEX] - 1);

                    reset();
                } else if (ESCAPE_SEQUENCE_END[tmpIndex] == data[i]) {
                    tmpIndex++;
                    if (tmpIndex == ESCAPE_SEQUENCE_END.length) {
                        // let's process incoming message
                        processEscapeSequenceMessage(Arrays.copyOfRange(eventMessage, 0, eventMessageIndex),
                            sampleCounters[CHANNEL_INDEX] == 0 ? 0 : sampleCounters[CHANNEL_INDEX] - 1);

                        reset();
                    }
                } else {
                    eventMessage[eventMessageIndex++] = data[i];
                }
            } else {
                if (ESCAPE_SEQUENCE_START[tmpIndex] == data[i]) {
                    tmpIndex++;
                    if (tmpIndex == ESCAPE_SEQUENCE_START.length) {
                        tmpIndex = 0; // reset index, we need it for sequence end
                        insideEscapeSequence = true;
                    }
                    continue;
                }

                byte[] sequence = Arrays.copyOfRange(escapeSequence, 0, escapeSequenceIndex);
                //noinspection ForLoopReplaceableByForEach
                for (int j = 0; j < sequence.length; j++) {
                    b = sequence[j];
                    // check if we have unfinished frame
                    if (frameStarted) {
                        // check if we have unfinished sample
                        if (sampleStarted) {
                            lsb = b & CLEANER;

                            // if less significant byte is also grater then 127 drop whole frame
                            if (lsb > 127) {
                                LOGW(TAG, "LSB > 127! DROP WHOLE FRAME!");

                                frameStarted = false;
                                sampleStarted = false;
                                currentChannel = 0;
                                continue;
                            }

                            // get sample value from most and least significant bytes
                            msb = msb & REMOVER;
                            msb = msb << 7;
                            lsb = lsb & REMOVER;
                            sample = (short) (((msb | lsb) - 512) * 30);

                            //if ((sample - prevSample) > 1) {
                            //    LOGD(TAG, "TEST: " + Arrays.toString(data));
                            //}
                            //prevSample = sample;

                            // calculate average sample
                            average = 0.0001 * sample + 0.9999 * average;
                            // use average to remove offset
                            sample = (short) (sample - average);

                            // apply additional filtering if necessary
                            if (filters != null) sample = filters.apply(sample);

                            channels[currentChannel][sampleCounters[currentChannel]++] = sample;

                            sampleStarted = false;
                            if (currentChannel >= channelCount - 1) frameStarted = false;
                        } else {
                            msb = b & CLEANER;
                            // we already started the frame so if msb is greater then 127 drop whole frame
                            if (msb > 127) {
                                LOGW(TAG, "MSB > 127 WITHIN THE FRAME! DROP WHOLE FRAME!");

                                frameStarted = false;
                                sampleStarted = false;
                                currentChannel = 0;
                            } else {
                                currentChannel++;

                                sampleStarted = true;
                            }
                        }
                    } else {
                        msb = b & CLEANER;
                        if (msb > 127) {
                            currentChannel = 0;

                            frameStarted = true;
                            sampleStarted = true;
                        } else {
                            LOGW(TAG, "MSB < 128 AT FRAME START! DROP!");

                            frameStarted = false;
                            sampleStarted = false;
                            currentChannel = 0;
                        }
                    }
                }

                reset();
            }
        }

        if (sampleCounters[CHANNEL_INDEX] == 0) return new SamplesWithMarkers();

        //LOGD(TAG, "SIZE: " + data.length + ", TOOK: " + (System.currentTimeMillis() - start));

        return new SamplesWithMarkers(Arrays.copyOfRange(channels[CHANNEL_INDEX], 0, sampleCounters[CHANNEL_INDEX]),
            Arrays.copyOfRange(events, 0, sampleCounters[CHANNEL_INDEX]));
    }

    // Resets all variables used for processing escape sequences
    private void reset() {
        insideEscapeSequence = false;
        tmpIndex = 0;
        escapeSequence = new byte[MAX_SEQUENCE_LENGTH];
        escapeSequenceIndex = 0;
        eventMessage = new byte[EVENT_MESSAGE_LENGTH];
        eventMessageIndex = 0;
    }

    // Processes escape sequence message and triggers appropriate listener
    private void processEscapeSequenceMessage(byte[] messageBytes, int sampleIndex) {
        final String message = new String(messageBytes);
        // check if it's board type message
        if (listener != null) {
            if (SampleStreamUtils.isHardwareTypeMsg(message)) {
                listener.onSpikerBoxHardwareTypeDetected(SampleStreamUtils.getBoardType(message));
            } else if (SampleStreamUtils.isSampleRateAndNumOfChannelsMsg(message)) {
                listener.onMaxSampleRateAndNumOfChannelsReply(SampleStreamUtils.getMaxSampleRate(message),
                    SampleStreamUtils.getChannelCount(message));
            } else if (SampleStreamUtils.isEventMsg(message)) {
                events[sampleIndex] = SampleStreamUtils.getEventNumber(message);
            }
        }
    }
}