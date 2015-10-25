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

package com.spazedog.lib.utilsLib.app.logic;


import com.spazedog.lib.utilsLib.HashBundle;

import java.lang.ref.WeakReference;

public class FragmentLogic {

    private WeakReference<FragmentConnector> FL_Fragment;

    public FragmentLogic(FragmentConnector connector) {
        FL_Fragment = new WeakReference<FragmentConnector>(connector);
    }

    public void onAttach() {
        FragmentConnector connector = FL_Fragment.get();

        if (connector != null) {
            ActivityConnector parent = connector.getParentConnector();

            if (parent != null) {
                parent.onFragmentAttachment(connector);
            }
        }
    }

    public void onDetach() {
        FragmentConnector connector = FL_Fragment.get();

        if (connector != null) {
            ActivityConnector parent = connector.getParentConnector();

            if (parent != null) {
                parent.onFragmentDetachment(connector);
            }
        }
    }

    public void sendMessage(int type, HashBundle data, boolean sticky) {
        FragmentConnector connector = FL_Fragment.get();

        if (connector != null) {
            ActivityConnector parent = connector.getParentConnector();

            if (parent != null) {
                parent.sendMessage(type, data, sticky);
            }
        }
    }
}
