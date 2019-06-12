package com.backyardbrains.drawing;

import android.opengl.GLSurfaceView;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public interface ZoomEnabledRenderer extends GLSurfaceView.Renderer {

    float UNKNOWN_FOCUS = -1f;

    void onHorizontalZoom(float zoomFactor, float zoomFocusX, float zoomFocusY);

    void onVerticalZoom(float zoomFactor, float zoomFocusX, float zoomFocusY);
}
