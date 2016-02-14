package com.backyardbrains.analysis;

public class BYBAnalysisType {
	public static final int				BYB_ANALYSIS_NONE				= -1;
	public static final int				BYB_ANALYSIS_FIND_SPIKES		= 0;
	public static final int				BYB_ANALYSIS_AUTOCORRELATION	= 1;
	public static final int				BYB_ANALYSIS_ISI				= 2;
	public static final int				BYB_ANALYSIS_CROSS_CORRELATION	= 3;
	public static final int				BYB_ANALYSIS_AVERAGE_SPIKE		= 4;
	
	public static String toString(int i){
		switch(i){
		case BYB_ANALYSIS_NONE:
			return "BYB ANALYSIS NONE";
		case BYB_ANALYSIS_FIND_SPIKES:
			return "BYB ANALYSIS FIND SPIKES";
		case BYB_ANALYSIS_AUTOCORRELATION:
			return "BYB ANALYSIS AUTOCORRELATION";
		case BYB_ANALYSIS_ISI:
			return "BYB ANALYSIS ISI";
		case BYB_ANALYSIS_CROSS_CORRELATION:
			return "BYB ANALYSIS CROSS CORRELATION";
		case BYB_ANALYSIS_AVERAGE_SPIKE:
			return "BYB ANALYSIS AVERAGE SPIKE";
		default:
			return "BYB ANALYSIS INVALID";
		}
	}
}
