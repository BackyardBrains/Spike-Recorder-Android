package com.backyardbrains.drawing.gl;

import android.support.annotation.NonNull;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlRectangleMask extends GlRectangle {

    @Override public void draw(@NonNull GL10 gl, float w, float h, float[] color) {
        gl.glColorMask(false, false, false, false);
        gl.glDepthMask(false);
        gl.glStencilFunc(GL10.GL_ALWAYS, 1, 1);
        gl.glStencilOp(GL10.GL_REPLACE, GL10.GL_REPLACE, GL10.GL_REPLACE);

        super.draw(gl, w, h, color);

        gl.glColorMask(true, true, true, true);
        gl.glDepthMask(true);
        gl.glStencilFunc(GL10.GL_EQUAL, 1, 1);
        gl.glStencilOp(GL10.GL_KEEP, GL10.GL_KEEP, GL10.GL_KEEP);
    }
}
