package com.backyardbrains.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
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
