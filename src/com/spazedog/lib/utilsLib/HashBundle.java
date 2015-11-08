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

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.spazedog.lib.utilsLib.JSONParcel.JSONException;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HashBundle implements MultiParcelable, Cloneable {

    public static final int PARCEL_ANDROID = 1;
    public static final int PARCEL_JSON = 2;

    protected final Object mParcelLock = new Object();

    protected static Field oBundleMapField;
    protected static Parcel oEmptyParcel;
    protected static JSONParcel oEmptyJSONParcel;

    protected Map<String, Object> mMap;
    protected Parcel mParcel;
    protected JSONParcel mJSONParcel;
    protected ClassLoader mClassLoader = null;
    protected int mDataSize = 0;
    protected boolean mIsParceled = false;

    public static MultiCreator<HashBundle> CREATOR = new MultiCreator<HashBundle>() {

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
    };

    static {
        oEmptyParcel = Parcel.obtain();
        oEmptyJSONParcel = new JSONParcel();

        try {
            Class<?> bundle = null;

            try {
                bundle = Class.forName("android.os.BaseBundle");

            } catch (ClassNotFoundException e) {
                bundle = Bundle.class;
            }

            oBundleMapField = bundle.getDeclaredField("mMap");
            oBundleMapField.setAccessible(true);

        } catch (Throwable e) {
            oBundleMapField = null;
        }
    }

    protected HashMap<String, Object> createNewMap() {
        return new HashMap<String, Object>();
    }

    /**
     * Create a new empty instance
     */
    public HashBundle() {
        mMap = createNewMap();
    }

    /**
     * Create a new bundle and add a single entry
     *
     * @param key
     *      The key for the entry
     *
     * @param value
     *      The value for the entry
     */
    public HashBundle(String key, Object value) {
        this();
        put(key, value);
    }

    /**
     * Create a new instance and copy {@link Parcel} data to it.
     * This will not unparcel the data, it will only copy the data to a new {@link Parcel}.
     * Unparceling will not be done until data is being accessed.
     *
     * @param source
     *      The {@link Parcel} data
     *
     * @param loader
     *      A {@link ClassLoader} that will be used when unparceling the data
     */
    public HashBundle(Parcel source, ClassLoader loader) {
        mClassLoader = loader;
        mIsParceled = true;

        int size = source.readInt();                // The size of the parcel data belonging to the HashBundle

        if (size > 0) {
            /*
             * We do not waste resources by unparceling everything just to get the size when someone uses #size()
             * Instead we cache the data size from the parcel data and use that when #size() is called while parceled
             */
            mDataSize = source.readInt();           // The size of the HashBundle

            int offset = source.dataPosition();

            // Get past the data beloning to this instance
            source.setDataPosition(offset + size);

            mParcel = Parcel.obtain();
            mParcel.setDataPosition(0);
            mParcel.writeInt(mDataSize);
            mParcel.appendFrom(source, offset, size);
            mParcel.setDataPosition(0);

        } else {
            mParcel = oEmptyParcel;
        }
    }

    /**
     * Create a new instance and copy {@link JSONParcel} data to it.
     * This will not unparcel the data, it will only copy the data to a new {@link JSONParcel}.
     * Unparceling will not be done until data is being accessed.
     *
     * @param source
     *      The {@link JSONParcel} data
     *
     * @param loader
     *      A {@link ClassLoader} that will be used when unparceling the data
     */
    public HashBundle(JSONParcel source, ClassLoader loader) throws JSONException {
        mClassLoader = loader;
        mIsParceled = true;

        int size = source.readInt();                // The size of the parcel data belonging to the HashBundle

        if (size > 0) {
            mDataSize = source.readInt();           // The size of the HashBundle
            int offset = source.getDataPosition();

            // Get past the data beloning to this instance
            source.setDataPosition(offset + size);

            mJSONParcel = new JSONParcel();
            mJSONParcel.writeInt(mDataSize);
            mJSONParcel.appendFrom(source, offset, size);
            mJSONParcel.setDataPosition(0);

        } else {
            mJSONParcel = oEmptyJSONParcel;
        }
    }

    /**
     * Create a new instance and wrap it around a {@link Map}.<br /><br />
     *
     * WARNING: This will not copy the content from the {@link Map}.
     * Any changes to the {@link Map} will affect this instance.
     *
     * @param map
     */
    public HashBundle(Map map) {
        mMap = (Map<String, Object>) map;
    }

    /**
     * Create a new instance and wrap it around a {@link Bundle}.<br /><br />
     *
     * WARNING: This will in most cases not copy the content from the {@link Bundle}.
     * Any changes to the {@link Bundle} could affect this instance.
     *
     * @param bundle
     */
    public HashBundle(Bundle bundle) {
        // Unparcel
        bundle.size();

        try {
            mMap = (Map<String, Object>) oBundleMapField.get(bundle);

        } catch (Throwable e) {
            mMap = createNewMap();
            Set<String> keys = bundle.keySet();

            for (String key : keys) {
                mMap.put(key, bundle.get(key));
            }
        }
    }

    /**
     * Create a new instance and copy the data from another instance.
     *
     * @param bundle
     *      The {@link HashBundle} containing the data to copy
     *
     * @throws RuntimeException
     */
    public HashBundle(HashBundle bundle) throws RuntimeException {
        mClassLoader = bundle.mClassLoader;
        putAll(bundle);

        if (!mIsParceled && mMap == null) {
            mMap = createNewMap();
        }
    }

    /**
     * Demanded by {@link Parcelable}
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes all the current data into a {@link Parcel}
     *
     * @param dest
     *      The {@link Parcel} that should receive the data
     *
     * @param flags
     *      Parcel flags
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (size() == 0) {
            dest.writeInt(0);

        } else if (mIsParceled && mParcel != null) {
            int size = mParcel.dataSize();

            dest.writeInt(size);
            dest.appendFrom(mParcel, 0, size);

        } else {
            int sizePos = dest.dataPosition();      // Position where our data begins
            int beginPos = sizePos + 2;             // Position where the actual Map data begins
            dest.writeInt(-1);                      // Placeholder for the total size of our data

            parcel(dest, 0);

            int endPos = dest.dataPosition();

            // Add the parcel size to the parcel
            dest.setDataPosition(sizePos);
            dest.writeInt((endPos - beginPos));     // Write the size of our data
            dest.setDataPosition(endPos);
        }
    }

    /**
     * Writes all the current data into a {@link JSONParcel}
     *
     * @param dest
     *      The {@link JSONParcel} that should receive the data
     *
     * @throws JSONException
     */
    @Override
    public void writeToJSON(JSONParcel dest) throws JSONException {
        if (size() == 0) {
            dest.writeInt(0);

        } else if (mIsParceled && mJSONParcel != null) {
            int size = mJSONParcel.getDataSize();

            dest.writeInt(size);
            dest.appendFrom(mJSONParcel, 0, size);

        } else {
            int sizePos = dest.getDataPosition();   // Position where our data begins
            int beginPos = sizePos + 2;             // Position where the actual Map data begins
            dest.writeInt(-1);                      // Placeholder for the total size of our data

            parcel(dest, 0);

            int endPos = dest.getDataPosition();

            // Add the parcel size to the parcel
            dest.setDataPosition(sizePos);
            dest.writeInt((endPos - beginPos));     // Write the size of our data
            dest.setDataPosition(endPos);
        }
    }

    protected void parcel(Object dest, int flags) {
        int parcelType = dest instanceof Parcel ? PARCEL_ANDROID : PARCEL_JSON;

        if (mIsParceled) {
            unparcel();
        }

        Set<String> keys = mMap.keySet();

        switch (parcelType) {
            case PARCEL_ANDROID:
                Parcel parcel = (Parcel) dest;
                parcel.writeInt(mMap.size());

                for (String key : keys) {
                    Object value = mMap.get(key);

                    parcel.writeString(key);
                    ParcelHelper.parcelData(value, parcel, flags);
                }

                break;

            case PARCEL_JSON:
                try {
                    JSONParcel jsonParcel = (JSONParcel) dest;
                    jsonParcel.writeInt(mMap.size());

                    for (String key : keys) {
                        Object value = mMap.get(key);

                        jsonParcel.writeString(key);
                        jsonParcel.writeValue(value);
                    }

                } catch (JSONException e) {
                    throw new Error(e.getMessage(), e);
                }
        }
    }

    public void parcel(int parcelType) {
        switch (parcelType) {
            case PARCEL_ANDROID:
                if (mParcel == null) {
                    Parcel parcel = Parcel.obtain();
                    parcel(parcel, 0);
                    parcel.setDataPosition(0);

                    mParcel = parcel;
                }

                break;

            case PARCEL_JSON:
                if (mJSONParcel == null) {
                    JSONParcel jsonParcel = new JSONParcel();
                    parcel(jsonParcel, 0);
                    jsonParcel.setDataPosition(0);

                    mJSONParcel = jsonParcel;
                }
        }

        if (mMap != null) {
            mDataSize = mMap.size();
            mMap.clear();
        }

        mIsParceled = true;
    }


    public void unparcel() {
        synchronized (mParcelLock) {
            if (mMap == null) {
                mMap = createNewMap();
            }

            if (mIsParceled && mParcel != null) {
                mMap.clear();

                if (mParcel != oEmptyParcel) {
                    int size = mParcel.readInt();
                    String key;
                    Object value;

                    for (int i=0; i < size; i++) {
                        key = mParcel.readString();
                        value = ParcelHelper.unparcelData(mParcel, mClassLoader);

                        mMap.put(key, value);
                    }
                }

            } else if (mIsParceled && mJSONParcel != null) {
                mMap.clear();

                if (mJSONParcel != oEmptyJSONParcel) {
                    try {
                        int size = mJSONParcel.readInt();
                        String key;
                        Object value;

                        for (int i = 0; i < size; i++) {
                            key = mJSONParcel.readString();
                            value = mJSONParcel.readValue(mClassLoader);

                            mMap.put(key, value);
                        }

                    } catch (JSONException e) {
                        throw new Error(e.getMessage(), e);
                    }
                }
            }

            if (mParcel != null) {
                mParcel.recycle();
                mParcel = null;
            }

            if (mJSONParcel != null) {
                mJSONParcel.clear();
                mJSONParcel = null;
            }

            /*
             * Stop other methods from calling
             */
            mIsParceled = false;
        }
    }

    public boolean isParceled() {
        return mIsParceled;
    }

    public boolean isParceled(int parcelType) {
        return mIsParceled && ((parcelType == PARCEL_ANDROID && mParcel != null) || (parcelType == PARCEL_JSON && mJSONParcel != null));
    }

    /**
     * Get the data size of this instance
     */
    public int size() {
        synchronized (mParcelLock) {
            if (mIsParceled) {
                return mDataSize;
            }

            return mMap.size();
        }
    }

    /**
     * Returns true if the mapping of this instance is empty, false otherwise.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Removes all elements from this instance
     */
    public void clear() {
        if (mIsParceled && mParcel != null) {
            mParcel.recycle();
            mParcel = null;

        } else if (mIsParceled && mJSONParcel != null) {
            mJSONParcel.clear();
            mJSONParcel = null;
        }

        mIsParceled = false;

        if (mMap != null) {
            mMap.clear();

        } else {
            mMap = createNewMap();
        }
    }

    /**
     * Returns true if the given key exists in this instance
     *
     * @param key
     *      The key to check for
     */
    public boolean containsKey(String key) {
        if (mIsParceled) {
            unparcel();
        }

        return mMap.containsKey(key);
    }

    /**
     * Returns true if the given value exists in this instance
     *
     * @param value
     *      The value to search for as an {@link Object}
     */
    public boolean containsValue(Object value) {
        if (mIsParceled) {
            unparcel();
        }

        return mMap.containsValue(value);
    }

    /**
     * Returns the entry with the given key as an object
     *
     * @param key
     *      The key for the entry
     */
    public Object get(String key) {
        if (mIsParceled) {
            unparcel();
        }

        return mMap.get(key);
    }

    /**
     * Insert data as an Object to the entry with the given key
     *
     * @param key
     *      The key for the entry
     *
     * @param value
     *      The value to insert
     */
    public void put(String key, Object value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    /**
     * Removes any entry with the given key from this instance
     *
     * @param key
     *      The key for the entry
     *
     * @return
     *      The value of the removed entry as an {@link Object}
     */
    public Object remove(String key) {
        if (mIsParceled) {
            unparcel();
        }

        return mMap.remove(key);
    }

    /**
     * Copy all the entries from the given {@link Bundle}
     * to this instance
     *
     * @param bundle
     *      The {@link Bundle} to copy from
     */
    public void putAll(Bundle bundle) {
        if (bundle.size() > 0) {
            if (mIsParceled) {
                unparcel();
            }

            // Unparcel the bundle
            bundle.size();

            try {
                Map<String, Object> map = (Map<String, Object>) oBundleMapField.get(bundle);
                mMap.putAll(map);

            } catch (IllegalAccessException e) {
                Set<String> keys = bundle.keySet();

                for (String key : keys) {
                    mMap.put(key, bundle.get(key));
                }
            }
        }
    }

    /**
     * Copy all the entries from the given {@link HashBundle}
     * to this instance
     *
     * @param bundle
     *      The {@link HashBundle} to copy from
     */
    public void putAll(HashBundle bundle) throws RuntimeException {
        if (bundle.size() > 0) {
            if (bundle.mIsParceled && bundle.mParcel != null && !mIsParceled && size() == 0) {
                if (bundle.mParcel == oEmptyParcel) {
                    mParcel = oEmptyParcel;

                } else {
                    mParcel = Parcel.obtain();
                    mParcel.appendFrom(bundle.mParcel, 0, bundle.mParcel.dataSize());
                    mParcel.setDataPosition(0);
                }

                mDataSize = bundle.mDataSize;
                mIsParceled = true;

            } else if (bundle.mIsParceled && bundle.mJSONParcel != null && !mIsParceled && size() == 0) {
                try {
                    if (bundle.mJSONParcel == oEmptyJSONParcel) {
                        mJSONParcel = oEmptyJSONParcel;

                    } else {
                        mJSONParcel = new JSONParcel();
                        mJSONParcel.appendFrom(bundle.mJSONParcel, 0, bundle.mJSONParcel.getDataSize());
                        mJSONParcel.setDataPosition(0);
                    }

                    mDataSize = bundle.mDataSize;
                    mIsParceled = true;

                } catch (JSONException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }

            } else {
                if (bundle.mIsParceled) {
                    bundle.unparcel();
                }

                if (mIsParceled) {
                    unparcel();
                }

                mMap.putAll(bundle.mMap);
            }
        }
    }

    /**
     * Copy all the entries from the given {@link Map}
     * to this instance
     *
     * @param map
     *      The {@link Map} to copy from
     */
    public void putAll(Map map) {
        if (map.size() > 0) {
            if (mIsParceled) {
                unparcel();
            }

            mMap.putAll(map);
        }
    }

    /**
     * Get all of the entry keys as a {@link Set}
     */
    public Set<String> keySet() {
        if (mIsParceled) {
            unparcel();
        }

        return mMap.keySet();
    }

    /**
     * Get all of the entries as a {@link Set} and wrapped in a {@link java.util.Map.Entry}
     */
    public Set<Map.Entry<String, Object>> entrySet() {
        if (mIsParceled) {
            unparcel();
        }

        return mMap.entrySet();
    }

    /**
     * Create and return a new instance with all the data from this instance copied to it
     *
     * @throws RuntimeException
     */
    public HashBundle clone() throws RuntimeException {
        /*
         * Do not unparcel this.
         * The specific constructor being used, is capable at handling this properly
         */
        return new HashBundle(this);
    }

    /**
     * Method from the original {@link Bundle} class to create {@link ClassCastException} warnings
     */
    protected void typeWarning(String key, Object value, String className, Object defaultValue, ClassCastException e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Key ");
        sb.append(key);
        sb.append(" expected ");
        sb.append(className);
        sb.append(" but value was a ");
        sb.append(value.getClass().getName());
        sb.append(".  The default value ");
        sb.append(defaultValue);
        sb.append(" was returned.");
        Log.w(getClass().getSimpleName(), sb.toString());
        Log.w(getClass().getSimpleName(), "Attempt to cast generated internal exception:", e);
    }


    /*
     * ================================================================
     * ----------------------------------------------------------------
     * STRING
     */

    public void putString(String key, String value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public void putStringArray(String key, String[] value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (String) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "String", defValue, e);
            }
        }

        return defValue;
    }

    public String[] getStringArray(String key) {
        return getStringArray(key, null);
    }

    public String[] getStringArray(String key, String[] defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (String[]) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "String[]", defValue, e);
            }
        }

        return defValue;
    }

    public void putStringList(String key, List<String> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public List<String> getStringList(String key) {
        return getStringList(key, null);
    }

    public List<String> getStringList(String key, List<String> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (List<String>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "List<String>", defValue, e);
            }
        }

        return defValue;
    }

    public void putStringSet(String key, Set<String> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public Set<String> getStringSet(String key) {
        return getStringSet(key, null);
    }

    public Set<String> getStringSet(String key, Set<String> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Set<String>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Set<String>", defValue, e);
            }
        }

        return defValue;
    }


    /*
     * ================================================================
     * ----------------------------------------------------------------
     * PARCELABLE
     */

    public void putParcelable(String key, Parcelable value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public void putParcelableArray(String key, Parcelable[] value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public Parcelable getParcelable(String key) {
        return getParcelable(key, null);
    }

    public Parcelable getParcelable(String key, Parcelable defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Parcelable) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Parcelable", defValue, e);
            }
        }

        return defValue;
    }

    public Parcelable[] getParcelableArray(String key) {
        return getParcelableArray(key, null);
    }

    public Parcelable[] getParcelableArray(String key, Parcelable[] defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Parcelable[]) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Parcelable[]", defValue, e);
            }
        }

        return defValue;
    }

    public void putParcelableList(String key, List<Parcelable> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public List<Parcelable> getParcelableList(String key) {
        return getParcelableList(key, null);
    }

    public List<Parcelable> getParcelableList(String key, List<Parcelable> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (List<Parcelable>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "List<Parcelable>", defValue, e);
            }
        }

        return defValue;
    }

    public void putParcelableSet(String key, Set<Parcelable> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public Set<Parcelable> getParcelableSet(String key) {
        return getParcelableSet(key, null);
    }

    public Set<Parcelable> getParcelableSet(String key, Set<Parcelable> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Set<Parcelable>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Set<Parcelable>", defValue, e);
            }
        }

        return defValue;
    }



    /*
     * ================================================================
     * ----------------------------------------------------------------
     * JSONPARCELABLE
     */

    public void putJSONParcelable(String key, JSONParcelable value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public void putJSONParcelableArray(String key, JSONParcelable[] value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public JSONParcelable getJSONParcelable(String key) {
        return getJSONParcelable(key, null);
    }

    public JSONParcelable getJSONParcelable(String key, JSONParcelable defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (JSONParcelable) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "JSONParcelable", defValue, e);
            }
        }

        return defValue;
    }

    public JSONParcelable[] getJSONParcelableArray(String key) {
        return getJSONParcelableArray(key, null);
    }

    public JSONParcelable[] getJSONParcelableArray(String key, JSONParcelable[] defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (JSONParcelable[]) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "JSONParcelable[]", defValue, e);
            }
        }

        return defValue;
    }

    public void putJSONParcelableList(String key, List<JSONParcelable> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public List<JSONParcelable> getJSONParcelableList(String key) {
        return getJSONParcelableList(key, null);
    }

    public List<JSONParcelable> getJSONParcelableList(String key, List<JSONParcelable> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (List<JSONParcelable>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "List<JSONParcelable>", defValue, e);
            }
        }

        return defValue;
    }

    public void putJSONParcelableSet(String key, Set<JSONParcelable> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public Set<JSONParcelable> getJSONParcelableSet(String key) {
        return getJSONParcelableSet(key, null);
    }

    public Set<JSONParcelable> getJSONParcelableSet(String key, Set<JSONParcelable> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Set<JSONParcelable>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Set<JSONParcelable>", defValue, e);
            }
        }

        return defValue;
    }



    /*
     * ================================================================
     * ----------------------------------------------------------------
     * INTEGER
     */

    public void putInt(String key, int value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public void putIntArray(String key, int[] value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Integer) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Integer", defValue, e);
            }
        }

        return defValue;
    }

    public int[] getIntArray(String key) {
        return getIntArray(key, null);
    }

    public int[] getIntArray(String key, int[] defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (int[]) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Integer[]", defValue, e);
            }
        }

        return defValue;
    }

    public void putIntList(String key, List<Integer> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public List<Integer> getIntList(String key) {
        return getIntList(key, null);
    }

    public List<Integer> getIntList(String key, List<Integer> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (List<Integer>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "List<Integer>", defValue, e);
            }
        }

        return defValue;
    }

    public void putIntSet(String key, Set<Integer> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public Set<Integer> getIntSet(String key) {
        return getIntSet(key, null);
    }

    public Set<Integer> getIntSet(String key, Set<Integer> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Set<Integer>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Set<Integer>", defValue, e);
            }
        }

        return defValue;
    }



    /*
     * ================================================================
     * ----------------------------------------------------------------
     * LONG
     */

    public void putLong(String key, long value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public void putLongArray(String key, long[] value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public long getLong(String key) {
        return getLong(key, 0l);
    }

    public long getLong(String key, long defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Long) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Long", defValue, e);
            }
        }

        return defValue;
    }

    public long[] getLongArray(String key) {
        return getLongArray(key, null);
    }

    public long[] getLongArray(String key, long[] defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (long[]) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Long[]", defValue, e);
            }
        }

        return defValue;
    }

    public void putLongList(String key, List<Long> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public List<Long> getLongList(String key) {
        return getLongList(key, null);
    }

    public List<Long> getLongList(String key, List<Long> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (List<Long>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "List<Long>", defValue, e);
            }
        }

        return defValue;
    }

    public void putLongSet(String key, Set<Long> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public Set<Long> getLongSet(String key) {
        return getLongSet(key, null);
    }

    public Set<Long> getLongSet(String key, Set<Long> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Set<Long>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Set<Long>", defValue, e);
            }
        }

        return defValue;
    }



    /*
     * ================================================================
     * ----------------------------------------------------------------
     * DOUBLE
     */

    public void putDouble(String key, double value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public void putDoubleArray(String key, double[] value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public double getDouble(String key) {
        return getDouble(key, 0d);
    }

    public double getDouble(String key, double defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Double) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Double", defValue, e);
            }
        }

        return defValue;
    }

    public double[] getDoubleArray(String key) {
        return getDoubleArray(key, null);
    }

    public double[] getDoubleArray(String key, double[] defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (double[]) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Double[]", defValue, e);
            }
        }

        return defValue;
    }

    public void putDoubleList(String key, List<Double> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public List<Double> getDoubleList(String key) {
        return getDoubleList(key, null);
    }

    public List<Double> getDoubleList(String key, List<Double> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (List<Double>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "List<Double>", defValue, e);
            }
        }

        return defValue;
    }

    public void putDoubleSet(String key, Set<Double> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public Set<Double> getDoubleSet(String key) {
        return getDoubleSet(key, null);
    }

    public Set<Double> getDoubleSet(String key, Set<Double> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Set<Double>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Set<Double>", defValue, e);
            }
        }

        return defValue;
    }



    /*
     * ================================================================
     * ----------------------------------------------------------------
     * FLOAT
     */

    public void putFloat(String key, float value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public void putFloatArray(String key, float[] value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public float getFloat(String key) {
        return getFloat(key, 0f);
    }

    public float getFloat(String key, float defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Float) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Float", defValue, e);
            }
        }

        return defValue;
    }

    public float[] getFloatArray(String key) {
        return getFloatArray(key, null);
    }

    public float[] getFloatArray(String key, float[] defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (float[]) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Float[]", defValue, e);
            }
        }

        return defValue;
    }

    public void putFloatList(String key, List<Float> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public List<Float> getFloatList(String key) {
        return getFloatList(key, null);
    }

    public List<Float> getFloatList(String key, List<Float> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (List<Float>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "List<Float>", defValue, e);
            }
        }

        return defValue;
    }

    public void putFloatSet(String key, Set<Float> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public Set<Float> getFloatSet(String key) {
        return getFloatSet(key, null);
    }

    public Set<Float> getFloatSet(String key, Set<Float> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Set<Float>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Set<Float>", defValue, e);
            }
        }

        return defValue;
    }



    /*
     * ================================================================
     * ----------------------------------------------------------------
     * BOOLEAN
     */

    public void putBoolean(String key, boolean value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public void putBooleanArray(String key, boolean[] value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Boolean) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Boolean", defValue, e);
            }
        }

        return defValue;
    }

    public boolean[] getBooleanArray(String key) {
        return getBooleanArray(key, null);
    }

    public boolean[] getBooleanArray(String key, boolean[] defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (boolean[]) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Boolean[]", defValue, e);
            }
        }

        return defValue;
    }

    public void putBooleanList(String key, List<Boolean> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public List<Boolean> getBooleanList(String key) {
        return getBooleanList(key, null);
    }

    public List<Boolean> getBooleanList(String key, List<Boolean> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (List<Boolean>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "List<Boolean>", defValue, e);
            }
        }

        return defValue;
    }

    public void putBooleanSet(String key, Set<Boolean> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public Set<Boolean> getBooleanSet(String key) {
        return getBooleanSet(key, null);
    }

    public Set<Boolean> getBooleanSet(String key, Set<Boolean> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Set<Boolean>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Set<Boolean>", defValue, e);
            }
        }

        return defValue;
    }



    /*
     * ================================================================
     * ----------------------------------------------------------------
     * BYTE
     */

    public void putByte(String key, byte value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public void putByteArray(String key, byte[] value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public byte getByte(String key) {
        return getByte(key, (byte) 0);
    }

    public byte getByte(String key, byte defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Byte) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Byte", defValue, e);
            }
        }

        return defValue;
    }

    public byte[] getByteArray(String key) {
        return getByteArray(key, null);
    }

    public byte[] getByteArray(String key, byte[] defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (byte[]) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Byte[]", defValue, e);
            }
        }

        return defValue;
    }

    public void putByteList(String key, List<Byte> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public List<Byte> getByteList(String key) {
        return getByteList(key, null);
    }

    public List<Byte> getByteList(String key, List<Byte> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (List<Byte>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "List<Byte>", defValue, e);
            }
        }

        return defValue;
    }

    public void putByteSet(String key, Set<Byte> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public Set<Byte> getByteSet(String key) {
        return getByteSet(key, null);
    }

    public Set<Byte> getByteSet(String key, Set<Byte> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Set<Byte>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Set<Byte>", defValue, e);
            }
        }

        return defValue;
    }



    /*
     * ================================================================
     * ----------------------------------------------------------------
     * CHARACTER
     */

    public void putChar(String key, char value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public void putCharArray(String key, char[] value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public char getChar(String key) {
        return getChar(key, (char) 0);
    }

    public char getChar(String key, char defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Character) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Char", defValue, e);
            }
        }

        return defValue;
    }

    public char[] getCharArray(String key) {
        return getCharArray(key, null);
    }

    public char[] getCharArray(String key, char[] defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (char[]) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Char[]", defValue, e);
            }
        }

        return defValue;
    }

    public void putCharacterList(String key, List<Character> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public List<Character> getCharacterList(String key) {
        return getCharacterList(key, null);
    }

    public List<Character> getCharacterList(String key, List<Character> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (List<Character>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "List<Character>", defValue, e);
            }
        }

        return defValue;
    }

    public void putCharacterSet(String key, Set<Character> value) {
        if (mIsParceled) {
            unparcel();
        }

        mMap.put(key, value);
    }

    public Set<Character> getCharacterSet(String key) {
        return getCharacterSet(key, null);
    }

    public Set<Character> getCharacterSet(String key, Set<Character> defValue) {
        if (mIsParceled) {
            unparcel();
        }

        Object value = mMap.get(key);

        if (value != null) {
            try {
                return (Set<Character>) value;

            } catch (ClassCastException e) {
                typeWarning(key, value, "Set<Character>", defValue, e);
            }
        }

        return defValue;
    }
}
