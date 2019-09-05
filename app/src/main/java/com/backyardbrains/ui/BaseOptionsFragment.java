package com.backyardbrains.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.backyardbrains.R;
import java.util.List;

import static com.backyardbrains.utils.LogUtils.makeLogTag;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public abstract class BaseOptionsFragment extends BaseFragment {

    public static final String TAG = makeLogTag(BaseOptionsFragment.class);

    @BindView(R.id.ibtn_back) ImageButton ibtnBack;
    @BindView(R.id.tv_title) TextView tvTitle;
    @BindView(R.id.rv_options) RecyclerView rvOptions;
    @BindView(R.id.tv_info) TextView tvInfo;

    private Unbinder unbinder;

    private OptionsAdapter adapter;

    //==============================================
    //  LIFECYCLE IMPLEMENTATIONS
    //==============================================

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_base_options, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupUI(view.getContext());

        return view;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    //==============================================
    //  ABSTRACT METHODS
    //==============================================

    protected abstract String getTitle();

    protected abstract View.OnClickListener getBackClickListener();

    //==============================================
    //  PUBLIC METHODS
    //==============================================

    /**
     * Sets available options.
     */
    void setOptions(@NonNull List<OptionsAdapter.OptionItem> options,
        @Nullable OptionsAdapter.Callback callback) {
        adapter.setOptions(options, callback);
    }

    /**
     * Shows/hides info overlay. If specified {@code info} is not {@code null} overlay is visible,
     * otherwise is gone.
     */
    void showInfo(@Nullable String info) {
        if (tvInfo != null) {
            tvInfo.setText(info);
            tvInfo.setVisibility(info != null ? View.VISIBLE : View.GONE);
        }
    }

    //==============================================
    //  PRIVATE METHODS
    //==============================================

    // Initializes user interface
    private void setupUI(@NonNull Context context) {
        tvTitle.setText(getTitle());
        ibtnBack.setOnClickListener(getBackClickListener());
        adapter = new OptionsAdapter(context);
        rvOptions.setAdapter(adapter);
        rvOptions.setHasFixedSize(true);
        rvOptions.setLayoutManager(new LinearLayoutManager(context));
        rvOptions.addItemDecoration(
            new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        tvInfo.setVisibility(View.GONE);
    }

    /**
     * Adapter for listing all the available options for the currently file.
     */
    static class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.OptionViewHolder> {

        private final LayoutInflater inflater;

        private List<OptionsAdapter.OptionItem> options;
        private Callback callback;

        interface Callback {
            void onClick(int id, @NonNull String name);
        }

        OptionsAdapter(@NonNull Context context) {
            super();

            this.inflater = LayoutInflater.from(context);
        }

        void setOptions(@NonNull List<OptionItem> options, @Nullable Callback callback) {
            this.options = options;
            this.callback = callback;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new OptionViewHolder(inflater.inflate(R.layout.item_option, parent, false),
                callback);
        }

        @Override public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
            final OptionItem item = options.get(position);
            holder.setOption(item);
        }

        @Override public int getItemCount() {
            return options != null ? options.size() : 0;
        }

        static class OptionItem {

            final int id;
            final String label;
            final boolean showArrow;

            OptionItem(int id, String label, boolean showArrow) {
                this.id = id;
                this.label = label;
                this.showArrow = showArrow;
            }
        }

        static class OptionViewHolder extends RecyclerView.ViewHolder {
            @BindView(R.id.tv_option) TextView tvOption;
            @BindView(R.id.iv_option) ImageView ivOption;

            OptionItem item;

            OptionViewHolder(@NonNull View view, @Nullable final Callback callback) {
                super(view);
                ButterKnife.bind(this, view);

                view.setOnClickListener(v -> {
                    if (callback != null) callback.onClick(item.id, item.label);
                });
            }

            void setOption(@NonNull OptionItem item) {
                this.item = item;

                tvOption.setText(item.label);
                ivOption.setVisibility(item.showArrow ? View.VISIBLE : View.GONE);
            }
        }
    }
}
