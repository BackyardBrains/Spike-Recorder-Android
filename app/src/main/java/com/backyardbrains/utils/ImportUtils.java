package com.backyardbrains.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.crashlytics.android.Crashlytics;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.greenrobot.essentials.io.IoUtils;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ImportUtils {

    @Retention(RetentionPolicy.SOURCE) @IntDef({
        ImportResultCode.SUCCESS, ImportResultCode.ERROR, ImportResultCode.ERROR_EXISTS, ImportResultCode.ERROR_OPEN,
        ImportResultCode.ERROR_SAVE
    }) public @interface ImportResultCode {
        int SUCCESS = 0;
        int ERROR = 1;
        int ERROR_EXISTS = 2;
        int ERROR_OPEN = 3;
        int ERROR_SAVE = 4;
    }

    public static class ImportResult {
        private final File file;
        private final @ImportResultCode int code;

        private ImportResult(@ImportResultCode int code, @Nullable File file) {
            this.code = code;
            this.file = file;
        }

        static ImportResult createResult(@Nullable File file) {
            return new ImportResult(ImportResultCode.SUCCESS, file);
        }

        static ImportResult createError(int code) {
            return new ImportResult(code, null);
        }

        public boolean isSuccessful() {
            return code == ImportResultCode.SUCCESS;
        }

        public int getCode() {
            return code;
        }

        public File getFile() {
            return file;
        }
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
    public static ImportResult importRecording(@NonNull Context context, String scheme, Uri uri) {
        if (scheme == null || uri == null) return ImportResult.createError(ImportResultCode.ERROR);

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
        if (file.exists()) return ImportResult.createError(ImportResultCode.ERROR_EXISTS);
        final InputStream is;
        final FileOutputStream fos;

        try {
            is = context.getContentResolver().openInputStream(uri);
            if (is == null) {
                return ImportResult.createError(
                    ImportResultCode.ERROR_OPEN); // you can't import file with the same name
            }

            fos = new FileOutputStream(file);
            IoUtils.copyAllBytes(is, fos);
            fos.flush();
            fos.getFD().sync();
            fos.close();
            is.close();

            return ImportResult.createResult(file);
        } catch (IOException e) {
            Crashlytics.logException(e);

            return ImportResult.createError(ImportResultCode.ERROR_SAVE);
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
