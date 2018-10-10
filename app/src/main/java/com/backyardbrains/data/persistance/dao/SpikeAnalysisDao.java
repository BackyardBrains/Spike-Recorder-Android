package com.backyardbrains.data.persistance.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import com.backyardbrains.data.persistance.entity.SpikeAnalysis;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Dao public interface SpikeAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) long insertSpikeAnalysis(SpikeAnalysis analysis);

    @Query("SELECT id FROM spike_analysis WHERE file_path = :filePath") long loadSpikeAnalysisId(String filePath);
}
