/*
 * Copyright (c) 2016, Wayne Tam
 * All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.angrygoat.buffer;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Wayne on 5/28/2015.
 * AbstractCircularBuffer
 */
@SuppressWarnings("unused") public abstract class CircularBuffer {
    public interface OnChangeListener {
        void onChanged(CircularBuffer buffer);
    }

    private static final int DEFAULT_BUFFER_SIZE = 3530752;

    protected int _bufStart = 0;
    protected int _bufEnd = 0;
    protected int _viewPtr = 0;
    protected volatile int _currOffset = 0;
    protected volatile int _bufferSize = 0;
    protected ConcurrentLinkedQueue<BufMark> _marks = new ConcurrentLinkedQueue<>();
    protected volatile boolean _wasMarked = false;

    // If blocking is true when reading, the minimum size that the buffer can be for the read to not block.
    // setting to -1 disables read blocking.
    protected int _minSize = -1;

    protected ReentrantLock _lock = new ReentrantLock(true);
    protected Condition _readCondition = _lock.newCondition();
    protected Condition _writeCondition = _lock.newCondition();
    protected OnChangeListener _listener = null;
    protected ExecutorService _threadPool = null;

    protected class BufMark {
        public int index;
        public boolean flag;

        public BufMark(int idx, boolean flg) {
            index = idx;
            flag = flg;
        }
    }

    public CircularBuffer() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public CircularBuffer(int capacity) {
        setCapacityInternal(capacity);
    }

    public void setOnChangeListener(OnChangeListener listener) {
        if (listener != null) {
            if (_threadPool == null || _threadPool.isShutdown()) _threadPool = Executors.newCachedThreadPool();
        } else {
            if (_threadPool != null) _threadPool.shutdownNow();
            _threadPool = null;
        }
        _listener = listener;
    }

    protected Runnable _notifyListener = new Runnable() {
        public void run() {
            if (_listener != null) _listener.onChanged(CircularBuffer.this);
        }
    };

    public ReentrantLock getLock() {
        return _lock;
    }

    public void setLock(ReentrantLock lock) {
        _lock = lock;
        if (_lock == null) _lock = new ReentrantLock(true);
        _readCondition = _lock.newCondition();
        _writeCondition = _lock.newCondition();
    }

    public void setCapacity(int capacity) {
        _lock.lock();
        try {
            clear();
            setCapacityInternal(capacity);
        } finally {
            _lock.unlock();
        }
    }

    public int getCapacity() {
        _lock.lock();
        try {
            return getCapacityInternal();
        } finally {
            _lock.unlock();
        }
    }

    abstract protected void setCapacityInternal(int capacity);

    abstract public int getCapacityInternal();

    public int size() {
        _lock.lock();
        try {
            return _bufferSize;
        } finally {
            _lock.unlock();
        }
    }

    public int peekSize() {
        _lock.lock();
        try {
            return _bufferSize - _currOffset;
        } finally {
            _lock.unlock();
        }
    }

    public int freeSpace() {
        _lock.lock();
        try {
            return getCapacityInternal() - _bufferSize;
        } finally {
            _lock.unlock();
        }
    }

    /**
     * Sets the minimum amount of data below which the read methods will block.
     * Setting this to -1 will disable blocking.
     *
     * @param size minimum amount of data before the blocking read will not block.
     */
    public void setMinSize(int size) {
        _lock.lock();
        try {
            _minSize = size;
        } finally {
            _readCondition.signalAll();
            _lock.unlock();
        }
    }

    public int getMinSize() {
        return _minSize;
    }

    public void clear() {
        _lock.lock();
        try {
            _viewPtr = _bufEnd = _bufStart = 0;
            _currOffset = _bufferSize = 0;
            _wasMarked = false;
            _marks.clear();
        } finally {
            _writeCondition.signalAll();
            _lock.unlock();
            if (_threadPool != null) _threadPool.submit(_notifyListener);
        }
    }

    /**
     * Set a mark at the current end of the buffer.
     * Read methods will only read to the mark even if there is more data
     * Once a mark has been reached it will be automatically removed.
     */
    public void mark() {
        _lock.lock();
        try {
            BufMark m = _marks.peek();
            if (m != null && m.index == _bufEnd) {
                if (_bufferSize == getCapacityInternal() && !m.flag) _marks.add(new BufMark(_bufEnd, true));
            } else {
                _marks.add(new BufMark(_bufEnd, _bufferSize == getCapacityInternal()));
            }
        } finally {
            _lock.unlock();
        }
    }

