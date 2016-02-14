package com.backyardbrains.analysis;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;


	 class BYBBaseAsyncAnalysis extends AsyncTask<short[], Void, Void> {
		public static final String TAG = "BYBAsyncAnalysis";
		protected int analysisType= 0;
		protected boolean bReady = false;
		
		protected Context context;
		protected short [] resultShorts;
		protected float [] resultFloats;
		protected boolean bReturnsShorts = true;
		
		public BYBBaseAsyncAnalysis(Context context, int analysisType){
			this.analysisType= analysisType;
			this.context = context;
		}
		public boolean isReady(){return bReady;}
		public void stop(){
			cancel(true);
		}
		public boolean isResultShorts(){return bReturnsShorts;}
		public void process(short[] data){
			//override this with the code to process data
		}
		public short [] getShortResult(){
			if(bReady && bReturnsShorts ){
				return resultShorts;
			}else{
				if(bReturnsShorts){
					Log.d(TAG, "getShortResult() " + BYBAnalysisType.toString(analysisType)+ " analysis not ready yet!!");
				}else{
					Log.e(TAG, "getShortResult() " + BYBAnalysisType.toString(analysisType)+ " doesn't return shorts!!");
				}
				return new short[0];
			}
		}
		public float[] getFloatResult(){
			if(bReady && !bReturnsShorts){
				return resultFloats;
			}else{
				if(!bReturnsShorts){
					Log.d(TAG, "getFloatResult() " + BYBAnalysisType.toString(analysisType)+ " analysis not ready yet!!");
				}else{
					Log.e(TAG, "getFloatResult() " + BYBAnalysisType.toString(analysisType)+ " doesn't return floats!!");
				}
				return new float[0];
			}
		}
		@Override
		protected Void doInBackground(short[]... params) {
			for (short[] f : params) {
				process(f);
			}
			// Log.d(getClass().getCanonicalName(),"Finished reading " +
			// recordingFile.getName());
			return null;
		}

		@Override
		protected void onCancelled(Void v) {

			Log.d(TAG, "onCancelled");
			bReady = false;
			if (context != null) {
				Intent i = new Intent();
				i.setAction("BYBSignalAnalysisCancelled");
				i.putExtra("analysisType",analysisType );
				context.sendBroadcast(i);
			}
		}

		@Override
		protected void onPostExecute(Void v) {

			Log.d(TAG, "onPostExecute");
			bReady = true;
			if (context != null) {
				Intent i = new Intent();
				i.setAction("BYBSignalAnalysisDone");
				i.putExtra("analysisType",analysisType );
				context.sendBroadcast(i);
			}
		}
		
		@Override
		protected void onPreExecute() {
			bReady = false;
			// data = null;
		}
	}
