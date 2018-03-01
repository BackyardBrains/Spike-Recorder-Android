package com.backyardbrains.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import com.backyardbrains.R;
import com.crashlytics.android.Crashlytics;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class BYBUtils {

    public static void showAlert(Activity activity, String title, String mensage) {
        AlertDialog alertDialog =
            new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.BYBAppStyle)).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(mensage);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    // ----------------------------------------------------------------------------------------
    @Nullable public static FloatBuffer getFloatBufferFromFloatArray(final float[] array, int length) {
        FloatBuffer buf = null;
        try {
            final ByteBuffer temp = ByteBuffer.allocateDirect(length * 4);
            temp.order(ByteOrder.nativeOrder());
            buf = temp.asFloatBuffer();
            buf.put(array, 0, length);
            buf.position(0);
        } catch (Exception e) {
            Crashlytics.logException(e);
        }
        return buf;
    }

    // ----------------------------------------------------------------------------------------
    public static boolean isValidBuffer(short[] buffer) {
        if (buffer == null) {
            return false;
        }
        return buffer.length > 0;
    }

    // ----------------------------------------------------------------------------------------
    public static int map(float glHeight, int in_min, int in_max, int out_min, int out_max) {
        return (int) ((glHeight - in_min) * (out_max - out_min) / (in_max - in_min) + out_min);
    }

    public static float map(float value, float in_min, float in_max, float out_min, float out_max) {
        return ((value - in_min) * (out_max - out_min) / (in_max - in_min) + out_min);
    }

    public static FloatBuffer floatArrayListToFloatBuffer(ArrayList<float[]> arrayList) {
        final FloatBuffer buf;
        int totalArraySize = 0;
        for (float[] lv : arrayList) {
            totalArraySize += lv.length;
        }
        final ByteBuffer temp = ByteBuffer.allocateDirect(totalArraySize * 4);
        temp.order(ByteOrder.nativeOrder());
        buf = temp.asFloatBuffer();

        for (float[] lv : arrayList) {
            for (float v : lv) {
                buf.put(v);
            }
        }
        buf.position(0);
        return buf;
    }

    /**
     * Generates a logarithmic scale of {@code size} values ranging between {@code min} and {@code max}.
     */
    public static float[] generateLogSpace(int min, int max, int size) {
        double logarithmicBase = Math.E;
        double minimums = Math.pow(10.0, min);
        double maximums = Math.pow(10.0, max);
        double logMin = Math.log(minimums);
        double logMax = Math.log(maximums);
        double delta = (logMax - logMin) / size;

        double accDelta = 0;
        float[] v = new float[size + 1];
        for (int i = 0; i <= size; ++i) {
            v[i] = (float) Math.pow(logarithmicBase, logMin + accDelta);
            accDelta += delta;
        }
        return v;
    }
}

