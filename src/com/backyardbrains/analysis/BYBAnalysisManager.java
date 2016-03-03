package com.backyardbrains.analysis;

import java.io.File;
import java.util.ArrayList;

import com.backyardbrains.BYBUtils;
import com.backyardbrains.audio.RecordingReader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BYBAnalysisManager implements RecordingReader.AudiofileReadListener, BYBBaseAsyncAnalysis.AnalysisListener {
	private static final String				TAG							= "BYBAnalysisManager";

	private Context							context;
	private RecordingReader					reader;
	private File							fileToAnalize;

	private BYBFindSpikesAnalysis			spikesAnalysis;
	private boolean							bSpikesDone					= false;
	private boolean							bProcessSpikes				= false;
	private BYBSpike[]						spikes;
	private float							maxSpikeValue, minSpikeValue;
	private int								totalNumSamples				= 0;

	private ArrayList<ArrayList<BYBSpike>>	spikeTrains;
	private boolean							bSpikeTrainsDone			= false;
	private boolean							bProcessSpikeTrains			= false;

	private ArrayList<ArrayList<ISIResult>>	ISI;
	private boolean							bProcessISI					= false;
	private boolean							bISIDone					= false;

	private int								selectedThreshold			= 0;
	private ArrayList<int[]>				thresholds;
	public static final int					maxThresholds				= 3;

	private ArrayList<ArrayList<Integer>>	autoCorrelation;
	private boolean							bProcessAutoCorrelation		= false;
	private boolean							bAutoCorrelationDone		= false;

	private ArrayList<ArrayList<Integer>>	crossCorrelation;
	private boolean							bProcessCrossCorrelation	= false;
	private boolean							bCrossCorrelationDone		= false;

	private boolean							bProcessAverageSpike		= false;
	private boolean							bAverageSpikeDone			= false;

	// -----------------------------------------------------------------------------------------------------------------------------
	public BYBAnalysisManager(Context context) {
		this.context = context.getApplicationContext();
		thresholds = new ArrayList<int[]>();
		thresholds.add(new int[2]);
		registerReceivers();
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public void close() {
		unregisterReceivers();
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	protected void reset() {
		Log.d(TAG, "RESET");
		if (reader != null) {
			reader.close();
			reader = null;
		}
		fileToAnalize = null;
		bSpikeTrainsDone = false;
		bProcessSpikeTrains = false;
		bSpikesDone = false;
		bProcessSpikes = false;
		bProcessISI = false;
		bISIDone = false;

		bProcessAutoCorrelation = false;
		bAutoCorrelationDone = false;
		bProcessCrossCorrelation = false;
		bCrossCorrelationDone = false;
		bProcessAverageSpike = false;
		bAverageSpikeDone = false;

		clearISI();
		clearSpikeTrains();
		spikes = null;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public int getTotalNumSamples() {
		return totalNumSamples;
	}

	public float getMaxSpikeValue() {
		return maxSpikeValue;
	}

	public float getMinSpikeValue() {
		return minSpikeValue;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	private void process() {
		Log.d(TAG, "process!");
		if (bSpikesDone) {
			if (bProcessSpikeTrains) {
				processSpikeTrains();
				bProcessSpikeTrains = false;
			}

			if (bProcessISI) {
				ISIAnalysis();
				bProcessISI = false;
			}
			if (bProcessAutoCorrelation) {
				autoCorrelationAnalysis();
				bProcessAutoCorrelation = false;
			}
			if (bProcessCrossCorrelation) {
				crossCorrelationAnalysis();
				bProcessCrossCorrelation = false;
			}
			if (bProcessAverageSpike) {
				averageSpikeAnalysis();
				bProcessAverageSpike = false;
			}
			Intent i = new Intent();
			i.setAction("BYBRenderAnalysis");
			i.putExtra("requestRender", true);
			context.sendBroadcast(i);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------------- AUTOCORRELATION
	// -----------------------------------------------------------------------------------------------------------------------------
	private void clearAutoCorrelation() {
		if (autoCorrelation != null) {
			for (int i = 0; i < autoCorrelation.size(); i++) {
				if (autoCorrelation.get(i) != null) {
					autoCorrelation.get(i).clear();
				}
			}
			autoCorrelation.clear();
			autoCorrelation = null;
		}
	}

	private void autoCorrelationAnalysis() {
		processSpikeTrains();
		if (!bAutoCorrelationDone) {
			clearAutoCorrelation();
			float maxtime = 0.1f;
			float binsize = 0.001f;

			// ArrayList<ArrayList<BYBSpike>> spikeTrains;
			autoCorrelation = new ArrayList<ArrayList<Integer>>();
			for (int i = 0; i < spikeTrains.size(); i++) {
				BYBSpike firstSpike;
				BYBSpike secondSpike;
				int n = (int) Math.ceil((maxtime + binsize) / binsize);

				int[] histogram = new int[n];
				for (int x = 0; x < n; x++) {
					histogram[x] = 0;
				}

				float minEdge = -binsize * 0.5f;
				float maxEdge = maxtime + binsize * 0.5f;
				float diff;
				int index;
				int mainIndex;
				int secIndex;

				for (mainIndex = 0; mainIndex < spikeTrains.get(i).size(); mainIndex++) {
					firstSpike = spikeTrains.get(i).get(mainIndex);
					// Check on left of spike
					for (secIndex = mainIndex; secIndex >= 0; secIndex--) {
						secondSpike = spikeTrains.get(i).get(secIndex);
						diff = firstSpike.time - secondSpike.time;
						if (diff > minEdge && diff < maxEdge) {
							index = (int) (((diff - minEdge) / binsize));
							histogram[index]++;
						} else {
							break;
						}
					}
					// check on right of spike
					for (secIndex = mainIndex + 1; secIndex < spikeTrains.get(i).size(); secIndex++) {
						secondSpike = spikeTrains.get(i).get(secIndex);
						diff = firstSpike.time - secondSpike.time;
						if (diff > minEdge && diff < maxEdge) {
							index = (int) (((diff - minEdge) / binsize));
							histogram[index]++;
						} else {
							break;
						}
					}
				}
				ArrayList<Integer> temp = new ArrayList<Integer>();

// NSMutableArray* histMA = [NSMutableArray arrayWithCapacity:n];
				for (int j = 0; j < n; j++) {
					temp.add(Integer.valueOf(histogram[j]));
					// [histMA addObject:[NSNumber numberWithInt:histogram[i]]];
				}
				autoCorrelation.add(temp);
			}

			bAutoCorrelationDone = true;
			bProcessAutoCorrelation = false;
		}
	}

	public ArrayList<ArrayList<Integer>> getAutoCorrelation() {
		return autoCorrelation;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------------- CROSS CORRELATION
	// -----------------------------------------------------------------------------------------------------------------------------
	private void clearCrossCorrelation() {
		if (crossCorrelation != null) {
			for (int i = 0; i < crossCorrelation.size(); i++) {
				if (crossCorrelation.get(i) != null) {
					crossCorrelation.get(i).clear();
				}
			}
			crossCorrelation.clear();
			crossCorrelation = null;
		}
	}

	private void crossCorrelationAnalysis() {
		processSpikeTrains();
		if (!bCrossCorrelationDone) {
			clearCrossCorrelation();
			float maxtime = 0.1f;
			float binsize = 0.001f;
			crossCorrelation = new ArrayList<ArrayList<Integer>>();

			for (int fSpikeTrainIndex = 0; fSpikeTrainIndex < spikeTrains.size(); fSpikeTrainIndex++) {
				for (int sSpikeTrainIndex = 0; sSpikeTrainIndex < spikeTrains.size(); sSpikeTrainIndex++) {

					// ArrayList<ArrayList<BYBSpike>> spikeTrains;
					ArrayList<BYBSpike> fspikeTrain = spikeTrains.get(fSpikeTrainIndex);
					ArrayList<BYBSpike> sspikeTrain = spikeTrains.get(sSpikeTrainIndex);
					ArrayList<Integer> temp = new ArrayList<Integer>();
					if (fspikeTrain.size() > 1 && sspikeTrain.size() > 1) {
						BYBSpike firstSpike;
						BYBSpike secondSpike;
						int n = (int) Math.ceil((2 * maxtime + binsize) / binsize);

						int[] histogram = new int[n];
						for (int x = 0; x < n; ++x) {
							histogram[x] = 0;
						}

						float minEdge = -maxtime - binsize * 0.5f;
						float maxEdge = maxtime + binsize * 0.5f;
						float diff;
						int index;
						int mainIndex;
						int secIndex;
						boolean insideInterval = false;
						// go through first spike train
						for (mainIndex = 0; mainIndex < fspikeTrain.size(); mainIndex++) {
							firstSpike = fspikeTrain.get(mainIndex);
							// Check on left of spike
							insideInterval = false;
							// go through second spike train
							for (secIndex = 0; secIndex < sspikeTrain.size(); secIndex++) {
								secondSpike = sspikeTrain.get(secIndex);
								diff = firstSpike.time - secondSpike.time;
								if (diff > minEdge && diff < maxEdge) {
									insideInterval = false;
									index = (int) (((diff - minEdge) / binsize));
									histogram[index]++;
								} else if (insideInterval) {
									break;
								}
							}
						}

						for (int j = 0; j < n; j++) {
							temp.add(Integer.valueOf(histogram[j]));
						}
					}
					crossCorrelation.add(temp);
				}
			}
		}

		bProcessCrossCorrelation = false;
		bCrossCorrelationDone = true;
	}

	public ArrayList<ArrayList<Integer>> getCrossCorrelation() {
		return crossCorrelation;
	}
	
	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------------- AVERAGE SPIKE
	// -----------------------------------------------------------------------------------------------------------------------------
	private void averageSpikeAnalysis() {
		processSpikeTrains();
		if (!bAverageSpikeDone) {

			bAverageSpikeDone = true;
			bProcessAverageSpike = false;
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------------- THRESHOLDS
	// -----------------------------------------------------------------------------------------------------------------------------
	public ArrayList<int[]> getAllThresholds() {
		return thresholds;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public int getMaxThresholds() {
		return maxThresholds;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public int getThresholdsSize() {
		return thresholds.size();
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public int getSelectedThresholdIndex() {
		return selectedThreshold;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public int[] getSelectedThresholds() {
		if (selectedThreshold >= 0 && selectedThreshold < thresholds.size()) {
			return thresholds.get(selectedThreshold);
		}
		int[] z = new int[2];
		return z;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public void selectThreshold(int index) {
		if (index >= 0 && index < maxThresholds) {
			selectedThreshold = index;
			updateThresholdHandles();
			Log.d(TAG, "selectThreshold " + index);
		}
	}

	// ----------------------------------------------------------------------------------------
	public void addThreshold() {
		if (thresholds.size() < maxThresholds) {
			thresholds.add(new int[2]);
			selectedThreshold = thresholds.size() - 1;
			updateThresholdHandles();
		}
	}

	// ----------------------------------------------------------------------------------------
	public void removeSelectedThreshold() {
		if (thresholds.size() > 0 && thresholds.size() > selectedThreshold) {
			thresholds.remove(selectedThreshold);
			selectedThreshold = thresholds.size() - 1;
			updateThresholdHandles();
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public void setThreshold(int group, int index, int value) {
		if (thresholds.size() > 0 && thresholds.size() > group && index < 2) {
			thresholds.get(group)[index] = value;
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	protected void updateThresholdHandles() {
		if (context != null) {
			Intent i = new Intent();
			i.setAction("BYBUpdateThresholdHandle");
			context.getApplicationContext().sendBroadcast(i);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------------- SPIKE TRAINS
	// -----------------------------------------------------------------------------------------------------------------------------
	private void clearSpikeTrains() {
		if (spikeTrains != null) {
			Log.d(TAG, "clearSPikeTrains");
			for (int i = 0; i < spikeTrains.size(); i++) {
				if (spikeTrains.get(i) != null) {
					spikeTrains.get(i).clear();
				}
			}
			spikeTrains.clear();
			spikeTrains = null;
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public ArrayList<ArrayList<BYBSpike>> getSpikesTrains() {
		return spikeTrains;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	protected ArrayList<ArrayList<BYBSpike>> processSpikeTrains() {
		if (!bSpikeTrainsDone) {
			clearSpikeTrains();
			Log.d(TAG, "processSpikeTrains");
			spikeTrains = new ArrayList<ArrayList<BYBSpike>>();
			for (int j = 0; j < thresholds.size(); j++) {
				int mn = Math.min(thresholds.get(j)[0], thresholds.get(j)[1]);
				int mx = Math.max(thresholds.get(j)[0], thresholds.get(j)[1]);
				ArrayList<BYBSpike> temp = new ArrayList<BYBSpike>();
				for (int i = 0; i < spikes.length; i++) {
					if (spikes[i].value >= mn && spikes[i].value <= mx) {
						temp.add(spikes[i]);
					}
				}
				Log.d(TAG, "Spike train added, size: " + temp.size());
				spikeTrains.add(temp);
			}
			bProcessSpikeTrains = false;
			bSpikeTrainsDone = true;
		}
		return spikeTrains;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------------- ISI (Inter Spike
	// Interval)
	// -----------------------------------------------------------------------------------------------------------------------------
	private void clearISI() {
		if (ISI != null) {
			Log.d(TAG, "clearISI");
			for (int i = 0; i < ISI.size(); i++) {
				if (ISI.get(i) != null) {
					ISI.get(i).clear();
				}
			}
			ISI.clear();
			ISI = null;
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public void ISIAnalysis() {
		processSpikeTrains();
		if (!bISIDone) {
			Log.d(TAG, "ISIAnalysis");
			// getSpikesTrains();
			// int spikesCount = spikesTrains.size();
			int bins = 100;
			float[] logspace = BYBUtils.generateLogSpace(-3, 1, bins - 1);

			clearISI();
			ISI = new ArrayList<ArrayList<ISIResult>>();

			for (int k = 0; k < spikeTrains.size(); k++) {
				int[] histogram = new int[bins];
				for (int x = 0; x < bins; x++) {
					histogram[x] = 0;
				}
				if (spikeTrains.get(k) != null) {
					float interspikeDistance;
					int spikesCount = spikeTrains.get(k).size();
					for (int i = 1; i < spikesCount; i++) {
						interspikeDistance = spikeTrains.get(k).get(i).time - spikeTrains.get(k).get(i - 1).time;
						for (int j = 1; j < bins; j++) {
							if (interspikeDistance >= logspace[j - 1] && interspikeDistance < logspace[j]) {
								histogram[j - 1]++;
								break;
							}
						}
					}
				}
				ArrayList<ISIResult> temp = new ArrayList<ISIResult>();
				for (int i = 0; i < bins; i++) {
					temp.add(new ISIResult(logspace[i], histogram[i]));
				}
				ISI.add(temp);
				bISIDone = true;
				bProcessISI = false;
			}
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	public ArrayList<ArrayList<ISIResult>> getISI() {
		return ISI;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------------- FILE READER
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

		reset();
		if (reader != null) {
			reader.close();
			reader = null;
		}
		fileToAnalize = file;
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
	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- AUDIO FILE READ LISTENER
	// -----------------------------------------------------------------------------------------------------------------------------
	public void audioFileRead() {
		findSpikes();
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- ASYNC ANALYSIS LISTENER
	// -----------------------------------------------------------------------------------------------------------------------------
	public void analysisDone(int analysisType) {
		if (spikesAnalysis != null) {
			spikes = spikesAnalysis.getSpikes();
			minSpikeValue = spikesAnalysis.getLowestPeak();
			maxSpikeValue = spikesAnalysis.getHighestPeak();
			totalNumSamples = spikesAnalysis.getTotalSamples();
			Log.d(TAG, "FindSpikes done: lowest: " + minSpikeValue + "  highest: " + maxSpikeValue + " totalSamples: " + totalNumSamples);
			spikesAnalysis = null;
			bSpikesDone = true;
			bProcessSpikes = false;
			// bProcessSpikeTrains = true;
			Log.d(TAG, "findSpike done");
			process();
		}
	}

	public void analysisCanceled(int analysisType) {
		bSpikesDone = false;
		bProcessSpikes = false;
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- FIND SPIKES
	// -----------------------------------------------------------------------------------------------------------------------------

	private void findSpikes() {
		Log.d(TAG, "findSpikes begin");
		spikesAnalysis = null;
		bProcessSpikes = true;
		spikesAnalysis = new BYBFindSpikesAnalysis((BYBBaseAsyncAnalysis.AnalysisListener) this, getReaderSamples(), getReaderSampleRate(), getReaderNumChannels());
	}

	public BYBSpike[] getSpikes() {
		if (bSpikesDone) {
			return spikes;
		} else {
			BYBSpike[] s = new BYBSpike[0];
			return s;
		}
	}

	public boolean spikesFound() {
		return bSpikesDone;
	}

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- BROADCAST RECEIVER INSTANCES
// -----------------------------------------------------------------------------------------------------------------------------
	private AnalizeFileListener analizeFileListener;

// -----------------------------------------------------------------------------------------------------------------------------
// ----------------------------------------- BROADCAST RECEIVERS CLASS
// -----------------------------------------------------------------------------------------------------------------------------
	private class AnalizeFileListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "AnalizeFileListener: onReceive");
			if (intent.hasExtra("filePath")) {
				String filePath = intent.getStringExtra("filePath");
// if (fileToAnalize != null) {
// if (!fileToAnalize.getAbsolutePath().equals(filePath)) {
// reset();
// }
// }
				if (intent.hasExtra("doISI")) {
					if (!bISIDone) {
						bProcessISI = true;
						bSpikeTrainsDone = false;
					}
				}
				if (intent.hasExtra("doAutoCorrelation")) {
					if (!bAutoCorrelationDone) {
						bProcessAutoCorrelation = true;
						bSpikeTrainsDone = false;
					}
				}
				if (intent.hasExtra("doCrossCorrelation")) {
					if (!bCrossCorrelationDone) {
						bProcessCrossCorrelation = true;
						bSpikeTrainsDone = false;
					}
				}
				if (intent.hasExtra("doAverageSpike")) {
					if (!bAverageSpikeDone) {
						bProcessAverageSpike = true;
						bSpikeTrainsDone = false;
					}
				}

				if (fileToAnalize != null) {
					if (!fileToAnalize.getAbsolutePath().equals(filePath)) {
						reset();
						load(filePath);
					} else {
						process();
					}
				} else {
					reset();
					load(filePath);
				}

			}
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- BROADCAST RECEIVERS TOGGLES
	// -----------------------------------------------------------------------------------------------------------------------------
	private void registerAnalizeFileListener(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBAnalizeFile");
			analizeFileListener = new AnalizeFileListener();
			context.registerReceiver(analizeFileListener, intentFilter);
		} else {
			context.unregisterReceiver(analizeFileListener);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- REGISTER RECEIVERS
	// -----------------------------------------------------------------------------------------------------------------------------

	public void registerReceivers() {
		registerAnalizeFileListener(true);
	}

	// -----------------------------------------------------------------------------------------------------------------------------
	// ----------------------------------------- UNREGISTER RECEIVERS
	// -----------------------------------------------------------------------------------------------------------------------------
	public void unregisterReceivers() {
		registerAnalizeFileListener(false);
	}
}
