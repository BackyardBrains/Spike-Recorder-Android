package com.backyardbrains.data.persistance.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
@Entity(tableName = "spikes", indices = @Index(value = "analysis_id"), foreignKeys = @ForeignKey(entity = SpikeAnalysis.class, parentColumns = "id", childColumns = "analysis_id", onDelete = ForeignKey.CASCADE))
public class Spike {

    @PrimaryKey(autoGenerate = true) private long id;
    @ColumnInfo(name = "analysis_id") private long analysisId;
    private float value;
    private int index;
    private float time;

    public Spike(float value, int index, float time) {
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

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }
}
