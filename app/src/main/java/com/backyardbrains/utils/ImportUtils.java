package com.backyardbrains.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.util.IOUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ImportUtils {

    @Retention(RetentionPolicy.SOURCE) @IntDef({
        ImportResult.SUCCESS, ImportResult.ERROR, ImportResult.ERROR_EXISTS, ImportResult.ERROR_OPEN,
        ImportResult.ERROR_SAVE
    }) public @interface ImportResult {
        int SUCCESS = 0;
        int ERROR = 1;
        int ERROR_EXISTS = 2;
        int ERROR_OPEN = 3;
        int ERROR_SAVE = 4;
    }

    /**
     * Checks whether specified {@code intent} holds valid data for import.
     */
    public static boolean checkImport(@NonNull Intent intent) {
        // FIXME: 05-Oct-18 For now we lean on OS and just check action and scheme because MIME type is not always set
        //                  but in the future we should find a way to be sure that WAV file is being imported
        return checkAction(intent.getAction()) && checkScheme(intent.getScheme());
    }

    /**
     * Imports and saves wav file located at specified {@code uri} to a BYB recordings directory.
     */
    public static @ImportResult int importRecording(@NonNull Context context, String scheme, Uri uri) {
        if (scheme == null || uri == null) return ImportResult.ERROR;

        String filename = null;
        if (scheme.equals(ContentResolver.SCHEME_FILE)) {
            filename = uri.getLastPathSegment();
        } else if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                filename = cursor.getString(columnIndex);
            }
            if (cursor != null) cursor.close();
        }

        final File file = RecordingUtils.createSharedRecordingFile(filename);
        if (file.exists()) return ImportResult.ERROR_EXISTS;
        final InputStream is;
        final FileOutputStream fos;

        try {
            is = context.getContentResolver().openInputStream(uri);
            if (is == null) return ImportResult.ERROR_OPEN; // you can't import file with the same name

            fos = new FileOutputStream(file);
            IOUtils.copyStream(is, fos);
            fos.flush();
            fos.getFD().sync();
            fos.close();
            is.close();

            return ImportResult.SUCCESS;
        } catch (IOException e) {
            Crashlytics.logException(e);

            return ImportResult.ERROR_SAVE;
        }
    }

    private static boolean checkAction(@Nullable String action) {
        return action != null && action.equals(Intent.ACTION_VIEW);
    }

    private static boolean checkScheme(@Nullable String scheme) {
        return scheme != null && (scheme.equals(ContentResolver.SCHEME_CONTENT) || scheme.equals(
            ContentResolver.SCHEME_FILE));
    }

    private static boolean checkMemeType(@Nullable String mimeType) {
        return mimeType != null && (mimeType.equals("audio/wave") || mimeType.equals("audio/wav") || mimeType.equals(
            "audio/x-wav") || mimeType.equals("audio/x-pn-wav") || mimeType.equals("audio/vnd.wave"));
    }
}
