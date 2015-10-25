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
import android.support.v4.app.Fragment;

import com.spazedog.lib.utilsLib.HashBundle;
import com.spazedog.lib.utilsLib.app.logic.ActivityConnector;
import com.spazedog.lib.utilsLib.app.logic.FragmentConnector;
import com.spazedog.lib.utilsLib.app.logic.FragmentLogic;

public class MsgFragment extends Fragment implements FragmentConnector {

    private FragmentLogic FL_Logic = new FragmentLogic(this);

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
        FL_Logic.sendMessage(type, new HashBundle(key, value), false);
    }

    @Override
    public final void sendMessage(int type, String key, Object value, boolean sticky) {
        FL_Logic.sendMessage(type, new HashBundle(key, value), sticky);
    }

    @Override
    public final void sendMessage(int type, HashBundle data, boolean sticky) {
        FL_Logic.sendMessage(type, data, false);
    }

    @Override
    public void onAttach(Activity activity) {
        FL_Logic.onAttach();
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        FL_Logic.onDetach();
    }
}
