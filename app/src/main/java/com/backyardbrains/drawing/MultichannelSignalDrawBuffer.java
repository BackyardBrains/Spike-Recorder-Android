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

import com.crashlytics.android.Crashlytics;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

public class MultichannelSignalDrawBuffer {

    private static final String TAG = makeLogTag(MultichannelSignalDrawBuffer.class);

    private final int frameCount;
    private final int channelCount;
    private final short[][] buffer;

    MultichannelSignalDrawBuffer(int channelCount, int frameCount) {
        this.frameCount = frameCount;
        this.channelCount = channelCount;

        buffer = new short[channelCount][];
        for (int i = 0; i < channelCount; i++) {
            buffer[i] = new short[frameCount];
        }
    }

    /**
     * Returns number of frames for buffer.
     */
    public int getFrameCount() {
        return frameCount;
    }

    /**
     * Returns channel count of the buffer
     */
    public int getChannelCount() {
        return channelCount;
    }

    /**
     * Returns buffer data for {@code channel} channel.
     */
    short[] getChannel(int channel) {
        return buffer[channel] != null ? buffer[channel] : new short[0];
    }

    /**
     * Returns buffer data.
     */
    short[][] getBuffer() {
        return buffer;
    }

    /**
     * Adds new {@code incoming} samples to the buffer's {@code channel} channel.
     */
    public void add(int channel, short[] incoming, int length) {
        if (buffer.length <= channel || buffer[channel] == null) return;

        short[] tmpBuffer = buffer[channel];
        try {
            if (length < tmpBuffer.length) {
                System.arraycopy(tmpBuffer, length, tmpBuffer, 0, tmpBuffer.length - length);
                System.arraycopy(incoming, 0, tmpBuffer, tmpBuffer.length - length, length);
            } else {
                System.arraycopy(incoming, length - tmpBuffer.length, tmpBuffer, 0, tmpBuffer.length);
            }
        } catch (Exception e) {
            LOGD(TAG, "Can't add incoming to buffer, it's larger then buffer - channel=" + channel + " src.length="
                + tmpBuffer.length + " srcPos=" + length + " dst.length=" + tmpBuffer.length + " dstPos=" + 0
                + " length=" + (tmpBuffer.length - length));
            Crashlytics.logException(e);
        }
    }
}
