package com.backyardbrains.filters;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.MaterialDialog;
import com.backyardbrains.R;
import com.backyardbrains.utils.ObjectUtils;

/**
 * @author Tihomir Leka <ticapeca at gmail.com>
 */
public class FilterSettingsDialog {

    private static final int FREQ_LOW_CUTOFF_HEART = 1;
    private static final int FREQ_HIGH_CUTOFF_HEART = 11;
    private static final int FREQ_LOW_CUTOFF_BRAIN = 1;
    private static final int FREQ_HIGH_CUTOFF_BRAIN = 40;
    private static final int FREQ_LOW_CUTOFF_PLANT = 5;
    private static final Filter[] FILTERS;

    static {
        FILTERS = new Filter[4];
        FILTERS[0] = new Filter("Raw (No filter)", Filter.FREQ_NO_CUT_OFF, Filter.FREQ_NO_CUT_OFF);
        FILTERS[1] = new Filter("Heart (EKG)", FREQ_HIGH_CUTOFF_HEART, FREQ_LOW_CUTOFF_HEART);
        FILTERS[2] = new Filter("Brain (EEG)", FREQ_HIGH_CUTOFF_BRAIN, FREQ_LOW_CUTOFF_BRAIN);
        FILTERS[3] = new Filter("Plant", Filter.FREQ_NO_CUT_OFF, FREQ_LOW_CUTOFF_PLANT);
    }

    private final MaterialDialog dialog;

    public interface FilterSelectionListener {
        void onFilterSelected(@NonNull Filter filter);
    }

    private final FilterSelectionListener listener;

    private Filter selectedFilter;

    public FilterSettingsDialog(@NonNull Context context, @Nullable Filter selectedFilter,
        @Nullable FilterSelectionListener listener) {
        this.listener = listener;
        this.selectedFilter = selectedFilter;

        AmModulationFilterAdapter adapter = new AmModulationFilterAdapter(context);
        dialog = new MaterialDialog.Builder(context).adapter(adapter, new LinearLayoutManager(context)).build();
    }

    /**
     * Shows the filter settings dialog.
     */
    public void show() {
        dialog.show();
    }

    /**
     * Dismisses the filter settings dialog.
     */
    public void dismiss() {
        dialog.dismiss();
    }

    class AmModulationFilterAdapter extends RecyclerView.Adapter<AmModulationFilterAdapter.FilterViewHolder> {

        private static final int VIEW_TYPE_FILTER = 0;
        private static final int VIEW_TYPE_CUSTOM = 1;
        private static final int POSITION_CUSTOM_FILTER = 4;
        private static final String TEXT_CUSTOM_FILTER = "Custom filter";

        private final LayoutInflater inflater;

        AmModulationFilterAdapter(@NonNull Context context) {
            this.inflater = LayoutInflater.from(context);
        }

        @Override public int getItemViewType(int position) {
            return position == POSITION_CUSTOM_FILTER ? VIEW_TYPE_CUSTOM : VIEW_TYPE_FILTER;
        }

        @Override public FilterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FilterViewHolder(inflater.inflate(R.layout.item_filter, parent, false),
                viewType == VIEW_TYPE_CUSTOM);
        }

        @Override public void onBindViewHolder(FilterViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            if (viewType == VIEW_TYPE_FILTER) {
                holder.setFilter(FILTERS[position]);
            } else {
                holder.setText(TEXT_CUSTOM_FILTER);
            }
        }

        // logarithmic scale y=exp(log(minx)+(x-minxx)*(log(maxx) - log(minx))/(maxx-minxx))

        @Override public int getItemCount() {
            return FILTERS.length + 1;
        }

        final class FilterViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.tv_filter_name) TextView tvFilterName;
            @BindColor(R.color.orange) @ColorInt int selectedColor;

            Filter filter;

            FilterViewHolder(View view, final boolean customFilter) {
                super(view);
                ButterKnife.bind(this, view);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        if (!customFilter) {
                            if (listener != null) listener.onFilterSelected(filter);
                        } else {

                        }

                        dialog.dismiss();
                    }
                });
            }

            public void setFilter(@NonNull Filter filter) {
                this.filter = filter;

                tvFilterName.setText(filter.getName());
                tvFilterName.setBackgroundColor(
                    ObjectUtils.equals(filter, selectedFilter) ? selectedColor : Color.TRANSPARENT);
            }

            public void setText(@NonNull String text) {
                tvFilterName.setText(text);
            }
        }
    }
}
