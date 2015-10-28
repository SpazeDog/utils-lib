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
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

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

    public MsgFragmentDialog() {
        setArguments(new Bundle());

        setStyle(0, R.style.App_Dialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = onCreateWindowView(inflater, container, savedInstanceState);

        if (layout != null) {
            ExtendedFrameLayout layoutWrapper = new ExtendedFrameLayout(getActivity());
            layoutWrapper.addView(layout);

            if (Configuration.getDisplaySW() < 600) {
                if (Configuration.inLandscape() && Configuration.getDisplayLW() >= 600) {
                    layoutWrapper.setMaxHeight( Conversion.dipToPixels(Configuration.getDisplayHeight() * 0.95f) );
                    layoutWrapper.setTotalWidth( Conversion.dipToPixels(Configuration.getDisplayWidth() * 0.75f) );

                } else if (Configuration.getDisplayLW() >= 600) {
                    layoutWrapper.setMaxHeight( Conversion.dipToPixels(Configuration.getDisplayHeight() * 0.90f) );
                    layoutWrapper.setTotalWidth( Conversion.dipToPixels(Configuration.getDisplayWidth() * 0.90f) );

                } else {
                    layoutWrapper.setMaxHeight( Conversion.dipToPixels(Configuration.getDisplayHeight() * 0.95f) );
                    layoutWrapper.setTotalWidth( Conversion.dipToPixels(Configuration.getDisplayWidth() * 0.95f) );
                }

            } else if (Configuration.inLandscape()) {
                layoutWrapper.setMaxHeight( Conversion.dipToPixels(Configuration.getDisplayHeight() * 0.85f) );
                layoutWrapper.setTotalWidth( Conversion.dipToPixels(Configuration.getDisplayWidth() * 0.45f) );

            } else {
                layoutWrapper.setMaxHeight( Conversion.dipToPixels(Configuration.getDisplayHeight() * 0.75f) );
                layoutWrapper.setTotalWidth( Conversion.dipToPixels(Configuration.getDisplayWidth() * 0.65f) );
            }

            return layoutWrapper;
        }

        return layout;
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
