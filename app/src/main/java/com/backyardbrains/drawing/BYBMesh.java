package com.backyardbrains.drawing;

import java.util.*;

import javax.microedition.khronos.opengles.GL10;

import com.backyardbrains.utils.BYBUtils;

public class BYBMesh {

	public static final int			LINES			= 0;
	public static final int			TRIANGLES		= 1;
	public static final int			TRIANGLE_STRIP	= 2;
	public static final int			POINTS			= 3;
	public static final int			LINE_STRIP		= 4;

	protected ArrayList<float[]>	vertices;
	protected ArrayList<float[]>	colors;
	protected ArrayList<float[]>	texCoords;

	protected int					mode			= TRIANGLES;

	public BYBMesh(int mode) {
		vertices = new ArrayList<float[]>();
		colors = new ArrayList<float[]>();
		texCoords = new ArrayList<float[]>();
		this.mode = mode;
	}

	public void clear() {
		vertices.clear();
		colors.clear();
		texCoords.clear();
	}

	public void addVertex(float[] v) {
		if (v.length == 2) vertices.add(v);
	}

	public void addVertex(float x, float y) {
		float[] v = new float[] { x, y };
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
		if (mode == LINES) {
			addLine(x, y, x + w, y, color);
			addLine(x + w, y, x + w, y + h, color);
			addLine(x, y + h, x + w, y + h, color);
			addLine(x, y, x, y + h, color);
		} else {
			addVertex(x, y);
			addVertex(x + w, y);
			addVertex(x, y + h);
			if (mode == TRIANGLES) {
				addVertex(x + w, y);
				addVertex(x, y + h);
			}
			addVertex(x + w, y + h);

			int n = (mode == TRIANGLES) ? 6 : 4;
			for (int i = 0; i < n; i++) {
				addColor(color);
			}
			if (bAutoAddTexCoords) {
				addTexCoord(0.0f, 0.0f);
				addTexCoord(1.0f, 0.0f);
				addTexCoord(0.0f, 1.0f);
				if (mode == TRIANGLES) {
					addTexCoord(1.0f, 0.0f);
					addTexCoord(0.0f, 1.0f);
				}
				addTexCoord(1.0f, 1.0f);
			}
		}
	}
	public void addQuadSmooth(float p0x, float p0y, float p1x, float p1y, float p2x, float p2y, float p3x, float p3y, float[] color){
		float [] c = new float [4];
		c[0] = color[0];
		c[1] = color[1];
		c[2] = color[2];
		c[0] = 0.0f;
		addQuad(p0x, p0y-1, p1x, p1y-1, p0x, p0y, p1x, p1y);
		addColor(c);
		addColor(c);
		if(mode == TRIANGLES){
			addColor(color);
			addColor(c);
		}
		addColor(color);
		addColor(color);
		addQuad(p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y, color);

		
		addQuad(p2x, p2y, p3x, p3y, p2x, p2y+1, p3x, p3y+1);
		addColor(color);
		addColor(color);
		if(mode == TRIANGLES){
			addColor(c);
			addColor(color);
		}
		addColor(c);
		addColor(c);

		
		
	}
	public void addQuad(float p0x, float p0y, float p1x, float p1y, float p2x, float p2y, float p3x, float p3y){
		addVertex(p0x, p0y);
		addVertex(p1x, p1y);
		if(mode == TRIANGLES){
			addVertex(p2x, p2y);
			addVertex(p1x, p1y);
		}
		addVertex(p2x, p2y);
		addVertex(p3x, p3y);
	}
	public void addQuad(float p0x, float p0y, float p1x, float p1y, float p2x, float p2y, float p3x, float p3y, float[] color){
		addQuad(p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y);
		int n = (mode == TRIANGLES)?6:4;
		for(int i =0; i < n; i++){
			addColor(color);
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
			case LINE_STRIP:
				gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, vertices.size());
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
