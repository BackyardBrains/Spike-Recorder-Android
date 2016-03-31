package com.backyardbrains.drawing;

import android.util.Log;

public class BYBColors {

	private static final String		TAG		= "BYBColors";

	public static final int			red		= 0;
	public static final int			green	= 1;
	public static final int			blue	= 2;
	public static final int			cyan	= 3;
	public static final int			magenta	= 4;
	public static final int			yellow	= 5;
	public static final int			orange	= 6;
	public static final int			gray	= 7;
	public static final int			white	= 8;
	public static final int			black	= 9;
	// ----------------------------------------------------------------------------------------
	public static final float[][]	colors	= { { 1.0f, 0.0f, 0.0f, 1.0f },						// red
														{ 0.0f, 1.0f, 0.0f, 1.0f },				// green
														{ 0.0f, 0.0f, 1.0f, 1.0f },				// blue
														{ 0.0f, 1.0f, 1.0f, 1.0f },				// cyan
														{ 1.0f, 0.0f, 1.0f, 1.0f },				// magenta
														{ 1.0f, 1.0f, 0.0f, 1.0f },				// yellow
														{ 1.0f, 0.5f, 0.0f, 1.0f }, 			// orange?
														{ 0.5f, 0.5f, 0.5f, 1.0f }, 			// gray
														{ 1.0f, 1.0f, 1.0f, 1.0f },				// white
														{ 0.0f, 0.0f, 0.0f, 1.0f }, };			// black
	public static final float [][] chosenColors = {colors[red], colors[yellow], colors[cyan]};
	
	// ----------------------------------------------------------------------------------------

	public static int asARGB(int rgba) {
		int argb = 0;
		for (int i = 1; i < 4; i++) {
			argb |= ((rgba >> (i*8)) & 0xff) << ((i - 1)*8);
		}
		argb |= (rgba & 0xff) << (3*8);
	//	Log.d(TAG, Integer.toHexString(rgba) + " asARGB: " + Integer.toHexString(argb));
		return argb;
	}

	// ----------------------------------------------------------------------------------------
	public static float[] asARGB(float[] rgba) {
		float[] argb = new float[4];
		if (rgba.length == 4) {
			for (int i = 0; i < 4; i++) {
				argb[i] = rgba[(i + 3) % 4];
			}
		}
		return argb;
	}

	// ----------------------------------------------------------------------------------------
	public static int getColorAsHexById(int id) {
		return getGlColorAsHex(getColorAsGlById(id));
	}

	// ----------------------------------------------------------------------------------------
	public static float[] getColorAsGlById(int id) {
		return colors[id];
	}

	// ----------------------------------------------------------------------------------------
	public static int getGlColorAsHex(float[] glc){
		int c = 0;
		for(int i = 0; i < glc.length; i++){
			c |= (((int)(glc[i]*0xff))&0xff) << ((3-i)*8);
		}
//		String msg = "getGlColor: ";
//		for(float a: glc){
//			msg += a + ", ";
//		}
//		msg += "AsHex: ";
//		
//		msg+= Integer.toHexString(c);
//		Log.d(TAG, msg);
		
		return c;
	}

	// ----------------------------------------------------------------------------------------
	public static float[] getHexAsGlColor(int hex) {
		float[] c = new float[4];
		for (int i = 0; i < 4; i++) {
			c[3 - i] = ((hex >> (i*8)) & 0xff) / (float) 0xff;
		}
		return c;
	}

}
