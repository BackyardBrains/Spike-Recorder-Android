package com.backyardbrains;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class WaveView {
	// Our vertices.
	private float vertices[] = {
		      -1.0f,  0.0f,  // 0, -
		      -0.5f,  0.0f,  // 1, /
		       0.0f,  1.0f,  // 2, |
		       0.25f, -0.5f,  // 3, /
		       0.75f, 0.0f,  // 4, _
		       1.0f, 0.0f,  // 5, _
		};

	//// The order we like to connect them.
	//private short[] indices = { 0, 1, 2, 3, 4, 5 };

	// Our vertex buffer.
	private FloatBuffer vertexBuffer;

	//// Our index buffer.
	//private ShortBuffer indexBuffer;

	public WaveView() {
		// a float is 4 bytes, therefore we multiply the number if
		// vertices with 4.
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		vertexBuffer = vbb.asFloatBuffer();
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);

		//	// short is 2 bytes, therefore we multiply the number if
		//	// vertices with 2.
		//
		//	ByteBuffer ibb = ByteBuffer.allocateDirect(indices.length * 2);
		//	ibb.order(ByteOrder.nativeOrder());
		//	indexBuffer = ibb.asShortBuffer();
		//	indexBuffer.put(indices);
		//	indexBuffer.position(0);
	}

	/**
	 * This function draws the spike data to the screen.
	 * @param gl
	 */
	public void draw(GL10 gl) {
		// Counter-clockwise winding.
		//gl.glFrontFace(GL10.GL_CCW); // OpenGL docs
		// Enable face culling.
		//gl.glEnable(GL10.GL_CULL_FACE); // OpenGL docs
		// What faces to remove with the face culling.
		//gl.glCullFace(GL10.GL_BACK); // OpenGL docs

		// Enabled the vertices buffer for writing and to be used during
		// rendering.
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);// OpenGL docs.
		// Specifies the location and data format of an array of vertex
		// coordinates to use when rendering.
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, // OpenGL docs
                                vertexBuffer );
	
		gl.glLineWidth(1.0f);
		// Let's color the line
		
		gl.glColor4f(0.0f, 1.0f, 0.0f, 1.0f);

		gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, vertices.length/2);
		//gl.glDrawElements(GL10.GL_LINE_STRIP, indices.length,// OpenGL docs
		//			  GL10.GL_UNSIGNED_SHORT, indexBuffer);

		// Disable the vertices buffer.
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY); // OpenGL docs
		// Disable face culling.
		//gl.glDisable(GL10.GL_CULL_FACE); // OpenGL docs
	}

}