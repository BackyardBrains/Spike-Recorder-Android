package com.backyardbrains;

public class BybConfigHolder {
	public BybConfigHolder(boolean autoScaled, int glWindowHorizontalSize,
			int glWindowVerticalSize) {
		configAlreadyAutoScaled = autoScaled;
		xSize = glWindowHorizontalSize;
		ySize = glWindowVerticalSize;
	}
	
	public int xSize = 0;
	public int ySize = 0;
	public boolean configAlreadyAutoScaled = false;
}
