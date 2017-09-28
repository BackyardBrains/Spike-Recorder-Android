package com.backyardbrains.filters;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.backyardbrains.R;
import com.backyardbrains.utils.ObjectUtils;
import com.backyardbrains.utils.ViewUtils;
import com.example.roman.thesimplerangebar.SimpleRangeBar;
import com.example.roman.thesimplerangebar.SimpleRangeBarOnChangeListener;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class FilterSettingsDialog {

    // Low cut-off frequency for EKG
    private static final int FREQ_LOW_CUTOFF_HEART = 1;
    // High cut-off frequency for EKG
    private static final int FREQ_HIGH_CUTOFF_HEART = 11;
    // Low cut-off frequency for EEG
    private static final int FREQ_LOW_CUTOFF_BRAIN = 1;
    // High cut-off frequency for EEG
    private static final int FREQ_HIGH_CUTOFF_BRAIN = 40;
    // High cut-off frequency for Plant
    private static final int FREQ_HIGH_CUTOFF_PLANT = 5;
    // Array of predefined filters (Raw, EKG, EEG, Plant, Custom filter)
    private static final Filter[] FILTERS;
    // Array of predefined filter names
    private static final String[] FILTER_NAMES = new String[] {
        "Raw (No filter)", "Heart (EKG)", "Brain (EEG)", "Plant", "Custom filter"
    };

    static {
        FILTERS = new Filter[5];
        FILTERS[0] = new Filter(Filter.FREQ_NO_CUT_OFF, Filter.FREQ_NO_CUT_OFF);
        FILTERS[1] = new Filter(FREQ_LOW_CUTOFF_HEART, FREQ_HIGH_CUTOFF_HEART);
        FILTERS[2] = new Filter(FREQ_LOW_CUTOFF_BRAIN, FREQ_HIGH_CUTOFF_BRAIN);
        FILTERS[3] = new Filter(Filter.FREQ_NO_CUT_OFF, FREQ_HIGH_CUTOFF_PLANT);
        FILTERS[4] = new Filter(Filter.FREQ_MIN_CUT_OFF, Filter.FREQ_MAX_CUT_OFF);
    }

    // Min and max logarithmic values used for simulation of logarithmic scale
    private static final double MIN_CUT_OFF_LOG = Math.log(1);
    private static final double MAX_CUT_OFF_LOG = Math.log(Filter.FREQ_MAX_CUT_OFF);

    @BindView(R.id.et_low_cut_off) EditText etLowCutOff;
    @BindView(R.id.et_high_cut_off) EditText etHighCutOff;
    @BindView(R.id.rb_cut_offs) SimpleRangeBar srbCutOffs;

    // Dialog for listing predefined filters
    private MaterialDialog filterSettingsDialog;
    // Dialog for setting custom filter
    private MaterialDialog customFilterDialog;

    /**
     * Listens for selection of one of predefined filters or setting of a custom filter.
     */
    public interface FilterSelectionListener {
        void onFilterSelected(@NonNull Filter filter);
    }

    private final FilterSelectionListener listener;

    private Filter selectedFilter;

    public FilterSettingsDialog(@NonNull Context context, @Nullable final FilterSelectionListener listener) {
        this.listener = listener;

        filterSettingsDialog = new MaterialDialog.Builder(context).adapter(new AmModulationFilterAdapter(context),
            new LinearLayoutManager(context)).build();

        customFilterDialog = new MaterialDialog.Builder(context).
            customView(R.layout.view_dialog_custom_filter, true).
            positiveText(R.string.action_set).
            negativeText(R.string.action_cancel).
            onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    if (listener != null) listener.onFilterSelected(constructCustomFilter());
                }
            }).
            cancelListener(new DialogInterface.OnCancelListener() {
                @Override public void onCancel(DialogInterface dialogInterface) {
                    filterSettingsDialog.show();
                }
            }).
            build();

        setupCustomFilterUI(customFilterDialog.getCustomView());
    }

    /**
     * Shows the filter settings dialog with all predefined filters. Specified {@link filter} is preselected.
     */
    public void show(@NonNull Filter filter) {
        selectedFilter = filter;

        boolean isCustom = true;
        for (Filter FILTER : FILTERS) {
            if (ObjectUtils.equals(filter, FILTER)) {
                isCustom = false;
                break;
            }
        }
        if (isCustom) FILTERS[FILTERS.length - 1] = filter;

        if (!filterSettingsDialog.isShowing()) filterSettingsDialog.show();
    }

    /**
     * Dismisses the filter settings dialog.
     */
    public void dismiss() {
        customFilterDialog.dismiss();
        filterSettingsDialog.dismiss();
    }

    // Shows the custom filter dialog.
    private void showCustomFilterDialog() {
        if (selectedFilter != null) {
            etLowCutOff.setText(String.valueOf(selectedFilter.getLowCutOffFrequency()));
            etHighCutOff.setText(String.valueOf(selectedFilter.getHighCutOffFrequency()));
            srbCutOffs.setThumbValues(cutOffToSlider(selectedFilter.getLowCutOffFrequency()),
                cutOffToSlider(selectedFilter.getHighCutOffFrequency()));
        }
        customFilterDialog.show();
    }

    // Initializes custom filter dialog UI
    private void setupCustomFilterUI(@Nullable View customFilterView) {
        if (customFilterView == null) return;

        ButterKnife.bind(this, customFilterView);

        // low cut-off
        etLowCutOff.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    setLowCutOffThumbValue();
                    ViewUtils.hideSoftKeyboard(textView);
                    return true;
                }
                return false;
            }
        });
        // high cut-off
        etHighCutOff.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    setHighCutOffThumbValue();
                    ViewUtils.hideSoftKeyboard(textView);
                    return true;
                }
                return false;
            }
        });
        // range bar
        srbCutOffs.setRanges(Filter.FREQ_MIN_CUT_OFF, Filter.FREQ_MAX_CUT_OFF);
        srbCutOffs.setOnSimpleRangeBarChangeListener(new SimpleRangeBarOnChangeListener() {
            @Override public void leftThumbValueChanged(long value) {
                // we need to handle 0 separately
                etLowCutOff.setText(String.valueOf(value == 0 ? 0 : Math.round(sliderToCutOff(value))));
            }

            @Override public void rightThumbValueChanged(long value) {
                // we need to handle 0 separately
                etHighCutOff.setText(String.valueOf(value == 0 ? 0 : Math.round(sliderToCutOff(value))));
            }
        });
    }

    // Validates currently set low cut-off frequency and updates range bar thumbs accordingly.
    private void setLowCutOffThumbValue() {
        int lowCutOff = Integer.valueOf(etLowCutOff.getText().toString());
        int highCutOff = Integer.valueOf(etHighCutOff.getText().toString());
        // min value can be 0
        if (lowCutOff < Filter.FREQ_MIN_CUT_OFF) lowCutOff = Filter.FREQ_MIN_CUT_OFF;
        // max value can be SAMPLE_RATE/2
        if (lowCutOff > Filter.FREQ_MAX_CUT_OFF) lowCutOff = Filter.FREQ_MAX_CUT_OFF;
        // if low cut-off is higher that high one increase the high one to that value
        if (lowCutOff > highCutOff) highCutOff = lowCutOff;

        srbCutOffs.setThumbValues(cutOffToSlider(lowCutOff), cutOffToSlider(highCutOff));
    }

    // Validates currently set high cut-off frequency and updates range bar thumbs accordingly.
    private void setHighCutOffThumbValue() {
        int lowCutOff = Integer.valueOf(etLowCutOff.getText().toString());
        int highCutOff = Integer.valueOf(etHighCutOff.getText().toString());
        // min value can be 0
        if (highCutOff < Filter.FREQ_MIN_CUT_OFF) highCutOff = Filter.FREQ_MIN_CUT_OFF;
        // max value can be SAMPLE_RATE/2
        if (highCutOff > Filter.FREQ_MAX_CUT_OFF) highCutOff = Filter.FREQ_MAX_CUT_OFF;
        // if high cut-off is lower that low one decrease the low one to that value
        if (highCutOff < lowCutOff) lowCutOff = highCutOff;

        // HACK!!! we first increase right thumb value by 1 because right thumb is always set first (inside library)
        // and it can't be set in front of the left one
        srbCutOffs.setThumbValues(cutOffToSlider(lowCutOff), cutOffToSlider(highCutOff) + 1);
        // now we can set the right thumb to it's right value
        srbCutOffs.setThumbValues(cutOffToSlider(lowCutOff), cutOffToSlider(highCutOff));
    }

    // Converts slider value to a corresponding value withing logarithmic scale
    private double sliderToCutOff(long sliderValue) {
        return Math.exp(
            MIN_CUT_OFF_LOG + (sliderValue - Filter.FREQ_MIN_CUT_OFF) * (MAX_CUT_OFF_LOG - MIN_CUT_OFF_LOG) / (
                Filter.FREQ_MAX_CUT_OFF - Filter.FREQ_MIN_CUT_OFF));
    }

    // Converts value from logarithmic scale to a corresponding slider value
    private long cutOffToSlider(double cutOffValue) {
        return (long) (
            ((Math.log(cutOffValue) - MIN_CUT_OFF_LOG) * (Filter.FREQ_MAX_CUT_OFF - Filter.FREQ_MIN_CUT_OFF) / (
                MAX_CUT_OFF_LOG - MIN_CUT_OFF_LOG)) + Filter.FREQ_MIN_CUT_OFF);
    }

    // Returns a new Filter with cut-off values currently set inside input fields
    private Filter constructCustomFilter() {
        int lowCutOff = Integer.valueOf(etLowCutOff.getText().toString());
        int highCutOff = Integer.valueOf(etHighCutOff.getText().toString());
        return new Filter(lowCutOff, highCutOff);
    }

    /**
     * Adapter for predefined signal filters (Raw, EKG, EEG, Plant, Custom filter).
     */
    class AmModulationFilterAdapter extends RecyclerView.Adapter<AmModulationFilterAdapter.FilterViewHolder> {

        private static final int POSITION_CUSTOM_FILTER = 4;

        private final LayoutInflater inflater;

        AmModulationFilterAdapter(@NonNull Context context) {
            this.inflater = LayoutInflater.from(context);
        }

        @Override public FilterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FilterViewHolder(inflater.inflate(R.layout.item_filter, parent, false));
        }

        @Override public void onBindViewHolder(FilterViewHolder holder, int position) {
            holder.setFilter(FILTERS[position]);
        }

        @Override public int getItemCount() {
            return FILTERS.length;
        }

        final class FilterViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.tv_filter_name) TextView tvFilterName;
            @BindColor(R.color.orange) @ColorInt int selectedColor;

            Filter filter;

            FilterViewHolder(View view) {
                super(view);
                ButterKnife.bind(this, view);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        // set selected filter
                        selectedFilter = filter;
                        // if custom filter is clicked open custom filter dialog
                        if (getAdapterPosition() == POSITION_CUSTOM_FILTER) {
                            showCustomFilterDialog();
                        } else {
                            // if non-custom filter is selected we need to reset custom filter
                            FILTERS[FILTERS.length - 1] = new Filter(Filter.FREQ_MIN_CUT_OFF, Filter.FREQ_MAX_CUT_OFF);
                            if (listener != null) listener.onFilterSelected(filter);
                        }

                        filterSettingsDialog.dismiss();
                    }
                });
            }

            void setFilter(@NonNull Filter filter) {
                this.filter = filter;

                tvFilterName.setText(FILTER_NAMES[getAdapterPosition()]);
                tvFilterName.setBackgroundColor(
                    ObjectUtils.equals(filter, selectedFilter) ? selectedColor : Color.TRANSPARENT);
            }
        }
    }
}
