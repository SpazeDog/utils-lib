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


import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class Configuration {

    /**
     * Get the height of the display
     */
    public static float getDisplayHeight() {
        return Conversion.pixelsToDip(Resources.getSystem().getDisplayMetrics().heightPixels);
    }

    /**
     * Get the width of the display
     */
    public static float getDisplayWidth() {
        return Conversion.pixelsToDip(Resources.getSystem().getDisplayMetrics().widthPixels);
    }

    /**
     * Get the smallest width of the display
     */
    public static float getDisplaySW() {
        int height = Resources.getSystem().getDisplayMetrics().heightPixels;
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;

        if (height >= width) {
            return Conversion.pixelsToDip(width);

        } else {
            return Conversion.pixelsToDip(height);
        }
    }

    /**
     * Get the highest width of the display
     */
    public static float getDisplayLW() {
        int height = Resources.getSystem().getDisplayMetrics().heightPixels;
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;

        if (height < width) {
            return Conversion.pixelsToDip(width);

        } else {
            return Conversion.pixelsToDip(height);
        }
    }

    /**
     * Check if the display is currently in Landscape mode
     */
    public static boolean inLandscape() {
        return Resources.getSystem().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * Set TypeFace for a View and all it's Sub-Views (If any)
     *
     * @param view
     *      The view
     *
     * @param typeFace
     *      The TypeFace to set
     */
    public static void setTypeFace(View view, Typeface typeFace) {
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setTypeFace(((ViewGroup) view).getChildAt(i), typeFace);
            }

        } else if (view instanceof TextView) {
            Typeface currentTf = ((TextView) view).getTypeface();
            Integer type = 0;

            if (currentTf != null) {
                type = currentTf.getStyle();
            }

            ((TextView) view).setTypeface(typeFace, type);
        }
    }
}
