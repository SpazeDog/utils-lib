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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Light Weight {@link Map} that encreases it's speed by specifically handling it's
 * keys as {@link Integer} which in turn encreases the binary search. For this reason
 * it can only store values using {@link Integer} as key.
 * The principle used here is based on Android's {@link android.util.SparseArray}
 */
public class SparseMap<T> extends MultiParcelableBuilder implements Map<Integer, T> {

    protected static final Object GC = new Object();
    protected static final Object[] OBJARRAY = new Object[0];
    protected static final int[] INTARRAY = new int[0];

    protected Object[] mMapValues;
    protected int[] mMapKeys;
    protected int mMapSize;
    protected int mGCSize;
    protected int mGCAlloc;

    protected MapCollections<Integer, T> mCollections;

    public SparseMap() {
        this(0);
    }

    public SparseMap(int capacity) {
        instantiate(capacity);
    }

    protected void instantiate(int dataSize) {
        mGCSize = 0;
        mMapSize = 0;

        if (dataSize > 0) {
            allocArrays(dataSize);

        } else {
            mMapValues = OBJARRAY;
            mMapKeys = INTARRAY;
            mGCAlloc = 0;
        }
    }

    protected void allocArrays(int dataSize) {
        int newMapSize = 0;

        if (mMapKeys == null || mMapKeys.length < dataSize) {
            newMapSize = (int) (dataSize < 10 ? 10 : dataSize * 1.35f) + 1;

        } else {
            newMapSize = (int) (dataSize < 10 ? 10 : dataSize * 1.55f) + 1;
        }

        mMapValues = new Object[newMapSize];
        mMapKeys = new int[newMapSize];
        mGCAlloc = (int) (newMapSize * 1.25f) + 1;
    }

    /*
     * From Android's android.utils.ContainerHelpers which is not available
     * on older platforms
     */
    protected int indexOfKey(int key) {
        int lo = 0;
        int hi = mMapSize - 1;

        while (lo <= hi) {
            final int mid = (lo + hi) >>> 1;
            final int midVal = mMapKeys[mid];

            if (midVal < key) {
                lo = mid + 1;
            } else if (midVal > key) {
                hi = mid - 1;
            } else {
                return mid;  // value found
            }
        }

        return ~lo;  // value not present
    }

    protected int indexOfValue(Object value) {
        for (int i = 0; i < mMapSize; i++) {
            if (value == null) {
                if (mMapValues[i] == null) {
                    return i;
                }

            } else {
                if (value.equals(mMapValues[i])) {
                    return i;
                }
            }
        }

        return -1;
    }

    protected void gc() {
        int x = 0;

        for (int i = 0; i < mMapSize; i++) {
            if (mMapValues[i] != GC) {
                if (i != x) {
                    mMapKeys[x] = mMapKeys[i];
                    mMapValues[x] = mMapValues[i];
                    mMapValues[i] = null;
                }

                x++;
            }
        }

        mMapSize = x;
        mGCSize = 0;

        int emptySize = mMapKeys.length - mMapSize;
        int threshold = (int) (mMapSize < 10 ? 10 : mMapSize * 1.75f) + 1;

        if (emptySize > threshold) {
            int[] oldMapKeys = mMapKeys;
            Object[] oldMapValues = mMapValues;

            allocArrays(mMapSize);

            System.arraycopy(oldMapKeys, 0, mMapKeys, 0, mMapSize);
            System.arraycopy(oldMapValues, 0, mMapValues, 0, mMapSize);
        }
    }

    @Override
    public int size() {
        return mMapSize - mGCSize;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < mMapSize; i++) {
            mMapValues[i] = null;
        }

        mMapSize = 0;
        mGCSize = 0;

