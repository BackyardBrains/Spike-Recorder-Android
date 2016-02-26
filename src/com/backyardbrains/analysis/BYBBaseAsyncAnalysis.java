package com.backyardbrains.analysis;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

class BYBBaseAsyncAnalysis {
	public static final String	TAG				= "BYBAsyncAnalysis";
	protected int				analysisType	= 0;
	protected boolean			bReady			= false;

	protected Context			context;
	protected AnalysisListener listener;
	
	protected short[]			resultShorts;
	protected float[]			resultFloats;
	protected boolean			bReturnsShorts;
	protected boolean bProcessShorts;
	asyncProcessShorts asyncShorts = null;
	asyncProcessFloats asyncFloats = null;
	public interface AnalysisListener{
		public void analysisDone(int analysisType);
		public void analysisCanceled(int analysisType);
		
	}
	public BYBBaseAsyncAnalysis(AnalysisListener listener, int analysisType, boolean bReturnsShorts, boolean bProcessShorts) {
		this(analysisType, bReturnsShorts, bProcessShorts);
		this.listener = listener;
		this.context = null;
	}
	public BYBBaseAsyncAnalysis(Context context, int analysisType, boolean bReturnsShorts, boolean bProcessShorts) {
		this(analysisType, bReturnsShorts, bProcessShorts);
		this.context = context;
		this.listener =null;
	}
	private BYBBaseAsyncAnalysis(int analysisType, boolean bReturnsShorts, boolean bProcessShorts) {
		this.analysisType = analysisType;
		this.bProcessShorts = bProcessShorts;
		this.bReturnsShorts = bReturnsShorts;
		if(bProcessShorts){
			asyncShorts = new asyncProcessShorts();
		}else{
			asyncFloats = new asyncProcessFloats();
		}
	}

	public boolean isReady() {
		return bReady;
	}
	public void execute(short[] data){
		if(bProcessShorts){
			asyncShorts.execute(data);
		}else{
			Log.d(TAG, "can't execute, doesn't process shorts");
		}
	}
	public void execute(float[] data){
		if(!bProcessShorts){
			asyncFloats.execute(data);
		}else{
			Log.d(TAG, "can't execute, doesn't process floats");
		}
	}

	public void stop() {
		if(bProcessShorts){
			asyncShorts.cancel(true);
		}else{
			asyncFloats.cancel(true);
		}
	}

	public boolean isResultShorts() {
		return bReturnsShorts;
	}

	public void process(short[] data) {
		// override this with the code to process data
	}
	public void process(float[] data) {
		// override this with the code to process data
	}

	
	public short[] getShortResult() {
		if (bReady && bReturnsShorts) {
			return resultShorts;
		} else {
			if (bReturnsShorts) {
				Log.d(TAG, "getShortResult() " + BYBAnalysisType.toString(analysisType) + " analysis not ready yet!!");
			} else {
				Log.e(TAG, "getShortResult() " + BYBAnalysisType.toString(analysisType) + " doesn't return shorts!!");
			}
			return new short[0];
		}
	}

	public float[] getFloatResult() {
		if (bReady && !bReturnsShorts) {
			return resultFloats;
		} else {
			if (!bReturnsShorts) {
				Log.d(TAG, "getFloatResult() " + BYBAnalysisType.toString(analysisType) + " analysis not ready yet!!");
			} else {
				Log.e(TAG, "getFloatResult() " + BYBAnalysisType.toString(analysisType) + " doesn't return floats!!");
			}
			return new float[0];
		}
	}

	protected void asyncOnCancelled() {

		Log.d(TAG, "onCancelled");
		bReady = false;
		if (context != null) {
			Intent i = new Intent();
			i.setAction("BYBSignalAnalysisCancelled");
			i.putExtra("analysisType", analysisType);
			i.putExtra("returnsShorts", bReturnsShorts);
			i.putExtra("processedShorts", bProcessShorts);
			context.sendBroadcast(i);
		}
		if(listener != null){
			listener.analysisCanceled(analysisType);
		}
	}

	protected void asyncPostExecute() {

		Log.d(TAG, "onPostExecute");
		bReady = true;
		if (context != null) {
			Intent i = new Intent();
			i.setAction("BYBSignalAnalysisDone");
			i.putExtra("analysisType", analysisType);
			i.putExtra("returnsShorts", bReturnsShorts);
			i.putExtra("processedShorts", bProcessShorts);
			context.sendBroadcast(i);
		}
		if(listener != null){
			listener.analysisDone(analysisType);
		}
	}

	protected void asyncPreExecute() {
		bReady = false;
		// data = null;
	}

	private class asyncProcessFloats extends AsyncTask<float[], Void, Void> {

		@Override
		protected Void doInBackground(float[]... params) {
			for (float[] f : params) {
				process(f);
			}
			return null;
		}

		@Override
		protected void onCancelled(Void v) {
			asyncOnCancelled();
		}

		@Override
		protected void onPostExecute(Void v) {
			asyncPostExecute();
		}

		@Override
		protected void onPreExecute() {
			asyncPreExecute();
		}
	}
	
	
	private class asyncProcessShorts extends AsyncTask<short[], Void, Void> {

		@Override
		protected Void doInBackground(short[]... params) {
			for (short[] f : params) {
				process(f);
			}
			return null;
		}

		@Override
		protected void onCancelled(Void v) {
			asyncOnCancelled();
		}

		@Override
		protected void onPostExecute(Void v) {
			asyncPostExecute();
		}

		@Override
		protected void onPreExecute() {
			asyncPreExecute();
		}
	}
}
