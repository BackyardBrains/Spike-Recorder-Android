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


import com.backyardbrains.BackyardBrainsMain;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.SurfaceHolder;

public class ContinuousGLSurfaceView extends GLSurfaceView  {

	@SuppressWarnings("unused")
	private static final String TAG = ContinuousGLSurfaceView.class.getCanonicalName();
//*
	public ContinuousGLSurfaceView(Context context, BYBBaseRenderer renderer) {
		super(context);
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
	//*/
}
