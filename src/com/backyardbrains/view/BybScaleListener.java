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

import android.support.annotation.Nullable;
import android.view.ScaleGestureDetector;
import com.backyardbrains.drawing.BYBBaseRenderer;

import static com.backyardbrains.utls.LogUtils.LOGE;
import static com.backyardbrains.utls.LogUtils.makeLogTag;

public class BybScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

    private static final String TAG = makeLogTag(BybScaleListener.class);

    private final BYBBaseRenderer renderer;

    private int sizeAtBeginningX = -1;
    private int sizeAtBeginningY = -1;
    private float scaleFactorX = 1.f;
    private float scaleFactorY = 1.f;
    private boolean horizontalScaling;
    private boolean scalingAxisDetermined;

    public BybScaleListener(@Nullable BYBBaseRenderer r) {
        super();

        this.renderer = r;
    }

    @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (renderer == null) return false;

        sizeAtBeginningX = renderer.getGlWindowHorizontalSize();
        sizeAtBeginningY = renderer.getGlWindowVerticalSize();
        scaleFactorX = 1.f;
        scaleFactorY = 1.f;
        scalingAxisDetermined = false;

        return true;
    }

    @Override public boolean onScale(ScaleGestureDetector detector) {
        if (renderer == null) return false;

        try {
            // determine scale factors for both axis
            scaleFactorX *= (1 + 2.5 * (1 - detector.getCurrentSpanX() / detector.getPreviousSpanX()));
            scaleFactorY *= (1 + 4 * (1 - detector.getCurrentSpanY() / detector.getPreviousSpanY()));

            final float xDiff = Math.abs(detector.getPreviousSpanX() - detector.getCurrentSpanX());
            final float yDiff = Math.abs(detector.getPreviousSpanY() - detector.getCurrentSpanY());
            // checks if this is the first scale cycle
            if (xDiff == 0 && yDiff == 0) return false;

            // determine scaling axis
            if (!scalingAxisDetermined) {
                horizontalScaling = xDiff > yDiff;
                scalingAxisDetermined = true;
            }

            // scale
            if (horizontalScaling) {
                renderer.setGlWindowHorizontalSize((int) (sizeAtBeginningX * scaleFactorX));
            } else {
                renderer.setGlWindowVerticalSize((int) (sizeAtBeginningY * scaleFactorY));
            }

            // TODO: 3/8/2017 Not sure what this line does, it's used in playback so need to check it there
            renderer.setScaleFocusX(detector.getFocusX());

            return true;
        } catch (IllegalStateException e) {
            LOGE(TAG, "Got invalid values back from Scale listener!");

            return false;
        } catch (NullPointerException e) {
            LOGE(TAG, "NPE while monitoring scale.");

            return false;
        }
    }
}
