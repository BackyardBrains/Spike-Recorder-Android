package com.backyardbrains.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

/**
 * Subclass of {@link RecyclerView} that enables creator to set an empty view that will be displayed when there are no
 * items to show.
 *
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class BybEmptyRecyclerView extends RecyclerView {

    private View emptyView;

    private final AdapterDataObserver observer = new AdapterDataObserver() {
        @Override public void onChanged() {
            checkIfEmpty();
        }

        @Override public void onItemRangeInserted(int positionStart, int itemCount) {
            checkIfEmpty();
        }

        @Override public void onItemRangeRemoved(int positionStart, int itemCount) {
            checkIfEmpty();
        }
    };

    public BybEmptyRecyclerView(Context context) {
        this(context, null);
    }

    public BybEmptyRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BybEmptyRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    void checkIfEmpty() {
        if (emptyView != null && getAdapter() != null) {
            final boolean emptyViewVisible = getAdapter().getItemCount() == 0;
            emptyView.setVisibility(emptyViewVisible ? VISIBLE : GONE);
            setVisibility(emptyViewVisible ? GONE : VISIBLE);
        }
    }

    @Override public void setAdapter(Adapter adapter) {
        final Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) oldAdapter.unregisterAdapterDataObserver(observer);
        super.setAdapter(adapter);
        if (adapter != null) adapter.registerAdapterDataObserver(observer);

        checkIfEmpty();
    }

    /**
     * Sets the empty view for this recycler view.
     */
    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
        checkIfEmpty();
    }

    /**
     * Checks whether empty view for this recycler view is set
     */
    public boolean isEmptyViewSet() {
        return emptyView != null;
    }
}