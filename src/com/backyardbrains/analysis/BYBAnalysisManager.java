package com.backyardbrains.analysis;

import java.io.File;
import java.util.ArrayList;

import com.backyardbrains.utls.BYBUtils;
import com.backyardbrains.audio.RecordingReader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BYBAnalysisManager implements RecordingReader.AudioFileReadListener, BYBBaseAsyncAnalysis.AnalysisListener {
	private static final String				TAG									= "BYBAnalysisManager";

	private Context							context;
	private RecordingReader					reader;
	private File							fileToAnalize;

	private BYBFindSpikesAnalysis			spikesAnalysis;
	private boolean							bSpikesDone							= false;
	private boolean							bProcessSpikes						= false;
	private BYBSpike[]						spikes;
	private float							maxSpikeValue, minSpikeValue;
	private int								totalNumSamples						= 0;

	private ArrayList<ArrayList<BYBSpike>>	spikeTrains;
	private boolean							bSpikeTrainsDone					= false;
	private boolean							bProcessSpikeTrains					= false;

	private ArrayList<ArrayList<ISIResult>>	ISI;
	private boolean							bProcessISI							= false;
	private boolean							bISIDone							= false;

	private int								selectedThreshold					= 0;
	private ArrayList<int[]>				thresholds;
	public static final int					maxThresholds						= 3;
	private boolean bThresholdsChanged = false;

	private ArrayList<ArrayList<Integer>>	autoCorrelation;
	private boolean							bProcessAutoCorrelation				= false;
	private boolean							bAutoCorrelationDone				= false;

	private ArrayList<ArrayList<Integer>>	crossCorrelation;
	private boolean							bProcessCrossCorrelation			= false;
	private boolean							bCrossCorrelationDone				= false;

	private AverageSpikeData[]				avr;
	private boolean							bProcessAverageSpike				= false;
	private boolean							bAverageSpikeDone					= false;

	public static final int					BUFFER_SIZE							= 524288;
	public static final float				AVERAGE_SPIKE_HALF_LENGTH_SECONDS	= 0.002f;

	private String lastProcess ="";

	// ---------------------------------------------------------------------------------------------
	public BYBAnalysisManager(Context context) {
		this.context = context.getApplicationContext();
		thresholds = new ArrayList<int[]>();
		thresholds.add(new int[2]);
		registerReceivers();
	}

	// ---------------------------------------------------------------------------------------------
	public void close() {
		unregisterReceivers();
	}

	// ---------------------------------------------------------------------------------------------
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
		bAverageSpikeDone = false;

		bProcessAutoCorrelation = false;
		bAutoCorrelationDone = false;
		bProcessCrossCorrelation = false;
		bCrossCorrelationDone = false;
		bProcessAverageSpike = false;
		bAverageSpikeDone = false;
		bThresholdsChanged = false;
		clearISI();
		clearSpikeTrains();
		clearAutoCorrelation();
		clearCrossCorrelation();
		clearAverageSpike();
		spikes = null;
		clearThresholds();
	}

	private void broadcastSetRenderer(String renderer){
//		"ISI"
//		"AutoCorrelation"
//		"CrossCorrelation"
//		"AverageSpike"
		if(context != null && !renderer.equalsIgnoreCase("")) {
			lastProcess = renderer;
			Log.w(TAG,"broadcastSetRenderer");
			Intent k = new Intent();
			k.setAction("BYBRenderAnalysis");
			k.putExtra(renderer, true);
			context.sendBroadcast(k);
		}
	}
	public boolean checkCurrentFilePath(String filePath){
		if (fileToAnalize != null) {
			 return fileToAnalize.getAbsolutePath().equals(filePath);
		}
		return false;
	}
	// ---------------------------------------------------------------------------------------------
	public int getTotalNumSamples() {
		return totalNumSamples;
	}

	public float getMaxSpikeValue() {
		return maxSpikeValue;
	}

	public float getMinSpikeValue() {
		return minSpikeValue;
	}

	// ---------------------------------------------------------------------------------------------
	private void process() {
		Log.d(TAG, "process!");
		if (spikesFound()) {
			String renderer = "";
			if (bProcessSpikeTrains) {
				processSpikeTrains();
				bProcessSpikeTrains = false;
			}
			if (bProcessISI) {
				ISIAnalysis();
				bProcessISI = false;
				renderer = "ISI";
			}
			if (bProcessAutoCorrelation) {
				autoCorrelationAnalysis();
				bProcessAutoCorrelation = false;
				renderer = "AutoCorrelation";
			}
			if (bProcessCrossCorrelation) {
				crossCorrelationAnalysis();
				bProcessCrossCorrelation = false;
				renderer = "CrossCorrelation";
			}
			if (bProcessAverageSpike) {
				averageSpikeAnalysis();
				bProcessAverageSpike = false;
				renderer = "AverageSpike";
			}
			if(!renderer.equals("")) {
				broadcastSetRenderer(renderer);
			}
			Intent i = new Intent();
			i.setAction("BYBRenderAnalysis");
			i.putExtra("requestRender", true);
			context.sendBroadcast(i);
		}
		 bThresholdsChanged = false;
	}

	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------------- AUTOCORRELATION
	// ---------------------------------------------------------------------------------------------
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
		if (!bAutoCorrelationDone || bThresholdsChanged) {
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

	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------------- CROSS CORRELATION
	// ---------------------------------------------------------------------------------------------
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
		if (!bCrossCorrelationDone || bThresholdsChanged) {
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

	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------------- AVERAGE SPIKE
	// ---------------------------------------------------------------------------------------------

	public class AverageSpikeData {

		public float[]	averageSpike;			// Main line
		public float	maxAverageSpike;
		public float	minAverageSpike;
		public float[]	topSTDLine;
		public float[]	bottomSTDLine;
		public float	maxStd;
		public float	minStd;

		public float[]	normAverageSpike;		// Main line
		public float	normMaxAverageSpike;
		public float	normMinAverageSpike;
		public float[]	normTopSTDLine;
		public float[]	normBottomSTDLine;
	

		public int		numberOfSamplesInData;
		public float	samplingRate;
		public int		countOfSpikes;
	};

	private void clearAverageSpike() {
		if(avr != null){
		for (int i = 0; i < avr.length; i++) {
			avr[i] = null;
		}
		avr = null;
		}
	}

	private void averageSpikeAnalysis() {
		processSpikeTrains();
		if (!bAverageSpikeDone || bThresholdsChanged) {
			// int channelIndex
			if (reader != null) {
				if (reader.isReady()) {
					//Log.d(TAG, "===================== start average spike------");
					int numberOfSamples = reader.getDataShorts().length;
					short[] data = reader.getDataShorts();
					int sampleRate = reader.getSampleRate();

					int halfSpikeLength = (int) (sampleRate * AVERAGE_SPIKE_HALF_LENGTH_SECONDS);
					int spikeLength = 2 * halfSpikeLength + 1;
					clearAverageSpike();
					avr = new AverageSpikeData[spikeTrains.size()];

					ArrayList<BYBSpike> tempSpikeTrain;

					for (int i = 0; i < spikeTrains.size(); i++) {
						avr[i] = new AverageSpikeData();
						
						avr[i].averageSpike = new float[spikeLength];
						avr[i].topSTDLine = new float[spikeLength];
						avr[i].bottomSTDLine = new float[spikeLength];

						avr[i].normAverageSpike = new float[spikeLength];
						avr[i].normTopSTDLine = new float[spikeLength];
						avr[i].normBottomSTDLine = new float[spikeLength];

						avr[i].numberOfSamplesInData = spikeLength;
						avr[i].samplingRate = sampleRate;
						avr[i].countOfSpikes = 0;
					}

					for (int spikeTrainIndex = 0; spikeTrainIndex < spikeTrains.size(); spikeTrainIndex++) {
						tempSpikeTrain = spikeTrains.get(spikeTrainIndex);
						for (int spikeIndex = 0; spikeIndex < tempSpikeTrain.size(); spikeIndex++) {

							int spikeSampleIndex = tempSpikeTrain.get(spikeIndex).index;

							// if our spike is outside current batch of samples
							// go to next channel
							if ((spikeSampleIndex + halfSpikeLength) >= numberOfSamples || spikeSampleIndex - halfSpikeLength < 0) {
								break;
							}
							// add spike to average buffer
							for (int i = 0; i < spikeLength; i++) {
								avr[spikeTrainIndex].averageSpike[i] += data[spikeSampleIndex - halfSpikeLength + i];
								avr[spikeTrainIndex].topSTDLine[i] += data[spikeSampleIndex - halfSpikeLength + i] * data[spikeSampleIndex - halfSpikeLength + i];
							}
							avr[spikeTrainIndex].countOfSpikes++;

						}
					}

					// divide sum of spikes with number of spikes
					// and find max and min
					for (int i = 0; i < spikeTrains.size(); i++) {
						if (avr[i].countOfSpikes > 1) {
							float divider = (float) avr[i].countOfSpikes;
							float mn = Float.MAX_VALUE;
							float mx = Float.MIN_VALUE;
							for (int j = 0; j < spikeLength; j++) {
								avr[i].averageSpike[j] /= divider;
								if (avr[i].averageSpike[j] > mx) mx = avr[i].averageSpike[j];
								if (avr[i].averageSpike[j] < mn) mn = avr[i].averageSpike[j];
							}
							avr[i].maxAverageSpike = mx;
							avr[i].minAverageSpike = mn;

							float[] temp = new float[spikeLength];
							for (int j = 0; j < spikeLength; j++) {
								avr[i].topSTDLine[j] /= divider;
								temp[j] = avr[i].averageSpike[j] * avr[i].averageSpike[j];
							}
							for (int j = 0; j < spikeLength; j++) {
								temp[j] = avr[i].topSTDLine[j] - temp[j];
							}

							// Calculate STD
							// mean of square sum
// vDSP_vsdiv (
// avr[i].topSTDLine,
// 1,
// &divider,
// avr[i].topSTDLine,
// 1,
// spikeLength
// );
// square of mean
// vDSP_vsq(
// avr[i].averageSpike
// ,1
// ,tempSqrBuffer
// ,1
// ,spikeLength);

							// substract mean of square summ and square of mean
							// vDSP_vsub has documentation error/contradiction
							// in order of operands
							// we will get variance
// vDSP_vsub (
// tempSqrBuffer,
// 1,
// avr[i].topSTDLine,
// 1,
// tempSqrBuffer,
// 1,
// spikeLength
// );
//
// calculate STD from variance
							for (int k = 0; k < spikeLength; k++) {
								temp[k] = (float) Math.sqrt(temp[k]);
							}

							// Make top line and bottom line around mean that
							// represent one STD deviation from mean

							// vDSP_vsub has documentation error/contradiction
							// in order of operands
							// bottom line

							for (int j = 0; j < spikeLength; j++) {
								avr[i].bottomSTDLine[j] = avr[i].averageSpike[j] - temp[j];
								avr[i].topSTDLine[j] = avr[i].averageSpike[j] + temp[j];
							}

//
// vDSP_vsub (
// tempSqrBuffer,
// 1,
// avr[i].averageSpike,
// 1,
// avr[i].bottomSTDLine,
// 1,
// spikeLength
// );

							// top line
// vDSP_vadd (
// avr[i].averageSpike,
// 1,
// tempSqrBuffer,
// 1,
// avr[i].topSTDLine,
// 1,
// spikeLength
// );

							// Find max and min of top and bottom std line
							// respectively
							// fined max
							avr[i].minStd = Float.MAX_VALUE;
							avr[i].maxStd = Float.MIN_VALUE;
							for (int j = 0; j < spikeLength; j++) {

								if (avr[i].maxStd < avr[i].topSTDLine[j]) avr[i].maxStd = avr[i].topSTDLine[j];
								if (avr[i].minStd > avr[i].bottomSTDLine[j]) avr[i].minStd = avr[i].bottomSTDLine[j];
							}

// vDSP_maxv(avr[i].topSTDLine,
// 1,
// &(avr[i].maxStd),
// spikeLength
// );
// //find min
// vDSP_minv (
// avr[i].bottomSTDLine,
// 1,
// &(avr[i].minStd),
// spikeLength
// );

						}
						float mn = Math.min(avr[i].minStd, avr[i].minAverageSpike);
						float mx = Math.max(avr[i].maxStd, avr[i].maxAverageSpike);
						for (int j = 0; j < spikeLength; j++) {
							avr[i].normAverageSpike[j]= BYBUtils.map(avr[i].averageSpike[j], mn, mx, 0.0f,1.0f);
							avr[i].normTopSTDLine[j] =  BYBUtils.map(avr[i].topSTDLine[j], mn, mx, 0.0f,1.0f);
							avr[i].normBottomSTDLine[j] =  BYBUtils.map(avr[i].bottomSTDLine[j], mn, mx, 0.0f,1.0f);
						}
						//Log.d(TAG, "Min: " + mn + "  Max: " + mx);
					}
					//Log.d(TAG, "numSpikeAverages: " + avr.length);
					//Log.d(TAG, "===================== end average spike------");
				}
			}
			
			
			
			bAverageSpikeDone = true;
			bProcessAverageSpike = false;
		}
	}

	public AverageSpikeData[] getAverageSpikes() {
		return avr;
	}

	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------------- THRESHOLDS
	// ---------------------------------------------------------------------------------------------
	public ArrayList<int[]> getAllThresholds() {
		return thresholds;
	}
	// ---------------------------------------------------------------------------------------------
	public int getMaxThresholds() {
		return maxThresholds;
	}

	// ---------------------------------------------------------------------------------------------
	public int getThresholdsSize() {
		return thresholds.size();
	}

	// ---------------------------------------------------------------------------------------------
	public int getSelectedThresholdIndex() {
		return selectedThreshold;
	}

	// ---------------------------------------------------------------------------------------------
	public int[] getSelectedThresholds() {
		if (selectedThreshold >= 0 && selectedThreshold < thresholds.size()) {
			//Log.d(TAG, "getSelectedThreshold ok " + thresholds.get(selectedThreshold)[0] + "  ,  " + thresholds.get(selectedThreshold)[1]);
			return thresholds.get(selectedThreshold);
		}
		//Log.d(TAG, "getSelectedThreshold invalid!!!!");
		int[] z = new int[2];
		return z;
	}

	// ---------------------------------------------------------------------------------------------
	public void selectThreshold(int index) {
		if (index >= 0 && index < maxThresholds) {
			selectedThreshold = index;
			updateThresholdHandles();
			//Log.d(TAG, "selectThreshold " + index);
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

	// ---------------------------------------------------------------------------------------------
	public void setThreshold(int group, int index, int value) {
		if (thresholds.size() > 0 && thresholds.size() > group && index < 2) {
			thresholds.get(group)[index] = value;
			bThresholdsChanged = true;
		}
	}

	public void clearThresholds() {
		thresholds.clear();
		selectedThreshold = 0;
		bThresholdsChanged = true;
		updateThresholdHandles();
	}

	// ---------------------------------------------------------------------------------------------
	protected void updateThresholdHandles() {
		if (context != null) {
			Intent i = new Intent();
			i.setAction("BYBUpdateThresholdHandle");
			context.getApplicationContext().sendBroadcast(i);
			bThresholdsChanged = true;
		}
	}

	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------------- SPIKE TRAINS
	// ---------------------------------------------------------------------------------------------
	private void clearSpikeTrains() {
		if (spikeTrains != null) {
			//Log.d(TAG, "clearSPikeTrains");
			for (int i = 0; i < spikeTrains.size(); i++) {
				if (spikeTrains.get(i) != null) {
					spikeTrains.get(i).clear();
				}
			}
			spikeTrains.clear();
			spikeTrains = null;
		}
	}

	// ---------------------------------------------------------------------------------------------
	public ArrayList<ArrayList<BYBSpike>> getSpikesTrains() {
		return spikeTrains;
	}

	// ---------------------------------------------------------------------------------------------
	protected ArrayList<ArrayList<BYBSpike>> processSpikeTrains() {
		if (!bSpikeTrainsDone|| bThresholdsChanged) {
			clearSpikeTrains();
			//Log.d(TAG, "processSpikeTrains");
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
				//Log.d(TAG, "Spike train added, size: " + temp.size());
				spikeTrains.add(temp);
			}
			bProcessSpikeTrains = false;
			bSpikeTrainsDone = true;
		}
		return spikeTrains;
	}

	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------------- ISI (Inter Spike Interval)
	// ---------------------------------------------------------------------------------------------
	private void clearISI() {
		if (ISI != null) {
			//Log.d(TAG, "clearISI");
			for (int i = 0; i < ISI.size(); i++) {
				if (ISI.get(i) != null) {
					ISI.get(i).clear();
				}
			}
			ISI.clear();
			ISI = null;
		}
	}

	// ---------------------------------------------------------------------------------------------
	public void ISIAnalysis() {
		processSpikeTrains();
		if (!bISIDone || bThresholdsChanged) {
			//Log.d(TAG, "ISIAnalysis");
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

	// ---------------------------------------------------------------------------------------------
	public ArrayList<ArrayList<ISIResult>> getISI() {
		return ISI;
	}

	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------------- FILE READER
	// ---------------------------------------------------------------------------------------------
	public int getReaderSampleRate() {
		if (reader != null) {
			if (reader.isReady()) {
				return reader.getSampleRate();
			}
		}
		return 0;
	}
	// ---------------------------------------------------------------------------------------------
	public int getReaderNumChannels() {
		if (reader != null) {
			if (reader.isReady()) {
				return reader.getNumChannels();
			}
		}
		return 0;
	}
	// ---------------------------------------------------------------------------------------------
	public short[] getReaderSamples() {
		if (reader != null) {
			if (reader.isReady()) {
				return reader.getDataShorts();
			}
		}
		short[] t = { 0, 0 };
		return t;
	}
	// ---------------------------------------------------------------------------------------------
	public boolean load(File file) {
		Log.d(TAG, "load");
		if (!file.exists()) return false;

		reset();
		if (reader != null) {
			reader.close();
			reader = null;
		}
		fileToAnalize = file;
		if (reader == null) {
			reader = new RecordingReader(fileToAnalize, (RecordingReader.AudioFileReadListener) this);
			Log.d(TAG, "loading audio file: " + fileToAnalize.getAbsolutePath());
		}
		return true;
	}
	// ---------------------------------------------------------------------------------------------
	public boolean load(String filePath) {
		return load(new File(filePath));
	}

	// ---------------------------------------------------------------------------------------------
	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- AUDIO FILE READ LISTENER
	// ---------------------------------------------------------------------------------------------
	public void audioFileRead() {
		findSpikes();
	}

	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- ASYNC ANALYSIS LISTENER
	// ---------------------------------------------------------------------------------------------
	public void analysisDone(int analysisType) {
		if (spikesAnalysis != null) {
			spikes = spikesAnalysis.getSpikes();
			minSpikeValue = spikesAnalysis.getLowestPeak();
			maxSpikeValue = spikesAnalysis.getHighestPeak();
			totalNumSamples = spikesAnalysis.getTotalSamples();
			//Log.d(TAG, "FindSpikes done: lowest: " + minSpikeValue + "  highest: " + maxSpikeValue + " totalSamples: " + totalNumSamples);
			spikesAnalysis = null;
			bSpikesDone = true;
			bProcessSpikes = false;
			// bProcessSpikeTrains = true;


			process();
		}
	}

	public void analysisCanceled(int analysisType) {
		if(analysisType == BYBAnalysisType.BYB_ANALYSIS_FIND_SPIKES) {
			bSpikesDone = false;
		}
		bProcessSpikes = false;
	}

	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- FIND SPIKES
	// ---------------------------------------------------------------------------------------------

	private void findSpikes() {
		//Log.d(TAG, "findSpikes begin");
		spikesAnalysis = null;
		bProcessSpikes = true;
		spikesAnalysis = new BYBFindSpikesAnalysis((BYBBaseAsyncAnalysis.AnalysisListener) this, getReaderSamples(), getReaderSampleRate(), getReaderNumChannels());
	}

	public BYBSpike[] getSpikes() {
		if (spikesFound()) {
			return spikes;
		} else {
			BYBSpike[] s = new BYBSpike[0];
			return s;
		}
	}

	public boolean spikesFound() {
		return ( getThresholdsSize() > 0 && spikes !=null && spikes.length > 0 && bSpikesDone);

	}

// ---------------------------------------------------------------------------------------------
// ----------------------------------------- BROADCAST RECEIVER INSTANCES
// ---------------------------------------------------------------------------------------------
	private AnalizeFileListener analizeFileListener;
	private AnalysisFragmentReadyListener analysisFragmentReadyListener;
// ---------------------------------------------------------------------------------------------
// ----------------------------------------- BROADCAST RECEIVERS CLASS
// ---------------------------------------------------------------------------------------------
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
					if (!bISIDone || bThresholdsChanged) {
						bProcessISI = true;
						bSpikeTrainsDone = false;
					}else{broadcastSetRenderer("ISI");}
				}
				if (intent.hasExtra("doAutoCorrelation")) {
					if (!bAutoCorrelationDone|| bThresholdsChanged) {
						bProcessAutoCorrelation = true;
						bSpikeTrainsDone = false;
					}else{broadcastSetRenderer("AutoCorrelation");}
				}
				if (intent.hasExtra("doCrossCorrelation")) {
					if (!bCrossCorrelationDone|| bThresholdsChanged) {
						bProcessCrossCorrelation = true;
						bSpikeTrainsDone = false;
					}else{broadcastSetRenderer("CrossCorrelation");}
				}
				if (intent.hasExtra("doAverageSpike")) {
					if (!bAverageSpikeDone|| bThresholdsChanged) {
						bProcessAverageSpike = true;
						bSpikeTrainsDone = false;
					}else{broadcastSetRenderer("AverageSpike");}

				}

				if (fileToAnalize != null) {
					if (!fileToAnalize.getAbsolutePath().equals(filePath)) {
						reset();
						// clearThresholds();
						load(filePath);
					} else {
						process();
					}
				} else {
					reset();
					// clearThresholds();
					load(filePath);
				}

			}
		}
	}
	private class AnalysisFragmentReadyListener extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			broadcastSetRenderer(lastProcess);
		}
	}
	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- BROADCAST RECEIVERS TOGGLES
	// ---------------------------------------------------------------------------------------------
	private void registerAnalizeFileListener(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBAnalizeFile");
			analizeFileListener = new AnalizeFileListener();
			context.registerReceiver(analizeFileListener, intentFilter);
		} else {
			context.unregisterReceiver(analizeFileListener);
		}
	}
	private void registerAnalysisFragmentReadyListener(boolean reg) {
		if (reg) {
			IntentFilter intentFilter = new IntentFilter("BYBAnalysisFragmentReady");
			analysisFragmentReadyListener = new AnalysisFragmentReadyListener();
			context.registerReceiver(analysisFragmentReadyListener, intentFilter);
		} else {
			context.unregisterReceiver(analysisFragmentReadyListener);
		}
	}
	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- REGISTER RECEIVERS
	// ---------------------------------------------------------------------------------------------

	public void registerReceivers() {
		registerAnalizeFileListener(true);
		registerAnalysisFragmentReadyListener(true);
	}

	// ---------------------------------------------------------------------------------------------
	// ----------------------------------------- UNREGISTER RECEIVERS
	// ---------------------------------------------------------------------------------------------
	public void unregisterReceivers() {
		registerAnalizeFileListener(false);
		registerAnalysisFragmentReadyListener(false);
	}
}
