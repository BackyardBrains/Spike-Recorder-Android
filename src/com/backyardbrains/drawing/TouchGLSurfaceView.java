package com.backyardbrains.drawing;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.SurfaceHolder;


public class TouchGLSurfaceView extends GLSurfaceView {
    private BYBAnalysisBaseRenderer renderer;
    public TouchGLSurfaceView(Context context, BYBAnalysisBaseRenderer renderer) {
        super(context);
        this.renderer = renderer;
        setRenderer(renderer);
    }
    //*
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        setKeepScreenOn(true);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        setKeepScreenOn(false);
        super.surfaceDestroyed(holder);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
//		Log.d(TAG, "onTouchEvent " + event.toString());
        if(renderer != null){
            renderer.onTouchEvent(event);
        }
        return true;
    }
}
