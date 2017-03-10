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

package com.backyardbrains;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.backyardbrains.analysis.BYBAnalysisManager;
import com.backyardbrains.audio.AudioService;
import com.backyardbrains.audio.AudioService.AudioServiceBinder;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.BuildConfig;
import io.fabric.sdk.android.Fabric;

public class BackyardBrainsApplication extends Application {
    private boolean bAudioServiceRunning = false;
    protected AudioService mAudioService;
    protected BYBAnalysisManager analysisManager;

    public boolean isTouchSupported() {
        return getPackageManager().hasSystemFeature("android.hardware.touchscreen");
    }

    public boolean isAudioServiceRunning() {
        return bAudioServiceRunning;
    }

    public void startAudioService() {
        if (!this.bAudioServiceRunning) {
            startService(new Intent(this, AudioService.class));
            bAudioServiceRunning = true;
            bindAudioService(true);
        }
    }

    public void stopAudioService() {
        if (this.bAudioServiceRunning) {
            bindAudioService(false);
            stopService(new Intent(this, AudioService.class));
            bAudioServiceRunning = false;
        }
    }

    @Override public void onCreate() {
        super.onCreate();

        // Init Crashlytics only in production
        if (!BuildConfig.DEBUG) Fabric.with(this, new Crashlytics());

        analysisManager = new BYBAnalysisManager(getApplicationContext());
        //startAudioService();
    }

    /**
     * Make sure we stop the {@link AudioService} when we exit
     */
    @Override public void onTerminate() {
        super.onTerminate();
        analysisManager.close();
        stopAudioService();
    }

    // ----------------------------------------------------------------------------------------
    protected void bindAudioService(boolean on) {
        if (on) {
            Intent intent = new Intent(this, AudioService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } else {
            unbindService(mConnection);
        }
    }

    // ----------------------------------------------------------------------------------------
    protected ServiceConnection mConnection = new ServiceConnection() {

        private boolean mAudioServiceIsBound;

        // Sets a reference in this activity to the {@link AudioService}, which
        // allows for {@link ByteBuffer}s full of audio information to be passed
        // from the {@link AudioService} down into the local
        // {@link OscilloscopeGLSurfaceView}
        //
        // @see
        // android.content.ServiceConnection#onServiceConnected(android.content.ComponentName,
        // android.os.IBinder)
        @Override public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            AudioServiceBinder binder = (AudioServiceBinder) service;
            mAudioService = binder.getService();
            mAudioServiceIsBound = true;
        }

        @Override public void onServiceDisconnected(ComponentName arg0) {
            mAudioService = null;
            mAudioServiceIsBound = false;
        }
    };

    // ----------------------------------------------------------------------------------------
    public AudioService getmAudioService() {
        return mAudioService;
    }

    public BYBAnalysisManager getAnalysisManager() {
        return analysisManager;
    }
}
