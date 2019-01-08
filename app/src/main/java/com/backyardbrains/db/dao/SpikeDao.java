package com.backyardbrains.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import com.backyardbrains.db.entity.Spike;
import com.backyardbrains.vo.SpikeIndexValue;
import com.backyardbrains.vo.SpikeIndexValueTime;
import java.util.List;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Dao public interface SpikeDao {

    @Insert void insertSpikes(Spike[] spikes);

    @Insert void insertSpikes(List<Spike> spikes);

    @Query("DELETE FROM spikes WHERE train_id = :trainId") void deleteSpikes(long trainId);

    @Query("SELECT `index`, value, time FROM spikes WHERE analysis_id = :analysisId AND channel = :channel AND value >= :startValue AND value <= :endValue")
    SpikeIndexValueTime[] loadSpikesForValueRange(long analysisId, int channel, float startValue, float endValue);

    @Query("SELECT `index`, value  FROM spikes WHERE analysis_id = :analysisId AND channel = :channel AND `index` >= :startIndex AND `index` <= :endIndex ORDER BY `index`")
    SpikeIndexValue[] loadSpikesForIndexRange(long analysisId, int channel, int startIndex, int endIndex);

    @Query("SELECT `index`, value  FROM spikes WHERE train_id = :trainId AND `index` >= :startIndex AND `index` <= :endIndex ORDER BY `index`")
    SpikeIndexValue[] loadSpikesByTrainForIndexRange(long trainId, int startIndex, int endIndex);

    @Query("SELECT time FROM spikes WHERE train_id = :trainId ORDER BY `index`") float[] loadSpikeTimes(long trainId);

    @Query("SELECT `index` FROM spikes WHERE train_id = :trainId ORDER BY `index`") int[] loadSpikeIndices(
        long trainId);
}
