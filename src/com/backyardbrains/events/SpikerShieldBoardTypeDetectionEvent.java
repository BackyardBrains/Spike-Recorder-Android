package com.backyardbrains.events;

import com.backyardbrains.utils.SpikerBoxBoardType;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class SpikerShieldBoardTypeDetectionEvent {

    private final @SpikerBoxBoardType int boardType;

    public SpikerShieldBoardTypeDetectionEvent(@SpikerBoxBoardType int boardType) {
        this.boardType = boardType;
    }

    public @SpikerBoxBoardType int getBoardType() {
        return boardType;
    }
}
