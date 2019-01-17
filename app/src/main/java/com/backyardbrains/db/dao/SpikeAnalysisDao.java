package com.backyardbrains.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import com.backyardbrains.db.entity.SpikeAnalysis;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Dao public interface SpikeAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) long insertSpikeAnalysis(SpikeAnalysis analysis);

    @Query("SELECT id FROM spike_analysis WHERE file_path = :filePath") long loadSpikeAnalysisId(String filePath);

    @Query("DELETE FROM spike_analysis WHERE file_path = :filePath") void deleteSpikeAnalysis(String filePath);
}
