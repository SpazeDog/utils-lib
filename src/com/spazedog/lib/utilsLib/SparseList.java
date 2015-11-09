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
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Light Weight {@link List} with a small Garbage Collector to reduce the
 * ammount of times having to re-organize the data array
 */
public class SparseList<T> extends MultiParcelableBuilder implements List<T> {

    protected static final Object GC = new Object();
    protected static final Object[] OBJARRAY = new Object[0];

    protected Object[] mListValues;
    protected int mListSize;
    protected int mGCSize;
    protected int mGCOffset;
    protected boolean mGCRange;

    public SparseList() {
        this(0);
    }

    public SparseList(int capacity) {
        instantiate(capacity);
    }

    protected void instantiate(int dataSize) {
        mListSize = 0;
        mGCSize = 0;
        mGCOffset = 0x7FFFFFFF;
        mGCRange = false;

        if (dataSize > 0) {
            allocArrays(dataSize);

        } else {
            mListValues = OBJARRAY;
        }
    }

    protected void allocArrays(int dataSize) {
        int newMapSize = 0;

        if (mListValues == null || mListValues.length < dataSize) {
            newMapSize = (int) (dataSize < 10 ? 10 : dataSize * 1.35f) + 1;

        } else {
            newMapSize = (int) (dataSize < 10 ? 10 : dataSize * 1.55f) + 1;
        }

        mListValues = new Object[newMapSize];
    }

    protected void gc() {
        if (mGCRange) {
            for (int i = mGCSize+1, x = mGCOffset; i < mListSize && x >= 0; i++, x++) {
                mListValues[x] = mListValues[i];
                mListValues[i] = null;
            }

            mListSize -= mGCSize;
            mGCRange = false;

        } else {
            int x = mGCOffset;

            for (int i=mGCOffset; i < mListSize; i++) {
                if (mListValues[i] != GC) {
                    if (i != x) {
                        mListValues[x] = mListValues[i];
                        mListValues[i] = null;
                    }

                    x++;
                }
            }

            mListSize -= mGCSize;
        }

        mGCSize = 0;
        mGCOffset = 0x7FFFFFFF;

        int emptySize = mListValues.length - mListSize;
        int threshold = (int) (mListSize < 10 ? 10 : mListSize * 1.75f) + 1;

        if (emptySize > threshold) {
            Object[] oldListValues = mListValues;

            allocArrays(mListSize);

            System.arraycopy(oldListValues, 0, mListValues, 0, mListSize);
        }
    }

    @Override
    public int size() {
        return mListSize - mGCSize;
    }

    @Override
    public boolean isEmpty() {
        return mListSize - mGCSize == 0;
    }

    @Override
    public boolean add(T object) {
        add((mListSize - mGCSize), object); return true;
    }

    @Override
    public T get(int location) {
        int index = location;

        if (!mGCRange && location >= mGCOffset) {
            gc();

        } else if (mGCRange) {
            index = location >= mGCOffset ? location + mGCSize : location;
        }

        if (index >= 0 && index < mListSize) {
            return (T) mListValues[index];

        } else {
            throw new ArrayIndexOutOfBoundsException("length=" + (mListSize - mGCSize) + "; index: " + location);
        }
    }

    @Override
    public T set(int location, T object) {
        int index = location;

        if (!mGCRange && location >= mGCOffset) {
            gc();

        } else if (mGCRange) {
            index = location >= mGCOffset ? location + mGCSize : location;
        }

        if (index >= 0 && index < mListSize) {
            T ret = (T) mListValues[index];
            mListValues[index] = object;

            return ret;

        } else {
            throw new ArrayIndexOutOfBoundsException("length=" + (mListSize - mGCSize) + "; index: " + location);
        }
    }

    @Override
    public void add(int location, T object) {
        int size = mListSize - mGCSize;
        boolean addLast = location == size;

        if (addLast || (location >= 0 && location <= size)) {
            if (addLast && mListSize < mListValues.length) {
                mListValues[mListSize] = object;
                mListSize++;

                return;

            } else if (addLast) {
                location = mListSize;
            }

            if (mGCSize > 0) {
                if (!addLast || location > mGCOffset) {
                    gc();

                    if (addLast) {
                        location = mListSize;
                    }

                } else if (!addLast) {
                    mGCOffset++;
                }
            }

            if (location >= mListValues.length) {
                Object[] oldListValues = mListValues;

                allocArrays(mListSize);

                System.arraycopy(oldListValues, 0, mListValues, 0, location);

                if (location != mListSize) {
                    System.arraycopy(oldListValues, location, mListValues, location+1, mListSize-location);
                }

            } else {
                System.arraycopy(mListValues, location, mListValues, location+1, mListSize-location);
            }

            mListValues[location] = object;
            mListSize++;

        } else {
            throw new ArrayIndexOutOfBoundsException("length=" + size + "; index: " + location);
        }
    }

    @Override
    public T remove(int location) {
        int index = location;

        if (!mGCRange && location > mGCOffset) {
            gc();

        } else if (mGCRange) {
            index = location >= mGCOffset ? location + mGCSize : location;
        }

        if (index >= 0 && index < mListSize) {
            T ret = (T) mListValues[index];
            mListValues[index] = GC;

            if (mGCSize == 0 || (mGCRange && index >= mGCOffset-1 && index <= (mGCOffset + mGCSize)+1)) {
                mGCRange = true;

            } else {
                mGCRange = false;
            }

            if (mGCOffset > index) {
                mGCOffset = index;
            }

            mGCSize++;

            return ret;

        } else {
            throw new ArrayIndexOutOfBoundsException("length=" + (mListSize - mGCSize) + "; index: " + location);
        }
    }

