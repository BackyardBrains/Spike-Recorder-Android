package com.backyardbrains.utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import com.backyardbrains.R;

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
    public static int map(float glHeight, int in_min, int in_max, int out_min, int out_max) {
        return (int) ((glHeight - in_min) * (out_max - out_min) / (in_max - in_min) + out_min);
    }
}

