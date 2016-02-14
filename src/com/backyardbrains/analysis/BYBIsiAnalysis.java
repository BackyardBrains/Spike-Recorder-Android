package com.backyardbrains.analysis;

import android.content.Context;

public class BYBIsiAnalysis  extends BYBBaseAsyncAnalysis {
	
	public BYBIsiAnalysis(Context context, int analysisType, short [] data){
		super(context, analysisType);
		execute(data);
	}
	@Override
	public void process(short[] data){
		
	}
}