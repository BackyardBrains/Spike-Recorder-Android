package com.backyardbrains.audio;

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

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class RecordingSaver implements ReceivesAudio {

	public static final String TAG = "RecordingSaver";
	private ByteArrayOutputStream mFileToRecordTo;
	private BufferedOutputStream bufferedStream;
	private DataOutputStream dataOutputStreamInstance;
	private File bybDirectory;

	public RecordingSaver(String filename) {
		initializeAndCreateFile(filename);
	}

	private void initializeAndCreateFile(String filename) {

		bybDirectory = createBybDirectory();

		mFileToRecordTo = new ByteArrayOutputStream();
		try {
			bufferedStream = new BufferedOutputStream(mFileToRecordTo);
		} catch (Exception e) {
			throw new IllegalStateException("Cannot open file for writing", e);
		}
		dataOutputStreamInstance = new DataOutputStream(bufferedStream);

	}

	private File createBybDirectory() {
		File BybDirectory = new File(Environment.getExternalStorageDirectory()
				+ "/BackyardBrains/");
		BybDirectory.mkdirs();
		return BybDirectory;
	}

	@Override
	public void receiveAudio(ByteBuffer audioInfo) {
		ShortBuffer sb = audioInfo.asShortBuffer();
		while (sb.hasRemaining()) {
			try {
				dataOutputStreamInstance.writeShort(Short.reverseBytes(sb.get()));
			} catch (IOException e) {
				throw new IllegalStateException(
						"Could not write bytes out to file");
			}
		}
	}

	public void finishRecording() {
		try {
			bufferedStream.close();
			new ConvertToWavefile().execute(mFileToRecordTo);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot close buffered writer.");
		}
	}

	private class ConvertToWavefile extends AsyncTask<ByteArrayOutputStream, Void, String> {

		private String convertToWave(ByteArrayOutputStream mFileToRecordToByteArrayOutputStream) throws IOException {
			byte[] mFileToRecordTo2 = mFileToRecordToByteArrayOutputStream.toByteArray();
			File outputFile = new File(bybDirectory, new SimpleDateFormat(
					"d_MMM_yyyy_HH_mm_s_a").format(new Date(System
					.currentTimeMillis()))
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
			datastream.writeShort(Short.reverseBytes((short) (mChannels
					* mBitsPerSample / 8))); // block align
			datastream.writeShort(Short.reverseBytes((short) mBitsPerSample));
			datastream.writeBytes("data");
			datastream.writeInt(Integer.reverseBytes((int) subchunk2size));

			datastream.write(mFileToRecordTo2);
			out.close();
			datastream.close();
			return outputFile.getName();
		}

		@Override
		protected String doInBackground(ByteArrayOutputStream... params) {
			StringBuilder s = new StringBuilder();
			for (ByteArrayOutputStream f : params) {
				try {
					String writtenFile = convertToWave(f);
					s.append(" - " + writtenFile);
					//f.delete();
					//f = null;
				} catch (IOException e) {
					Log.e(TAG, "Couldn't write wav file ");
					e.printStackTrace();
					f = null;
				}
			}
			Log.d(getClass().getCanonicalName(), "Finished writing out "+s.toString());
			return "Finished writing file to SD Card" + s.toString();
		}

	}

}
