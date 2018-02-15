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


import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

/**
 * Google created {@link HandlerThread} that could be used to get a different {@link Looper} for {@link Handler}'s.
 * But why create a class that is only meant to be used within another class? It would be much simpler to
 * extend the {@link Handler} class and have it take care of this automatically. This is what this class does.
 * This class will create a {@link Handler} that handles messages from within a new {@link Thread}.
 */
public class ThreadHandler extends Handler {

    /**
     *
     */
    protected static Looper createLooper(String name, int priority) {
        HandlerThread thread = new HandlerThread(name, priority);
        thread.start();

        return thread.getLooper();
    }

    /**
     *
     */
    public ThreadHandler(String name) {
        this(name, Process.THREAD_PRIORITY_BACKGROUND, null);
    }

    /**
     *
     */
    public ThreadHandler(String name, int priority) {
        this(name, priority, null);
    }

    /**
     *
     */
    public ThreadHandler(String name, int priority, Callback callback) {
        super(createLooper(name, priority), callback);
    }

    /**
     *
     */
    public void terminate() {
        Looper looper = getLooper();

        if (looper != null) {
            looper.quit();
        }
    }

    /**
     *
     */
    @TargetApi(VERSION_CODES.JELLY_BEAN_MR2)
    public void shutdown() {
        Looper looper = getLooper();

        if (looper != null) {
            looper.quitSafely();
        }
    }
}
