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

package com.backyardbrains.view;

import android.util.Log;
import android.util.Pair;
import com.backyardbrains.drawing.BYBBaseRenderer;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector.Simple2DOnScaleGestureListener;
import com.crashlytics.android.Crashlytics;

public class ScaleListener extends Simple2DOnScaleGestureListener {

    private static final String TAG = "BYBScaleListener";

    int xSizeAtBeginning = -1;
    int ySizeAtBeginning = -1;
    private BYBBaseRenderer renderer = null;

    private boolean bMutuallyExclusive = true;
    private int frame = 0;
    private static final int NUM_FRAMES_FOR_EXCL_DET = 0;
    private static final int EXCLUSIVE_AXIS_HORIZONTAL = 0;
    private static final int EXCLUSIVE_AXIS_VERTICAL = 1;
    private static final int EXCLUSIVE_AXIS_NONE = -1;
    private int exclusiveAxis = -1;

    public ScaleListener() {
        super();
    }

    public ScaleListener(BYBBaseRenderer r) {
        super();
        this.renderer = r;
    }

    public void setRenderer(BYBBaseRenderer r) {
        renderer = r;
    }

    @Override public boolean onScaleBegin(TwoDimensionScaleGestureDetector detector) {
        if (renderer != null) {
            xSizeAtBeginning = renderer.getGlWindowWidth();
            ySizeAtBeginning = renderer.getGlWindowHeight();
            ////Log.d(TAG, "onScaleBegin");
            //			return true;
            frame = 0;
            exclusiveAxis = EXCLUSIVE_AXIS_NONE;
        }
        return super.onScaleBegin(detector);
    }

    @Override public boolean onScale(TwoDimensionScaleGestureDetector detector) {

        if (renderer != null) {
            try {

                if (frame == NUM_FRAMES_FOR_EXCL_DET && bMutuallyExclusive) {
                    final Pair<Float, Float> span = detector.getCurrentSpan();
                    if (span.first >= span.second) {
                        exclusiveAxis = EXCLUSIVE_AXIS_HORIZONTAL;
                    } else {
                        exclusiveAxis = EXCLUSIVE_AXIS_VERTICAL;
                    }
                } else {
                    final Pair<Float, Float> scaleModifier = detector.getScaleFactor();
                    if (exclusiveAxis == EXCLUSIVE_AXIS_NONE || exclusiveAxis == EXCLUSIVE_AXIS_HORIZONTAL) {
                        int newXsize = (int) (xSizeAtBeginning / scaleModifier.first);
                        renderer.setGlWindowWidth(newXsize);
                    }
                    if (exclusiveAxis == EXCLUSIVE_AXIS_NONE || exclusiveAxis == EXCLUSIVE_AXIS_VERTICAL) {
                        int newYsize = (int) (ySizeAtBeginning * scaleModifier.second);
                        renderer.setGlWindowHeight(newYsize);
                    }
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Got invalid values back from Scale listener!");
                Crashlytics.logException(e);
                //				return false;
            } catch (NullPointerException e) {
                Log.e(TAG, "NPE while monitoring scale.");
                Crashlytics.logException(e);
                //				return false;
            }
            //			return true;
        }
        frame++;
        //		return false;
        return super.onScale(detector);
    }

    @Override public void onScaleEnd(TwoDimensionScaleGestureDetector detector) {

        super.onScaleEnd(detector);
    }
}
