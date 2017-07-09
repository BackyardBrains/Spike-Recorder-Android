package com.backyardbrains.utils;

import android.os.Environment;
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
    private static final String BYB_RECORDING_NAME_PREFIX = "BYB_";

    static {
        BYB_DIRECTORY =
            new File(Environment.getExternalStorageDirectory() + File.separator + BYB_DIRECTORY_NAME + File.separator);
    }

    /**
     * Creates and returns new {@link File} for recording.
     */
    public static File createRecordingFile() {
        return new File(BYB_DIRECTORY,
            BYB_RECORDING_NAME_PREFIX + DateUtils.format_d_MMM_yyyy_HH_mm_s_a(new Date(System.currentTimeMillis())));
    }
}
