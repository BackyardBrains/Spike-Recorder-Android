package com.backyardbrains.analysis;

import android.content.Context;

public class BYBAverageSpikeAnalysis  extends BYBBaseAsyncAnalysis {
	
	public BYBAverageSpikeAnalysis(Context context, int analysisType, short [] data){
		super(context, BYBAnalysisType.BYB_ANALYSIS_AVERAGE_SPIKE, true, true);
		execute(data);
	}
	@Override
	public void process(short[] data){
		
	}
}
