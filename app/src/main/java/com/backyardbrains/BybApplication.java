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
import com.backyardbrains.utils.RecordingUtils;
import org.greenrobot.eventbus.EventBus;

public class BybApplication extends Application {

    @Override public void onCreate() {
        super.onCreate();

        // initialize event bus
        EventBus.builder()
            .logNoSubscriberMessages(false)
            .sendNoSubscriberEvent(false)
            .throwSubscriberException(BuildConfig.DEBUG)
            .installDefaultEventBus();
        RecordingUtils.setMainDirectory(this);
    }

    public boolean isTouchSupported() {
        return getPackageManager().hasSystemFeature("android.hardware.touchscreen");
    }
}
