package com.backyardbrains.events;

import com.backyardbrains.utils.HumanSpikerBoardState;


public class SpikerBoxP300AudioDetectedEvent {

    private final @HumanSpikerBoardState int humanSpikerBoardAudioState;

    public SpikerBoxP300AudioDetectedEvent(@HumanSpikerBoardState int humanSpikerBoardAudioState) {
        this.humanSpikerBoardAudioState = humanSpikerBoardAudioState;
    }

    public @HumanSpikerBoardState int getHumanSpikerBoardAudioState() {
        return humanSpikerBoardAudioState;
    }
}
