package com.backyardbrains.events;

import com.backyardbrains.utils.ExpansionBoardType;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ExpansionBoardTypeDetectionEvent {

    private final @ExpansionBoardType int expansionBoardType;

    public ExpansionBoardTypeDetectionEvent(@ExpansionBoardType int expansionBoardType) {
        this.expansionBoardType = expansionBoardType;
    }

    public @ExpansionBoardType int getExpansionBoardType() {
        return expansionBoardType;
    }
}
