package com.backyardbrains.analysis;

import android.content.Context;

public class BYBIsiAnalysis  extends BYBBaseAsyncAnalysis {
	
	public BYBIsiAnalysis(Context context, short [] data){
		super(context, BYBAnalysisType.BYB_ANALYSIS_ISI, true, true);
		execute(data);
	}
	@Override
	public void process(short[] data){
		
	}
}