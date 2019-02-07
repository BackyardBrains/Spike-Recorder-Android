package com.backyardbrains.utils;

import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import com.backyardbrains.R;

public class BYBUtils {

    public static void showAlert(Activity activity, String title, String mensage) {
        AlertDialog alertDialog =
            new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.BYBAppStyle)).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(mensage);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    // ----------------------------------------------------------------------------------------
    public static float map(float value, float inMin, float inMax, float outMin, float outMax) {
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }
}

