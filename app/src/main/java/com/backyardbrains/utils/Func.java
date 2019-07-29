package com.backyardbrains.utils;

import androidx.annotation.Nullable;

/**
 * Generic interface for handling basic functional programming
 *
 * @param <T> input type
 * @param <V> output type
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public interface Func<T, V> {
    @Nullable V apply(@Nullable T source);
}
