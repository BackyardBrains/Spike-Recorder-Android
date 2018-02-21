package com.backyardbrains.data.persistance.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
@Entity(tableName = "spike_analysis", indices = @Index(value = "file_path", unique = true)) public class SpikeAnalysis {

    @PrimaryKey(autoGenerate = true) private long id;
    @NonNull @ColumnInfo(name = "file_path") private String filePath;

    public SpikeAnalysis(@NonNull String filePath) {
        this.filePath = filePath;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull public String getFilePath() {
        return filePath;
    }

    public void setFilePath(@NonNull String filePath) {
        this.filePath = filePath;
    }
}
