package com.spazedog.lib.utilsLib.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Fast List implementation
 *
 * This List makes use of modular arithmetic to reduce the workflow
 * of having to re-organize the internal array on each change to the list.
 */
public final class SwiftList<T> implements List<T> {

    /** * */
    public Object[] mQueue = null;

    /** * */
    private int mInitSize;

    /** * */
    private final float mResize = 1.35f;

    /** * */
    private final float mDownsize = 0.55f;

    /** * */
    private int mFront = 0;

    /** * */
    private int mLength = 0;

    /**
     *
     */
    private void refactor() {
        if (mQueue == null) {
            mQueue = new Object[ mInitSize ];

        } else {
            int minLength = mLength < mInitSize ? mInitSize : mLength;

            if (mLength >= mQueue.length || ((int) (mQueue.length * mDownsize)) > minLength) {
                Object[] newQueue = new Object[ ((int) (minLength * mResize)) + 1 ];

                for (int i=0; i < mLength; i++) {
                    int x = (mFront + i) % mQueue.length;

                    newQueue[i] = mQueue[x];
                }

                mQueue = newQueue;
                mFront = 0;
            }
        }
    }

    /**
     *
     */
    public SwiftList() {
        mInitSize = 10;
    }

    /**
     *
     */
    public SwiftList(int capacity) {
        if (capacity < 10) {
            capacity = 10;
        }

        mInitSize = capacity;
    }

    /**
     *
     */
    @Override
    public int size() {
        return mLength;
    }

    /**
     *
     */
    @Override
    public boolean isEmpty() {
        return mLength == 0;
    }

    /**
     *
     */
    @Override
    public T get(int location) {
        if (location >= 0 && location <= mLength) {
            int index = (mFront + location) % mQueue.length;

            return (T) mQueue[index];
        }

        throw new ArrayIndexOutOfBoundsException("length=" + mLength + "; index: " + location);
    }

    /**
     *
     */
    @Override
    public T set(int location, T object) {
        if (location >= 0 && location < mLength) {
            int index = (mFront + location) % mQueue.length;
            T ret = (T) mQueue[index];

            mQueue[index] = object;

            return ret;
        }

        throw new ArrayIndexOutOfBoundsException("length=" + mLength + "; index: " + location);
    }

    /**
     *
     */
    @Override
    public boolean add(T object) {
        add(mLength, object); return true;
    }

    /**
     *
     */
    @Override
    public void add(int location, T object) {
        if (location >= 0 && location <= mLength) {
            refactor();

            int index = (mFront + location) % mQueue.length;

            if (location == mLength) {
                // Add the element to the end of the list
                mQueue[index] = object;

            } else {
                int lastIndex = (mFront + mLength) % mQueue.length;

                if (location == 0 && mFront != 0) {
                    // Prepends to the beginning by moving the front back
                    mFront--;
                    mQueue[mFront] = object;

                } else {
                    if (mFront != 0 && location < ((int) (mLength / 2))) {
                        // Move some elements backward, move the front and add the element in between

                        for (int i = 0; i < location; i++) {
                            int dst = (mFront + i - 1) % mQueue.length;
                            int src = (mFront + i) % mQueue.length;

                            mQueue[dst] = mQueue[src];
                        }

                        mFront--;
                        index--;

                    } else {
                        // Move some elements forward and add the element in between

                        for (int i = mLength; i > location; i--) {
                            int dst = (mFront + i) % mQueue.length;
                            int src = (mFront + i - 1) % mQueue.length;

                            mQueue[dst] = mQueue[src];
                        }
                    }

                    mQueue[index] = object;
                }
            }

            mLength++;

        } else {
            throw new ArrayIndexOutOfBoundsException("length=" + mLength + "; index: " + location);
        }
    }

    /**
     *
     */
    @Override
    public void clear() {
        if (mLength > 0) {
            for (int i=0; i < mLength; i++) {
                int index = (mFront + i) % mQueue.length;

                mQueue[index] = null;
            }

            mLength = 0;
            mFront = 0;

            refactor();
        }
    }

