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

package com.backyardbrains.audio;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * A simple interface to attach to services which allows a callback into
 * android's built-in classes from an unrelated thread
 *
 * @author Nathan Dotz <nate@backyardbrains.com>
 */
public interface ReceivesAudio {

    /**
     * Called by mic thread to pass audio {@link ByteBuffer} into a service. The service should then do as it sees fit
     * with the data.
     */
    void receiveAudio(ByteBuffer audioInfo);

    /**
     * Called by playback thread to pass audio {@link ByteBuffer} into a service along with the positions of the last
     * read byte.
     */
    void receiveAudio(ByteBuffer audioInfo, long lastBytePosition);

    void receiveAudio(ShortBuffer audioInfo);
}
