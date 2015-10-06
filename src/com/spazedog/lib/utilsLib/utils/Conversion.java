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

package com.spazedog.lib.utilsLib.utils;


import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.support.annotation.RawRes;

public class Conversion {

    /**
     * Convert dip/dp to pixels
     *
     * @param dips
     *      The Dip value to convert
     */
    public static int dipToPixels(float dips) {
        return (int) (dips * Resources.getSystem().getDisplayMetrics().density + 0.5f);
    }

    /**
     * Convert pixels to dip/dp
     *
     * @param pixels
     *      The pixels value to convert
     */
    public static float pixelsToDip(int pixels) {
        return (float) (pixels / Resources.getSystem().getDisplayMetrics().density + 0.5f);
    }

    /**
     * @see #attrToRes(Theme, int)
     */
    public static @RawRes int attrToRes(Context context, @AttrRes int attr) {
        return attrToRes(context.getTheme(), attr);
    }

    /**
     * Convert an Attribute id into a Resource id
     *
     * @param theme
     *      The Theme which resources to look through
     *
     * @param attr
     *      The Attribute id
     */
    public static @RawRes int attrToRes(Theme theme, @AttrRes int attr) {
        TypedArray a = theme.obtainStyledAttributes(new int[]{attr});
        int res = a.getResourceId(0, 0);
        a.recycle();

        return res;
    }
}

