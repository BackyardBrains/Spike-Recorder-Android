package com.backyardbrains.data.persistance;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.support.annotation.NonNull;
import com.backyardbrains.data.persistance.dao.SpikeAnalysisDao;
import com.backyardbrains.data.persistance.dao.SpikeDao;
import com.backyardbrains.data.persistance.dao.SpikeTrainDao;
import com.backyardbrains.data.persistance.dao.TrainDao;
import com.backyardbrains.data.persistance.entity.Spike;
import com.backyardbrains.data.persistance.entity.SpikeAnalysis;
import com.backyardbrains.data.persistance.entity.SpikeTrain;
import com.backyardbrains.data.persistance.entity.Train;

/**
 * @author Tihomir Leka <ticapeca at gmail.com.
 */
@Database(entities = {
    SpikeAnalysis.class, Spike.class, Train.class, SpikeTrain.class
}, version = 3) public abstract class SpikeRecorderDatabase extends RoomDatabase {

    private static SpikeRecorderDatabase INSTANCE;

    public static SpikeRecorderDatabase get(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (SpikeRecorderDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), SpikeRecorderDatabase.class,
                        "byb-spike-recorder").fallbackToDestructiveMigration().build();
                }
                return INSTANCE;
            }
        }
        return INSTANCE;
    }

    public abstract SpikeAnalysisDao spikeAnalysisDao();

    public abstract SpikeDao spikeDao();

    public abstract TrainDao trainDao();

    public abstract SpikeTrainDao spikeTrainDao();
}
