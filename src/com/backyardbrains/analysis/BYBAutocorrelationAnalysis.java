package com.backyardbrains.analysis;

import android.content.Context;

public class BYBAutocorrelationAnalysis extends BYBBaseAsyncAnalysis {
	
	public BYBAutocorrelationAnalysis(Context context, int analysisType, short [] data){
		super(context, BYBAnalysisType.BYB_ANALYSIS_AUTOCORRELATION, true, true);
		execute(data);
	}
	@Override
	public void process(short[] data){
		
	}
}
