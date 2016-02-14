package com.backyardbrains.analysis;

import android.content.Context;

public class BYBCrossCorrelationAnalysis extends BYBBaseAsyncAnalysis {
	
	public BYBCrossCorrelationAnalysis(Context context, int analysisType, short [] data){
		super(context, analysisType);
		execute(data);
	}
	@Override
	public void process(short[] data){
		
	}
}
