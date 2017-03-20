package com.backyardbrains.utls;

import android.support.annotation.Nullable;

/**
 * Generic interface for handling basic functional programming
 *
 * @param <T> input type
 * @param <V> output type
 * @author Tihomir Leka <ticapeca at gmail.com>.
 */
public interface Func<T, V> {
    @Nullable V apply(@Nullable T source);
}
