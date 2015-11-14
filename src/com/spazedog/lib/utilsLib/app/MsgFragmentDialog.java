/*
 * This file is part of the UtilsLib Project: https://github.com/spazedog/utils-lib
 *
 * Copyright (c) 2015 Daniel Bergløv
 *
 * UtilsLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * UtilsLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with UtilsLib. If not, see <http://www.gnu.org/licenses/>
 */

package com.spazedog.lib.utilsLib.app;


import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.spazedog.lib.utilsLib.HashBundle;
import com.spazedog.lib.utilsLib.R;
import com.spazedog.lib.utilsLib.app.logic.ActivityConnector;
import com.spazedog.lib.utilsLib.app.logic.FragmentConnector;
import com.spazedog.lib.utilsLib.app.logic.FragmentLogic;
import com.spazedog.lib.utilsLib.app.widget.ExtendedFrameLayout;
import com.spazedog.lib.utilsLib.utils.Configuration;
import com.spazedog.lib.utilsLib.utils.Conversion;

public class MsgFragmentDialog extends DialogFragment implements FragmentConnector {

    private FragmentLogic FL_Logic;

    private float mMaxHeight = -1;
    private float mMaxWidth = -1;
    private boolean mHasDialogLayout = false;
    private ExtendedFrameLayout mFrameLayout;
    private FrameLayout mRootLayout;

    public MsgFragmentDialog() {
        setArguments(new Bundle());

        setStyle(STYLE_NO_FRAME, 0);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();

        window.setBackgroundDrawable(new ColorDrawable(0));
        window.setAttributes(layoutParams);
        window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = onCreateWindowView(inflater, container, savedInstanceState);

        if (layout == null) {
            layout = onCreateDialogView(inflater, container, savedInstanceState);

            if (layout == null) {
                return null;
            }

            mHasDialogLayout = true;
        }

        if (mMaxHeight < 0) {
            mMaxHeight = mHasDialogLayout ? 540f : 920f;
        }

        if (mMaxWidth < 0) {
            mMaxWidth = mHasDialogLayout ? 360f : 560f;
        }

        mFrameLayout = new ExtendedFrameLayout(getActivity());
        mFrameLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        mFrameLayout.addView(layout);
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            mFrameLayout.setBackground(getResources().getDrawable(R.drawable.dialog_background));
        } else {
            mFrameLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_background));
        }

        mRootLayout = new FrameLayout(getActivity());
        mRootLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mRootLayout.addView(mFrameLayout);

        return mRootLayout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateLayout();
    }

    private void updateLayout() {
        if (mFrameLayout != null) {
            boolean inLandscape = Configuration.inLandscape();
            float displayHeight = Configuration.getDisplayHeight();
            float displayWidth = Configuration.getDisplayWidth();
            float displaySW = Configuration.getDisplaySW();
            float displayLW = Configuration.getDisplayLW();
            float spacingHeight = 0f;
            float spacingWidth = 0f;

            if (inLandscape && displayLW >= 600) {
                spacingHeight = displaySW >= 600 ? 0.95f : 0.985f;
                spacingWidth = mHasDialogLayout ? 0.75f : 0.85f;

            } else if (!inLandscape && displayLW >= 600) {
                spacingHeight = mHasDialogLayout ? 0.80f : 0.90f;

                if (displaySW >= 340) {
                    spacingWidth = mHasDialogLayout ? 0.87f : 0.97f;

                } else {
                    spacingWidth = mHasDialogLayout ? 0.95f : 0.985f;
                }

            } else if (inLandscape) {
                spacingHeight = 0.985f;
                spacingWidth = mHasDialogLayout ? 0.95f : 0.985f;

            } else {
                spacingHeight = mHasDialogLayout ? 0.95f : 0.985f;
                spacingWidth = mHasDialogLayout ? 0.95f : 0.985f;
            }

            float height = displayHeight * spacingHeight;
            float width = displayWidth * spacingWidth;

            if (height > mMaxHeight) {
                float newSpacingHeight = mMaxHeight / displayHeight;

                if (newSpacingHeight < spacingHeight) {
                    spacingHeight = newSpacingHeight;
                }
            }

            if (width > mMaxWidth) {
                float newSpacingWidth = mMaxWidth / displayWidth;

                if (newSpacingWidth < spacingWidth) {
                    spacingWidth = newSpacingWidth;
                }
            }

            int paddingHeight = Conversion.dipToPixels( ((displayHeight * (1f - spacingHeight)) / 2) );
            int paddingWidth = Conversion.dipToPixels( ((displayWidth * (1f - spacingWidth)) / 2) );

            mFrameLayout.setMaxHeight(Conversion.dipToPixels(displayHeight));
            mFrameLayout.setTotalWidth(Conversion.dipToPixels(displayWidth));

            mRootLayout.setPadding(paddingWidth, paddingHeight, paddingWidth, paddingHeight);
        }
    }

    public void setMaxSize(float maxWidth, float maxHeight) {
        mMaxHeight = maxHeight;
        mMaxWidth = maxWidth;

        updateLayout();
    }

    public View onCreateDialogView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }

    public View onCreateWindowView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }

    @Override
    public void onReceiveMessage(int type, HashBundle data, boolean isSticky) {}

    @Override
    public final ActivityConnector getParentConnector() {
        Activity activity = getActivity();

        if (activity instanceof ActivityConnector) {
            return (ActivityConnector) activity;
        }

        return null;
    }

    @Override
    public final void sendMessage(int type, String key, Object value) {
        FL_Logic.sendMessage(type, new HashBundle(key, value), false, 0);
    }

    @Override
    public final void sendMessage(int type, String key, Object value, boolean sticky) {
        FL_Logic.sendMessage(type, new HashBundle(key, value), sticky, 0);
    }

    @Override
    public final void sendMessage(int type, String key, Object value, boolean sticky, int event) {
        FL_Logic.sendMessage(type, new HashBundle(key, value), sticky, event);
    }

    @Override
    public final void sendMessage(int type, HashBundle data, boolean sticky) {
        FL_Logic.sendMessage(type, data, false, 0);
    }

    @Override
    public final void sendMessage(int type, HashBundle data, boolean sticky, int event) {
        FL_Logic.sendMessage(type, data, false, event);
    }

    @Override
    public void onAttach(Activity activity) {
        FL_Logic = new FragmentLogic(this);
        FL_Logic.onAttach();
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        FL_Logic.onDetach();
        FL_Logic = null;
    }

    @Override
    public void onDestroyView() {
		/*
		 * Fix for the compatibility library issue: http://code.google.com/p/android/issues/detail?id=17423
		 */
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }

        super.onDestroyView();
    }
}
