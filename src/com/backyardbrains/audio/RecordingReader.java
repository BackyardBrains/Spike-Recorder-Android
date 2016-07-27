package com.backyardbrains.audio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import com.backyardbrains.BackyardBrainsMain;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

public class RecordingReader {

	public static final String		TAG				= RecordingReader.class.getCanonicalName();
	private BufferedInputStream		bufferedStream	= null;
	private File					recordingFile;
	private ReadFromWavefile		asyncReader;
	private boolean					bReady			= false;
	private byte[]					data;

	protected int					format			= 0;
	protected int					numChannels		= 0;
	protected int					sampleRate		= 0;
	protected int					byteRate		= 0;
	protected short					blockAlign		= 0;
	protected int					bitsPerSample	= 0;

	Context							context			= null;
	private AudiofileReadListener	listener		= null;

	public interface AudiofileReadListener {
		public void audioFileRead();
	}

	// ----------------------------------------------------------------------------------------
	public RecordingReader(File f, AudiofileReadListener listener) {
		this(f);
		this.listener = listener;
	}

	// ----------------------------------------------------------------------------------------
	public RecordingReader(File f) {
		try {
			loadFile(f);
		} catch (IOException e) {
			Log.e(TAG, "Couldn't load wav file ");
			e.printStackTrace();
		}
	}

	// ----------------------------------------------------------------------------------------
	public RecordingReader(String filePath) {
		this(new File(filePath));
	}

	// ----------------------------------------------------------------------------------------
	public RecordingReader(File f, Context context) {
		this(f);
		this.context = context.getApplicationContext();
	}

	// ----------------------------------------------------------------------------------------
	public RecordingReader(String filePath, Context context) {
		this(new File(filePath), context);
	}

	// ----------------------------------------------------------------------------------------
	public void loadFile(File f) throws IOException {
		bReady = false;
		asyncReader = new ReadFromWavefile();
		recordingFile = f;
		if (recordingFile != null) {
			if (recordingFile.exists()) {
				try {
					bufferedStream = new BufferedInputStream(new FileInputStream(recordingFile));

					asyncReader.execute(bufferedStream);
					// byte [] buff = convertFromWave(bufferedStream);
// byte [] orig = new byte [buff.length];
// if(buff.length != orig.length){//Log.d("RecordingReader","Different size
// buffers");}
// for(int i = 0; i < buff.length && i < orig.length; i++){
// if(buff[i] != orig[i]){
// //Log.d("RecordingReader", "samples not equal"); }
// }
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
			bReady = false;
			data = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ----------------------------------------------------------------------------------------
	private void fileReadDone() throws IOException {
		bReady = true;
		if (bufferedStream != null) {
			try {
				bufferedStream.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		bufferedStream = null;
		asyncReader = null;
		recordingFile = null;
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

	public int getByteRate() {
		return byteRate;
	}

	public short getBlockAlign() {
		return blockAlign;
	}

	public int getBitsPerSample() {
		return bitsPerSample;
	}

	public byte[] getData() {
		if (bReady && data != null) {
			return data;
		} else {
			byte[] b = new byte[0];
			return b;
		}
	}

	// ----------------------------------------------------------------------------------------
	public short[] getDataShorts() {
		short[] samples;
		if (bReady && data != null) {
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
		return bReady;
	}

	// ----------------------------------------------------------------------------------------
	private byte[] convertFromWave(BufferedInputStream in) throws IOException {
		/* RIFF header */
		readId(in, "RIFF");
		int numBytes = readInt(in) - 36;
		readId(in, "WAVE");

		/* fmt chunk */
		readId(in, "fmt ");
		if (16 != readInt(in)) throw new IOException("fmt chunk length not 16");
		format = readShort(in);
		numChannels = readShort(in);
		sampleRate = readInt(in);
		byteRate = readInt(in);
		blockAlign = readShort(in);
		bitsPerSample = readShort(in);
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
		//Log.d(TAG, "Successfully read file. numBytes " + mNumBytes + " format " + format + " numChannels " + numChannels + " samplerate: " + sampleRate + " byteRate: " + byteRate + " blockAlign: " + blockAlign + " bitsPerSample: " + bitsPerSample);
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
	private class ReadFromWavefile extends AsyncTask<BufferedInputStream, Void, Void> {

		@Override
		protected Void doInBackground(BufferedInputStream... params) {
			for (BufferedInputStream f : params) {
				try {
					data = convertFromWave(f);
				} catch (IOException e) {
					Log.e(TAG, "Couldn't read wav file ");
					e.printStackTrace();
				}
			}
			//Log.d(getClass().getCanonicalName(), "Finished reading " + recordingFile.getName());
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {
			try {
				fileReadDone();
			} catch (IOException e) {
				e.printStackTrace();
			}
			//Log.d(TAG, "onPostExecute: bReady = true");
			if (listener != null) {
				listener.audioFileRead();
			} else if (context != null) {
				Intent i = new Intent();
				i.setAction("BYBAudioFileRead");
				context.sendBroadcast(i);
			}

		}

		@Override
		protected void onPreExecute() {
			bReady = false;
			data = null;
		}
	}

}
