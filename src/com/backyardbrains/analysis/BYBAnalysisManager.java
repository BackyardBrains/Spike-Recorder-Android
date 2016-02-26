package com.backyardbrains.analysis;

import java.io.File;


import com.backyardbrains.audio.RecordingReader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BYBAnalysisManager implements RecordingReader.AudiofileReadListener, BYBBaseAsyncAnalysis.AnalysisListener {

	private RecordingReader			reader;
	private static final String		TAG				= "BYBAnalysisManager";
	private FindSpikesListener		findSpikesListener;
	private File					fileToAnalize;

	private BYBFindSpikesAnalysis	spikesAnalysis;
	private Context					context;
	private boolean					bSpikesFound	= false;
	private BYBSpike[]				spikes;

	// -----------------------------------------------------------------------------------------------------------------------------
	public BYBAnalysisManager(Context context) {
		this.context = context;
		registerFindSpikesListener(true);
	}
	public void close(){
		registerFindSpikesListener(false);
		
	}
	// -----------------------------------------------------------------------------------------------------------------------------
	public int getReaderSampleRate() {
		if (reader != null) {
			if (reader.isReady()) {
				return reader.getSampleRate();
			}
		}
		return 0;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public int getReaderNumChannels() {
		if (reader != null) {
			if (reader.isReady()) {
				return reader.getNumChannels();
			}
		}
		return 0;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public short[] getReaderSamples() {
		if (reader != null) {
			if (reader.isReady()) {
				return reader.getDataShorts();
			}
		}
		short[] t = { 0, 0 };
		return t;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public boolean load(File file) {
		if (!file.exists()) return false;
		fileToAnalize = file;
		if (reader != null) {
			reader.close();
			reader = null;
		}
		if (reader == null) {
			reader = new RecordingReader(fileToAnalize, (RecordingReader.AudiofileReadListener) this);
			Log.d(TAG, "loading audio file: " + fileToAnalize.getAbsolutePath());
		}
		return true;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public boolean load(String filePath) {
		return load(new File(filePath));
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public void audioFileRead() {
		spikesAnalysis = null;
		spikesAnalysis = new BYBFindSpikesAnalysis((BYBBaseAsyncAnalysis.AnalysisListener) this, getReaderSamples(), getReaderSampleRate(), getReaderNumChannels());
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public void analysisDone(int analysisType) {
		if (spikesAnalysis != null) {
			spikes = spikesAnalysis.getSpikes();
			spikesAnalysis = null;
			bSpikesFound = true;
		}
	}

	public void analysisCanceled(int analysisType) {
		bSpikesFound = false;
	}

	
	public BYBSpike[] getSpikes(){
		if(bSpikesFound){
		return spikes;
		}else{
			BYBSpike [] s = new BYBSpike[0];
			return s;
		}
	}
	public boolean spikesFound(){return bSpikesFound;}
// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- BROADCAST RECEIVERS CLASS
// -----------------------------------------------------------------------------------------------------------------------------
	private class FindSpikesListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("filePath")) {
				String filePathFindSpikes = intent.getStringExtra("filePath");
				if (fileToAnalize != null) {
					if (fileToAnalize.getAbsolutePath() == filePathFindSpikes) {
						return;
					}
					fileToAnalize = null;
				}
				load(filePathFindSpikes);
				Log.d(TAG, "FindSpikesListener: onReceive");
			}
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- BROADCAST RECEIVERS TOGGLES
	// -----------------------------------------------------------------------------------------------------------------------------
	private void registerFindSpikesListener(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBFindPeaks");
			findSpikesListener = new FindSpikesListener();
			context.registerReceiver(findSpikesListener, intentFilter);
		} else {
			context.unregisterReceiver(findSpikesListener);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- REGISTER RECEIVERS
	// -----------------------------------------------------------------------------------------------------------------------------

	public void registerReceivers() {
		registerFindSpikesListener(true);
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- UNREGISTER RECEIVERS
	// -----------------------------------------------------------------------------------------------------------------------------
	public void unregisterReceivers() {
		registerFindSpikesListener(false);
	}
}
