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

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.util.Arrays;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class FftDrawBuffer {

    private static final String TAG = makeLogTag(FftDrawBuffer.class);

    private final int windowCount;
    private final int windowSize;

    private float[][] buffer;

    FftDrawBuffer(int windowCount, int windowSize) {
        this.windowCount = windowCount;
        this.windowSize = windowSize;

        buffer = new float[windowCount][windowSize];
        for (int i = 0; i < windowCount; i++)
            Arrays.fill(buffer[i], -1);
    }

    /**
     * Returns buffer window size.
     */
    public int getWindowSize() {
        return windowSize;
    }

    /**
     * Returns buffer window count.
     */
    public int getWindowCount() {
        return windowCount;
    }

    /**
     * Returns window at {@code position} position.
     */
    public float[] getWindow(int position) {
        return position < windowCount ? buffer[position] : new float[0];
    }

    /**
     * Returns buffer data.
     */
    float[][] getBuffer() {
        return buffer;
    }

    /**
     * Adds new {@code incoming} samples to the buffer.
     */
    public void add(float[][] incoming, int length) {
        try {
            int counter = 0;
            final int len;
            if (length < windowCount) {
                len = windowCount - length;
                for (int i = length; i < windowCount; i++) {
                    System.arraycopy(buffer[i], 0, buffer[counter++], 0, windowSize);
                }
                counter = 0;
                for (int i = 0; i < length; i++) {
                    System.arraycopy(incoming[i], 0, buffer[len + counter++], 0, windowSize);
                }
            } else {
                for (int i = length - windowCount; i < length; i++) {
                    System.arraycopy(incoming[i], 0, buffer[counter++], 0, windowSize);
                }
            }
        } catch (Exception e) {
            LOGD(TAG, "Can't add incoming to buffer, it's larger then buffer - src.length=" + windowCount + " srcPos="
                + length + " dst.length=" + windowCount + " dstPos=" + 0 + " length=" + (windowCount - length));
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    /**
     * Clears the buffer.
     */
    public void clear() {
        for (int i = 0; i < windowCount; i++)
            Arrays.fill(buffer[i], -1);
    }
}
