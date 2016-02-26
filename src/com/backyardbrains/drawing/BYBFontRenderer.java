package com.backyardbrains.drawing;

import com.backyardbrains.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.opengl.GLUtils;

import javax.microedition.khronos.opengles.GL10;


public class BYBFontRenderer {

	int[] textures = new int [1];
	public BYBFontRenderer(){}
	public void drawText( GL10 gl, Context context){
		// Create an empty, mutable bitmap
		/*
		Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444);
		// get a canvas to paint over the bitmap
		Canvas canvas = new Canvas(bitmap);
		//bitmap.
		

		// get a background image from resources
		// note the image format must match the bitmap format
		
		// Draw the text
		
		/*
		Paint textPaint = new Paint();
		textPaint.setTextSize(32);
		textPaint.setAntiAlias(true);
		textPaint.setARGB(0xff,0xff,0xff,0xff);
		// draw the text centered
		canvas.drawText("Hello World", 16,112, textPaint);
		//*/
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
	}
	
	
	
		public void loadGLTexture(GL10 gl, Context context) {
			// loading texture
			Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444);
			// get a canvas to paint over the bitmap
			Canvas canvas = new Canvas(bitmap);
			//bitmap.
			

			// get a background image from resources
			// note the image format must match the bitmap format
			
			// Draw the text
			
			Paint textPaint = new Paint();
			textPaint.setTextSize(32);
			textPaint.setAntiAlias(true);
			textPaint.setARGB(0xff,0xff,0xff,0xff);
			// draw the text centered
			canvas.drawText("Hello World", 16,112, textPaint);
			
//			Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.android);

			// generate one texture pointer
			gl.glGenTextures(1, textures, 0);
			// ...and bind it to our array
			gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
			
			// create nearest filtered texture
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

			//Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
//			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
//			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
			
			// Use Android GLUtils to specify a two-dimensional texture image from our bitmap 
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
			
			// Clean up
			bitmap.recycle();
		}
	
	
}
