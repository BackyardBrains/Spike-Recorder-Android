package com.backyardbrains.data.persistance.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Entity(tableName = "spike_trains", primaryKeys = {
    "spike_id", "train_id"
}, foreignKeys = {
    @ForeignKey(entity = Spike.class, parentColumns = "id", childColumns = "spike_id", onDelete = ForeignKey.CASCADE),
    @ForeignKey(entity = Train.class, parentColumns = "id", childColumns = "train_id", onDelete = ForeignKey.CASCADE)
}, indices = {
    @Index(value = "spike_id"), @Index("train_id")
}) public class SpikeTrain {

    @ColumnInfo(name = "spike_id") private long spikeId;
    @ColumnInfo(name = "train_id") private long trainId;

    public SpikeTrain(long spikeId, long trainId) {
        this.spikeId = spikeId;
        this.trainId = trainId;
    }

    public long getSpikeId() {
        return spikeId;
    }

    public void setSpikeId(long spikeId) {
        this.spikeId = spikeId;
    }

    public long getTrainId() {
        return trainId;
    }

    public void setTrainId(long trainId) {
        this.trainId = trainId;
    }
}
