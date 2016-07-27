/*
 * Backyard Brains Android App
 * Copyright (C) 2011 Backyard Brains
 * by Nathan Dotz <nate (at) backyardbrains.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.backyardbrains.drawing;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;

import com.backyardbrains.BackyardBrainsMain;
import com.backyardbrains.audio.AudioService;

public class WaveformRenderer extends  BYBBaseRenderer {

	private static final String TAG = "WaveformRenderer";

	//----------------------------------------------------------------------------------------
	public WaveformRenderer(Context context){//, AudioService audioService) {
		super(context);//, audioService);
	}
//	// ----------------------------------------------------------------------------------------
//	@Override
//	public void setGlWindowHorizontalSize(int newX) {
//	//	//Log.d(TAG, "SetGLHorizontalSize "+getGlWindowHorizontalSize() + " newX: " + newX);	
//		super.setGlWindowHorizontalSize(newX);
//	}
//	@Override
//	// ----------------------------------------------------------------------------------------
//	public void setGlWindowVerticalSize(int newY) {
//		super.setGlWindowVerticalSize(newY);
//		////Log.d(TAG, "SetGLVerticalSize "+getGlWindowVerticalSize() + " newY: " + newY);
//	}

	//----------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------
	@Override
	protected void postDrawingHandler(GL10 gl) {}
	//----------------------------------------------------------------------------------------
	@Override
	protected void drawingHandler(GL10 gl) {
		setGlWindow(gl, getGlWindowHorizontalSize(), mBufferToDraws.length);
		FloatBuffer mVertexBuffer = getWaveformBuffer(mBufferToDraws);

		//firstBufferDrawnCheck();
		autoScaleCheck();

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glLineWidth(1f);
		gl.glColor4f(0f, 1f, 0f, 1f);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mVertexBuffer);
		gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertexBuffer.limit() / 2);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		
		

		
	}
}
