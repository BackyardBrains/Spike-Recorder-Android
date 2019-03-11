package com.backyardbrains.view;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import butterknife.ButterKnife;
import com.backyardbrains.R;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ChannelSettingsView extends ConstraintLayout {

    public ChannelSettingsView(Context context) {
        this(context, null);
    }

    public ChannelSettingsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChannelSettingsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_channel_settings, this);
        ButterKnife.bind(this);

        setupUI();
    }

    private void setupUI() {

    }
}
