package com.backyardbrains.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import com.backyardbrains.db.entity.Train;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Dao public interface TrainDao {

    @Insert void insertTrain(Train train);

    @Insert void insertTrains(Train[] trains);

    @Delete void deleteTrain(Train train);

    @Query("SELECT * FROM trains WHERE analysis_id = :analysisId AND channel = :channel AND `order` = :order")
    Train loadTrain(long analysisId, int channel, int order);

    @Query("SELECT * FROM trains WHERE analysis_id = :analysisId AND `order` = :order ORDER BY `order`")
    Train[] loadTrains(long analysisId, int order);

    @Query("SELECT DISTINCT * FROM trains WHERE analysis_id = :analysisId ORDER BY `order`") Train[] loadTrains(
        long analysisId);

    @Query("SELECT * FROM trains WHERE analysis_id = :analysisId AND channel = :channel ORDER BY `order`")
    Train[] loadTrainsByChannel(long analysisId, int channel);

    @Query("SELECT COUNT(*) FROM trains WHERE analysis_id = :analysisId") int loadTrainCount(long analysisId);

    @Query("UPDATE trains SET `order` = `order` - 1 WHERE analysis_id = :analysisId AND `order` > :order")
    void updateTrainsAfterOrder(long analysisId, int order);
}
