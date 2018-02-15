package com.spazedog.lib.utilsLib.collection;

/**
 *
 */
public final class SwiftQueue<T> implements Pool<T> {

    /** * */
    private volatile Object[] mQueue = null;

    /** * */
    private int mInitSize;

    /** * */
    private final float mResize = 1.35f;

    /** * */
    private final float mDownsize = 0.55f;

    /** * */
    private volatile int mFront = 0;

    /** * */
    private volatile int mLength = 0;

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
    public SwiftQueue() {
        mInitSize = 10;
    }

    /**
     *
     */
    public SwiftQueue(int capacity) {
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

        int last = (mFront + mLength) % mQueue.length;

        mQueue[last] = value;
        mLength++;

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
                int first = mFront % mQueue.length;
                ret = (T) mQueue[first];
                mQueue[first] = null;
                mLength--;

                if (mLength > 0) {
                    mFront++;

                } else {
                    mFront = 0;
                }

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
            int first = mFront % mQueue.length;

            return (T) mQueue[first];
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
                int x = (mFront + i) % mQueue.length;

                mQueue[x] = null;
            }

            mFront = 0;
            mLength = 0;

            refactor();
        }
    }
}
