/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
 * Modifications (C) 2016 MrWasdennnoch@xda
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.lollipop.gravitybox.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.util.Locale;

import tk.wasdennnoch.lockmod.R;

public class SeekBarFloatPreference extends Preference implements OnSeekBarChangeListener, View.OnClickListener {

    private static final int RAPID_PRESS_TIMEOUT = 1;

    private int mMinimum = 0;
    private int mMaximum = 100;
    private int mInterval = 1;
    private int mFactor = 10;
    private float mDefaultValue = (float) mMinimum / mFactor;
    private boolean mMonitorBoxEnabled = false;
    private String mMonitorBoxUnit = null;

    private TextView mMonitorBox;
    private SeekBar mBar;
    private ImageButton mBtnPlus;
    private ImageButton mBtnMinus;


    private float mFloatValue;
    private float mTmpFloatValue;
    private int mValue;
    private int mTmpValue;
    private boolean mRapidlyPressing = false;
    private int mFormatCount;
    private Locale mFormatLocale = Locale.ENGLISH;
    private Handler mHandler;

    private Runnable mRapidPressTimeout = new Runnable() {
        @Override
        public void run() {
            mRapidlyPressing = false;
            setValue(mTmpFloatValue);
        }
    };

    public SeekBarFloatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (attrs != null) {
            mMinimum = attrs.getAttributeIntValue(null, "minimum", 0);
            mMaximum = attrs.getAttributeIntValue(null, "maximum", 100);
            mInterval = attrs.getAttributeIntValue(null, "interval", 1);
            mFactor = attrs.getAttributeIntValue(null, "factor", 10);
            mDefaultValue = (float) mMinimum / mFactor;
            mMonitorBoxEnabled = attrs.getAttributeBooleanValue(null, "monitorBoxEnabled", false);
            mMonitorBoxUnit = attrs.getAttributeValue(null, "monitorBoxUnit");
        }

        mHandler = new Handler();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);

        View layout = View.inflate(getContext(), R.layout.slider_preference, null);

        mMonitorBox = (TextView) layout.findViewById(R.id.monitor_box);
        mMonitorBox.setVisibility(mMonitorBoxEnabled ? View.VISIBLE : View.GONE);
        mBar = (SeekBar) layout.findViewById(R.id.seek_bar);
        mBar.setMax(mMaximum - mMinimum);
        mBar.setOnSeekBarChangeListener(this);
        mBar.setProgress(mValue - mMinimum);
        mBtnPlus = (ImageButton) layout.findViewById(R.id.btnPlus);
        mBtnPlus.setOnClickListener(this);
        mBtnMinus = (ImageButton) layout.findViewById(R.id.btnMinus);
        mBtnMinus.setOnClickListener(this);
        mFormatCount = 0;
        String factor = String.valueOf(mFactor);
        while (factor.endsWith("0")) {
            factor = factor.substring(0, factor.length() - 1);
            mFormatCount++;
        }
        setMonitorBoxText();
        return layout;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getFloat(index, mDefaultValue);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        float val = restoreValue ? getPersistedFloat(mFloatValue) : (float) defaultValue;
        setValue(val);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) return;

        float prog = (float) ((progress) / mInterval * mInterval + mMinimum) / mFactor;
        setValue(prog);
    }

    public void setValue(float progress) {
        mFloatValue = progress;
        mValue = (int) (progress * mFactor);
        if (isPersistent()) {
            persistFloat(mFloatValue);
        }
        if (mBar != null) {
            mBar.setProgress(mValue - mMinimum);
            setMonitorBoxText();
        }
    }

    private void setMonitorBoxText() {
        setMonitorBoxText(mFloatValue);
    }

    private void setMonitorBoxText(float value) {
        if (mMonitorBoxEnabled) {
            String text = String.format(mFormatLocale, "%." + mFormatCount + "f", value);
            if (mMonitorBoxUnit != null) text += mMonitorBoxUnit;
            mMonitorBox.setText(text);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        onProgressChanged(seekBar, seekBar.getProgress(), true);
    }

    @Override
    public void onClick(View v) {
        if (mRapidlyPressing) {
            mHandler.removeCallbacks(mRapidPressTimeout);
        } else {
            mRapidlyPressing = true;
            mTmpValue = mValue;
            mTmpFloatValue = mFloatValue;
        }
        mHandler.postDelayed(mRapidPressTimeout, RAPID_PRESS_TIMEOUT);

        if (v == mBtnPlus && ((mTmpValue + mInterval) <= mMaximum)) {
            mTmpValue += mInterval;
        } else if (v == mBtnMinus && ((mTmpValue - mInterval) >= mMinimum)) {
            mTmpValue -= mInterval;
        }
        mTmpFloatValue = (float) mTmpValue / mFactor;

        mBar.setProgress(mTmpValue - mMinimum);
        setMonitorBoxText(mTmpFloatValue);
    }

}