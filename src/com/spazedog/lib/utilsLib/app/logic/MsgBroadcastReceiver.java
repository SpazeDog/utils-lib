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

public interface MsgBroadcastReceiver {

    /*
     * Message types sent by the Activities
     */
    public static final int MSG_ACTIVITY_RESULT = -1;
    public static final int MSG_BACKSTACK_CHANGE = -2;
    public static final int MSG_FRAGMENT_ATTACHMENT = -3;
    public static final int MSG_FRAGMENT_DETACHMENT = -4;

    /*
     * Method that should be overwritten by activity or fragment that needs to
     * receiver internal messages.
     */
    public void onReceiveMessage(int type, HashBundle data, boolean isSticky);
}
