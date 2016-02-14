package com.backyardbrains.analysis;

import android.content.Context;

public class BYBFindSpikesAnalysis extends BYBBaseAsyncAnalysis {
	
	public BYBFindSpikesAnalysis(Context context, int analysisType, short [] data){
		super(context, analysisType);
		execute(data);
	}
	@Override
	public void process(short[] data){
		
	}
}