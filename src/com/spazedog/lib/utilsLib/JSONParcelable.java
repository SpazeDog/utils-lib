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

package com.spazedog.lib.utilsLib;

import com.spazedog.lib.utilsLib.JSONParcel.JSONException;

/**
 * This interface should be added to any class that wishes to be used
 * with {@link JSONParcel}
 */
public interface JSONParcelable {

    /**
     * Called on a {@link JSONParcelable} object when it is
     * parsed to {@link JSONParcel#writeJSONParcelable(JSONParcelable)}
     *
     * @param dest
     *     The {@link JSONParcelable} instance that this object was added to
     */
    public void writeToJSON(JSONParcel dest) throws JSONException;

    /**
     * This interface should be implemeted in a class instance and stored
     * on a static field named <code>CREATOR</code> within the {@link JSONParcelable} object.
     * The name of the field is used to add compatibility with Android's {@link android.os.Parcelable}
     *
     * @param <T>
     *     The {@link Class} type that this creator creates
     */
    public interface JSONCreator<T extends JSONParcelable> {
        /**
         * Called by {@link JSONParcel#readJSONParcelable(ClassLoader)} when reading a
         * JSON {@link String} containing data from an instance of this {@link JSONParcelable} object
         *
         * @param source
         *      The {@link JSONParcel} calling this method
         *
         * @param loader
         *      The ClassLoader that this object is being created in
         */
        public T createFromJSON(JSONParcel source, ClassLoader loader) throws JSONException;

        /**
         * This is not at the moment being used by {@link JSONParcel}.
         * It is added for compatibillity with {@link android.os.Parcelable} and
         * for future usage. This method should return an array of the type <code>T</code>
         *
         * @param size
         *      The size of the array being created
         */
        public T[] newArray(int size);
    }
}
