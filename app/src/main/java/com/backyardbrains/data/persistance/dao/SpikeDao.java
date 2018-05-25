package com.backyardbrains.data.persistance.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import com.backyardbrains.data.SpikeValueAndIndex;
import com.backyardbrains.data.persistance.entity.Spike;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Dao public interface SpikeDao {

    @Insert void insertSpikes(Spike[] spikes);

    @Query("SELECT * FROM spikes WHERE analysis_id = :analysisId ORDER BY `index` ASC") Spike[] loadSpikes(
            long analysisId);

    @Query("SELECT value, `index` FROM spikes WHERE analysis_id = :analysisId AND spikes.`index` >= :startIndex AND spikes.`index` <= :endIndex ORDER BY spikes.`index`")
    SpikeValueAndIndex[] loadSpikeValuesAndIndicesForRange(long analysisId, int startIndex, int endIndex);

    //@Query("SELECT * FROM spikes WHERE analysis_id = :analysisId AND id IN (SELECT spike_id FROM spike_trains WHERE analysis_id = :analysisId AND train_id = :trainId)")
    //List<Spike> loadSpikes(long analysisId, long trainId);
}
