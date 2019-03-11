package com.backyardbrains.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import com.backyardbrains.R;
import com.backyardbrains.dsp.Filters;
import com.backyardbrains.filters.Filter;
import com.backyardbrains.utils.ApacheCommonsLang3Utils;
import com.backyardbrains.utils.ObjectUtils;
import com.backyardbrains.utils.ViewUtils;
import com.example.roman.thesimplerangebar.SimpleRangeBar;
import com.example.roman.thesimplerangebar.SimpleRangeBarOnChangeListener;
import java.util.List;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class FilterSettingsView extends ConstraintLayout {

    private static final Filter NO_FILTER = new Filter(Filter.FREQ_NO_CUT_OFF, Filter.FREQ_NO_CUT_OFF);
    private static final Filter[] FILTERS = new Filter[] {
        Filters.FILTER_HEART, Filters.FILTER_BRAIN, Filters.FILTER_MUSCLE, Filters.FILTER_PLANT,
        Filters.FILTER_NEURON_PRO
    };

    @BindViews({
        R.id.btn_filter_heart, R.id.btn_filter_brain, R.id.btn_filter_muscle, R.id.btn_filter_plant,
        R.id.btn_filter_neuro
    }) List<Button> btnPresets;
    @BindView(R.id.et_low_cut_off) EditText etLowCutOff;
    @BindView(R.id.et_high_cut_off) EditText etHighCutOff;
    @BindView(R.id.rb_cut_offs) SimpleRangeBar srbCutOffs;

    /**
     * Interface definition for a callback to be invoked when one of filters is set.
     */
    public interface OnFilterSetListener {
        /**
         * Listener that is invoked when filter is set.
         *
         * @param filter The set filter.
         */
        void onFilterSet(@NonNull Filter filter);

        /**
         * Listener that is invoked when one of notch filters is set (50Hz or 60Hz).
         *
         * @param filter The set notch filter.
         */
        void onNotchFilterSet(@Nullable Filter filter);
    }

    private OnFilterSetListener listener;

    private double minCutOff = Filters.FREQ_MIN_CUT_OFF;
    private double maxCutOff = Filters.FREQ_LOW_MAX_CUT_OFF;
    private double minCutOffLog = Math.log(1d);
    private double maxCutOffLog = Math.log(maxCutOff);

    private final OnClickListener presetOnClickListener = v -> {
        setFilter((Filter) v.getTag());

        updatePresetButtonsAndTriggerListener();
    };

    private final SimpleRangeBarOnChangeListener rangeBarOnChangeListener = new SimpleRangeBarOnChangeListener() {
        @Override public void leftThumbValueChanged(long value) {
            // we need to handle 0 separately
            setCutOffValue(etLowCutOff, value != 0 ? Math.round(thumbToCutOff(value)) : 0d);

            updatePresetButtonsAndTriggerListener();
        }

        @Override public void rightThumbValueChanged(long value) {
            // we need to handle 0 separately
            setCutOffValue(etHighCutOff, value != 0 ? Math.round(thumbToCutOff(value)) : 0d);

            updatePresetButtonsAndTriggerListener();
        }
    };

    public FilterSettingsView(Context context) {
        this(context, null);
    }

    public FilterSettingsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FilterSettingsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    /**
     * Registers a callback to be invoked when filter is set.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnFilterChangeListener(@Nullable OnFilterSetListener listener) {
        this.listener = listener;
    }

    /**
     * Sets up the UI depending on the specified {@code filter} and {@code maxCutOff} frequency.
     */
    public void setFilter(@NonNull Filter filter, double maxCutOff) {
        if (ObjectUtils.equals(filter, NO_FILTER)) filter = new Filter(minCutOff, maxCutOff);
        updateMaxCutOff(maxCutOff);
        setFilter(filter);

        updatePresetButtons(filter.getLowCutOffFrequency(), filter.getHighCutOffFrequency());
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_filter_settings, this);
        ButterKnife.bind(this);

        setupUI();
    }

    private void setupUI() {
        // presets
        for (int i = 0; i < btnPresets.size(); i++) {
            btnPresets.get(i).setTag(FILTERS[i]);
            btnPresets.get(i).setOnClickListener(presetOnClickListener);
        }
        // low cut-off
        etLowCutOff.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                updateLowCutOff();
                ViewUtils.hideSoftKeyboard(textView);

                updatePresetButtonsAndTriggerListener();
                return true;
            }
            return false;
        });
        // high cut-off
        etHighCutOff.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                updateHighCutOff();
                ViewUtils.hideSoftKeyboard(textView);

                updatePresetButtonsAndTriggerListener();
                return true;
            }
            return false;
        });
        // range bar
        srbCutOffs.setRanges((long) minCutOff, (long) maxCutOff);
        srbCutOffs.setOnSimpleRangeBarChangeListener(rangeBarOnChangeListener);
    }

    // Update preset filter buttons
    private void updatePresetButtons(double lowCutOffFreq, double highCutOffFreq) {
        // select filter preset if necessary
        for (int i = 0; i < FILTERS.length; i++) {
            if (FILTERS[i].isEqual(lowCutOffFreq, highCutOffFreq)) {
                btnPresets.get(i).setBackgroundResource(R.drawable.circle_gray_white_active);
            } else {
                btnPresets.get(i).setBackgroundResource(R.drawable.circle_gray_white);
            }
        }
    }

    // Updates all local variables and controls depending on the max cut-off value
    private void updateMaxCutOff(double cutOffValue) {
        maxCutOff = cutOffValue;
        maxCutOffLog = Math.log(maxCutOff);
        srbCutOffs.setRanges((long) minCutOff, (long) maxCutOff);
    }

    // Sets specified filter as current filter.
    void setFilter(@NonNull Filter filter) {
        // Currently selected filter
        etLowCutOff.setText(String.valueOf(filter.getLowCutOffFrequency()));
        etHighCutOff.setText(String.valueOf(filter.getHighCutOffFrequency()));
        updateLowCutOff();
        updateHighCutOff();
    }

    // Validates currently set low cut-off frequency and updates range bar thumbs accordingly.
    @SuppressWarnings("WeakerAccess") void updateLowCutOff() {
        String lowCutOffStr = etLowCutOff.getText().toString();
        String highCutOffStr = etHighCutOff.getText().toString();
        double lowCutOff = ApacheCommonsLang3Utils.isNotBlank(lowCutOffStr) ? Double.valueOf(lowCutOffStr) : 0d;
        double highCutOff = ApacheCommonsLang3Utils.isNotBlank(highCutOffStr) ? Double.valueOf(highCutOffStr) : 0d;

        // fix cut-off value if it's lower than minimum and higher than maximum
        lowCutOff = validateCutOffMinMax(lowCutOff);
        // if low cut-off is higher that high one increase the high one to that value
        if (lowCutOff > highCutOff) highCutOff = lowCutOff;

        // set thumbs values
        updateUI(lowCutOff, highCutOff);
    }

    // Validates currently set high cut-off frequency and updates range bar thumbs accordingly.
    @SuppressWarnings("WeakerAccess") void updateHighCutOff() {
        String lowCutOffStr = etLowCutOff.getText().toString();
        String highCutOffStr = etHighCutOff.getText().toString();
        double lowCutOff = ApacheCommonsLang3Utils.isNotBlank(lowCutOffStr) ? Double.valueOf(lowCutOffStr) : 0d;
        double highCutOff = ApacheCommonsLang3Utils.isNotBlank(highCutOffStr) ? Double.valueOf(highCutOffStr) : 0d;

        // fix cut-off value if it's lower than minimum and higher than maximum
        highCutOff = validateCutOffMinMax(highCutOff);
        // if high cut-off is lower that low one decrease the low one to that value
        if (highCutOff < lowCutOff) lowCutOff = highCutOff;

        // set thumbs values
        updateUI(lowCutOff, highCutOff);
    }

    // Validates the passed cut-off value and corrects it if it goes below min or above max.
    private double validateCutOffMinMax(double cutOff) {
        // min value can be 0
        if (cutOff < minCutOff) cutOff = minCutOff;
        // max value can be SAMPLE_RATE/2
        if (cutOff > maxCutOff) cutOff = maxCutOff;

        return cutOff;
    }

    // Updates the UI of the input fields and range bar
    private void updateUI(double lowCutOff, double highCutOff) {
        // we need to remove range bar change listener so it doesn't trigger setting of input fields
        srbCutOffs.setOnSimpleRangeBarChangeListener(null);
        // this is kind of a hack because thumb values can only be set both at once and right thumb is always set first
        // within the library, so when try to set a value for both thumbs and the value is lower then the current left
        // thumb value, right thumb value is set at the current left thumb value, and that's why we always set the left
        // thumb value first
        srbCutOffs.setThumbValues(cutOffToThumb(lowCutOff), srbCutOffs.getRightThumbValue());
        srbCutOffs.setThumbValues(srbCutOffs.getLeftThumbValue(), cutOffToThumb(highCutOff));
        // also update input fields
        setCutOffValue(etLowCutOff, lowCutOff);
        setCutOffValue(etHighCutOff, highCutOff);
        // add the listener again
        srbCutOffs.setOnSimpleRangeBarChangeListener(rangeBarOnChangeListener);
    }

    // Updates text property of the specified EditText with specified cutOffValue
    // and sets selection at the end of the EditText
    @SuppressWarnings("WeakerAccess") void setCutOffValue(@NonNull EditText et, double cutOffValue) {
        et.setText(String.valueOf((int) cutOffValue));
        if (et.hasFocus()) et.setSelection(et.getText().length());
    }

    // Converts range value to a corresponding value withing logarithmic scale
    @SuppressWarnings("WeakerAccess") double thumbToCutOff(long thumbValue) {
        return Math.exp(
            minCutOffLog + (thumbValue - minCutOff) * (maxCutOffLog - minCutOffLog) / (maxCutOff - minCutOff));
    }

    // Converts value from logarithmic scale to a corresponding range value
    private long cutOffToThumb(double cutOffValue) {
        return (long) (
            ((Math.log(cutOffValue) - minCutOffLog) * (maxCutOff - minCutOff) / (maxCutOffLog - minCutOffLog))
                + minCutOff);
    }

    // Returns a new Filter with cut-off values currently set inside input fields
    void updatePresetButtonsAndTriggerListener() {
        String lowCutOffStr = etLowCutOff.getText().toString();
        String highCutOffStr = etHighCutOff.getText().toString();
        double lowCutOff = ApacheCommonsLang3Utils.isNotBlank(lowCutOffStr) ? Double.valueOf(lowCutOffStr) : 0d;
        double highCutOff = ApacheCommonsLang3Utils.isNotBlank(highCutOffStr) ? Double.valueOf(highCutOffStr) : 0d;
        // if low and high cut off frequencies are at min and max set them to -1 so filtering is skipped
        if (lowCutOff == minCutOff && highCutOff == maxCutOff) {
            lowCutOff = Filter.FREQ_NO_CUT_OFF;
            highCutOff = Filter.FREQ_NO_CUT_OFF;
        }
        // update preset buttons
        updatePresetButtons(lowCutOff, highCutOff);

        if (listener != null) listener.onFilterSet(new Filter(lowCutOff, highCutOff));
    }
}
