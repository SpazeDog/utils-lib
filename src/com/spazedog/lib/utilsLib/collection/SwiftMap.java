package com.spazedog.lib.utilsLib.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class SwiftMap<K,V> implements Map<K,V> {

    /** * */
    private MapCollections<K,V> mCollections;

    /** * */
    private final Object mDeleted = new Object();

    /** * */
    private Object[] mArray;

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
            mArray = new Object[ (mInitSize<<1) ];

        } else {
            int length = mLength - mGCLength;
            int minLength = length < mInitSize ? mInitSize : length;

            if (mLength >= mHashKeys.length || ((int) (mHashKeys.length * mDownsize)) > minLength) {
                if (mGCLength > 0) {
                    int x = 0;
                    int kx = 0;
                    int vx = 1;

                    for (int i=0,k=0,v=1; k < (mLength<<1); i++,k+=2,v+=2) {
                        if (mArray[v] != mDeleted) {
                            if (kx != k) {
                                mHashKeys[x] = mHashKeys[x];
                                mArray[kx] = mArray[k];
                                mArray[vx] = mArray[v];
                            }

                            x++;
                            kx += 2;
                            vx += 2;
                        }
                    }

                    mLength -= mGCLength;
                    mGCLength = 0;
                    minLength = mLength < mInitSize ? mInitSize : mLength;
                }

                if (mLength >= mHashKeys.length || ((int) (mHashKeys.length * mDownsize)) > minLength) {
                    int[] newHashKeys = new int[ ((int) (minLength * mResize)) + 1 ];
                    Object[] newArray = new Object[newHashKeys.length << 1];

                    System.arraycopy(newHashKeys, 0, mHashKeys, 0, mLength);
                    System.arraycopy(newArray, 0, mArray, 0, mLength << 1);

                    mHashKeys = newHashKeys;
                    mArray = newArray;
                }
            }
        }
    }

    /**
     *
     */
    private int keyIndex(Object key, boolean findEmpty) {
        if (size() == 0) {
            return -1;
        }

        int hash = key == null ? 0 : key.hashCode();
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

            if ((key == null && mArray[lo<<1] == null) || (key != null && key.equals(mArray[lo<<1]))) {
                return mArray[lo<<1] == mDeleted ? ~lo : lo;
            }

            int end = lo+1;
            int begin = lo-1;
            int empty = -1;

        	/* First check if the current index or any following ones matches the key that we are looking for
        	 */
            for (; end < mLength && mHashKeys[end] == hash; end++) {
                if ((key == null && mArray[end<<1] == null) || (key != null && key.equals(mArray[end<<1]))) {
                    return mArray[end<<1] == mDeleted ? ~end : end;

                } else if (findEmpty && empty < 0 && mArray[end<<1] == mDeleted) {
                    empty = end;
                }
            }

        	/* If no key was found, let's check to see if it comes before the index we found
        	 */
            for (; begin >= 0 && mHashKeys[begin] == hash; begin--) {
                if ((key == null && mArray[begin<<1] == null) || (key != null && key.equals(mArray[begin<<1]))) {
                    return mArray[begin<<1] == mDeleted ? ~begin : begin;

                } else if (findEmpty && empty < 0 && mArray[begin<<1] == mDeleted) {
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
    private int valueIndex(Object value) {
        for (int i=0; i < mLength; i++) {
            if ((value == null && mArray[i<<1+1] == null)
                    || (value != null && value.equals(mArray[i<<1+1]))) {

                return i;
            }
        }

        return -1;
    }

    /**
     *
     */
    public SwiftMap() {
        mInitSize = 10;
    }

    /**
     *
     */
    public SwiftMap(int capacity) {
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
        return mLength - mGCLength;
    }

    /**
     *
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     *
     */
    @Override
    public void clear() {
        if (size() > 0) {
            for (int i = 0; i < mLength<<1; i++) {
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
    public boolean containsKey(Object key) {
        return keyIndex(key, false) >= 0;
    }

    /**
     *
     */
    @Override
    public boolean containsValue(Object value) {
        return valueIndex(value) >= 0;
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        int index = keyIndex(key, false);

        if (index >= 0) {
            return (V) mArray[(index<<1)+1];
        }

        return null;
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        int index = keyIndex(key, false);

        if (index >= 0) {
            int vk = (index<<1)+1;
            V ret = (V) mArray[vk];

            mArray[vk-1] = mDeleted;
            mArray[vk] = mDeleted;
            mGCLength++;

            refactor();

            return ret;
        }

        return null;
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    @Override
    public V put(K key, V value) {
        refactor();

        int index = keyIndex(key, true);
        int kk = (index < 0 ? ~index : index)<<1;
        int vk = kk+1;

        if (index < 0 && mArray[vk] != mDeleted) {
            index = ~index;

            System.arraycopy(mHashKeys, index, mHashKeys, index+1, mLength - index);
            System.arraycopy(mArray, kk, mArray, (index+1)<<1, (mLength - index) << 1);

            mHashKeys[index] = key.hashCode();
            mArray[kk] = key;
            mArray[vk] = value;
            mLength++;

            return null;
        }

        V ret = (V) mArray[vk];
        mArray[vk] = value;
        mHashKeys[index] = key.hashCode();

        if (ret == mDeleted) {
            mGCLength--;

            return null;
        }

        return ret;
    }

    /**
     *
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        for (Iterator<? extends Entry<? extends K, ? extends V>> iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<? extends K, ? extends V> e = iterator.next();
            put(e.getKey(), e.getValue());
        }
    }

    /**
     *
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        return getCollection().getMapEntrySet();
    }

    /**
     *
     */
    @Override
    public Set<K> keySet() {
        return getCollection().getMapKeySet();
    }

    /**
     *
     */
    @Override
    public Collection<V> values() {
        return getCollection().getMapValues();
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    private MapCollections<K, V> getCollection() {
        if (mCollections == null) {
            mCollections = new MapCollections<K, V>() {

                @Override
                K colGetKeyAt(int index) {
                    index = index<<1;

                    while (index < mArray.length) {
                        if (mArray[index] != mDeleted) {
                            return (K) mArray[index];
                        }

                        index += 2;
                    }

                    return null;
                }

                @Override
                V colGetValueAt(int index) {
                    index = (index<<1)+1;

                    while (index < mArray.length) {
                        if (mArray[index] != mDeleted) {
                            return (V) mArray[index];
                        }

                        index += 2;
                    }

                    return null;
                }

                @Override
                int colGetSize() {
                    return size();
                }

                @Override
                int colIndexOfKey(Object key) {
                    return keyIndex(key, false);
                }

                @Override
                int colIndexOfValue(Object value) {
                    return valueIndex(value);
                }
            };
        }

        return mCollections;
    }
}
