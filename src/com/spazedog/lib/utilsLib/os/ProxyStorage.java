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

package com.spazedog.lib.utilsLib.os;


import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import com.spazedog.lib.utilsLib.os.ProxyStorage.ProxyWrapper;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * When working with IPC/AIDL it comes handy to create listeners and other things that might
 * need to be stored some where. When storing Binders it's often necessary to check if they have died
 * and then remove them. Implementing listeners for this can some times take a less than small ammount of code,
 * especially since Android's {@link IBinder.DeathRecipient} interface does not parse the Binder object that died, meaning
 * that you need an implementation per Binder. <br /><br />
 *
 * This class makes it very easy to create a storage for proxies ({@link IInterface}).
 * It works just like creating a {@link Set} with <code>add</code>, <code>remove</code> and
 * implementation of {@link Iterable}. The class will automatically listen for dead Binders and
 * remove them. The storage for the proxies uses CopyOnWrite method, which means that
 * they are safe to iterate, even if proxies are removed at the same time.
 */
public class ProxyStorage<T extends IInterface> implements Iterable<ProxyWrapper<T>> {

    /**
     * This is a wrapper for proxies that is used when storing them.
     */
    public interface ProxyWrapper<P extends IInterface> {
        /**
         * Get the {@link IBinder} from the proxy
         */
        public IBinder getBinder();

        /**
         * Get the proxy {@link IInterface}
         */
        public P getProxy();

        /**
         * Shortcut for {@link ProxyStorage#remove(IInterface)}
         */
        public boolean remove();
    }

    /**
     * Internal Usage
     */
    private class ProxyWrapperImpl implements IBinder.DeathRecipient, ProxyWrapper<T> {

        T mProxy;

        protected ProxyWrapperImpl() {}

        @Override
        public void binderDied() {
            ProxyStorage.this.remove(mProxy);
        }

        @Override
        public IBinder getBinder() {
            return mProxy != null ? mProxy.asBinder() : null;
        }

        @Override
        public T getProxy() {
            return mProxy;
        }

        @Override
        public boolean remove() {
            return ProxyStorage.this.remove(mProxy);
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
     * Internal Usage
     */
    private final Set<ProxyWrapper<T>> mProxies = new CopyOnWriteArraySet<ProxyWrapper<T>>();

    /**
     * Return an {@link Iterator} for the content in this class instance. <br /><br />
     *
     * The {@link Iterator} will contain {@link ProxyWrapper} objects wrapping the {@link IInterface} proxies
     */
    @Override
    public Iterator<ProxyWrapper<T>> iterator() {
        return mProxies.iterator();
    }

    /**
     * Add a proxy to this storage.
     * If a proxy with the same {@link IBinder} already exists, this will not be added.
     *
     * @param proxy
     *      The {@link IInterface} proxy
     */
    public boolean add(T proxy) {
        IBinder binder = proxy.asBinder();

        for (ProxyWrapper<T> cur : mProxies) {
            if (cur.getBinder() == binder) {
                return true;
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
     * Remove a proxy from this storage
     *
     * @param proxy
     *      The {@link IInterface} proxy
     */
    public boolean remove(T proxy) {
        IBinder binder = proxy.asBinder();
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
}
