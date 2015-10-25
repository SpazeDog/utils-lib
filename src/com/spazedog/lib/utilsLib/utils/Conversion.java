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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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

    public static Bitmap drawableToBitmap(Drawable drawable) {
        return drawableToBitmap(drawable, 0f, 0f);
    }

    public static Bitmap drawableToBitmap(Drawable drawable, float width, float height) {
        if (drawable != null) {
            int canvasWidth = 0;
            int canvasHeight = 0;

            if (width <= 0) {
                canvasWidth = drawable.getIntrinsicWidth();

            } else {
                canvasWidth = dipToPixels(width);
            }

            if (height <= 0) {
                canvasHeight = drawable.getIntrinsicHeight();

            } else if (width != height) {
                canvasHeight = dipToPixels(height);

            } else {
                canvasHeight = canvasWidth;
            }

            Bitmap bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            return bitmap;
        }

        return null;
    }
}