    @Override
    public boolean remove(Object value) {
        int x=0;

        if (value == null) {
            for (int i = 0; i < mListSize; i++) {
                if (mListValues[i] == null) {
                    mListValues[i] = GC;
                    mGCSize++;
                    x++;

                    if (mGCOffset > i) {
                        mGCOffset = i;
                    }
                }
            }

        } else {
            for (int i = 0; i < mListSize; i++) {
                if (value.equals(mListValues[i])) {
                    if (mListValues[i] == null) {
                        mListValues[i] = GC;
                        mGCSize++;
                        x++;

                        if (mGCOffset > i) {
                            mGCOffset = i;
                        }
                    }
                }
            }
        }

        if (x > 0) {
            mGCRange = false;
        }

        return x > 0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < mListSize; i++) {
            mListValues[i] = null;
        }

        mListSize = 0;
        mGCSize = 0;
        mGCOffset = 0x7FFFFFFF;
        mGCRange = false;

        /*
         * Reduce the array if needed (No need for 10,000+ entries in an empty list)
         */
        gc();
    }

    @Override
    public int indexOf(Object value) {
        if (mGCSize > 0) {
            gc();
        }

        if (value == null) {
            for (int i = 0; i < mListSize; i++) {
                if (mListValues[i] == null) {
                    return i;
                }
            }

        } else {
            for (int i = 0; i < mListSize; i++) {
                if (value.equals(mListValues[i])) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public int lastIndexOf(Object value) {
        if (mGCSize > 0) {
            gc();
        }

        if (value == null) {
            for (int i = mListSize-1; i >= 0; i--) {
                if (mListValues[i] == null) {
                    return i;
                }
            }

        } else {
            for (int i = mListSize-1; i >= 0; i--) {
                if (value.equals(mListValues[i])) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public boolean contains(Object object) {
        return indexOf(object) >= 0;
    }

    @Override
    public Object[] toArray() {
        if (mGCSize > 0) {
            gc();
        }

        Object[] ret = new Object[mListSize];
        System.arraycopy(mListValues, 0, ret, 0, mListSize);

        return ret;
    }

    @Override
    public <T1> T1[] toArray(T1[] array) {
        if (mGCSize > 0) {
            gc();
        }

        System.arraycopy(mListValues, 0, array, 0, mListSize);

        return array;
    }

    @Override
    public List<T> subList(int start, int end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int location, Collection<? extends T> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<T> listIterator() {
        return new SparseIterator(0);
    }

    @Override
    public ListIterator<T> listIterator(int location) {
        return new SparseIterator(location);
    }

    @Override
    public Iterator<T> iterator() {
        return new SparseIterator(0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public SparseList(JSONParcel source, ClassLoader loader) throws JSONParcel.JSONException{
        int size = source.readInt();

        instantiate(size);

        for (int i=0; i < size; i++) {
            mListValues[i] = source.readValue(loader);
        }

        mListSize = size;
    }

    public SparseList(Parcel source, ClassLoader loader) {
        int size = source.readInt();

        instantiate(size);

        for (int i=0; i < size; i++) {
            mListValues[i] = unparcelData(source, loader);
        }

        mListSize = size;
    }

    @Override
    public void writeToJSON(JSONParcel dest) throws JSONParcel.JSONException {
        super.writeToJSON(dest);

        if (mGCSize > 0) {
            gc();
        }

        dest.writeInt(mListSize);

        for (int i=0; i < mListSize; i++) {
            dest.writeValue(mListValues[i]);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        if (mGCSize > 0) {
            gc();
        }

        dest.writeInt(mListSize);

        for (int i=0; i < mListSize; i++) {
            parcelData(mListValues[i], dest, flags);
        }
    }

    protected class SparseIterator implements ListIterator<T> {

        private int mCursor = 0;
        private int mCurrent = 0;

        protected SparseIterator(int index) {
            mCursor = index;

            if (mCursor < 0 || (mCursor >= SparseList.this.size() && mCursor != 0)) {
                throw new ArrayIndexOutOfBoundsException("length=" + SparseList.this.size() + "; index=" + mCursor);
            }
        }

        @Override
        public boolean hasNext() {
            return mCursor < SparseList.this.size();
        }

        @Override
        public boolean hasPrevious() {
            return mCursor > 0 && SparseList.this.size() > 0;
        }

        @Override
        public int nextIndex() {
            return mCursor+1;
        }

        @Override
        public int previousIndex() {
            return mCursor-1;
        }

        @Override
        public T previous() {
            int position = mCursor;

            if (position >= SparseList.this.size()) {
                throw new ConcurrentModificationException();
            }

            mCursor--;

            return SparseList.this.get(position);
        }

        @Override
        public T next() {
            mCurrent = mCursor;
            mCursor++;

            if (mCurrent >= size()) {
                if ((mCurrent - SparseList.this.size()) > 0 || mCurrent > mListValues.length ) {
                    throw new ConcurrentModificationException();
                }

                throw new NoSuchElementException();
            }

            return SparseList.this.get(mCurrent);
        }

        @Override
        public void add(T object) {
            SparseList.this.add(mCurrent, object);
        }

        @Override
        public void remove() {
            SparseList.this.remove(mCurrent);
        }

        @Override
        public void set(T object) {
            SparseList.this.set(mCurrent, object);
        }
    }
}
