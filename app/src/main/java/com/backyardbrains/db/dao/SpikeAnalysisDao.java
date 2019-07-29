package com.backyardbrains.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.backyardbrains.db.entity.SpikeAnalysis;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Dao public interface SpikeAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) long insertSpikeAnalysis(SpikeAnalysis analysis);

    @Query("SELECT * FROM spike_analysis WHERE file_path = :filePath") SpikeAnalysis loadSpikeAnalysis(String filePath);

    @Query("UPDATE spike_analysis SET file_path = :newFilePath WHERE file_path = :oldFilePath")
    void updateSpikeAnalysisFilePath(String oldFilePath, String newFilePath);

    @Query("DELETE FROM spike_analysis WHERE file_path = :filePath") void deleteSpikeAnalysis(String filePath);
}
