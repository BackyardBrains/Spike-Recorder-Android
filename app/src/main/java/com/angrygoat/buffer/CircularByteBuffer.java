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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings("UnusedDeclaration") public class CircularByteBuffer extends CircularBuffer {

    private byte[] _circBuffer;

    public CircularByteBuffer() {
        super();
    }

    public CircularByteBuffer(int capacity) {
        super(capacity);
    }

    @Override protected void setCapacityInternal(int capacity) {
        _circBuffer = new byte[capacity];
    }

    @Override public int getCapacityInternal() {
        return _circBuffer.length;
    }

    public InputStream getInputStream() {
        return new ReadStream();
    }

    public OutputStream getOutputStream() {
        return new WriteStream();
    }

    private class ReadStream extends InputStream {

        private byte[] singleByteBuf = new byte[1];
        private boolean isClosed = false;
        private boolean isEOS = false;

        @Override public int available() {
            return CircularByteBuffer.this.size();
        }

        @Override public int read() throws IOException {
            if (isClosed) throw new IOException("Stream was closed");
            if (isEOS) return -1;
            _lock.lock();
            try {
                if (_wasMarked) {
                    _wasMarked = false;
                    isEOS = true;
                    return -1;
                }
                int result = CircularByteBuffer.this.readFully(singleByteBuf, 0, 1);
                if (result < 1) return -1;
                return singleByteBuf[0] & 0x000000FF;
            } finally {
                _lock.unlock();
            }
        }

        @SuppressWarnings("NullableProblems") @Override public int read(byte[] buf, int offset, int len)
            throws IOException {
            if (isClosed) throw new IOException("Stream was closed");
            if (isEOS) return -1;
            _lock.lock();
            try {
                if (_wasMarked) {
                    _wasMarked = false;
                    isEOS = true;
                    return -1;
                }
                int result = CircularByteBuffer.this.read(buf, offset, len, true);
                if (result < 0) {
                    return -1;
                }
                return result;
            } finally {
                _lock.unlock();
            }
        }

        @Override public void close() throws IOException {
            isClosed = true;
            _lock.lock();
            try {
                _wasMarked = false;
            } finally {
                _lock.unlock();
            }
            super.close();
        }
    }

    private class WriteStream extends OutputStream {

        private byte[] buf = new byte[1];

        @SuppressWarnings("NullableProblems") @Override public void write(byte[] buf, int offset, int len)
            throws IOException {
            CircularByteBuffer.this.write(buf, offset, len, true);
        }

        @Override public void write(int data) throws IOException {
            buf[0] = (byte) (data & 0x000000ff);
            CircularByteBuffer.this.write(buf, 0, 1, true);
        }
    }

    public int write(byte[] data) {
        return write(data, 0, data.length, false);
    }

    public int write(byte[] data, int length) {
        return write(data, 0, length, false);
    }

    public int write(byte[] data, int offset, int length, boolean blocking) {
        int len = length;
        _lock.lock();
        try {
            if (length > 0) {
                int emptySize = _circBuffer.length - _bufferSize;
                while (blocking && emptySize < length) {
                    try {
                        _writeCondition.await();
                    } catch (InterruptedException e) {
                        return -1;
                    }
                    emptySize = _circBuffer.length - _bufferSize;
                }
                if (emptySize > 0) {
                    if (len > emptySize) len = emptySize;

                    int tmpIdx = _bufEnd + len;
                    int tmpLen;
                    if (tmpIdx > _circBuffer.length) {
                        tmpLen = _circBuffer.length - _bufEnd;
                        System.arraycopy(data, offset, _circBuffer, _bufEnd, tmpLen);
                        _bufEnd = (tmpIdx) % _circBuffer.length;
                        System.arraycopy(data, tmpLen + offset, _circBuffer, 0, _bufEnd);
                    } else {
                        System.arraycopy(data, offset, _circBuffer, _bufEnd, len);
                        _bufEnd = (tmpIdx) % _circBuffer.length;
                    }
                    _bufferSize += len;
                    return len;
                }
            }
            return 0;
        } finally {
            _readCondition.signalAll();
            _lock.unlock();
            if (_threadPool != null) _threadPool.submit(_notifyListener);
        }
    }

    /**
     * Read data from the buffer without removing it.
     *
     * @param data the output array
     * @param length amount of data to read
     * @return the amount of data read
     */
    public int peek(byte[] data, int length) {
        int len = length;
        _lock.lock();
        try {
            int remSize = _bufferSize - _currOffset;
            if (length > 0 && remSize > 0) {
                if (len > remSize) len = remSize;
                int tmpIdx = _viewPtr + len;
                int tmpLen;
                if (tmpIdx > _circBuffer.length) {
                    tmpLen = _circBuffer.length - _viewPtr;
                    System.arraycopy(_circBuffer, _viewPtr, data, 0, tmpLen);
                    _viewPtr = (tmpIdx) % _circBuffer.length;
                    System.arraycopy(_circBuffer, 0, data, tmpLen, _viewPtr);
                } else {
                    System.arraycopy(_circBuffer, _viewPtr, data, 0, len);
                    _viewPtr = (tmpIdx) % _circBuffer.length;
                }
                _currOffset += len;
                return len;
            }
            return 0;
        } finally {
            _lock.unlock();
        }
    }

    public int readFully(byte[] data, int offset, int length) {
        _lock.lock();
        try {
            if (length > 0) {
                int minSize = _minSize < 0 ? 0 : _minSize;
                while (_bufferSize - minSize < length) {
                    try {
                        _readCondition.await();
                    } catch (InterruptedException e) {
                        _wasMarked = false;
                        return -1;
                    }
                }
                return read(data, offset, length, false);
            }
            return 0;
        } finally {
            _writeCondition.signalAll();
            _lock.unlock();
        }
    }

    public int read(byte[] data, int length, boolean blocking) {
        return read(data, 0, length, blocking);
    }

    public int read(byte[] data, int offset, int length, boolean blocking) {
        int len = length;
        _lock.lock();
        _wasMarked = false;
        try {
            if (length > 0) {
                while (blocking && _minSize > -1 && _bufferSize <= _minSize) {
                    try {
                        _readCondition.await();
                    } catch (InterruptedException e) {
                        return -1;
                    }
                }
                int minSize = _minSize < 0 ? 0 : _minSize;
                if (_bufferSize > 0) {
                    if (len > _bufferSize - minSize) len = _bufferSize - minSize;
                    int tmpLen;
                    BufMark m = _marks.peek();
                    if (m != null) {
                        tmpLen = calcMarkSize(m);
                        if (tmpLen <= len) {
                            _marks.poll();
                            len = tmpLen;
                            _wasMarked = true;
                        }
                    }
                    if (len > 0) {
                        int tmpIdx = _bufStart + len;
                        if (tmpIdx > _circBuffer.length) {
                            tmpLen = _circBuffer.length - _bufStart;
                            System.arraycopy(_circBuffer, _bufStart, data, offset, tmpLen);
                            _bufStart = (tmpIdx) % _circBuffer.length;
                            System.arraycopy(_circBuffer, 0, data, offset + tmpLen, _bufStart);
                        } else {
                            System.arraycopy(_circBuffer, _bufStart, data, offset, len);
                            _bufStart = (tmpIdx) % _circBuffer.length;
                        }
                        if (tmpIdx < _viewPtr) {
                            _currOffset = _viewPtr - _bufStart;
                        } else {
                            _viewPtr = _bufStart;
                            _currOffset = 0;
                        }
                        _bufferSize -= len;
                    }
                    return len;
                }
            }
            return 0;
        } finally {
            _writeCondition.signalAll();
            _lock.unlock();
            if (_threadPool != null) _threadPool.submit(_notifyListener);
        }
    }
}
