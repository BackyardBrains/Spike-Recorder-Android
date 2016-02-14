package com.backyardbrains.analysis;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import com.backyardbrains.analysis.*;
import com.backyardbrains.drawing.BYBBaseRenderer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;

public class BYBSignalAnalysis {
	private static final String TAG = "BYBSignalAnalysis"; 
	private Context	context;
	private float [] data = null;

	private int analysisType;
	
	private BYBBaseAsyncAnalysis analyzer;
	
	public float[] getResults(){
		if(analyzer != null){
			if(analyzer.isReady())
			if(analyzer.isResultShorts()){
				if(data == null){
					short [] sr = analyzer.getShortResult();
					
					data = new float [sr.length];
					for(int i = 0; i < sr.length; i++){
						data[i] = (float)sr[i];
					}
				}
				return data;
			}else{
				return analyzer.getFloatResult();
			}
		}
			Log.d(TAG,"getResults(): analyzer is null!");
			return new float[0];
	}
	
	public BYBSignalAnalysis(Context context, short [] data, int analysisType) {
		this.context = context.getApplicationContext();
		this.analysisType = analysisType;
		analyze(data);
	}

	public void analyze(short [] data){
		resetAnalyzer();
		switch(analysisType){
		case BYBAnalysisType.BYB_ANALYSIS_AUTOCORRELATION:
				analyzer = new BYBAutocorrelationAnalysis(context, analysisType, data);
			break;
			case BYBAnalysisType.BYB_ANALYSIS_AVERAGE_SPIKE:
				analyzer = new BYBAverageSpikeAnalysis(context, analysisType, data);
				break;
			case BYBAnalysisType.BYB_ANALYSIS_CROSS_CORRELATION:
				analyzer = new BYBCrossCorrelationAnalysis(context, analysisType, data);
				break;
			case BYBAnalysisType.BYB_ANALYSIS_FIND_SPIKES:
				analyzer = new BYBFindSpikesAnalysis(context, analysisType, data);
				break;
			case BYBAnalysisType.BYB_ANALYSIS_ISI:
				analyzer = new BYBIsiAnalysis(context, analysisType, data);
				break;
			case BYBAnalysisType.BYB_ANALYSIS_NONE:
				Log.d("BYBSignalAnalysis", "analysisType is none!!");
			default:
				break;
		}
	}
	
	public void resetAnalyzer(){
		if(analyzer != null){
			analyzer.stop();
			analyzer= null;
		}
		data = null;
	}
	
}
