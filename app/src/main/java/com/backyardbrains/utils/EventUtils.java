package com.backyardbrains.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class EventUtils {

    private static final String NO_ID = "NO_ID";

    /**
     * Logs a {@link CustomEvent} with Fabric Answers.
     */
    public static void logCustom(@NonNull String name, @Nullable Pair<String, String>[] customAttributes) {
        final CustomEvent event = new CustomEvent("Video Played");
        if (customAttributes != null) {
            final int len = customAttributes.length;
            for (Pair<String, String> customAttribute : customAttributes) {
                event.putCustomAttribute(customAttribute.first, customAttribute.second);
            }
        }
        Answers.getInstance().logCustom(event);
    }

    /**
     * Logs a {@link ContentViewEvent} with Fabric Answers.
     */
    public static void logView(@NonNull String name, @NonNull String type, @Nullable String id,
        @Nullable Pair<String, String>[] customAttributes) {
        final ContentViewEvent event =
            new ContentViewEvent().putContentName(name).putContentType(type).putContentId(id != null ? id : NO_ID);
        if (customAttributes != null) {
            final int len = customAttributes.length;
            for (Pair<String, String> customAttribute : customAttributes) {
                event.putCustomAttribute(customAttribute.first, customAttribute.second);
            }
        }
        Answers.getInstance().logContentView(event);
    }
}
