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


import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;

import com.spazedog.lib.utilsLib.HashBundle;
import com.spazedog.lib.utilsLib.SparseMap;
import com.spazedog.lib.utilsLib.app.MsgContext;
import com.spazedog.lib.utilsLib.app.MsgContext.MsgListener;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class ActivityLogic {

    public static final int MSG_ACTIVITY_RESULT = -1;
    public static final int MSG_BACKSTACK_CHANGE = -2;
    public static final int MSG_FRAGMENT_ATTACHMENT = -3;
    public static final int MSG_FRAGMENT_DETACHMENT = -4;

    protected Set<FragmentConnector> AL_Fragments = Collections.newSetFromMap(new WeakHashMap<FragmentConnector, Boolean>());
    protected Map<Integer, HashBundle> AL_StickyMsg = new SparseMap<HashBundle>();
    protected WeakReference<ActivityConnector<?>> AL_Activity;
    protected Handler AL_Handler;
    protected MsgContext mContext;

    private MsgListener mMessageReceiver = new MsgListener() {
        @Override
        public void onReceiveMessage(int type, HashBundle data, boolean sticky) {
            synchronized(AL_Fragments) {
                if (sticky) {
                    AL_StickyMsg.put(type, data);

                } else {
                    AL_StickyMsg.remove(type);
                }

                Set<MsgBroadcaster> broadcasters = new HashSet<MsgBroadcaster>(AL_Fragments);
                broadcasters.add(AL_Activity.get());

                AL_Handler.obtainMessage(type, new Object[]{broadcasters, data}).sendToTarget();
            }
        }
    };

    private OnBackStackChangedListener mBackstackListener = new OnBackStackChangedListener() {
        @Override
        public void onBackStackChanged() {
            sendMessage(-2, null, false);
        }
    };

    protected class BroadcastHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Object[] msgObject = (Object[]) msg.obj;
            Set<MsgBroadcaster> broadcasters = (Set<MsgBroadcaster>) msgObject[0];
            HashBundle data = (HashBundle) msgObject[1];

            for (MsgBroadcaster broadcaster : broadcasters) {
                broadcaster.onReceiveMessage(msg.what, data, false);
            }
        }
    }

    public ActivityLogic(ActivityConnector connector) {
        AL_Activity = new WeakReference<ActivityConnector<?>>(connector);
        AL_Handler = new BroadcastHandler();
    }

    public void onCreate() {
        FragmentActivity activity = AL_Activity.get().getActivity();
        activity.getSupportFragmentManager().addOnBackStackChangedListener(mBackstackListener);

        mContext = new MsgContext(activity);
        mContext.setMsgListener(mMessageReceiver);
    }

    public void onDestroy() {
        FragmentActivity activity = AL_Activity.get().getActivity();
        activity.getSupportFragmentManager().removeOnBackStackChangedListener(mBackstackListener);

        mContext.setMsgListener(null);
        mContext = null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        HashBundle bundle = new HashBundle();
        bundle.putInt("requestCode", requestCode);
        bundle.putInt("resultCode", resultCode);

        if (data != null) {
            bundle.putParcelable("intent", data);
        }

        sendMessage(-1, bundle, false);
    }

    public void onFragmentAttachment(FragmentConnector connector) {
        synchronized (AL_Fragments) {
            sendMessage(-3, null, false);

            AL_Fragments.add(connector);

            for (Map.Entry<Integer, HashBundle> entry: AL_StickyMsg.entrySet()) {
                connector.onReceiveMessage(entry.getKey(), entry.getValue(), true);
            }
        }
    }

    public void onFragmentDetachment(FragmentConnector connector) {
        synchronized (AL_Fragments) {
            AL_Fragments.remove(connector);

            sendMessage(-4, null, false);
        }
    }

    public void sendMessage(int type, HashBundle data, boolean sticky) {
        mContext.sendMessage(type, data, sticky);
    }
}
