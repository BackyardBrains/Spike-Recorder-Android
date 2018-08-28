package com.backyardbrains.events;

import com.backyardbrains.utils.SpikerBoxHardwareType;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class SpikerBoxHardwareTypeDetectionEvent {

    private final @SpikerBoxHardwareType int hardwareType;

    public SpikerBoxHardwareTypeDetectionEvent(@SpikerBoxHardwareType int hardwareType) {
        this.hardwareType = hardwareType;
    }

    public @SpikerBoxHardwareType int getHardwareType() {
        return hardwareType;
    }
}
