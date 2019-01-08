package com.backyardbrains.db;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import com.backyardbrains.db.dao.SpikeAnalysisDao;
import com.backyardbrains.db.dao.SpikeDao;
import com.backyardbrains.db.dao.TrainDao;
import com.backyardbrains.db.entity.Spike;
import com.backyardbrains.db.entity.SpikeAnalysis;
import com.backyardbrains.db.entity.Train;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
@Database(entities = {
    SpikeAnalysis.class, Spike.class, Train.class
}, version = 6) public abstract class SpikeRecorderDatabase extends RoomDatabase {

    private static SpikeRecorderDatabase INSTANCE;

    public static SpikeRecorderDatabase get(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (SpikeRecorderDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), SpikeRecorderDatabase.class,
                        "byb-spike-recorder")
                        .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                        .fallbackToDestructiveMigration()
                        .build();
                }
                return INSTANCE;
            }
        }
        return INSTANCE;
    }

    /**
     * Migrate from:
     * version 3  to version 4 without changes
     */
    @VisibleForTesting private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            // actually nothing changed... db version was updated by mistake
        }
    };

    /**
     * Migrate from:
     * version 1 - using the SQLiteDatabase API
     * to
     * version 2 - using Room
     */
    @VisibleForTesting private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            // create new table for spikes
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `spikes_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `analysis_id` INTEGER NOT NULL, `train_id` INTEGER NOT NULL, `index` INTEGER NOT NULL, `value` REAL NOT NULL, `time` REAL NOT NULL, FOREIGN KEY(`analysis_id`) REFERENCES `spike_analysis`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");

            // create indexes for the new table
            database.execSQL(
                "CREATE  INDEX `index_spikes_train_id_index_value_time` ON `spikes_new` (`train_id`, `index`, `value`, `time`)");
            database.execSQL(
                "CREATE  INDEX `index_spikes_analysis_id_index_value` ON `spikes_new` (`analysis_id`, `index`, `value`, `time`)");

            // merge data from old spikes table and spike_trains cross table and copy it to the new spikes table
            database.execSQL("INSERT INTO spikes_new (analysis_id, train_id, `index`, value, time) "
                + "SELECT spikes.analysis_id as analysis_id, IFNULL(train_id, 0), `index`, value, time "
                + "FROM spikes LEFT JOIN spike_trains ON spikes.id = spike_trains.spike_id ORDER BY spikes.id");

            // drop old spikes table
            database.execSQL("DROP TABLE IF EXISTS spikes");

            // drop old spike_trains cross table
            database.execSQL("DROP TABLE IF EXISTS spike_trains");

            // change the new table name to the correct one
            database.execSQL("ALTER TABLE spikes_new RENAME TO spikes");
        }
    };

    @VisibleForTesting private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            // add column to trains and spikes tables that holds information about the channel the train/spike belongs to
            database.execSQL("ALTER TABLE trains ADD COLUMN channel INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE spikes ADD COLUMN channel INTEGER NOT NULL DEFAULT 0");
            // drop old indexes we need to create a new one
            database.execSQL("DROP INDEX IF EXISTS `index_trains_analysis_id_order`");
            database.execSQL("DROP INDEX IF EXISTS `index_spikes_train_id_index_value_time`");
            database.execSQL("DROP INDEX IF EXISTS `index_spikes_analysis_id_index_value_time`");
            // create indexes that include new column
            database.execSQL(
                "CREATE  INDEX `index_trains_analysis_id_channel_order` ON `trains` (`analysis_id`, `channel`, `order`)");
            database.execSQL(
                "CREATE  INDEX `index_spikes_train_id_channel_index_value_time` ON `spikes` (`train_id`, `channel`, `index`, `value`, `time`)");
            database.execSQL(
                "CREATE  INDEX `index_spikes_analysis_id_channel_index_value_time` ON `spikes` (`analysis_id`, `channel`, `index`, `value`, `time`)");
        }
    };

    public abstract SpikeAnalysisDao spikeAnalysisDao();

    public abstract SpikeDao spikeDao();

    public abstract TrainDao trainDao();
}
