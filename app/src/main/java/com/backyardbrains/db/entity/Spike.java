package com.backyardbrains.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Entity(tableName = "spikes", indices = {
    @Index(value = { "train_id", "channel", "index", "value", "time" }), @Index(value = {
    "analysis_id", "channel", "index", "value", "time"
})
}, foreignKeys = @ForeignKey(entity = SpikeAnalysis.class, parentColumns = "id", childColumns = "analysis_id", onDelete = ForeignKey.CASCADE))
public class Spike {

    @PrimaryKey(autoGenerate = true) private long id;
    @ColumnInfo(name = "analysis_id") private long analysisId;
    @ColumnInfo(name = "train_id") private long trainId;
    private int channel;
    private int index;
    private float value;
    private float time;

    public Spike() {
    }

    @Ignore public Spike(int channel, float value, int index, float time) {
        this.trainId = 0;
        this.channel = channel;
        this.value = value;
        this.index = index;
        this.time = time;
    }

    @Ignore public Spike(long analysisId, long trainId, int channel, float value, int index, float time) {
        this.analysisId = analysisId;
        this.trainId = trainId;
        this.channel = channel;
        this.value = value;
        this.index = index;
        this.time = time;
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

    public long getTrainId() {
        return trainId;
    }

    public void setTrainId(long trainId) {
        this.trainId = trainId;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }
}
