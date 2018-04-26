package com.backyardbrains.utils;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.File;
import java.util.Date;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class RecordingUtils {

    /**
     * Holds BYB recordings directory.
     */
    public static final File BYB_DIRECTORY;

    // Name of the BYB recording folder
    private static final String BYB_DIRECTORY_NAME = "BackyardBrains";
    // Prefix for all BYB recordings
    // BYB_Recording_2018-02-28_13.42.21.wav
    // BYB_Recording_2018-02-28_13.42.21-events.txt
    private static final String BYB_RECORDING_NAME_PREFIX = "BYB_Recording_";
    // BYB audio file extension
    private static final String BYB_RECORDING_EXT = ".wav";
    // BYB events file suffix
    private static final String BYB_EVENTS_NAME_SUFFIX = "-events";
    // BYB events file extension
    private static final String BYB_EVENTS_EXT = ".txt";

    static {
        BYB_DIRECTORY =
            new File(Environment.getExternalStorageDirectory() + File.separator + BYB_DIRECTORY_NAME + File.separator);
        //noinspection ResultOfMethodCallIgnored
        BYB_DIRECTORY.mkdir();
    }

    /**
     * Creates and returns new {@link File} for recording.
     */
    public static File createRecordingFile() {
        return new File(BYB_DIRECTORY,
            BYB_RECORDING_NAME_PREFIX + DateUtils.format_yyyy_MM_dd_HH_mm_ss(new Date(System.currentTimeMillis()))
                + BYB_RECORDING_EXT);
    }

    /**
     * Creates and returns new {@link File} for accompanying events for the specified {@code file}.
     */
    public static File createEventsFile(@NonNull File file) {
        // PATCH FOR FILES THAT HAVE NO EXTENSION
        int endIndex = file.getName().lastIndexOf(".");
        String fileName = endIndex < 0 ? file.getName() : file.getName().substring(0, file.getName().lastIndexOf("."));
        return new File(BYB_DIRECTORY, fileName + BYB_EVENTS_NAME_SUFFIX + BYB_EVENTS_EXT);
    }

    /**
     * Returns {@code true} if specified {@code file} is an events file, {@code false} otherwise.
     */
    public static boolean isEventsFile(@NonNull File file) {
        return file.getName().endsWith(BYB_EVENTS_NAME_SUFFIX + BYB_EVENTS_EXT);
    }

    /**
     * Returns an events text {@link File} that accompanies the specified audio {@code file} if it exists, {@code null}
     * otherwise.
     */
    @Nullable public static File getEventFile(@NonNull File file) {
        File f = createEventsFile(file);
        return f.exists() ? f : null;
    }
}