    /**
     *
     */
    @Override
    public T remove(int location) {
        if (mLength > 0 && location < mLength && location >= 0) {
            int index = (mFront + location) % mQueue.length;
            T ret = (T) mQueue[index];

            mQueue[index] = null;
            mLength--;

            // There are still items in the list, let's see how we should handle them.
            if (mLength > 0) {
                if (index == mFront) {
                    // This item was the first in the list, push the front ahead
                    mFront++;

                } else if (index != mLength) {
                    // This was not the last or first item, so we need to move some things around

                    if (location > ((int) (mLength / 2))) {
                        /* The removed item was placed above the middle if the list.
                         * We move everything that is placed after it, one position back.
                         */

                        for (int i=location; i < mLength; i++) {
                            int dst = (mFront + i) % mQueue.length;
                            int src = (mFront + i + 1) % mQueue.length;

                            mQueue[dst] = mQueue[src];
                        }

                        int last = (mFront + mLength + 1) % mQueue.length;

                        mQueue[last] = null;

                    } else {
                        /* The removed item was placed before the middle of the list.
                         * We move everything that is placed before it, one position ahead and push the front.
                         */

                        for (int i=location; i > 0; i--) {
                            int dst = (mFront + i) % mQueue.length;
                            int src = (mFront + i - 1) % mQueue.length;

                            mQueue[dst] = mQueue[src];
                        }

                        mQueue[mFront] = null;
                        mFront++;
                    }
                }

            } else {
                // This was the last item, reset front to 0
                mFront = 0;
            }

            refactor();

            return ret;
        }

        throw new ArrayIndexOutOfBoundsException("length=" + mLength + "; index: " + location);
    }

    /**
     *
     */
    @Override
    public boolean remove(Object value) {
        if (mLength > 0) {
            for (int i=0; i < mLength; i++) {
                int index = (mFront + i) % mQueue.length;

                if ((value == null && mQueue[index] == null)
                        || (value != null && value.equals(mQueue[index]))) {

                    remove(i);

                    return true;
                }
            }
        }

        return false;
    }

    /**
     *
     */
    @Override
    public int indexOf(Object value) {
        if (mLength > 0) {
            for (int i=0; i < mLength; i++) {
                int index = (mFront + i) % mQueue.length;

                if ((value == null && mQueue[index] == null)
                        || (value != null && value.equals(mQueue[index]))) {

                    return i;
                }
            }
        }

        return -1;
    }

    /**
     *
     */
    @Override
    public int lastIndexOf(Object value) {
        if (mLength > 0) {
            for (int i=mLength-1; i >= 0; i--) {
                int index = (mFront + i) % mQueue.length;

                if ((value == null && mQueue[index] == null)
                        || (value != null && value.equals(mQueue[index]))) {

                    return i;
                }
            }
        }

        return -1;
    }

    /**
     *
     */
    @Override
    public boolean contains(Object object) {
        return indexOf(object) >= 0;
    }

    /**
     *
     */
    @Override
    public Object[] toArray() {
        Object[] ret = new Object[mLength];

        if (mLength > 0) {
            for (int i=0; i < mLength; i++) {
                int index = (mFront + i) % mQueue.length;

                ret[i] = mQueue[index];
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
            for (int i=0; i < mLength; i++) {
                int index = (mFront + i) % mQueue.length;

                array[i] = (TI) mQueue[index];
            }
        }

        return array;
    }

    /**
     *
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private int mPointer = mFront;
            private int mLastFront = mFront;

            private int getPointer() {
                if (mLastFront != mFront) {
                    mPointer += (mFront - mLastFront);
                    mLastFront = mFront;
                }

                return mPointer;
            }

            @Override
            public boolean hasNext() {
                return getPointer() < mLength;
            }

            @SuppressWarnings("unchecked")
            @Override
            public T next() {
                int pointer = getPointer();

                if (pointer < mLength) {
                    mPointer++;

                    return (T) mQueue[pointer];
                }

                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                SwiftList.this.remove(mPointer);
            }
        };
    }

    @Override
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<T> listIterator(int location) {
        throw new UnsupportedOperationException();
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
}
