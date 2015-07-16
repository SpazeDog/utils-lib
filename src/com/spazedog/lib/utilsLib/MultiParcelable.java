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


import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple convenience interface for adding both {@link JSONParcelable} and {@link Parcelable}
 * to a class
 */
public interface MultiParcelable extends JSONParcelable, Parcelable {

    public interface MultiCreator<T extends MultiParcelable> extends JSONParcelable.JSONCreator<T>, Parcelable.Creator<T> {

    }

    /**
     * Helper class that can be used to wrap {@link Parcel#writeValue(Object)} and {@link Parcel#readValue(ClassLoader)}
     * in order to fix some of the issues with {@link Parcel}
     */
    public static class ParcelHelper {

        protected static int PARCEL_VAR_PARCELABLE;
        protected static int PARCEL_VAR_PARCELABLEARRAY;
        protected static final int PARCEL_VAR_SET = -512;

        static {
            try {
                Field parcelableField = Parcel.class.getDeclaredField("VAL_PARCELABLE");
                parcelableField.setAccessible(true);

                Field parcelableArrayField = Parcel.class.getDeclaredField("VAL_PARCELABLEARRAY");
                parcelableArrayField.setAccessible(true);

                PARCEL_VAR_PARCELABLE = (Integer) parcelableField.get(null);
                PARCEL_VAR_PARCELABLEARRAY = (Integer) parcelableArrayField.get(null);

            } catch (Throwable e) {
                PARCEL_VAR_PARCELABLE = 4;
                PARCEL_VAR_PARCELABLEARRAY = 16;
            }
        }

        /**
         * Fix issue in Android's {@link Parcel} by making sure that {@link Parcelable} objects
         * are handled as {@link Parcelable} objects before checking for {@link Map}, {@link List} etc. <br /><br />
         *
         * It also adds support for parceling {@link Set} objects
         *
         * @param value
         *      The value to parcel
         *
         * @param dest
         *      The {@link Parcel} object that the value should be written to
         *
         * @param flags
         */
        public static void parcelData(Object value, Parcel dest, int flags) {
            if (value != null && value instanceof Parcelable) {
                dest.writeInt(PARCEL_VAR_PARCELABLE);
                dest.writeParcelable((Parcelable) value, flags);

            } else if (value != null && value instanceof Parcelable[]) {
                dest.writeInt(PARCEL_VAR_PARCELABLEARRAY);
                dest.writeParcelableArray((Parcelable[]) value, flags);

            } else if (value != null && value instanceof Set) {
                dest.writeInt(PARCEL_VAR_SET);
                dest.writeInt(((Set<Object>) value).size());

                for (Object entry : (Set<Object>) value) {
                    parcelData(entry, dest, flags);
                }

            } else {
                dest.writeValue(value);
            }
        }

        /**
         * Check for {@link Set} objects before parsing it to {@link Parcel#readValue(ClassLoader)}
         *
         * @param source
         *      The {@link Parcel} object to read from
         *
         * @param loader
         *      The {@link ClassLoader} that should be used to load value classes
         */
        public static Object unparcelData(Parcel source, ClassLoader loader) {
            int pos = source.dataPosition();
            int type = source.readInt();

            switch (type) {
                case PARCEL_VAR_SET:
                    Set<Object> set = new HashSet<Object>();
                    int length = source.readInt();

                    for (int i=0; i < length; i++) {
                        set.add(unparcelData(source, loader));
                    }

                    return set;

                default:
                    source.setDataPosition(pos);
                    return source.readValue(loader);
            }
        }
    }
}
