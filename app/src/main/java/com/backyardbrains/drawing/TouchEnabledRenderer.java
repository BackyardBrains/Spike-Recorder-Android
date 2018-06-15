package com.backyardbrains.drawing;

import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public interface TouchEnabledRenderer extends GLSurfaceView.Renderer {

    void onTouchEvent(MotionEvent event);
}
