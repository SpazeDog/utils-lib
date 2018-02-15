package com.spazedog.lib.utilsLib.collection;

import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Special to store IInterface proxy instances.
 *
 * This set uses DeathRecipient listeners to automatically remove
 * proxy instances that has died.
 *
 * Internally the set uses CopyOnWriteArraySet to store the instances,
 * and as such this set is thread safe.
 */
public final class ProxySet<T extends IInterface> implements Set<T> {

    /**
     *
     */
    public interface ProxyWrapper<P extends IInterface> {
        /**
         * Get the {@link IBinder} from the proxy
         */
        IBinder getBinder();

        /**
         * Get the proxy {@link IInterface}
         */
        P getProxy();
    }

    /**
     *
     */
    private class ProxyWrapperImpl implements IBinder.DeathRecipient, ProxyWrapper<T> {

        T mProxy;

        protected ProxyWrapperImpl() {}

        @Override
        public void binderDied() {
            ProxySet.this.remove(mProxy);
        }

        @Override
        public IBinder getBinder() {
            return mProxy != null ? mProxy.asBinder() : null;
        }

        @Override
        public T getProxy() {
            return mProxy;
        }

        protected void link(T proxy) throws RemoteException {
            IBinder binder = proxy.asBinder();

            if (binder != null) {
                binder.linkToDeath(this, 0);
            }

            mProxy = proxy;
        }

        protected T unlink() throws RemoteException {
            T proxy = mProxy;
            IBinder binder = proxy.asBinder();

            if (binder != null) {
                binder.unlinkToDeath(this, 0);
            }

            mProxy = null;

            return proxy;
        }
    }

    /**
     *
     */
    private class ProxyIterator implements Iterator<T> {

        private Iterator<ProxyWrapper<T>> mIterator;

        public ProxyIterator(Iterator<ProxyWrapper<T>> itt) {
            mIterator = itt;
        }

        @Override
        public boolean hasNext() {
            return mIterator.hasNext();
        }

        @SuppressWarnings("unchecked")
        @Override
        public T next() {
            return mIterator.next().getProxy();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /** * */
    private Set<ProxyWrapper<T>> mProxies;

    /**
     *
     */
    public ProxySet() {
        mProxies = new CopyOnWriteArraySet<ProxyWrapper<T>>();
    }

    /**
     *
     */
    public ProxySet(Set<ProxyWrapper<T>> container) {
        mProxies = container;
    }

    /**
     *
     */
    @Override
    public boolean add(T proxy) {
        IBinder binder = proxy.asBinder();

        for (ProxyWrapper<T> cur : mProxies) {
            if (cur.getBinder() == binder) {
                return false;
            }
        }

        try {
            ProxyWrapperImpl impl = new ProxyWrapperImpl();
            impl.link(proxy);

            mProxies.add(impl);

        } catch (RemoteException e) {
            return false;
        }

        return true;
    }

    /**
     *
     */
    @Override
    public void clear() {
        for (ProxyWrapper<T> cur : mProxies) {
            try {
                ((ProxyWrapperImpl) cur).unlink();

            } catch (RemoteException e) {}
        }

        mProxies.clear();
    }

    /**
     *
     */
    @Override
    public boolean contains(Object value) {
        for (ProxyWrapper<T> cur : mProxies) {
            if (cur.getBinder() == value) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     */
    @Override
    public boolean isEmpty() {
        return mProxies.isEmpty();
    }

    /**
     *
     */
    @Override
    public Iterator<T> iterator() {
        return new ProxyIterator( mProxies.iterator() );
    }

    /**
     *
     */
    @Override
    public boolean remove(Object proxy) {
        IBinder binder = ((IInterface) proxy).asBinder();
        ProxyWrapperImpl impl = null;

        for (ProxyWrapper<T> cur : mProxies) {
            if (cur.getBinder() == binder) {
                impl = (ProxyWrapperImpl) cur; break;
            }
        }

        try {
            if (impl != null && mProxies.remove(impl)) {
                impl.unlink();
            }

        } catch (RemoteException e) {
            return false;
        }

        return true;
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
        return mProxies.size();
    }

    /**
     *
     */
    @Override
    public Object[] toArray() {
        return mProxies.toArray();
    }

    /**
     *
     */
    @SuppressWarnings("unchecked")
    @Override
    public <TI> TI[] toArray(TI[] array) {
        return mProxies.toArray(array);
    }
}
