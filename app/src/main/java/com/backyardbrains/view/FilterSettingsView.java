package com.backyardbrains.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.Setter;
import butterknife.ViewCollections;
import com.backyardbrains.R;
import com.backyardbrains.dsp.Filters;
import com.backyardbrains.filters.BandFilter;
import com.backyardbrains.filters.NotchFilter;
import com.backyardbrains.utils.ApacheCommonsLang3Utils;
import com.backyardbrains.utils.ObjectUtils;
import com.backyardbrains.utils.ViewUtils;
import com.example.roman.thesimplerangebar.SimpleRangeBar;
import com.example.roman.thesimplerangebar.SimpleRangeBarOnChangeListener;
import java.util.List;

import static com.backyardbrains.utils.LogUtils.LOGD;
import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class FilterSettingsView extends ConstraintLayout {

    private static final String TAG = makeLogTag(FilterSettingsView.class);

    static final BandFilter[] FILTERS = new BandFilter[] {
        Filters.FILTER_BAND_HEART, Filters.FILTER_BAND_BRAIN, Filters.FILTER_BAND_MUSCLE,
        Filters.FILTER_BAND_PLANT, Filters.FILTER_BAND_NEURON_PRO, Filters.FILTER_BAND_HUMAN_PRO,
        Filters.FILTER_BAND_HHI_BOX
    };
    private static final BandFilter NO_FILTER = new BandFilter(Filters.FREQ_NO_CUT_OFF, Filters.FREQ_NO_CUT_OFF);

    private static final Setter<View, OnClickListener> INIT_PRESETS = (view, value, index) -> {
        view.setTag(FILTERS[index]);
        view.setOnClickListener(value);
    };

    private static final Setter<View, BandFilter> SELECT_PRESET = (view, value, index) -> {
        if (FILTERS[index].isEqual(value.getLowCutOffFrequency(), value.getHighCutOffFrequency())) {
            view.setBackgroundResource(R.drawable.circle_gray_white_active);
        } else {
            view.setBackgroundResource(R.drawable.circle_gray_white);
        }
    };

    @BindViews({
        R.id.btn_filter_heart, R.id.btn_filter_brain, R.id.btn_filter_muscle, R.id.btn_filter_plant,
        R.id.btn_filter_neuro, R.id.btn_filter_human, R.id.btn_filter_hhi_box
    }) List<Button> btnPresets;
    @BindView(R.id.et_low_cut_off) EditText etLowCutOff;
    @BindView(R.id.et_high_cut_off) EditText etHighCutOff;
    @BindView(R.id.rb_cut_offs) SimpleRangeBar srbCutOffs;
    @BindView(R.id.cb_notch_filter_50hz) CheckBox cb50HzNotchFilter;
    @BindView(R.id.cb_notch_filter_60hz) CheckBox cb60HzNotchFilter;

    /**
     * Interface definition for a callback to be invoked when one of filters is set.
     */
    public interface OnFilterSetListener {
        /**
         * Listener that is invoked when filter is set.
         *
         * @param filter The set filter.
         */
        void onBandFilterSet(@Nullable BandFilter filter);

        /**
         * Listener that is invoked when one of notch filters is set (50Hz or 60Hz).
         *
         * @param filter The set notch filter.
         */
        void onNotchFilterSet(@Nullable NotchFilter filter);
    }

    private OnFilterSetListener listener;

    private double minCutOff = Filters.FREQ_MIN_CUT_OFF;
    private double maxCutOff = Filters.FREQ_LOW_MAX_CUT_OFF;
    private double minCutOffLog = Math.log(1d);
    private double maxCutOffLog = Math.log(maxCutOff);

    private final OnClickListener presetOnClickListener = v -> {
        setBandFilter((BandFilter) v.getTag());

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
     * Sets up the band filter UI depending on the specified {@code filter} and {@code maxCutOff} frequency.
     */
    public void setBandFilter(@NonNull BandFilter filter, double maxCutOff) {
        if (ObjectUtils.equals(filter, NO_FILTER)) filter = new BandFilter(minCutOff, maxCutOff);
        updateMaxCutOff(maxCutOff);
        setBandFilter(filter);

        // select filter preset if necessary
        ViewCollections.set(btnPresets, SELECT_PRESET, filter);
    }

    /**
     * Sets up the notch filter UI depending on the specified {@code filter}.
     */
    public void setNotchFilter(NotchFilter filter) {
        cb50HzNotchFilter.setChecked(ObjectUtils.equals(filter, Filters.FILTER_NOTCH_50HZ));
        cb60HzNotchFilter.setChecked(ObjectUtils.equals(filter, Filters.FILTER_NOTCH_60HZ));
    }

    // Inflates view layout
    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_filter_settings, this);
        ButterKnife.bind(this);

        setupUI();
    }

    // Initial UI setup
    private void setupUI() {
        // presets
        ViewCollections.set(btnPresets, INIT_PRESETS, presetOnClickListener);
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
        // notch filters
        cb50HzNotchFilter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && cb60HzNotchFilter.isChecked()) cb60HzNotchFilter.setChecked(false);
            if (listener != null) listener.onNotchFilterSet(isChecked ? Filters.FILTER_NOTCH_50HZ : null);
        });
        cb60HzNotchFilter.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && cb50HzNotchFilter.isChecked()) cb50HzNotchFilter.setChecked(false);
            if (listener != null) listener.onNotchFilterSet(isChecked ? Filters.FILTER_NOTCH_60HZ : null);
        });
    }

    // Updates all local variables and controls depending on the max cut-off value
    private void updateMaxCutOff(double cutOffValue) {
        maxCutOff = cutOffValue;
        maxCutOffLog = Math.log(maxCutOff);
        srbCutOffs.setRanges((long) minCutOff, (long) maxCutOff);
    }

    // Sets specified filter as current band filter.
    private void setBandFilter(@NonNull BandFilter filter) {
        LOGD(TAG, "setBandFilter(" + filter.getLowCutOffFrequency() + ", " + filter.getHighCutOffFrequency() + ")");

        // Currently selected filter
        double lowCutOff = filter.getLowCutOffFrequency();
        double highCutOff = filter.getHighCutOffFrequency();

        // fix cut-off value if it's lower than minimum and higher than maximum
        lowCutOff = validateCutOffMinMax(lowCutOff);
        highCutOff = validateCutOffMinMax(highCutOff);
        // if low cut-off is higher that high one increase the high one to that value
        if (lowCutOff > highCutOff) highCutOff = lowCutOff;
        // if high cut-off is lower that low one decrease the low one to that value
        if (highCutOff < lowCutOff) lowCutOff = highCutOff;

        etLowCutOff.setText(String.valueOf(lowCutOff));
        etHighCutOff.setText(String.valueOf(highCutOff));

        // set thumbs values
        updateUI(lowCutOff, highCutOff);
    }

    // Validates currently set low cut-off frequency and updates range bar thumbs accordingly.
    void updateLowCutOff() {
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
    void updateHighCutOff() {
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
        // also update input fields
        setCutOffValue(etLowCutOff, lowCutOff);
        setCutOffValue(etHighCutOff, highCutOff);

        srbCutOffs.post(() -> {
            // this is kind of a hack because thumb values can only be set both at once and right thumb is always set first
            // within the library, so when try to set a value for both thumbs and the value is lower then the current left
            // thumb value, right thumb value is set at the current left thumb value, and that's why we always set the left
            // thumb value first
            srbCutOffs.setThumbValues(cutOffToThumb(lowCutOff), srbCutOffs.getRightThumbValue());
            srbCutOffs.setThumbValues(srbCutOffs.getLeftThumbValue(), cutOffToThumb(highCutOff));
            // add the listener again
            srbCutOffs.setOnSimpleRangeBarChangeListener(rangeBarOnChangeListener);
        });
    }

    // Updates text property of the specified EditText with specified cutOffValue
    // and sets selection at the end of the EditText
    void setCutOffValue(@NonNull EditText et, double cutOffValue) {
        et.setText(String.valueOf((int) cutOffValue));
        if (et.hasFocus()) et.setSelection(et.getText().length());
    }

    // Converts range value to a corresponding value withing logarithmic scale
    double thumbToCutOff(long thumbValue) {
        return Math.exp(
            minCutOffLog + (thumbValue - minCutOff) * (maxCutOffLog - minCutOffLog) / (maxCutOff - minCutOff));
    }

    // Converts value from logarithmic scale to a corresponding range value
    private long cutOffToThumb(double cutOffValue) {
        return (long) (
            ((Math.log(cutOffValue) - minCutOffLog) * (maxCutOff - minCutOff) / (maxCutOffLog - minCutOffLog))
                + minCutOff);
    }

    // Updates preset buttons and triggers OnFilterSetListener.onBandFilterSet() callback
    void updatePresetButtonsAndTriggerListener() {
        String lowCutOffStr = etLowCutOff.getText().toString();
        String highCutOffStr = etHighCutOff.getText().toString();
        double lowCutOff = ApacheCommonsLang3Utils.isNotBlank(lowCutOffStr) ? Double.valueOf(lowCutOffStr) : 0d;
        double highCutOff = ApacheCommonsLang3Utils.isNotBlank(highCutOffStr) ? Double.valueOf(highCutOffStr) : 0d;
        // if low and high cut off frequencies are at min and max set them to -1 so filtering is skipped
        if (lowCutOff == minCutOff && highCutOff == maxCutOff) {
            lowCutOff = Filters.FREQ_NO_CUT_OFF;
            highCutOff = Filters.FREQ_NO_CUT_OFF;
        }
        final BandFilter filter = new BandFilter(lowCutOff, highCutOff);

        // select filter preset if necessary
        ViewCollections.set(btnPresets, SELECT_PRESET, filter);

        if (listener != null) listener.onBandFilterSet(filter);
    }
}
