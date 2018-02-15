package com.spazedog.lib.utilsLib.collection;

/**
 *
 */
public final class SwiftStack<T> implements Pool<T> {

    /** * */
    private volatile Object[] mStack = null;

    /** * */
    private int mInitSize;

    /** * */
    private final float mResize = 1.35f;

    /** * */
    private final float mDownsize = 0.55f;

    /** * */
    private volatile int mLength = 0;

    /**
     *
     */
    private void refactor() {
        if (mStack == null) {
            mStack = new Object[ mInitSize ];

        } else {
            int minLength = mLength < mInitSize ? mInitSize : mLength;

            if (mLength >= mStack.length || ((int) (mStack.length * mDownsize)) > minLength) {
                Object[] newStack = new Object[ ((int) (minLength * mResize)) + 1 ];

                for (int i=0; i < mLength; i++) {
                    newStack[i] = mStack[i];
                }

                mStack = newStack;
            }
        }
    }

    /**
     *
     */
    public SwiftStack() {
        mInitSize = 10;
    }

    /**
     *
     */
    public SwiftStack(int capacity) {
        if (capacity < 10) {
            capacity = 10;
        }

        mInitSize = capacity;
    }

    /**
     *
     */
    @Override
    public synchronized void add(T value) {
        refactor();

        mStack[ mLength++ ] = value;

        notifyAll();
    }

    /**
     *
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized T poll(long timeout) {
        T ret = null;
        long time = timeout > 0 ? System.currentTimeMillis() + timeout : 0;

        for (;;) {
            if (mLength > 0) {
                ret = (T) mStack[--mLength];
                mStack[mLength] = null;

                refactor();

                break;

            } else if (timeout >= 0) {
                try {
                    wait(timeout);

                    if (timeout > 0 && System.currentTimeMillis() >= time) {
                        continue;
                    }

                } catch (InterruptedException e) {}
            }
        }

        return ret;
    }

    /**
     *
     */
    @Override
    public synchronized T poll() {
        return poll(0);
    }

    /**
     *
     */
    @Override
    public synchronized T pop() {
        return poll(-1);
    }

    /**
     *
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized T peek() {
        if (mLength > 0) {
            return (T) mStack[mLength-1];
        }

        return null;
    }

    /**
     *
     */
    @Override
    public synchronized void clear() {
        if (mLength > 0) {
            for (int i=0; i < mLength; i++) {
                mStack[i] = null;
            }

            mLength = 0;

            refactor();
        }
    }
}
