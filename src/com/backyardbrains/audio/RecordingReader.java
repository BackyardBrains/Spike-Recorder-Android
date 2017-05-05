package com.backyardbrains.audio;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class RecordingReader {

    public static final String TAG = makeLogTag(RecordingReader.class);

    private final AudioFileReadListener listener;

    private File file;
    private BufferedInputStream bufferedStream = null;
    private ReadWaveFileAsyncTask asyncReader;
    private boolean ready;
    private byte[] data;

    private int format = 0;
    private int numChannels = 0;
    private int sampleRate = 0;

    private Context context = null;

    public interface AudioFileReadListener {
        void audioFileRead();
    }

    public RecordingReader(@NonNull File file, @Nullable AudioFileReadListener listener) {
        try {
            loadFile(file);
        } catch (IOException e) {
            LOGE(TAG, "Couldn't load wav file ");
            e.printStackTrace();
        }

        this.listener = listener;
    }

    // Loads specified audio file
    private void loadFile(File f) throws IOException {
        ready = false;

        asyncReader = new ReadWaveFileAsyncTask();
        file = f;
        if (file != null) {
            if (file.exists()) {
                try {
                    bufferedStream = new BufferedInputStream(new FileInputStream(file));
                    asyncReader.execute(bufferedStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ----------------------------------------------------------------------------------------
    public void close() {
        try {
            fileReadDone();
            ready = false;
            data = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------------------------------------------
    private void fileReadDone() throws IOException {
        ready = true;
        if (bufferedStream != null) {
            try {
                bufferedStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        bufferedStream = null;
        asyncReader = null;
        file = null;
    }
    // ----------------------------------------------------------------------------------------
    // ---------------------------------- GETTERS
    // ----------------------------------------------------------------------------------------

    public int getFormat() {
        return format;
    }

    public int getNumChannels() {
        return numChannels;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public byte[] getData() {
        if (ready && data != null) {
            return data;
        } else {
            return new byte[0];
        }
    }

    // ----------------------------------------------------------------------------------------
    public short[] getDataShorts() {
        short[] samples;
        if (ready && data != null) {
            ShortBuffer sb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            samples = new short[sb.limit()];
            sb.get(samples);
        } else {
            samples = new short[0];
        }
        return samples;
    }

    // ----------------------------------------------------------------------------------------
    public boolean isReady() {
        return ready;
    }

    // ----------------------------------------------------------------------------------------
    private byte[] convertFromWave(BufferedInputStream in) throws IOException {
        /* RIFF header */
        readId(in, "RIFF");
        final int numBytes = readInt(in) - 36;
        readId(in, "WAVE");

		/* fmt chunk */
        readId(in, "fmt ");
        if (16 != readInt(in)) throw new IOException("fmt chunk length not 16");
        format = readShort(in);
        numChannels = readShort(in);
        sampleRate = readInt(in);
        final int byteRate = readInt(in);
        final short blockAlign = readShort(in);
        final int bitsPerSample = readShort(in);
        if (byteRate != numChannels * sampleRate * bitsPerSample / 8) {
            throw new IOException("fmt.ByteRate field inconsistent");
        }
        if (blockAlign != numChannels * bitsPerSample / 8) {
            throw new IOException("fmt.BlockAlign field inconsistent");
        }

		/* data chunk */
        readId(in, "data");
        int mNumBytes = readInt(in) * 2 / numChannels / bitsPerSample * 2;

        byte[] buff = new byte[mNumBytes];

        int readSize = in.read(buff);
        if (readSize == -1) throw new IOException("wav data end before expected");
        if (readSize != mNumBytes) throw new IOException("wav data size differs from what header says");
        //LOGD(TAG, "Successfully read file. numBytes " + mNumBytes + " format " + format + " numChannels " + numChannels + " samplerate: " + sampleRate + " byteRate: " + byteRate + " blockAlign: " + blockAlign + " bitsPerSample: " + bitsPerSample);
        return buff;
    }

    // ----------------------------------------------------------------------------------------
    private static void readId(InputStream in, String id) throws IOException {
        for (int i = 0; i < id.length(); i++) {
            if (id.charAt(i) != in.read()) throw new IOException(id + " tag not present");
        }
    }

    // ----------------------------------------------------------------------------------------
    private static int readInt(InputStream in) throws IOException {
        return in.read() | (in.read() << 8) | (in.read() << 16) | (in.read() << 24);
    }

    // ----------------------------------------------------------------------------------------
    private static short readShort(InputStream in) throws IOException {
        return (short) (in.read() | (in.read() << 8));
    }

    // ----------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------
    private class ReadWaveFileAsyncTask extends AsyncTask<BufferedInputStream, Void, Void> {

        @Override protected Void doInBackground(BufferedInputStream... params) {
            for (BufferedInputStream f : params) {
                try {
                    data = convertFromWave(f);
                } catch (IOException e) {
                    LOGE(TAG, "Couldn't read wav file ");
                    e.printStackTrace();
                }
            }
            //LOGD(TAG, "Finished reading " + file.getName());
            return null;
        }

        @Override protected void onPostExecute(Void v) {
            try {
                fileReadDone();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //LOGD(TAG, "onPostExecute: ready = true");
            if (listener != null) {
                listener.audioFileRead();
            } else if (context != null) {
                Intent i = new Intent();
                i.setAction("BYBAudioFileRead");
                context.sendBroadcast(i);
            }
        }

        @Override protected void onPreExecute() {
            ready = false;
            data = null;
        }
    }
}
