package com.backyardbrains.analysis;

import android.content.Context;

public class BYBCrossCorrelationAnalysis extends BYBBaseAsyncAnalysis {
	
	public BYBCrossCorrelationAnalysis(Context context, int analysisType, short [] data){
		super(context, BYBAnalysisType.BYB_ANALYSIS_CROSS_CORRELATION, true, true);
		execute(data);
	}
	@Override
	public void process(short[] data){
		
	}
}
