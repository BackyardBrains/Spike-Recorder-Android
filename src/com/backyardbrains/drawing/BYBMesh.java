package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;

import javax.microedition.khronos.opengles.GL10;

import com.backyardbrains.BYBUtils;

import android.util.Log;

public class BYBMesh {

	public static final int			LINES		= 0;
	public static final int			TRIANGLES	= 1;
	public static final int			TRIANGLE_STRIP	= 2;
	public static final int			POINTS	= 3;
	
	protected ArrayList<float[]>	vertices;
	protected ArrayList<float[]>	colors;
	protected ArrayList<float[]>	texCoords;

	protected int					mode		= TRIANGLES;

	public BYBMesh(int mode) {
		vertices = new ArrayList<float[]>();
		colors = new ArrayList<float[]>();
		texCoords = new ArrayList<float[]>();
		this.mode = mode;
	}
	
	public void clear(){
		vertices.clear();
		colors.clear();
		texCoords.clear();
	}
	public void addVertex(float[] v) {
		if (v.length == 2) vertices.add(v);
	}

	public void addVertex(float x, float y) {
		float[] v = new float[]{ x, y };
		vertices.add(v);
	}

	public void addColor(float[] c) {
		if (c.length == 4) {
			colors.add(c);
		}
		if (c.length == 3) {
			float[] nc = new float[4];
			for (int i = 0; i < 3; i++) {
				nc[i] = c[i];
			}
			nc[3] = 1.0f;
			colors.add(nc);
		}
	}

	public void addColor(float c) {
		float[] v = { c, c, c, 1.0f };
		colors.add(v);
	}

	public void addTexCoord(float u, float v) {
		float[] t = { u, v };
		texCoords.add(t);
	}

	public void addTexCoord(float[] t) {
		if (t.length == 2) {
			texCoords.add(t);
		}
	}

	public int getNumVertices() {
		return vertices.size();
	}

	public int getNumColors() {
		return colors.size();
	}

	public int getNumTexCoords() {
		return texCoords.size();
	}

	public void addRectangle(float x, float y, float w, float h, float[] color) {
		addRectangle(x, y, w, h, color, false);
	}
	public void addRectangle(float x, float y, float w, float h, float[] color, boolean bAutoAddTexCoords) {
		addVertex(x, y);
		addVertex(x + w, y);
		addVertex(x, y + h);
		if(mode == TRIANGLES){
			addVertex(x + w, y);
			addVertex(x, y + h);
		}
		addVertex(x + w, y + h);
		
		int n = (mode == TRIANGLES)?6:4;
		for (int i = 0; i < n; i++) {
			addColor(color);
		}
		if(bAutoAddTexCoords){
			addTexCoord(0.0f, 0.0f);
			addTexCoord(1.0f, 0.0f);
			addTexCoord(0.0f, 1.0f);
			if(mode == TRIANGLES){
				addTexCoord(1.0f, 0.0f);
				addTexCoord(0.0f, 1.0f);
			}
			addTexCoord(1.0f, 1.0f);
		}
	}

	public void addLine(float p0x, float p0y, float p1x, float p1y, float[] color) {
		addVertex(p0x, p0y);
		addVertex(p1x, p1y);
		addColor(color);
		addColor(color);
	}

	public void draw(GL10 gl) {
		boolean bDrawVerts = (vertices.size() > 0);
		boolean bDrawColors = colors.size() > 0;
		boolean bDrawTexCoords = texCoords.size() > 0;

		if (bDrawVerts) {
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

			if (bDrawColors) gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
			if (bDrawTexCoords) gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			// gl.glLineWidth(1f);
			if (bDrawVerts) gl.glVertexPointer(2, GL10.GL_FLOAT, 0, BYBUtils.floatArrayListToFloatBuffer(vertices));
			if (bDrawColors) gl.glColorPointer(4, GL10.GL_FLOAT, 0, BYBUtils.floatArrayListToFloatBuffer(colors));
			if (bDrawTexCoords) gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, BYBUtils.floatArrayListToFloatBuffer(texCoords));
			switch (mode) {
			case TRIANGLES:
				gl.glDrawArrays(GL10.GL_TRIANGLES, 0, vertices.size());
				break;
			case LINES:
				gl.glDrawArrays(GL10.GL_LINES, 0, vertices.size());
				break;
			case TRIANGLE_STRIP:
				gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.size());
				break;
			case POINTS:
				gl.glDrawArrays(GL10.GL_POINTS, 0, vertices.size());
				break;
			}
			if (bDrawColors) gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
			if (bDrawTexCoords) gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		}
	}
}
