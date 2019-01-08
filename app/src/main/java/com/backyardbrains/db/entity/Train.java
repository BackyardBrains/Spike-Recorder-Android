package com.backyardbrains.db.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Entity(tableName = "trains", indices = @Index(value = {
    "analysis_id", "channel", "order"
}), foreignKeys = @ForeignKey(entity = SpikeAnalysis.class, parentColumns = "id", childColumns = "analysis_id", onDelete = ForeignKey.CASCADE))
public class Train {

    @PrimaryKey(autoGenerate = true) private long id;
    private int channel;
    @ColumnInfo(name = "analysis_id") private long analysisId;
    @ColumnInfo(name = "lower_threshold") private int lowerThreshold;
    @ColumnInfo(name = "upper_threshold") private int upperThreshold;
    private int order;
    @ColumnInfo(name = "lower_left") private boolean lowerLeft;

    public Train(long analysisId, int channel, int lowerThreshold, int upperThreshold, int order, boolean lowerLeft) {
        this.analysisId = analysisId;
        this.channel = channel;
        this.lowerThreshold = lowerThreshold;
        this.upperThreshold = upperThreshold;
        this.order = order;
        this.lowerLeft = lowerLeft;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(long analysisId) {
        this.analysisId = analysisId;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getLowerThreshold() {
        return lowerThreshold;
    }

    public void setLowerThreshold(int lowerThreshold) {
        this.lowerThreshold = lowerThreshold;
    }

    public int getUpperThreshold() {
        return upperThreshold;
    }

    public void setUpperThreshold(int upperThreshold) {
        this.upperThreshold = upperThreshold;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isLowerLeft() {
        return lowerLeft;
    }

    public void setLowerLeft(boolean lowerLeft) {
        this.lowerLeft = lowerLeft;
    }
}
