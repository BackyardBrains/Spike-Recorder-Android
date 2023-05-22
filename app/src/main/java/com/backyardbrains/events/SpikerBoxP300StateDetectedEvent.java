package com.backyardbrains.events;

import com.backyardbrains.utils.HumanSpikerBoardState;


public class SpikerBoxP300StateDetectedEvent {

    private final @HumanSpikerBoardState int humanSpikerBoardState;

    public SpikerBoxP300StateDetectedEvent(@HumanSpikerBoardState int humanSpikerBoardState) {
        this.humanSpikerBoardState = humanSpikerBoardState;
    }

    public @HumanSpikerBoardState int getHumanSpikerBoardState() {
        return humanSpikerBoardState;
    }
}
