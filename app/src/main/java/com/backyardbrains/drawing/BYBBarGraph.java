package com.backyardbrains.drawing;

import android.util.Log;

import javax.microedition.khronos.opengles.GL10;

public class BYBBarGraph {

	protected BYBMesh mesh = null;
	protected BYBMesh axisMesh = null;
	protected BYBMesh boxMesh = null;
	protected float minV, maxV, minH, maxH;
	protected int numDivsV, numDivsH;
	protected float x, y, w,h;
	protected boolean bAxisHSet = false;
	protected boolean bAxisVSet = false;
	float markerLength = 10;
	float margin = markerLength + 10;
	float l, r, t,b;
	
	protected float [] axisColor = BYBColors.getColorAsGlById(BYBColors.white);
	
	public BYBBarGraph(float [] values, float x, float y, float w, float h, float[] color){
		float barWidth = (Math.abs(w) - margin)/values.length;
		mesh =  new BYBMesh(BYBMesh.TRIANGLES);
		l = (w<0)?x+w:x + margin;
		r = (w<0)?x:x+w;
		b = (h<0)?y:y+h - margin;
		t = (h<0)?y+h:y;
		for(int i = 0; i < values.length; i++){
			mesh.addRectangle(l+i*barWidth, b, barWidth, -(b-t) * values[i], color);
		}
		this.x = x; this.y = y; this.w = w; this.h = h;
		
	}
	public void setAxisColor(float[] c){
		if(c.length == 4){
			axisColor = c;
		}
	}
	public void setVerticalAxis(float min, float max, int numDivs){
		if(axisMesh == null){
			axisMesh  = new BYBMesh(BYBMesh.LINES);
		}
		minV = min; maxV = max; numDivsV = numDivs;

		float inc = (Math.abs(h) - margin)/numDivs;
		axisMesh.addLine(l, b, l, t, axisColor);
		for(int i = 0; i < numDivs+1; i++){
			axisMesh.addLine(l, t+ inc*i, l-markerLength,t+ inc*i, axisColor);
		}
		bAxisVSet = true;
	}
	
	public void makeBox(float [] color){
		if(boxMesh == null){
			boxMesh = new BYBMesh(BYBMesh.LINES);
		}
		boxMesh.addLine(x, y, x+w, y, color);
		boxMesh.addLine(x+w, y, x+w, y+h, color);
		boxMesh.addLine(x, y+h, x+w, y+h, color);
		boxMesh.addLine(x, y, x, y+h, color);
	}
	
	public void setHorizontalAxis(float min, float max, int numDivs){
		if(axisMesh == null){
			axisMesh  = new BYBMesh(BYBMesh.LINES);
		}
		float inc = (Math.abs(w) - margin)/numDivs;
		axisMesh.addLine(l, b, r, b, axisColor);
		for(int i = 0; i < numDivs+1; i++){
			axisMesh.addLine(l+ inc*i, b, l+ inc*i,b+markerLength, axisColor);
		}
		minH = min; maxH = max; numDivsH = numDivs;
		bAxisHSet = true;
	}
	public void draw(GL10 gl){
		mesh.draw(gl);
		if(boxMesh!= null){
			boxMesh.draw(gl);
		}
		if(axisMesh != null){
			if(axisMesh.getNumVertices() > 0){
				axisMesh.draw(gl);
			}
		}
	}
	
	
}
