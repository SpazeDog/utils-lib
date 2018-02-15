package com.spazedog.lib.utilsLib.collection;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This is a rewritten version of Android's hidden MapCollections.
 * It works as a helper class for easier including collections to custom {@link Map} implementations.
 *
 * At the moment, these tools only support lookups. There is no support for altering maps using the collection tools.
 */
public abstract class MapCollections<K,V> {

    protected Set<Map.Entry<K, V>> mMapEntrySet;
    protected Set<K> mMapKeySet;
    protected Collection<V> mMapValues;

    public Set<Map.Entry<K, V>> getMapEntrySet() {
        return mMapEntrySet != null ? mMapEntrySet : (mMapEntrySet = new EntrySet());
    }

    public Set<K> getMapKeySet() {
        return mMapKeySet != null ? mMapKeySet : (mMapKeySet = new KeySet());
    }

    public Collection<V> getMapValues() {
        return mMapValues != null ? mMapValues : (mMapValues = new ValuesCollection());
    }

    /**
     * Base Iterator
     */
    protected abstract class MapIterator<T> implements Iterator<T> {
        int mNextIndex = 0;
        int mCurrentIndex = 0;

        abstract T getNextOccurrence();

        @Override
        public boolean hasNext() {
            return mNextIndex < colGetSize();
        }

        @Override
        public T next() {
            T ret = getNextOccurrence();
            mCurrentIndex = mNextIndex;
            mNextIndex++;
            return (T) ret;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Map Values Iterator
     */
    protected class ValuesIterator extends MapIterator<V> {

        @Override
        protected V getNextOccurrence() {
            return (V) colGetValueAt(mNextIndex);
        }
    }

    /**
     * Map Keys Iterator
     */
    protected class KeysIterator extends MapIterator<K> {

        @Override
        protected K getNextOccurrence() {
            return (K) colGetKeyAt(mNextIndex);
        }
    }

    /**
     * Map Entry Iterator
     */
    protected class EntryIterator extends MapIterator<Map.Entry<K, V>> implements Map.Entry<K, V> {

        @Override
        protected Map.Entry<K, V> getNextOccurrence() {
            return this;
        }

        @Override
        public K getKey() {
            return colGetKeyAt(mCurrentIndex);
        }

        @Override
        public V getValue() {
            return colGetValueAt(mCurrentIndex);
        }

        @Override
        public V setValue(V object) {
            throw new UnsupportedOperationException();
        }

        public boolean equals(Object object) {
            if (object != null && object instanceof Map.Entry) {
                Map.Entry<?,?> entry = (Map.Entry<?,?>) object;

                Object key1 = colGetKeyAt(mCurrentIndex);
                Object key2 = entry.getKey();

                if (key1 == key2 || (key1 != null && key1.equals(key2))) {
                    Object val1 = colGetValueAt(mCurrentIndex);
                    Object val2 = entry.getValue();

                    return val1 == val2 || (val1 != null && val1.equals(val2));
                }
            }

            return false;
        }

        public int hashCode() {
            Object key = colGetKeyAt(mCurrentIndex);
            Object val = colGetValueAt(mCurrentIndex);

            return (key==null   ? 0 : key.hashCode()) ^
                    (val==null ? 0 : val.hashCode());
        }
    }

    /**
     * Map Base Set
     */
    protected abstract class MapSet<T> extends AbstractSet<T> {

        @Override
        public abstract boolean contains(Object object);

        @Override
        public abstract Iterator<T> iterator();

        @Override
        public boolean isEmpty() {
            return colGetSize() == 0;
        }

        @Override
        public int size() {
            return colGetSize();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T1> T1[] toArray(T1[] array) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object object) {
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
        public boolean containsAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(T object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends T> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Map Key Set
     */
    protected class KeySet extends MapSet<K> {

        @Override
        public boolean contains(Object object) {
            if (object != null) {
                return colIndexOfKey(object) >= 0;
            }

            return false;
        }

        @Override
        public Iterator<K> iterator() {
            return new KeysIterator();
        }
    }

    /**
     * Map Entry Set
     */
    protected class EntrySet extends MapSet<Map.Entry<K,V>> {

        @Override
        public boolean contains(Object object) {
            if (object != null && object instanceof Map.Entry) {
                Map.Entry<?,?> entry = (Map.Entry<?,?>) object;
                int index = colIndexOfKey(entry.getKey());

                if (index >= 0) {
                    Object val1 = colGetValueAt(index);
                    Object val2 = entry.getValue();

                    return val1 == val2 || (val1 != null && val1.equals(val2));
                }
            }

            return false;
        }

        @Override
        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }
    }

    /**
     * Map Values Collection
     */
    protected class ValuesCollection implements Collection<V> {

        @Override
        public boolean contains(Object object) {
            return colIndexOfValue(object) >= 0;
        }

        @Override
        public Iterator<V> iterator() {
            return new ValuesIterator();
        }

        @Override
        public int size() {
            return colGetSize();
        }

        @Override
        public boolean isEmpty() {
            return colGetSize() == 0;
        }

        @Override
        public boolean add(V object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends V> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
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
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] array) {
            throw new UnsupportedOperationException();
        }
    };

    /*
     * Abstract method for the class implementing the above features
     */
    abstract K colGetKeyAt(int index);
    abstract V colGetValueAt(int index);
    abstract int colGetSize();
    abstract int colIndexOfKey(Object key);
    abstract int colIndexOfValue(Object value);
}
