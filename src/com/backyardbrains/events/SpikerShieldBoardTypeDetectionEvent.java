package com.backyardbrains.events;

import com.backyardbrains.utils.SpikerShieldBoardType;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class SpikerShieldBoardTypeDetectionEvent {

    private final @SpikerShieldBoardType int boardType;

    public SpikerShieldBoardTypeDetectionEvent(@SpikerShieldBoardType int boardType) {
        this.boardType = boardType;
    }

    public @SpikerShieldBoardType int getBoardType() {
        return boardType;
    }
}
