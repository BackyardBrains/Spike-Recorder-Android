package com.backyardbrains.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.constraintlayout.widget.ConstraintLayout;
import butterknife.Action;
import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.Setter;
import butterknife.ViewCollections;
import com.backyardbrains.R;
import com.backyardbrains.drawing.Colors;
import java.util.Arrays;
import java.util.List;

/**
 * @author Tihomir Leka <tihomir at backyardbrains.com>
 */
public class ChannelSettingsView extends ConstraintLayout {

    private static final Setter<View, OnClickListener> INIT_CHANNEL_COLORS = (view, value, index) -> {
        int channel = (index - 1) % Colors.CHANNEL_COLORS.length;
        view.setTag(index == 0 ? Colors.BLACK : Colors.CHANNEL_COLORS[channel]);
        view.setOnClickListener(value);
    };

    private static final Action<ImageButton> RESET_CHANNEL_COLORS_SELECTION =
        (view, index) -> view.setImageDrawable(null);

    @BindView(R.id.tv_channel_name) TextView tvChannelName;
    @BindView(R.id.hsvChannelColors) HorizontalScrollView hsvChannelColors;
    @BindViews({
        R.id.ibtn_channel_0, R.id.ibtn_channel_1, R.id.ibtn_channel_2, R.id.ibtn_channel_3, R.id.ibtn_channel_4,
        R.id.ibtn_channel_5, R.id.ibtn_channel_6
    }) List<ImageButton> ibtnChannelColors;

    /**
     * Interface definition for a callback to be invoked when channel color is changed.
     */
    public interface OnColorChangeListener {
        /**
         * Listener that is invoked when color is changed.
         *
         * @param prevColor Previously selected color.
         * @param newColor Newly selected color.
         * @param channelIndex Index of the channel.
         */
        void onColorChanged(@NonNull @Size(4) float[] prevColor, @NonNull @Size(4) float[] newColor, int channelIndex);
    }

    private OnColorChangeListener listener;

    private int channelIndex;
    private @Size(4) float[] prevColor;

    private final OnClickListener channelColorOnClickListener = v -> {
        final @Size(4) float[] newColor = (float[]) v.getTag();
        if (Arrays.equals(prevColor, newColor)) return;

        // remove check mark from all colors
        ViewCollections.run(ibtnChannelColors, RESET_CHANNEL_COLORS_SELECTION);
        // select the clicked one
        ((ImageButton) v).setImageResource(R.drawable.ic_check_gray_dark_24dp);

        if (listener != null) listener.onColorChanged(prevColor, newColor, channelIndex);
        prevColor = newColor;
    };

    private final Setter<ImageButton, float[]> setChannelColorSelection = (view, value, index) -> {
        float[] compareColor = index == 0 ? Colors.BLACK : Colors.CHANNEL_COLORS[index - 1];
        if (Arrays.equals(compareColor, value)) {
            view.setImageResource(R.drawable.ic_check_gray_dark_24dp);
            // scroll far right if selected color is in the second half of the row
            hsvChannelColors.postDelayed(() -> hsvChannelColors.fullScroll(
                index > Colors.CHANNEL_COLORS.length / 2 ? View.FOCUS_RIGHT : View.FOCUS_LEFT), 100);
        } else {
            view.setImageDrawable(null);
        }
    };

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

    /**
     * Registers a callback to be invoked when channel color is changed.
     *
     * @param listener The callback that will be run. This value may be {@code null}.
     */
    public void setOnColorChangeListener(@Nullable OnColorChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Sets index of this channel.
     */
    public void setChannelIndex(int index) {
        channelIndex = index;

        tvChannelName.setText(String.format(getContext().getString(R.string.template_channel_name), (index + 1)));
    }

    /**
     * Checks circle with specified {@code color} and un-checks all the other circles
     */
    public void setChannelColor(@Size(4) float[] color) {
        ViewCollections.set(ibtnChannelColors, setChannelColorSelection, color);

        prevColor = color;
    }

    // Inflates view layout and setup UI
    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_channel_settings, this);
        ButterKnife.bind(this);

        // init channel colors
        ViewCollections.set(ibtnChannelColors, INIT_CHANNEL_COLORS, channelColorOnClickListener);
    }
}
