package ru.ifmo.android_2016.irc.ui.span;

import android.os.Parcel;
import android.support.v4.graphics.ColorUtils;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;

/**
 * Created by ghost on 12/4/2016.
 */

public final class ChangeableForegroundColorSpan extends ForegroundColorSpan {
    private static float lightness = 180.f / 256;
    private static boolean changed = true;

    private boolean toColor = true;
    private int correctedColor = 0;

    private Integer getCorrectedColor(int color) {
        if (changed) {
            float[] hls = new float[3];
            ColorUtils.colorToHSL(color, hls);
            hls[2] = lightness;
            changed = false;
            this.correctedColor = ColorUtils.HSLToColor(hls);
        }
        return this.correctedColor;
    }

    public ChangeableForegroundColorSpan(Integer color) {
        super(color != null ? color : 0);
        if (color == null) {
            toColor = false;
        } else {
            this.correctedColor = color;
        }
    }

    public ChangeableForegroundColorSpan(int color) {
        super(color);
        this.correctedColor = color;
    }

    @Override
    public int getForegroundColor() {
        return getCorrectedColor(super.getForegroundColor());
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        if (toColor) ds.setColor(getForegroundColor());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    protected ChangeableForegroundColorSpan(Parcel in) {
        super(in);
    }

    public static final Creator<ChangeableForegroundColorSpan> CREATOR = new Creator<ChangeableForegroundColorSpan>() {
        @Override
        public ChangeableForegroundColorSpan createFromParcel(Parcel source) {
            return new ChangeableForegroundColorSpan(source);
        }

        @Override
        public ChangeableForegroundColorSpan[] newArray(int size) {
            return new ChangeableForegroundColorSpan[size];
        }
    };

    public static void setLightness(float lightness) {
        ChangeableForegroundColorSpan.lightness = lightness;
        changed = true;
    }
}
