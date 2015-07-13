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

package com.spazedog.lib.utilsLib.internal;

import android.os.Parcel;
import android.os.Parcelable;

import com.spazedog.lib.utilsLib.HashBundle;
import com.spazedog.lib.utilsLib.JSONParcel;
import com.spazedog.lib.utilsLib.JSONParcel.JSONException;
import com.spazedog.lib.utilsLib.JSONParcelable;

/**
 * @hide
 */
public class HashBundleCreator implements Parcelable.Creator<HashBundle>, JSONParcelable.JSONCreator<HashBundle> {

    @Override
    public HashBundle createFromParcel(Parcel source) {
        return new HashBundle(source, getClass().getClassLoader());
    }

    @Override
    public HashBundle createFromJSON(JSONParcel source, ClassLoader loader) {
        try {
            return new HashBundle(source, loader);

        } catch (JSONException e) {
            throw new Error(e.getMessage(), e);
        }
    }

    @Override
    public HashBundle[] newArray(int size) {
        return new HashBundle[size];
    }
}
