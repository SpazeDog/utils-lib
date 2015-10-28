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

public interface MsgBroadcastDelivery {

    /*
     * Supported Events
     */
    public static int EVENT_CREATE = 0x00000001;
    public static int EVENT_START = 0x00000003;
    public static int EVENT_RESUME = 0x00000007;
    public static int EVENT_PAUSE = 0x0000000F;
    public static int EVENT_STOP = 0x0000001F;

    /*
     * Quick send a single data set to all receivers
     */
    public void sendMessage(int type, String key, Object value);

    /*
     * Quick send a single data set to all receivers
     */
    public void sendMessage(int type, String key, Object value, boolean sticky);

    /*
     * Quick send a single data set to all receivers at a specific event
     */
    public void sendMessage(int type, String key, Object value, boolean sticky, int event);

    /*
     * Send a {@link HashBundle} to all receivers
     */
    public void sendMessage(int type, HashBundle data, boolean sticky);

    /*
     * Send a {@link HashBundle} to all receivers at a specific event
     */
    public void sendMessage(int type, HashBundle data, boolean sticky, int event);
}