    /**
     * Remove the latest mark.
     */
    public void unmark() {
        _lock.lock();
        try {
            _marks.poll();
        } finally {
            _lock.unlock();
        }
    }

    public boolean isMarked() {
        _lock.lock();
        try {
            return !_marks.isEmpty();
        } finally {
            _lock.unlock();
        }
    }

    public boolean wasMarked() {
        return wasMarked(false);
    }

    /**
     * Check if the latest read had reached a mark.
     *
     * @param clear clear the "was marked" status.
     */
    public boolean wasMarked(boolean clear) {
        _lock.lock();
        try {
            boolean wasMarked = _wasMarked;
            if (clear) _wasMarked = false;
            return wasMarked;
        } finally {
            _lock.unlock();
        }
    }

    /**
     * Check how much data can be read before a mark is reached
     */
    public int getMarkedSize() {
        _lock.lock();
        try {
            return calcMarkSize(_marks.peek());
        } finally {
            _lock.unlock();
        }
    }

    protected int calcMarkSize(BufMark m) {
        if (m != null) {
            if (m.index < _bufStart) {
                return (getCapacityInternal() - _bufStart) + m.index;
            } else if (m.index == _bufStart) {
                if (m.flag) return _bufferSize;
            } else {
                return m.index - _bufStart;
            }
        }
        return 0;
    }

    /**
     * Remove data from the head of the buffer
     *
     * @param length amount of data to remove.
     */
    public int flush(int length) {
        int len = length;
        _lock.lock();
        try {
            if (_bufferSize == 0) return 0;

            if (length > 0) {
                if (len > _bufferSize) len = _bufferSize;
                _bufStart = (_bufStart + len) % getCapacityInternal();
                if (_bufStart == _bufEnd) {
                    _viewPtr = _bufStart;
                    _currOffset = 0;
                } else if (_viewPtr == _bufEnd) {
                    if (_bufStart < _viewPtr) {
                        _currOffset = _viewPtr - _bufStart;
                    } else {
                        _currOffset = _viewPtr + (getCapacityInternal() - _bufStart);
                    }
                } else if ((_bufStart > _bufEnd && _viewPtr > _bufEnd) || (_bufStart < _bufEnd && _viewPtr < _bufEnd)) {
                    if (_bufStart < _viewPtr) {
                        _currOffset = _viewPtr - _bufStart;
                    } else {
                        _viewPtr = _bufStart;
                        _currOffset = 0;
                    }
                } else if (_bufStart < _viewPtr) {
                    _viewPtr = _bufStart;
                    _currOffset = 0;
                } else {
                    _currOffset = _viewPtr + (getCapacityInternal() - _bufStart);
                }
                _bufferSize -= len;
                BufMark m = _marks.peek();
                while (m != null && ((_bufEnd > _bufStart && (m.index < _bufStart || m.index > _bufEnd)) || (
                    m.index < _bufStart && m.index > _bufEnd))) {
                    _marks.poll();
                    m = _marks.peek();
                }
            } else if (length < 0) {
                _bufStart = _viewPtr;
                _bufferSize -= _currOffset;
                _currOffset = 0;
            }
            return len;
        } finally {
            _writeCondition.signalAll();
            _lock.unlock();
            if (_threadPool != null) _threadPool.submit(_notifyListener);
        }
    }

    /**
     * Resets the peek pointer to the head of the buffer.
     */
    public void rewind() {
        _lock.lock();
        try {
            _viewPtr = _bufStart;
            _currOffset = 0;
        } finally {
            _lock.unlock();
        }
    }

    /**
     * Sets the position at which the peek command will read from
     *
     * @param position the offset from the head of the buffer.
     */
    public void setPeekPosition(int position) {
        _lock.lock();
        try {
            if (position < _bufferSize) {
                _viewPtr = (_bufStart + position) % getCapacityInternal();
                _currOffset = position;
            } else {
                _viewPtr = _bufEnd;
                _currOffset = _bufferSize;
            }
        } finally {
            _lock.unlock();
        }
    }

    public int getPeekPosition() {
        _lock.lock();
        try {
            return _currOffset;
        } finally {
            _lock.unlock();
        }
    }
}
