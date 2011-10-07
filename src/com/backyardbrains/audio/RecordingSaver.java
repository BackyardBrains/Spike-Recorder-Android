package com.backyardbrains.audio;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class RecordingSaver implements ReceivesAudio {

	public static final String TAG = "RecordingSaver";
	private File mFileToRecordTo;
	private BufferedOutputStream bufferedStream;
	private DataOutputStream dataOutputStreamInstance;

	public RecordingSaver(String filename) {
		initializeAndCreateFile(filename);
	}

	private void initializeAndCreateFile(String filename) {
		mFileToRecordTo = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ "/" + filename);

		if (mFileToRecordTo == null) {
			throw new IllegalStateException("File to record to is null");
		}

		try {
			mFileToRecordTo.createNewFile();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot create file: "
					+ mFileToRecordTo.toString() + " with error "
					+ e.getMessage());
		}

		try {
			bufferedStream = new BufferedOutputStream(new FileOutputStream(
					mFileToRecordTo));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Cannot open file for writing", e);
		}
		dataOutputStreamInstance = new DataOutputStream(bufferedStream);

	}

	@Override
	public void receiveAudio(ByteBuffer audioInfo) {
		ShortBuffer sb = audioInfo.asShortBuffer();
		while (sb.hasRemaining()) {
			try {
				dataOutputStreamInstance.writeShort(sb.get());
			} catch (IOException e) {
				throw new IllegalStateException(
						"Could not write bytes out to file");
			}
		}
	}

	public void finishRecording() {
		try {
			bufferedStream.close();
			// @TODO - turn this into an AsyncTask call.
			new ConvertToWavefile().execute(mFileToRecordTo);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot close buffered writer.");
		}
	}

	private class ConvertToWavefile extends AsyncTask<File, Void, String> {

		private void convertToWave(File mFileToRecordTo2) throws IOException {
			// TODO Auto-generated method stub
			FileInputStream in = new FileInputStream(mFileToRecordTo2);
			File outputFile = new File(mFileToRecordTo2.getAbsolutePath()
					+ ".wav");
			FileOutputStream out = new FileOutputStream(outputFile);

			int mSampleRate = 44100;
			int mChannels = 1;
			int mBitsPerSample = 16;

			long subchunk2size = mFileToRecordTo2.length() / 2 // # of samples
					* mChannels * (mBitsPerSample / 2);

			long chunksize = subchunk2size + 36;
			long longSampleRate = mSampleRate;
			long byteRate = mSampleRate * mChannels * (mBitsPerSample / 2);

			byte[] header = new byte[44];
			header[0] = 'R'; // RIFF/WAVE header
			header[1] = 'I';
			header[2] = 'F';
			header[3] = 'F';
			header[4] = (byte) (chunksize & 0xff);
			header[5] = (byte) ((chunksize >> 8) & 0xff);
			header[6] = (byte) ((chunksize >> 16) & 0xff);
			header[7] = (byte) ((chunksize >> 24) & 0xff);
			header[8] = 'W';
			header[9] = 'A';
			header[10] = 'V';
			header[11] = 'E';
			header[12] = 'f'; // 'fmt ' chunk
			header[13] = 'm';
			header[14] = 't';
			header[15] = ' ';
			header[16] = 16; // 4 bytes: size of 'fmt ' chunk
			header[17] = 0;
			header[18] = 0;
			header[19] = 0;
			header[20] = 1; // format = 1
			header[21] = 0;
			header[22] = (byte) mChannels;
			header[23] = 0;
			header[24] = (byte) (longSampleRate & 0xff);
			header[25] = (byte) ((longSampleRate >> 8) & 0xff);
			header[26] = (byte) ((longSampleRate >> 16) & 0xff);
			header[27] = (byte) ((longSampleRate >> 24) & 0xff);
			header[28] = (byte) (byteRate & 0xff);
			header[29] = (byte) ((byteRate >> 8) & 0xff);
			header[30] = (byte) ((byteRate >> 16) & 0xff);
			header[31] = (byte) ((byteRate >> 24) & 0xff);
			header[32] = (byte) (mChannels * mBitsPerSample / 8); // block align
			header[33] = 0;
			header[34] = 16; // bits per sample
			header[35] = 0;
			header[36] = 'd';
			header[37] = 'a';
			header[38] = 't';
			header[39] = 'a';
			header[40] = (byte) (subchunk2size & 0xff);
			header[41] = (byte) ((subchunk2size >> 8) & 0xff);
			header[42] = (byte) ((subchunk2size >> 16) & 0xff);
			header[43] = (byte) ((subchunk2size >> 24) & 0xff);
			out.write(header, 0, 44);

			// byte[] buffer = new byte[(int) mFileToRecordTo2.length()];
			// in.read(buffer);
			// out.write(buffer);

			byte tempA;
			byte tempB;
			long counter = 0;
			while (counter < mFileToRecordTo2.length() && in.available() > 0) {
				tempA = (byte) in.read();
				tempB = (byte) in.read();
				out.write(tempB);
				out.write(tempA);
			}
			in.close();
			out.close();
		}

		@Override
		protected String doInBackground(File... params) {
			StringBuilder s = new StringBuilder();
			for (File f : params) {
				try {
					convertToWave(f);
					s.append(" - " + f.getName());
					f.delete();
				} catch (IOException e) {
					Log.e(TAG, "Couldn't write file: " + f.getAbsolutePath());
					e.printStackTrace();
				}
			}
			return "Finished writing file tos SD Card" + s.toString();
		}

	}

}
