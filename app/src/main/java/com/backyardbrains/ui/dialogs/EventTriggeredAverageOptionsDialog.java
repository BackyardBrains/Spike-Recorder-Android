package com.backyardbrains.ui.dialogs;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.Action;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.Setter;
import butterknife.ViewCollections;
import com.afollestad.materialdialogs.MaterialDialog;
import com.backyardbrains.R;
import com.backyardbrains.utils.EventUtils;
import com.backyardbrains.utils.ViewUtils;
import java.util.Arrays;
import java.util.List;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class EventTriggeredAverageOptionsDialog {

    // Action that initializes event buttons
    private static final Action<Button> INIT_EVENT_BUTTONS = (button, index) -> {
        // set value of the event as tag so we can reference it on click
        button.setTag(String.valueOf(index));
        // set event value as text
        button.setText(String.valueOf(index));
    };

    // Setter that resets event buttons UI
    private static final Setter<Button, String[]> RESET_EVENT_BUTTONS = (button, value, index) -> {
        // set button as inactive
        button.setBackgroundResource(R.drawable.circle_gray_white);
        // enable button only if linked event is available
        button.setEnabled(false);
        button.setAlpha(.5f);
        for (String event : value) {
            if (index == Integer.valueOf(event)) {
                button.setEnabled(true);
                button.setAlpha(1f);
                break;
            }
        }
    };

    @SuppressWarnings("WeakerAccess") @BindViews({
        R.id.btn_event_0, R.id.btn_event_1, R.id.btn_event_2, R.id.btn_event_3, R.id.btn_event_4, R.id.btn_event_5,
        R.id.btn_event_6, R.id.btn_event_7, R.id.btn_event_8, R.id.btn_event_9
    }) List<Button> eventButtons;
    @BindView(R.id.cb_remove_noise_intervals) CheckBox cbRemoveNoiseIntervals;
    @BindView(R.id.cb_compute_confidence_intervals) CheckBox cbComputeConfidenceIntervals;
    @BindView(R.id.sp_confidence_intervals_event) Spinner spConfidenceIntervalsEvent;

    private final MaterialDialog optionsDialog;
    private final ArrayAdapter<String> confidenceIntervalsEventAdapter;

    private String[] eventNames = new String[EventUtils.MAX_EVENT_COUNT];
    private SparseBooleanArray selectedButtons = new SparseBooleanArray();
    private int selectedCount = 0;

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
        public String confidenceIntervalsEvent;
    }

    public EventTriggeredAverageOptionsDialog(@NonNull Context context, @Nullable OnSelectOptionsListener listener) {
        final MaterialDialog.SingleButtonCallback positiveCallback = (dialog, which) -> {
            // at least one of the events needs to be selected
            if (selectedCount == 0) {
                ViewUtils.toast(context, context.getString(R.string.error_message_validation_event_selection));
                return;
            }

            int counter = 0;
            int len = eventButtons.size();
            Button button;
            for (int i = 0; i < len; i++) {
                button = eventButtons.get(i);
                if (button.isEnabled() && selectedButtons.get(i)) eventNames[counter++] = (String) button.getTag();
            }
            if (listener != null) {
                final Options options = new Options();
                options.events = eventNames;
                options.eventCount = counter;
                options.removeNoiseIntervals = cbRemoveNoiseIntervals.isChecked();
                options.confidenceIntervalsEvent =
                    cbComputeConfidenceIntervals.isChecked() ? (String) spConfidenceIntervalsEvent.getSelectedItem()
                        : null;
                listener.onOptionsSelected(options);
            }

            dialog.dismiss();
        };

        optionsDialog =
            new MaterialDialog.Builder(context).customView(R.layout.view_event_triggered_average_analysis_options,
                false).positiveText(R.string.label_show).onPositive(positiveCallback).autoDismiss(false).build();
        confidenceIntervalsEventAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);

        setupUI(optionsDialog.getCustomView());
    }

    @SuppressWarnings("unused") @OnClick({
        R.id.btn_event_0, R.id.btn_event_1, R.id.btn_event_2, R.id.btn_event_3, R.id.btn_event_4, R.id.btn_event_5,
        R.id.btn_event_6, R.id.btn_event_7, R.id.btn_event_8, R.id.btn_event_9
    }) void onEventClick(Button button) {
        int index = Integer.valueOf((String) button.getTag());
        boolean selected = !selectedButtons.get(index);
        selectedButtons.put(index, selected);
        selectedCount += selected ? 1 : -1;
        if (selectedButtons.get(index)) {
            eventButtons.get(index).setBackgroundResource(R.drawable.circle_gray_white_active);
        } else {
            eventButtons.get(index).setBackgroundResource(R.drawable.circle_gray_white);
        }

        final boolean enable = selectedCount != 0;
        cbRemoveNoiseIntervals.setEnabled(enable);
        cbComputeConfidenceIntervals.setEnabled(enable);
        if (enable && cbComputeConfidenceIntervals.isChecked()) {
            spConfidenceIntervalsEvent.setVisibility(View.VISIBLE);
            populateEventsDropdown();
        } else {
            spConfidenceIntervalsEvent.setVisibility(View.INVISIBLE);
        }
    }

    @OnCheckedChanged(R.id.cb_compute_confidence_intervals) void onComputeConfidenceIntervalsChanged(
        @SuppressWarnings("unused") CompoundButton button, boolean isChecked) {
        if (isChecked) populateEventsDropdown();
        spConfidenceIntervalsEvent.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Shows the event triggered average options dialog with all available events. Specified {@code eventNames} are
     * enabled for selection.
     */
    public void show(@NonNull String[] eventNames, int eventCount) {
        // reset all previously set settings
        reset(eventNames, eventCount);

        if (!optionsDialog.isShowing()) optionsDialog.show();
    }

    // Initializes user interface
    private void setupUI(@Nullable View view) {
        if (view == null) return;

        ButterKnife.bind(this, view);
        ViewCollections.run(eventButtons, INIT_EVENT_BUTTONS);

        spConfidenceIntervalsEvent.setAdapter(confidenceIntervalsEventAdapter);
    }

    // Resets UI and all local variables
    private void reset(String[] eventNames, int eventCount) {
        // reset number of selected events
        selectedCount = 0;
        // reset selected buttons
        selectedButtons.clear();
        // reset event buttons UI
        if (eventButtons != null) {
            ViewCollections.set(eventButtons, RESET_EVENT_BUTTONS, Arrays.copyOfRange(eventNames, 0, eventCount));
        }
    }

    // Populates events dropdown with the selected event buttons
    private void populateEventsDropdown() {
        int len = eventButtons.size();
        Button b;
        confidenceIntervalsEventAdapter.clear();
        for (int i = 0; i < len; i++) {
            b = eventButtons.get(i);
            if (b.isEnabled() && selectedButtons.get(i)) confidenceIntervalsEventAdapter.add((String) b.getTag());
        }
        confidenceIntervalsEventAdapter.notifyDataSetChanged();
    }
}
