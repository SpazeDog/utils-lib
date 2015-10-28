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

package com.spazedog.lib.utilsLib.app.logic;


import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.util.Log;

import com.spazedog.lib.utilsLib.HashBundle;
import com.spazedog.lib.utilsLib.app.MsgContext;
import com.spazedog.lib.utilsLib.app.MsgContext.MsgContextListener;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ActivityLogic {

    public static final int MSG_ACTIVITY_RESULT = -1;
    public static final int MSG_BACKSTACK_CHANGE = -2;
    public static final int MSG_FRAGMENT_ATTACHMENT = -3;
    public static final int MSG_FRAGMENT_DETACHMENT = -4;

    protected static final int FLAG_CREATE = 0x00000001;
    protected static final int FLAG_START = 0x00000002;
    protected static final int FLAG_RESUME = 0x00000004;
    protected static final int FLAG_PAUSE = 0x00000008;
    protected static final int FLAG_STOP = 0x00000010;

    protected class StateRunnable implements Runnable {

        private final int mState;

        public StateRunnable(int state) {
            mState = state;
        }

        @Override
        public void run() {
            AL_STATE = mState;

            for (MsgContainer msg : AL_MSGS.values()) {
                if (!msg.INVOKED && msg.EVENT != 0 && (AL_STATE & msg.EVENT) == msg.EVENT) {
                    Set<MsgBroadcastReceiver> broadcasters = getBroadcasterSet();

                    msg.INVOKED = true;
                    int type = msg.TYPE;
                    HashBundle data = msg.DATA;

                    if (!msg.STICKY) {
                        AL_MSGS.remove(type);
                    }

                    for (MsgBroadcastReceiver broadcaster : broadcasters) {
                        broadcaster.onReceiveMessage(type, data, false);
                    }
                }
            }
        }
    }

    private MsgContextListener mMessageReceiver = new MsgContextListener() {
        @Override
        public void onReceiveMessage(int type, HashBundle data, boolean sticky, int event) {
            synchronized(AL_Fragments) {
                if (sticky || (event != 0 && (AL_STATE & event) != event)) {
                    MsgContainer msg = AL_MSGS.get(type);

                    if (msg == null) {
                        msg = new MsgContainer();
                    }

                    msg.INVOKED = false;
                    msg.STICKY = sticky;
                    msg.EVENT = event;
                    msg.TYPE = type;
                    msg.DATA = data;

                    AL_MSGS.put(type, msg);

                } else {
                    AL_MSGS.remove(type);
                }

                Log.d("LifeCycle Test", "Logic: Receiving message type(" + type + "), data(" + (data != null ? "object" : "null") + "), sticky(" + (sticky ? "true" : "false") + "), event(" + event + ")");

                if (event == 0 || (AL_STATE & event) == event) {
                    final int msgType = type;
                    final HashBundle msgData = data;

                    AL_Activity.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Set<MsgBroadcastReceiver> broadcasters = getBroadcasterSet();

                            for (MsgBroadcastReceiver broadcaster : broadcasters) {
                                broadcaster.onReceiveMessage(msgType, msgData, false);
                            }
                        }
                    });
                }
            }
        }
    };

    private OnBackStackChangedListener mBackstackListener = new OnBackStackChangedListener() {
        @Override
        public void onBackStackChanged() {
            sendMessage(-2, null, false, 0);
        }
    };

    protected static class MsgContainer {
        public boolean INVOKED = false;
        public boolean STICKY = false;
        public int TYPE = 0;
        public int EVENT = 0;
        public HashBundle DATA;
    }

    protected Runnable AL_RUN_CREATE = new StateRunnable(MsgBroadcastDelivery.EVENT_CREATE);
    protected Runnable AL_RUN_START = new StateRunnable(MsgBroadcastDelivery.EVENT_START);
    protected Runnable AL_RUN_RESUME = new StateRunnable(MsgBroadcastDelivery.EVENT_RESUME);
    protected Runnable AL_RUN_PAUSE = new StateRunnable(MsgBroadcastDelivery.EVENT_PAUSE);
    protected Runnable AL_RUN_STOP = new StateRunnable(MsgBroadcastDelivery.EVENT_STOP);

    protected Set<FragmentConnector> AL_Fragments = Collections.newSetFromMap(new WeakHashMap<FragmentConnector, Boolean>());
    protected Map<Integer, MsgContainer> AL_MSGS = new ConcurrentHashMap<Integer, MsgContainer>();
    protected ActivityConnector<?> AL_Activity;
    protected Handler AL_Handler;
    protected MsgContext AL_Context;
    protected int AL_STATE = 0;

    public ActivityLogic(ActivityConnector connector) {
        AL_Activity = connector;
    }

    public void onCreate() {
        AL_Handler = getMainHandler();

        AL_STATE = MsgBroadcastDelivery.EVENT_CREATE & ~FLAG_CREATE;
        AL_Handler.post(AL_RUN_CREATE);

        FragmentActivity activity = AL_Activity.getActivity();
        activity.getSupportFragmentManager().addOnBackStackChangedListener(mBackstackListener);

        AL_Context = new MsgContext(activity);
        AL_Context.setMsgListener(mMessageReceiver);
    }

    public void onRestart() {
        AL_STATE = MsgBroadcastDelivery.EVENT_CREATE;
    }

    public void onStart() {
        AL_STATE = MsgBroadcastDelivery.EVENT_START & ~FLAG_START;
        AL_Handler.post(AL_RUN_START);
    }

    public void onResume() {
        AL_STATE = MsgBroadcastDelivery.EVENT_RESUME & ~FLAG_RESUME;
        AL_Handler.post(AL_RUN_RESUME);
    }

    public void onPause() {
        AL_STATE = MsgBroadcastDelivery.EVENT_PAUSE & ~FLAG_PAUSE;
        AL_Handler.post(AL_RUN_PAUSE);
    }

    public void onStop() {
        AL_STATE = MsgBroadcastDelivery.EVENT_STOP & ~FLAG_STOP;
        AL_Handler.post(AL_RUN_STOP);
    }

    public void onDestroy() {
        FragmentActivity activity = AL_Activity.getActivity();
        activity.getSupportFragmentManager().removeOnBackStackChangedListener(mBackstackListener);

        AL_Context.setMsgListener(null);
        AL_Context = null;
        AL_Handler = null;
        AL_Activity = null;
        AL_MSGS.clear();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        HashBundle bundle = new HashBundle();
        bundle.putInt("requestCode", requestCode);
        bundle.putInt("resultCode", resultCode);

        if (data != null) {
            bundle.putParcelable("intent", data);
        }

        sendMessage(-1, bundle, false, 0);
    }

    public void onFragmentAttachment(FragmentConnector connector) {
        sendMessage(-3, null, false, 0);

        AL_Fragments.add(connector);

        if (AL_MSGS.size() > 0) {
            final FragmentConnector msgConnector = connector;

            AL_Activity.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (MsgContainer msg : AL_MSGS.values()) {
                        if (msg.STICKY && (msg.EVENT == 0 || (AL_STATE & msg.EVENT) == msg.EVENT)) {
                            msgConnector.onReceiveMessage(msg.TYPE, msg.DATA, true);

                        } else if (msg.STICKY) {
                            msg.INVOKED = false;
                        }
                    }
                }
            });
        }
    }

    public void onFragmentDetachment(FragmentConnector connector) {
        AL_Fragments.remove(connector);

        sendMessage(-4, null, false, 0);
    }

    public void sendMessage(int type, HashBundle data, boolean sticky, int event) {
        AL_Context.sendMessage(type, data, sticky, event);
    }

    protected Set<MsgBroadcastReceiver> getBroadcasterSet() {
        Set<MsgBroadcastReceiver> broadcasters = new HashSet<MsgBroadcastReceiver>(AL_Fragments);
        broadcasters.add(AL_Activity);

        return broadcasters;
    }

    protected Handler getMainHandler() {
        Activity activity = AL_Activity.getActivity();
        Class<?> clazz = activity.getClass();
        Field field = null;

        do {
            try {
                field = clazz.getDeclaredField("mHandler");

            }  catch (NoSuchFieldException e) {}

        } while (field == null && (clazz = clazz.getSuperclass()) != null);

        if (field != null) {
            field.setAccessible(true);

            try {
                return (Handler) field.get(activity);

            } catch (IllegalAccessException e) {}
        }

        /*
         * Not what we need, but better than NULL
         */
        return new Handler();
    }
}
