package com.backyardbrains.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.backyardbrains.R;
import com.backyardbrains.utils.ViewUtils;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class EmptyView extends FrameLayout {

    @BindView(R.id.pb_empty_view_progress) ProgressBar pbLoad;
    @BindView(R.id.tv_empty_view_text) TextView tvTagline;

    public EmptyView(Context context) {
        this(context, null);
    }

    public EmptyView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmptyView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EmptyView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override public void setVisibility(int visibility) {
        setClickable(visibility != View.GONE);
        super.setVisibility(visibility);
    }

    @SuppressLint("ObsoleteSdkInt") private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_empty_view, this);

        ButterKnife.bind(this);

        // for pre-21 SDK we need to tint the progress bar programmatically (post-21 SDK will do it through styles)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ViewUtils.tintDrawable(pbLoad.getIndeterminateDrawable(),
                ContextCompat.getColor(getContext(), R.color.orange));
        }

        setLoading();
    }

    /**
     * Sets empty view tagline.
     */
    public void setTagline(CharSequence s) {
        tvTagline.setText(s);
    }

    /**
     * Shows progress bar and hides the tagline.
     */
    public void setLoading() {
        pbLoad.setVisibility(VISIBLE);
        tvTagline.setVisibility(GONE);
    }

    /**
     * Hides progress bar and shows the tagline.
     */
    public void setEmpty() {
        pbLoad.setVisibility(GONE);
        tvTagline.setVisibility(VISIBLE);
    }
}
