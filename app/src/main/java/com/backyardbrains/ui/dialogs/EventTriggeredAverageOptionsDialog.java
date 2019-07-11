package com.backyardbrains.ui.dialogs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import com.backyardbrains.R;
import com.backyardbrains.drawing.Colors;
import com.backyardbrains.utils.EventUtils;
import java.util.Arrays;
import java.util.List;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class EventTriggeredAverageOptionsDialog {

    private static final ButterKnife.Action<Button> INIT_EVENT_BUTTONS = (button, index) -> {
        button.setTag(String.valueOf(index));
        button.setText(String.valueOf(index));
    };

    private static final ButterKnife.Setter<Button, String[]> ENABLE_AVAILABLE_EVENTS = (view, value, index) -> {
        view.setEnabled(false);
        view.setAlpha(.5f);
        for (String event : value) {
            if (index == Integer.valueOf(event)) {
                view.setEnabled(true);
                view.setAlpha(1f);
                break;
            }
        }
    };

    @SuppressWarnings("WeakerAccess") @BindViews({
        R.id.btn_event_0, R.id.btn_event_1, R.id.btn_event_2, R.id.btn_event_3, R.id.btn_event_4, R.id.btn_event_5,
        R.id.btn_event_6, R.id.btn_event_7, R.id.btn_event_8, R.id.btn_event_9
    }) List<Button> eventButtons;
    @BindView(R.id.cb_remove_noise_intervals) CheckBox cbRemoveNoiseIntervals;

    private final MaterialDialog optionsDialog;

    private String[] eventNames = new String[EventUtils.MAX_EVENT_COUNT];
    private SparseBooleanArray tmpSelected = new SparseBooleanArray();

    /**
     * Listens for selection of available options.
     */
    public interface OnSelectOptionsListener {
        void onOptionsSelected(@NonNull Options options);
    }

    public class Options {
        public String[] events;
        public int eventCount;
        public boolean removeNoiseIntervals;
    }

    @SuppressWarnings("WeakerAccess") final OnSelectOptionsListener listener;

    public EventTriggeredAverageOptionsDialog(@NonNull Context context, @Nullable OnSelectOptionsListener listener) {
        this.listener = listener;

        optionsDialog =
            new MaterialDialog.Builder(context).customView(R.layout.view_event_triggered_average_analysis_options,
                false).positiveText(R.string.label_show).onPositive((dialog, which) -> {
                int counter = 0;
                int len = eventButtons.size();
                Button button;
                for (int i = 0; i < len; i++) {
                    button = eventButtons.get(i);
                    if (button.isEnabled() && tmpSelected.get(i)) eventNames[counter++] = (String) button.getTag();
                }
                if (listener != null) {
                    final Options options = new Options();
                    options.events = eventNames;
                    options.eventCount = counter;
                    options.removeNoiseIntervals = cbRemoveNoiseIntervals.isChecked();
                    listener.onOptionsSelected(options);
                }
            }).build();

        setupUI(optionsDialog.getCustomView());
    }

    @SuppressWarnings("unused") @OnClick({
        R.id.btn_event_0, R.id.btn_event_1, R.id.btn_event_2, R.id.btn_event_3, R.id.btn_event_4, R.id.btn_event_5,
        R.id.btn_event_6, R.id.btn_event_7, R.id.btn_event_8, R.id.btn_event_9
    }) void onEventClick(Button button) {
        int index = Integer.valueOf((String) button.getTag());
        tmpSelected.put(index, !tmpSelected.get(index));
        if (tmpSelected.get(index)) {
            eventButtons.get(index).setBackgroundResource(R.drawable.circle_gray_white_active);
        } else {
            eventButtons.get(index).setBackgroundResource(R.drawable.circle_gray_white);
        }
    }

    /**
     * Shows the event triggered average options dialog with all available events. Specified {@code eventNames} are
     * enabled for selection.
     */
    public void show(@NonNull String[] eventNames, int eventCount) {
        // reset selected buttons
        tmpSelected.clear();
        // enable buttons for available events
        if (eventButtons != null) {
            ButterKnife.apply(eventButtons, ENABLE_AVAILABLE_EVENTS, Arrays.copyOfRange(eventNames, 0, eventCount));
        }

        if (!optionsDialog.isShowing()) optionsDialog.show();
    }

    // Initializes user interface
    private void setupUI(@Nullable View view) {
        if (view == null) return;

        ButterKnife.bind(this, view);
        ButterKnife.apply(eventButtons, INIT_EVENT_BUTTONS);
    }
}
