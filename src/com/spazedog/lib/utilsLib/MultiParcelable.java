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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
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

    public interface MultiClassLoaderCreator<T extends MultiParcelable> extends MultiCreator<T> {
        public T createFromParcel(Parcel source, ClassLoader classLoader);
    }

    /**
     * Helper class that can be used to wrap {@link Parcel#writeValue(Object)} and {@link Parcel#readValue(ClassLoader)}
     * in order to fix some of the issues with {@link Parcel}
     */
    public static class ParcelHelper {

        protected static Method CreatorMethod;

        protected static int PARCEL_VAR_PARCELABLE;
        protected static int PARCEL_VAR_PARCELABLEARRAY;
        protected static int PARCEL_VAR_LIST;
        protected static int PARCEL_VAR_MAP;
        protected static final int PARCEL_VAR_SET = -512;

        static {
            try {
                CreatorMethod = Parcel.class.getDeclaredMethod("readParcelableCreator", ClassLoader.class);

                Field parcelableField = Parcel.class.getDeclaredField("VAL_PARCELABLE");
                parcelableField.setAccessible(true);

                Field parcelableArrayField = Parcel.class.getDeclaredField("VAL_PARCELABLEARRAY");
                parcelableArrayField.setAccessible(true);

                Field listField = Parcel.class.getDeclaredField("VAL_LIST");
                listField.setAccessible(true);

                Field mapField = Parcel.class.getDeclaredField("VAL_MAP");
                mapField.setAccessible(true);

                PARCEL_VAR_PARCELABLE = (Integer) parcelableField.get(null);
                PARCEL_VAR_PARCELABLEARRAY = (Integer) parcelableArrayField.get(null);
                PARCEL_VAR_LIST = (Integer) listField.get(null);
                PARCEL_VAR_MAP = (Integer) mapField.get(null);

            } catch (Throwable e) {
                PARCEL_VAR_PARCELABLE = 4;
                PARCEL_VAR_PARCELABLEARRAY = 16;
                PARCEL_VAR_LIST = 2;
                PARCEL_VAR_MAP = 11;
            }
        }

        /**
         * @see #unparcelData(Parcel, ClassLoader)
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

            } else if (value != null && value instanceof List) {
                dest.writeInt(PARCEL_VAR_LIST);
                dest.writeInt(((List<Object>) value).size());

                for (Object entry : (List<Object>) value) {
                    parcelData(entry, dest, flags);
                }

            } else if (value != null && value instanceof Map) {
                dest.writeInt(PARCEL_VAR_MAP);
                dest.writeInt(((Map<Object, Object>) value).size());

                Set<Map.Entry<Object, Object>> entries = ((Map<Object, Object>) value).entrySet();

                for (Map.Entry<Object, Object> entry : entries) {
                    parcelData(entry.getKey(), dest, flags);
                    parcelData(entry.getValue(), dest, flags);
                }

            } else {
                dest.writeValue(value);
            }
        }

        /**
         * This method makes sure that Parcelable objects are handled as the very first,
         * unlike Android's Parcel that takes Maps, Lists etc first no mater if you use a
         * Parcelable version. It also makes sure that any Parcelable objects that include
         * the {@link MultiClassLoaderCreator} interface is handed the ClassLoader used to unparcel the object.
         * Last it handles Maps, Lists and Sets to make sure that containing Parcelable objects are
         * handled the correct way. In other words, this method fixes a lot of Parcel issues.
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
            Object value = null;

            if (type == PARCEL_VAR_PARCELABLE) {
                pos = source.dataPosition();

                try {
                    Parcelable.Creator<? extends Parcelable> creator = (Creator<? extends Parcelable>) CreatorMethod.invoke(source, loader);

                    if (creator instanceof MultiClassLoaderCreator) {
                        value = ((MultiClassLoaderCreator) creator).createFromParcel(source, loader);

                    } else {
                        value = creator.createFromParcel(source);
                    }

                } catch (Throwable e) {
                    source.setDataPosition(pos);
                    value = source.readParcelable(loader);
                }

            } else if (type == PARCEL_VAR_PARCELABLEARRAY) {
                int size = source.readInt();

                if (size >= 0) {
                    Parcelable[] parray = new Parcelable[size];

                    for (int i=0; i < size; i++) {
                        pos = source.dataPosition();

                        try {
                            Parcelable.Creator<? extends Parcelable> creator = (Creator<? extends Parcelable>) CreatorMethod.invoke(source, loader);

                            if (creator instanceof MultiClassLoaderCreator) {
                                parray[i] = ((MultiClassLoaderCreator) creator).createFromParcel(source, loader);

                            } else {
                                parray[i] = creator.createFromParcel(source);
                            }

                        } catch (Throwable e) {
                            source.setDataPosition(pos);
                            parray[i] = source.readParcelable(loader);
                        }
                    }

                    value = parray;
                }

            } else if (type == PARCEL_VAR_SET) {
                Set<Object> set = new HashSet<Object>();
                int length = source.readInt();

                for (int i=0; i < length; i++) {
                    set.add(unparcelData(source, loader));
                }

                value = set;

            } else if (type == PARCEL_VAR_LIST) {
                List<Object> list = new ArrayList<Object>();
                int length = source.readInt();

                for (int i=0; i < length; i++) {
                    list.add(unparcelData(source, loader));
                }

                value = list;

            } else if (type == PARCEL_VAR_MAP) {
                Map<Object, Object> map = new HashMap<Object, Object>();
                int length = source.readInt();

                for (int i=0; i < length; i++) {
                    Object key = unparcelData(source, loader);
                    map.put(key, unparcelData(source, loader));
                }

                value = map;

            } else {
                source.setDataPosition(pos);
                value = source.readValue(loader);
            }

            return value;
        }
    }
}
