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
public abstract class FilterSettingsDialog {

    private static final Filter NO_FILTER = new Filter(Filter.FREQ_NO_CUT_OFF, Filter.FREQ_NO_CUT_OFF);

    @BindView(R.id.et_low_cut_off) EditText etLowCutOff;
    @BindView(R.id.et_high_cut_off) EditText etHighCutOff;
    @BindView(R.id.rb_cut_offs) SimpleRangeBar srbCutOffs;

    // Dialog for listing predefined filters
    private MaterialDialog filterSettingsDialog;
    // Dialog for setting custom filter
    private MaterialDialog customFilterDialog;

    private final SimpleRangeBarOnChangeListener rangeBarOnChangeListener = new SimpleRangeBarOnChangeListener() {
        @Override public void leftThumbValueChanged(long value) {
            // we need to handle 0 separately
            etLowCutOff.setText(String.valueOf(value == 0 ? 0 : Math.round(thumbToCutOff(value))));
        }

        @Override public void rightThumbValueChanged(long value) {
            // we need to handle 0 separately
            etHighCutOff.setText(String.valueOf(value == 0 ? 0 : Math.round(thumbToCutOff(value))));
        }
    };

    /**
     * Listens for selection of one of predefined filters or setting of a custom filter.
     */
    public interface FilterSelectionListener {
        void onFilterSelected(@NonNull Filter filter);
    }

    private final FilterSelectionListener listener;
    private final double minCutOffLog;
    private final double maxCutOffLog;

    private Filter customFilter;
    private Filter selectedFilter;

    public FilterSettingsDialog(@NonNull Context context, @Nullable final FilterSelectionListener listener) {
        this.listener = listener;
        this.minCutOffLog = Math.log(1);
        this.maxCutOffLog = Math.log(getMaxCutOff());
        this.customFilter = new Filter(getMinCutOff(), getMaxCutOff());

        filterSettingsDialog =
            new MaterialDialog.Builder(context).adapter(new FiltersAdapter(context), new LinearLayoutManager(context))
                .build();

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

    protected abstract double getMinCutOff();

    protected abstract double getMaxCutOff();

    protected abstract Filter[] getFilters();

    protected abstract String[] getFilterNames();

    /**
     * Shows the filter settings dialog with all predefined filters. Specified {@code filter} is preselected.
     */
    public void show(@NonNull Filter filter) {
        selectedFilter = filter;

        boolean isCustom = !ObjectUtils.equals(filter, NO_FILTER);
        for (Filter f : getFilters()) {
            if (ObjectUtils.equals(filter, f)) {
                isCustom = false;
                break;
            }
        }
        if (isCustom) customFilter = filter;

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
            srbCutOffs.setThumbValues(cutOffToThumb(selectedFilter.getLowCutOffFrequency()),
                cutOffToThumb(selectedFilter.getHighCutOffFrequency()));
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
                    updateLowCutOff();
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
                    updateHighCutOff();
                    ViewUtils.hideSoftKeyboard(textView);
                    return true;
                }
                return false;
            }
        });
        // range bar
        srbCutOffs.setRanges((long) getMinCutOff(), (long) getMaxCutOff());
        srbCutOffs.setOnSimpleRangeBarChangeListener(rangeBarOnChangeListener);
    }

    // Validates currently set low cut-off frequency and updates range bar thumbs accordingly.
    private void updateLowCutOff() {
        double lowCutOff = Double.valueOf(etLowCutOff.getText().toString());
        double highCutOff = Double.valueOf(etHighCutOff.getText().toString());

        // fix cut-off value if it's lower than minimum and higher than maximum
        lowCutOff = validateCutOffMinMax(lowCutOff);
        // if low cut-off is higher that high one increase the high one to that value
        if (lowCutOff > highCutOff) highCutOff = lowCutOff;

        // set thumbs values
        updateUI(lowCutOff, highCutOff);
    }

    // Validates currently set high cut-off frequency and updates range bar thumbs accordingly.
    private void updateHighCutOff() {
        double lowCutOff = Double.valueOf(etLowCutOff.getText().toString());
        double highCutOff = Double.valueOf(etHighCutOff.getText().toString());

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
        if (cutOff < getMinCutOff()) cutOff = getMinCutOff();
        // max value can be SAMPLE_RATE/2
        if (cutOff > getMaxCutOff()) cutOff = getMaxCutOff();

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
        etLowCutOff.setText(String.valueOf(lowCutOff));
        etHighCutOff.setText(String.valueOf(highCutOff));
        // add the listener again
        srbCutOffs.setOnSimpleRangeBarChangeListener(rangeBarOnChangeListener);
    }

    // Converts range value to a corresponding value withing logarithmic scale
    private double thumbToCutOff(long thumbValue) {
        return Math.exp(minCutOffLog + (thumbValue - getMinCutOff()) * (maxCutOffLog - minCutOffLog) / (getMaxCutOff()
            - getMinCutOff()));
    }

    // Converts value from logarithmic scale to a corresponding range value
    private long cutOffToThumb(double cutOffValue) {
        return (long) (
            ((Math.log(cutOffValue) - minCutOffLog) * (getMaxCutOff() - getMinCutOff()) / (maxCutOffLog - minCutOffLog))
                + getMinCutOff());
    }

    // Returns a new Filter with cut-off values currently set inside input fields
    private Filter constructCustomFilter() {
        double lowCutOff = Double.valueOf(etLowCutOff.getText().toString());
        double highCutOff = Double.valueOf(etHighCutOff.getText().toString());
        return new Filter(lowCutOff, highCutOff);
    }

    /**
     * Adapter for predefined signal filters (Raw, EKG, EEG, Plant, Custom filter).
     */
    class FiltersAdapter extends RecyclerView.Adapter<FiltersAdapter.FilterViewHolder> {

        private final LayoutInflater inflater;

        FiltersAdapter(@NonNull Context context) {
            this.inflater = LayoutInflater.from(context);
        }

        @NonNull @Override public FilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new FilterViewHolder(inflater.inflate(R.layout.item_filter, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull FilterViewHolder holder, int position) {
            holder.setFilter(getFilter(position));
        }

        @Override public int getItemCount() {
            return getFilters().length + 2; // for No filter and Custom filter
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
                        if (ObjectUtils.equals(customFilter, filter)) {
                            showCustomFilterDialog();
                        } else {
                            // if non-custom filter is selected we need to reset custom filter
                            customFilter = new Filter(getMinCutOff(), getMaxCutOff());
                            if (listener != null) listener.onFilterSelected(filter);
                        }

                        filterSettingsDialog.dismiss();
                    }
                });
            }

            void setFilter(@NonNull Filter filter) {
                this.filter = filter;

                tvFilterName.setText(getFilterName(getAdapterPosition()));
                tvFilterName.setBackgroundColor(
                    ObjectUtils.equals(filter, selectedFilter) ? selectedColor : Color.TRANSPARENT);
            }
        }

        private Filter getFilter(int position) {
            if (position == 0) {
                return NO_FILTER;
            } else if (position == getFilters().length + 1) {
                return customFilter;
            } else {
                return getFilters()[position - 1]; // -1 because of "No filter"
            }
        }

        private String getFilterName(int position) {
            if (position == 0) {
                return "Raw (No filter)";
            } else if (position == getFilters().length + 1) {
                return "Custom filter";
            } else {
                return getFilterNames()[position - 1]; // -1 because of "No filter"
            }
        }
    }
}
