package com.backyardbrains.data.persistance.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import com.backyardbrains.data.persistance.entity.Train;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Dao public interface TrainDao {

    @Insert void insertTrain(Train train);

    @Delete void deleteTrain(Train train);

    @Query("DELETE FROM trains WHERE analysis_id = :analysisId AND `order` = :trainOrder") void deleteTrain(
            long analysisId, int trainOrder);

    @Query("SELECT * FROM trains WHERE analysis_id = :analysisId AND `order` = :order") Train loadTrain(long analysisId,
                                                                                                        int order);

    @Query("SELECT * FROM trains WHERE analysis_id = :analysisId ORDER BY `order` ASC") Train[] loadTrains(
            long analysisId);

    @Query("SELECT COUNT(*) FROM trains WHERE analysis_id = :analysisId") int loadTrainCount(long analysisId);

    @Query("UPDATE trains SET `order` = `order` - 1 WHERE analysis_id = :analysisId AND `order` > :trainOrder")
    void updateTrainsAfterOrder(long analysisId, int trainOrder);
}
