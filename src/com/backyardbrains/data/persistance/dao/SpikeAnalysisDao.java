package com.backyardbrains.data.persistance.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.support.annotation.NonNull;
import com.backyardbrains.data.persistance.entity.SpikeAnalysis;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
@Dao public interface SpikeAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE) long insertSpikeAnalysis(@NonNull SpikeAnalysis analysis);

    @Query("SELECT * FROM spike_analysis WHERE file_path = :filePath") SpikeAnalysis loadSpikeAnalysis(
        @NonNull String filePath);

    @Query("SELECT id FROM spike_analysis WHERE file_path = :filePath") long loadSpikeAnalysisId(
        @NonNull String filePath);
}
