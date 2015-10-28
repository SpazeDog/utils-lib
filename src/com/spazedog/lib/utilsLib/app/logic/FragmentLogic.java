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

public class FragmentLogic {

    private FragmentConnector FL_Fragment;

    public FragmentLogic(FragmentConnector connector) {
        FL_Fragment = connector;
    }

    public void onAttach() {
        ActivityConnector parent = FL_Fragment.getParentConnector();

        if (parent != null) {
            parent.onFragmentAttachment(FL_Fragment);
        }
    }

    public void onDetach() {
        ActivityConnector parent = FL_Fragment.getParentConnector();

        if (parent != null) {
            parent.onFragmentDetachment(FL_Fragment);
        }

        FL_Fragment = null;
    }

    public void sendMessage(int type, HashBundle data, boolean sticky, int event) {
        ActivityConnector parent = FL_Fragment.getParentConnector();

        if (parent != null) {
            parent.sendMessage(type, data, sticky, event);
        }
    }
}
