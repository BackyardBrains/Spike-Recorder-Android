package com.backyardbrains.drawing.gl;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import com.backyardbrains.drawing.Colors;
import com.backyardbrains.utils.ViewUtils;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class GlLabelWithCircle extends GlLabel {

    private final static float CIRCLE_RADIUS_DP = 5f;
    private final static float CIRCLE_RIGHT_MARGIN = 10f;

    private final GlCircle glCircle;

    private final float circleRadius;
    private final float circleX;

    public GlLabelWithCircle(@NonNull Context context, @NonNull GL10 gl) {
        super(context, gl);

        final Resources resources = context.getResources();

        glCircle = new GlCircle();

        circleRadius = ViewUtils.dpToPx(resources, CIRCLE_RADIUS_DP);
        circleX = circleRadius + ViewUtils.dpToPx(resources, CIRCLE_RIGHT_MARGIN);
    }

    @Override public void draw(@NonNull GL10 gl, float width, float height, @NonNull String text, float[] textColor,
        float[] backgroundColor) {
        draw(gl, width, height, text, textColor, backgroundColor, Colors.BLACK);
    }

    public void draw(@NonNull GL10 gl, float width, float height, @NonNull String text, float[] textColor,
        float[] backgroundColor, float[] circleColor) {
        super.draw(gl, width, height, text, textColor, backgroundColor);

        // draw circle
        gl.glPushMatrix();
        gl.glTranslatef(circleX, height * .5f, 0f);
        glCircle.draw(gl, circleRadius, circleColor);
        gl.glPopMatrix();
    }
}
