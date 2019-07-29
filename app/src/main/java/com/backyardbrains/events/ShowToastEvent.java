package com.backyardbrains.events;

import androidx.annotation.NonNull;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ShowToastEvent {

    private final String toast;

    public ShowToastEvent(@NonNull String toast) {
        this.toast = toast;
    }

    public String getToast() {
        return toast;
    }
}
