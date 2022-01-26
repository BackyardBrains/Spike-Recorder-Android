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

package com.backyardbrains.drawing;

import androidx.annotation.Nullable;
import android.view.ScaleGestureDetector;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import static com.backyardbrains.utils.LogUtils.LOGE;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

    private static final String TAG = makeLogTag(ScaleListener.class);

    private static final float SCALE_FACTOR_MULTIPLIER = 1.1f;

    private final BaseWaveformRenderer renderer;

    private boolean horizontalScaling;
    private boolean scalingAxisDetermined;

    ScaleListener(@Nullable BaseWaveformRenderer r) {
        super();

        this.renderer = r;
    }

    @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (renderer == null) return false;

        scalingAxisDetermined = false;

        return true;
    }

    @Override public boolean onScale(ScaleGestureDetector detector) {
        if (renderer == null) return false;

        try {
            // determine scale factors for both axis
            float scaleFactorX =
                (1 + SCALE_FACTOR_MULTIPLIER * (1 - detector.getCurrentSpanX() / detector.getPreviousSpanX()));

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
                renderer.onHorizontalZoom(scaleFactorX, detector.getFocusX(), detector.getFocusY());
            } else {
                renderer.onVerticalZoom((float) Math.pow(detector.getScaleFactor(), 2), detector.getFocusX(),
                    detector.getFocusY());
            }

            return true;
        } catch (IllegalStateException e) {
            LOGE(TAG, "Got invalid values back from Scale listener!");
            FirebaseCrashlytics.getInstance().recordException(e);

            return false;
        } catch (NullPointerException e) {
            LOGE(TAG, "NPE while monitoring scale.");
            FirebaseCrashlytics.getInstance().recordException(e);

            return false;
        }
    }
}
