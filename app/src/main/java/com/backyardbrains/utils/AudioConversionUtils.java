package com.backyardbrains.utils;

import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class AudioConversionUtils {

    private static final String TAG = makeLogTag(AudioConversionUtils.class);

    /**
     * Interface definition for a callback to be invoked during audio file conversion progress.
     */
    public interface ToWavConversionProgressListener {
        /**
         * Listener that is invoked while audio file is being converted to WAV.
         */
        void onConversionProgress(float progress);

        /**
         * Listener that is invoked on audio file conversion to WAV completion.
         */
        void onConversionComplete();
    }

    /**
     * Converts specified audio file {@code in} to WAV format and writes it to specified {@code out}
     * file. If conversion is successful {@link MediaFormat} of the output file is returned.
     *
     * @throws IOException
     */
    @Nullable public static MediaFormat convertToWav(@NonNull File in, @NonNull File out,
        @Nullable ToWavConversionProgressListener listener) throws IOException {
        final MediaExtractor extractor = new MediaExtractor();
        final FileInputStream source = new FileInputStream(in);
        extractor.setDataSource(source.getFD());
        source.close();

        if (extractor.getTrackCount() > 1) {
            extractor.release();
            throw new IOException("File has wrong number of tracks.");
        }
        final MediaFormat format = extractor.getTrackFormat(0);
        final String mime = format.getString(MediaFormat.KEY_MIME);
        if (!mime.startsWith("audio/")) {
            extractor.release();
            throw new IOException("File is not an audio file.");
        }
        final long duration = format.getLong(MediaFormat.KEY_DURATION);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            format.setString(MediaFormat.KEY_FRAME_RATE, null);
        }

        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        ByteBuffer[] codecInputBuffers = new ByteBuffer[0];
        ByteBuffer[] codecOutputBuffers = new ByteBuffer[0];

        MediaFormat outputFormat = null;
        final MediaCodec codec;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            codec = MediaCodec.createDecoderByType(mime);
        } else {
            final MediaCodecList mcl = new MediaCodecList(MediaCodecList.ALL_CODECS);
            final String codecName = mcl.findDecoderForFormat(format);
            codec = MediaCodec.createByCodecName(codecName);
        }
        codec.configure(format, null, null, 0);
        codec.start();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            codecInputBuffers = codec.getInputBuffers();
            codecOutputBuffers = codec.getOutputBuffers();
        }

        extractor.selectTrack(0);

        // and stream to write samples to
        final OutputStream outputStream;
        try {
            outputStream = new FileOutputStream(out);
        } catch (FileNotFoundException e) {
            throw new IOException(
                "Could not build OutputStream from audio file: " + out.getAbsolutePath(), e);
        }

        byte[] buffer = new byte[5000];
        while (!sawOutputEOS && noOutputCounter < 50) {
            noOutputCounter++;
            // microseconds
            int CODEC_TIMEOUT = 5000;
            if (!sawInputEOS) {
                final int inputBufferId = codec.dequeueInputBuffer(CODEC_TIMEOUT);
                if (inputBufferId >= 0) {
                    final ByteBuffer inputBuffer;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        inputBuffer = codecInputBuffers[inputBufferId];
                    } else {
                        inputBuffer = codec.getInputBuffer(inputBufferId);
                    }
                    if (inputBuffer != null) {
                        int sampleCount = extractor.readSampleData(inputBuffer, 0);
                        long presentationTimeUs = 0;
                        if (sampleCount < 0) {
                            LOGD(TAG, "saw input EOS.");
                            sawInputEOS = true;
                            sampleCount = 0;
                        } else {
                            presentationTimeUs = extractor.getSampleTime();
                        }
                        codec.queueInputBuffer(inputBufferId, 0, sampleCount, presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                        if (!sawInputEOS) extractor.advance();
                    }
                }
            }

            final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            final int outputBufferId = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT);
            if (outputBufferId >= 0) {
                if (info.size > 0) noOutputCounter = 0;

                final ByteBuffer outputBuffer;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    outputBuffer = codecOutputBuffers[outputBufferId];
                } else {
                    outputBuffer = codec.getOutputBuffer(outputBufferId);
                }
                outputFormat = codec.getOutputFormat();
                if (outputBuffer != null) {
                    if (buffer.length < info.size) buffer = new byte[info.size];

                    if (listener != null) {
                        listener.onConversionProgress(info.presentationTimeUs * 100f / duration);
                    }

                    outputBuffer.get(buffer, 0, info.size);
                    outputStream.write(buffer, 0, info.size);

                    codec.releaseOutputBuffer(outputBufferId, false);

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        LOGD(TAG, "saw output EOS.");
                        sawOutputEOS = true;
                    }
                }
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                LOGD(TAG, "output buffers have changed.");

                codecOutputBuffers = codec.getOutputBuffers();
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                LOGD(TAG, "output format has changed to " + codec.getOutputFormat());
            } else {
                LOGD(TAG, "dequeueOutputBuffer returned " + outputBufferId);
            }
        }

        codec.stop();
        codec.release();
        extractor.release();

        outputStream.flush();
        outputStream.close();

        if (listener != null) listener.onConversionComplete();

        return outputFormat;
    }
}
