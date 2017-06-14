/*
 * Backyard Brains Android App
 * Copyright (C) 2011 Backyard Brains
 * by Nathan Dotz <nate (at) backyardbrains.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.backyardbrains.audio;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecordingSaver implements ReceivesAudio {

    public static final String TAG = "RecordingSaver";
    private ByteArrayOutputStream mArrayToRecordTo;
    private BufferedOutputStream bufferedStream;
    private DataOutputStream dataOutputStreamInstance;
    private File bybDirectory;
    protected String filename = "";
    protected Context context;

    public RecordingSaver(@NonNull Context context, @NonNull String filePrefix) {
        initializeAndCreateFile(filePrefix, context);
    }

    /**
     * Create a the BackyardBrains directory on the sdcard if it doesn't exist,
     * then set up a file output stream in that directory which we'll use to
     * write to later
     */
    private void initializeAndCreateFile(String filename, Context context) {

        this.context = context.getApplicationContext();
        bybDirectory = createBybDirectory();

        mArrayToRecordTo = new ByteArrayOutputStream();
        try {
            bufferedStream = new BufferedOutputStream(mArrayToRecordTo);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot open file for writing", e);
        }
        dataOutputStreamInstance = new DataOutputStream(bufferedStream);
        this.filename = filename;
    }

    /**
     * Create our directory on the SD card
     *
     * @return the File instance of our directory
     */
    private File createBybDirectory() {
        final File bybDirectory = new File(Environment.getExternalStorageDirectory() + "/BackyardBrains/");
        bybDirectory.mkdirs();
        return bybDirectory;
    }

    /**
     * Our data comes in big-endian, but we need to write 16-bit PCM in
     * little-endian, so we'll loop through the buffer, reversing bytes as we
     * spit out to the our data output stream.
     */
    @Override public void receiveAudio(ShortBuffer audioInfo) {
    }

    @Override public void receiveAudio(ByteBuffer audioInfo) {
        ShortBuffer sb = audioInfo.asShortBuffer();
        while (sb.hasRemaining()) {
            try {
                dataOutputStreamInstance.writeShort(Short.reverseBytes(sb.get()));
            } catch (IOException e) {
                throw new IllegalStateException("Could not write bytes out to file");
            }
        }
    }

    @Override public void receiveAudio(ByteBuffer audioInfo, long lastBytePosition) {
    }

    /**
     * close the stream that our data is being sent to, then convert the array
     * we've been caching to in RAM to a wave file after it's done writing PCM
     * to the disk.
     */
    public void finishRecording() {
        try {
            bufferedStream.close();
            new ConvertToWaveFile().execute(mArrayToRecordTo);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot close buffered writer.");
        }
    }

    private class ConvertToWaveFile extends AsyncTask<ByteArrayOutputStream, Void, String> {

        /**
         * Takes a ByteArrayOutputStream provided to our execute() method, and
         * add a PCM WAVE header and write it out to the disk.
         *
         * @throws IOException
         */
        private String convertToWave(ByteArrayOutputStream byteData) throws IOException {
            byte[] mFileToRecordTo2 = byteData.toByteArray();
            File outputFile = new File(bybDirectory,
                filename + new SimpleDateFormat("d_MMM_yyyy_HH_mm_s_a").format(new Date(System.currentTimeMillis()))
                    + ".wav");
            FileOutputStream out = new FileOutputStream(outputFile);
            DataOutputStream datastream = new DataOutputStream(out);

            int mSampleRate = 44100;
            int mChannels = 1;
            int mBitsPerSample = 16;

            long subchunk2size = mFileToRecordTo2.length / 2 // # of samples
                * mChannels * (mBitsPerSample / 2);

            long chunksize = subchunk2size + 36;
            int byteRate = mSampleRate * mChannels * mBitsPerSample / 8;

            datastream.writeBytes("RIFF");
            datastream.writeInt(Integer.reverseBytes((int) chunksize));
            datastream.writeBytes("WAVEfmt ");
            datastream.writeInt(Integer.reverseBytes(16));
            datastream.writeShort(Short.reverseBytes((short) 1));
            datastream.writeShort(Short.reverseBytes((short) mChannels));
            datastream.writeInt(Integer.reverseBytes(mSampleRate));
            datastream.writeInt(Integer.reverseBytes(byteRate));
            datastream.writeShort(Short.reverseBytes((short) (mChannels * mBitsPerSample / 8))); // block align
            datastream.writeShort(Short.reverseBytes((short) mBitsPerSample));
            datastream.writeBytes("data");
            datastream.writeInt(Integer.reverseBytes((int) subchunk2size));

            datastream.write(mFileToRecordTo2);
            out.close();
            datastream.close();
            return outputFile.getAbsolutePath();
        }

        /**
         * Whip through the list of ByteArrays (currently only the one) and
         * convert each to proper WAV formats
         */
        @Override protected String doInBackground(ByteArrayOutputStream... params) {
            StringBuilder s = new StringBuilder();
            String writtenFile = "";
            for (ByteArrayOutputStream f : params) {
                try {
                    writtenFile = convertToWave(f);
                    s.append(" - " + writtenFile);
                    // f.delete();
                    // f = null;
                } catch (IOException e) {
                    Log.e(TAG, "Couldn't write wav file ");
                    e.printStackTrace();
                    f = null;
                }
            }
            //Log.d(getClass().getCanonicalName(),"Finished writing out " + s.toString());

            return writtenFile;//"Finished writing file to SD Card" + s.toString();
        }

        @Override protected void onPostExecute(String s) {
            //Log.d(TAG, "onPostExecute");
            Intent i = new Intent();
            i.setAction("BYBRecordingSaverSuccessfulSave");
            context.sendBroadcast(i);
        }
    }
}
