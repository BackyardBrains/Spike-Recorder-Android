package com.backyardbrains.drawing.gl;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import com.android.texample.GLText;
import com.backyardbrains.utils.ViewUtils;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlLabel {

    private final static float FONT_SIZE_SP = 14f;

    private final GlRectangle glRectangle;
    private final GLText glText;

    public GlLabel(@NonNull Context context, @NonNull GL10 gl) {
        final Resources resources = context.getResources();

        glRectangle = new GlRectangle();
        glText = new GLText(gl, context.getAssets());
        glText.load("Roboto-Regular.ttf", (int) ViewUtils.spToPx(resources, FONT_SIZE_SP), 0, 0);
    }

    public void draw(@NonNull GL10 gl, float width, float height, @NonNull String text, @Size(4) float[] textColor,
        @Nullable @Size(4) float[] backgroundColor) {
        // draw label background
        if (backgroundColor != null) glRectangle.draw(gl, width, height, backgroundColor);

        // draw label text
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        glText.begin(textColor[0], textColor[1], textColor[2], textColor[3]);
        glText.drawC(text, width * .5f, height * .5f);
        glText.end();
        gl.glDisable(GL10.GL_BLEND);
        gl.glDisable(GL10.GL_TEXTURE_2D);
    }
}
