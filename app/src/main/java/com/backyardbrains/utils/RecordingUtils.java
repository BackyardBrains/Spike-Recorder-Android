package com.backyardbrains.utils;

import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.util.Date;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class RecordingUtils {

    // BYB audio file extension
    public static final String BYB_RECORDING_EXT = ".wav";

    // Holds BYB recordings directory.
    private static final File BYB_DIRECTORY;
    // Name of the BYB recording folder
    private static final String BYB_DIRECTORY_NAME = "BackyardBrains";
    // Prefix for all internally recorded BYB recordings
    // BYB_Recording_3-1-2018_4:30:33.wav
    // BYB_Recording_3-1-2018+4:30:33-events.txt
    private static final String BYB_RECORDING_NAME_PREFIX = "BYB_Recording_";
    // Prefix for all imported recording
    private static final String BYB_SHARED_RECORDING_NAME_PREFIX = "Shared ";
    // Prefix for all imported recordings which name wasn't retrievable
    private static final String BYB_GENERIC_SHARED_RECORDING_NAME_PREFIX = "Shared Recording ";
    // BYB events file suffix
    private static final String BYB_EVENTS_NAME_SUFFIX = "-events";
    // BYB events file extension
    private static final String BYB_EVENTS_EXT = ".txt";

    static {
        BYB_DIRECTORY = new File(
            Environment.getExternalStorageDirectory() + File.separator + BYB_DIRECTORY_NAME
                + File.separator);
        //noinspection ResultOfMethodCallIgnored
        BYB_DIRECTORY.mkdir();
    }

    /**
     * Returns {@link File} to the BYB recordings directory.
     */
    @NonNull public static File getRecordingsDirectory() {
        return BYB_DIRECTORY;
    }

    /**
     * Creates and returns new {@link File} for recording.
     */
    @NonNull public static File createRecordingFile() {
        return new File(BYB_DIRECTORY,
            BYB_RECORDING_NAME_PREFIX + DateUtils.format_yyyy_MM_dd_HH_mm_ss(
                new Date(System.currentTimeMillis())) + BYB_RECORDING_EXT);
    }

    /**
     * Creates and returns new {@link File} for recordings with the specified {@code filename}. If parameter is {@code
     * null} a generic shared name will be used (e.g. Shared Recording 1).
     */
    @SuppressWarnings("WeakerAccess") @NonNull public static File createSharedRecordingFile(
        @Nullable String filename) {
        if (filename != null) {
            return new File(BYB_DIRECTORY, BYB_SHARED_RECORDING_NAME_PREFIX + filename);
        } else {
            int counter = 0;
            String name =
                BYB_GENERIC_SHARED_RECORDING_NAME_PREFIX + (++counter) + BYB_RECORDING_EXT;
            File f = new File(BYB_DIRECTORY, name);
            while (f.exists()) {
                filename =
                    BYB_GENERIC_SHARED_RECORDING_NAME_PREFIX + (++counter) + BYB_RECORDING_EXT;
                f = new File(BYB_DIRECTORY, filename);
            }

            return f;
        }
    }

    /**
     * Creates and returns new {@link File} for recording with the same file name as specified
     * {@code file} but with WAV extension.
     */
    @NonNull public static File createConvertedRecordingFile(@NonNull File file) {
        return new File(BYB_DIRECTORY, getFileNameWithoutExtension(file) + BYB_RECORDING_EXT);
    }

    /**
     * Creates and returns new {@link File} for accompanying events for the specified {@code file}.
     */
    @NonNull public static File createEventsFile(@NonNull File file) {
        // PATCH FOR FILES THAT HAVE NO EXTENSION
        int endIndex = file.getName().lastIndexOf(".");
        String fileName = endIndex < 0 ? file.getName()
            : file.getName().substring(0, file.getName().lastIndexOf("."));
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

    /**
     * Returns name of the specified {@code file} without the file extension.
     */
    public static String getFileNameWithoutExtension(@NonNull File file) {
        final int extIndex = file.getName().lastIndexOf('.');
        return extIndex < 0 ? file.getName() : file.getName().substring(0, extIndex);
    }
}