        gc();
    }

    @Override
    public boolean containsKey(Object key) {
        return containsKey((int) ((Integer) key));
    }

    public boolean containsKey(int key) {
        return indexOfKey(key) >= 0;
    }

    @Override
    public boolean containsValue(Object value) {
        return indexOfValue(value) >= 0;
    }

    @Override
    public T get(Object key) {
        return get((int) ((Integer) key), null);
    }

    public T get(int key) {
        return get(key, null);
    }

    public T get(int key, T defValue) {
        int index = indexOfKey(key);

        if (index >= 0 && mMapValues[index] != GC) {
            return (T) mMapValues[index];
        }

        return defValue;
    }

    @Override
    public T remove(Object key) {
        return remove((int) ((Integer) key));
    }

    public T remove(int key) {
        int index = indexOfKey(key);

        if (index >= 0 && mMapValues[index] != GC) {
            T ret = (T) mMapValues[index];
            mMapValues[index] = GC;
            mGCSize++;

            if (mGCSize > mGCAlloc) {
                gc();
            }

            return ret;
        }

        return null;
    }

    @Override
    public T put(Integer key, T value) {
        int index = indexOfKey(key);

        if (index >= 0 || ((index = ~index) < mMapSize && mMapValues[index] == GC)) {
            T ret = null;

            if (mMapValues[index] != GC) {
                ret = (T) mMapValues[index];

            } else {
                mMapKeys[index] = key;
                mGCSize--;
            }

            mMapValues[index] = value;

            return ret;
        }

        if (mGCSize > 0 && mMapSize >= mMapKeys.length) {
            gc();
            index = ~indexOfKey(key);
        }

        if (mMapSize <= mMapKeys.length-1) {
            System.arraycopy(mMapKeys, index, mMapKeys, index + 1, mMapSize - index);
            System.arraycopy(mMapValues, index, mMapValues, index + 1, mMapSize - index);

        } else {
            int[] oldMapKeys = mMapKeys;
            Object[] oldMapValues = mMapValues;

            allocArrays(mMapSize);

            System.arraycopy(oldMapKeys, 0, mMapKeys, 0, index);
            System.arraycopy(oldMapValues, 0, mMapValues, 0, index);

            if (index != mMapSize) {
                System.arraycopy(oldMapKeys, index, mMapKeys, index + 1, mMapSize - index);
                System.arraycopy(oldMapValues, index, mMapValues, index + 1, mMapSize - index);
            }
        }

        mMapKeys[index] = key;
        mMapValues[index] = value;
        mMapSize++;

        return null;
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends T> map) {
        for (Iterator<? extends Entry<? extends Integer, ? extends T>> iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<? extends Integer, ? extends T> e = iterator.next();
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public Set<Entry<Integer, T>> entrySet() {
        return getCollection().getMapEntrySet();
    }

    @Override
    public Set<Integer> keySet() {
        return getCollection().getMapKeySet();
    }

    @Override
    public Collection<T> values() {
        return getCollection().getMapValues();
    }

    protected MapCollections<Integer, T> getCollection() {
        if (mCollections == null) {
            mCollections = new MapCollections<Integer, T>() {

                @Override
                Integer colGetKeyAt(int index) {
                    if (SparseMap.this.mGCSize > 0) {
                        SparseMap.this.gc();
                    }

                    return SparseMap.this.mMapKeys[index];
                }

                @Override
                T colGetValueAt(int index) {
                    if (SparseMap.this.mGCSize > 0) {
                        SparseMap.this.gc();
                    }

                    return (T) SparseMap.this.mMapValues[index];
                }

                @Override
                int colGetSize() {
                    return size();
                }

                @Override
                int colIndexOfKey(Object key) {
                    if (SparseMap.this.mGCSize > 0) {
                        SparseMap.this.gc();
                    }

                    return SparseMap.this.indexOfKey((int) ((Integer) key));
                }

                @Override
                int colIndexOfValue(Object value) {
                    if (SparseMap.this.mGCSize > 0) {
                        SparseMap.this.gc();
                    }

                    return SparseMap.this.indexOfValue(value);
                }
            };
        }

        return mCollections;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public SparseMap(JSONParcel source, ClassLoader loader) throws JSONParcel.JSONException{
        int size = source.readInt();

        instantiate(size);

        for (int i=0; i < size; i++) {
            mMapKeys[i] = source.readInt();
            mMapValues[i] = source.readValue(loader);
        }

        mMapSize = size;
    }

    public SparseMap(Parcel source, ClassLoader loader) {
        int size = source.readInt();

        instantiate(size);

        for (int i=0; i < size; i++) {
            mMapKeys[i] = source.readInt();
            mMapValues[i] = unparcelData(source, loader);
        }

        mMapSize = size;
    }

    @Override
    public void writeToJSON(JSONParcel dest) throws JSONParcel.JSONException {
        super.writeToJSON(dest);

        if (mGCSize > 0) {
            gc();
        }

        dest.writeInt(mMapSize);

        for (int i=0; i < mMapSize; i++) {
            dest.writeInt(mMapKeys[i]);
            dest.writeValue(mMapValues[i]);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        if (mGCSize > 0) {
            gc();
        }

        dest.writeInt(mMapSize);

        for (int i=0; i < mMapSize; i++) {
            dest.writeInt(mMapKeys[i]);
            parcelData(mMapValues[i], dest, flags);
        }
    }
}
