package com.backyardbrains.events;

import android.support.annotation.NonNull;
import com.backyardbrains.analysis.BYBAnalysisType;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class AnalyzeAudioFileEvent {

    private final String filePath;
    private final @BYBAnalysisType int type;

    public AnalyzeAudioFileEvent(@NonNull String filePath, @BYBAnalysisType int type) {
        this.filePath = filePath;
        this.type = type;
    }

    public String getFilePath() {
        return filePath;
    }

    public @BYBAnalysisType int getType() {
        return type;
    }
}
