package com.backyardbrains.utils;

import android.support.annotation.NonNull;
import android.util.SparseArray;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class MarkerUtils {

    /**
     * Checks if events text file for the wav file at specified {@code filePath} exists and if yes, tries to parse it
     * and returns {@link SparseArray} with all the events.
     */
    public static SparseArray<String> parseEvents(@NonNull String filePath, int sampleRate) {
        final SparseArray<String> events = new SparseArray<>();
        final File file = RecordingUtils.getEventFile(new File(filePath));
        if (file != null) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith("#")) {
                        String[] lineArr = line.split(",\\s+");
                        if (lineArr.length == 2) {
                            events.put((int) (Float.valueOf(lineArr[1]) * sampleRate), lineArr[0]);
                        }
                    }
                }
                br.close();
            } catch (IOException e) {
                return new SparseArray<>();
            }
        }

        return events;
    }
}
