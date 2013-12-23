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

import com.backyardbrains.drawing.OscilloscopeRenderer;
import com.backyardbrains.view.TwoDimensionScaleGestureDetector.Simple2DOnScaleGestureListener;

public class ScaleListener extends Simple2DOnScaleGestureListener {

	private static final String TAG = ScaleListener.class.getCanonicalName();

	int xSizeAtBeginning = -1;
	int ySizeAtBeginning = -1;
	private OscilloscopeRenderer renderer;

	public ScaleListener(OscilloscopeRenderer r) {
		super();
		this.renderer = r;
	}

	@Override
	public boolean onScaleBegin(TwoDimensionScaleGestureDetector detector) {
		xSizeAtBeginning = renderer.getGlWindowHorizontalSize();
		ySizeAtBeginning = renderer.getGlWindowVerticalSize();

		return super.onScaleBegin(detector);
	}

	@Override
	public boolean onScale(TwoDimensionScaleGestureDetector detector) {

		try {
			final Pair<Float, Float> scaleModifier = detector.getScaleFactor();
			int newXsize = (int) (xSizeAtBeginning / scaleModifier.first);
			renderer.setGlWindowHorizontalSize(newXsize);

			int newYsize = (int) (ySizeAtBeginning * scaleModifier.second);

			renderer.setGlWindowVerticalSize(newYsize);
		} catch (IllegalStateException e) {
			Log.e(TAG, "Got invalid values back from Scale listener!");
		} catch (NullPointerException e) {
			Log.e(TAG, "NPE while monitoring scale.");
		}
		return super.onScale(detector);
	}

	@Override
	public void onScaleEnd(TwoDimensionScaleGestureDetector detector) {
		super.onScaleEnd(detector);
	}
}
