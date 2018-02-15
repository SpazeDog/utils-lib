package com.spazedog.lib.utilsLib.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 *
 */
public final class SwiftSet<T> implements Set<T> {

    /** * */
    private final Object mDeleted = new Object();

    /** * */
    public Object[] mArray = null;

    /** * */
    private int[] mHashKeys;

    /** * */
    private int mInitSize;

    /** * */
    private final float mResize = 1.35f;

    /** * */
    private final float mDownsize = 0.55f;

    /** * */
    private int mLength = 0;

    /** * */
    private int mGCLength = 0;

    /**
     *
     */
    private void refactor() {
        if (mArray == null) {
            mHashKeys = new int[mInitSize];
            mArray = new Object[mInitSize];

        } else {
            int length = mLength - mGCLength;
            int minLength = length < mInitSize ? mInitSize : length;

            if (mLength >= mHashKeys.length || ((int) (mHashKeys.length * mDownsize)) > minLength) {
                if (mGCLength > 0) {
                    int x = 0;

                    for (int i=0; i < mLength; i++) {
                        if (mArray[i] != mDeleted) {
                            if (x != i) {
                                mHashKeys[x] = mHashKeys[i];
                                mArray[x] = mArray[i];
                            }

                            x++;
                        }
                    }

                    mLength -= mGCLength;
                    mGCLength = 0;
                    minLength = mLength < mInitSize ? mInitSize : mLength;
                }

                if (mLength >= mHashKeys.length || ((int) (mHashKeys.length * mDownsize)) > minLength) {
                    int[] newHashKeys = new int[ ((int) (minLength * mResize)) + 1 ];
                    Object[] newArray = new Object[newHashKeys.length];

                    System.arraycopy(newHashKeys, 0, mHashKeys, 0, mLength);
                    System.arraycopy(newArray, 0, mArray, 0, mLength);

                    mHashKeys = newHashKeys;
                    mArray = newArray;
                }
            }
        }
    }

    /**
     *
     */
    private int valueIndex(Object value, boolean findEmpty) {
        if (size() == 0) {
            return -1;
        }

        int hash = value == null ? 0 : value.hashCode();
        int lo = 0;
        int hi = mLength - 1;

        /* Start by locating an index containing this hash value
         */
        while (lo <= hi) {
            final int mid = (lo + hi) >>> 1;
            final int midVal = mHashKeys[mid];

            if (midVal < hash) {
                lo = mid + 1;
            } else if (midVal > hash) {
                hi = mid - 1;
            } else {
                lo = ~mid;

                break;  // value found
            }
        }

        /* If we did find an index, let's locate the correct one, if present
         */
        if (lo < 0) {
            lo = ~lo;

            if ((value == null && mArray[lo] == null) || (value != null && value.equals(mArray[lo]))) {
                return mArray[lo] == mDeleted ? ~lo : lo;
            }

            int end = lo+1;
            int begin = lo-1;
            int empty = -1;

        	/* First check if the current index or any following ones matches the key that we are looking for
        	 */
            for (; end < mLength && mHashKeys[end] == hash; end++) {
                if ((value == null && mArray[end] == null) || (value != null && value.equals(mArray[end]))) {
                    return mArray[end] == mDeleted ? ~end : end;

                } else if (findEmpty && empty < 0 && mArray[end] == mDeleted) {
                    empty = end;
                }
            }

        	/* If no key was found, let's check to see if it comes before the index we found
        	 */
            for (; begin >= 0 && mHashKeys[begin] == hash; begin--) {
                if ((value == null && mArray[begin] == null) || (value != null && value.equals(mArray[begin]))) {
                    return mArray[begin] == mDeleted ? ~begin : begin;

                } else if (findEmpty && empty < 0 && mArray[begin] == mDeleted) {
                    empty = begin;
                }
            }

            return empty < 0 ? ~end : ~empty;
        }

        return ~lo;  // value not present
    }

    /**
     *
     */
    public SwiftSet() {
        mInitSize = 10;
    }

    /**
     *
     */
    public SwiftSet(int capacity) {
        if (capacity < 10) {
            capacity = 10;
        }

        mInitSize = capacity;
    }

    /**
     *
     */
    @Override
    public boolean add(T value) {
        refactor();

        int index = valueIndex(value, true);

        if (index < 0 && mArray[~index] != mDeleted) {
            index = ~index;

            System.arraycopy(mHashKeys, index, mHashKeys, index+1, mLength - index);
            System.arraycopy(mArray, index, mArray, index+1, mLength - index);

            mHashKeys[index] = value.hashCode();
            mArray[index] = value;
            mLength++;

            return true;

        } else if (index < 0) {
            index = ~index;
            mHashKeys[index] = value.hashCode();
            mArray[index] = value;
            mGCLength--;

            return true;
        }

        return false;
    }

    /**
     *
     */
    @Override
    public void clear() {
        if (size() > 0) {
            for (int i = 0; i < mLength; i++) {
                mArray[i] = mDeleted;
            }

            mGCLength = mLength;

            refactor();
        }
    }

    /**
     *
     */
    @Override
    public boolean contains(Object value) {
        return valueIndex(value, false) >= 0;
    }

    /**
     *
     */
    @Override
    public boolean isEmpty() {
        return size() > 0;
    }

    /**
     *
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private int mPointer = 0;

            @Override
            public boolean hasNext() {
                for (int i=mPointer; i < mLength; i++) {
                    if (mArray[i] != mDeleted) {
                        return true;
                    }
                }

                return false;
            }

            @SuppressWarnings("unchecked")
            @Override
            public T next() {
                while (mPointer < mLength) {
                    int key = mPointer;

                    if (mArray[mPointer++] != mDeleted) {
                        return (T) mArray[key++];
                    }
                }

                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                mArray[mPointer] = mDeleted;
                mGCLength++;

                refactor();
            }
        };
    }

    /**
     *
     */
    @Override
    public boolean remove(Object value) {
        int index = valueIndex(value, false);

        if (index >= 0) {
            mArray[index] = mDeleted;
            mGCLength++;

            refactor();

            return true;
        }

        return false;
    }

    /**
     *
     */
    @Override
    public boolean addAll(Collection<? extends T> arg0) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     */
    @Override
    public boolean containsAll(Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     */
    @Override
    public boolean removeAll(Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     */
    @Override
    public boolean retainAll(Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     */
    @Override
    public int size() {
        return mLength - mGCLength;
    }

    /**
     *
     */
    @Override
    public Object[] toArray() {
        Object[] ret = new Object[mLength];

        if (mLength > 0) {
            for (int i=0,x=0; i < mLength; i++) {
                if (mArray[i] != mDeleted) {
                    ret[x++] = mArray[i];
                }
            }
        }

        return ret;
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    @Override
    public <TI> TI[] toArray(TI[] array) {
        if (mLength > 0) {
            for (int i=0,x=0; i < mLength; i++) {
                if (mArray[i] != mDeleted) {
                    array[x++] = (TI) mArray[i];
                }
            }
        }

        return array;
    }
}
