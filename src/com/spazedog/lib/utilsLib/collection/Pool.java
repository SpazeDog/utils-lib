package com.spazedog.lib.utilsLib.collection;

/**
 *
 */
public interface Pool<T> {

    /**
     *
     */
    void add(T value);

    /**
     *
     */
    T poll(long timeout);

    /**
     *
     */
    T poll();

    /**
     *
     */
    T pop();

    /**
     *
     */
    T peek();

    /**
     *
     */
    void clear();
}
