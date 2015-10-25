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

package com.spazedog.lib.utilsLib.app;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.spazedog.lib.utilsLib.HashBundle;

public class MsgContext extends ContextWrapper {

    public interface MsgListener {
        void onReceiveMessage(int type, HashBundle data, boolean sticky);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mListener != null) {
                int type = intent.getIntExtra("type", 0);
                HashBundle data = (HashBundle) intent.getParcelableExtra("data");
                boolean sticky = intent.getBooleanExtra("sticky", false);

                mListener.onReceiveMessage(type, data, sticky);
            }
        }
    };

    protected MsgListener mListener;

    public MsgContext(Context base) {
        super(base.getApplicationContext());
    }

    public void sendMessage(int type, String key, Object value) {
        sendMessage(type, new HashBundle(key, value), false);
    }

    public void sendMessage(int type, String key, Object value, boolean sticky) {
        sendMessage(type, new HashBundle(key, value), sticky);
    }

    public void sendMessage(int type, HashBundle data, boolean sticky) {
        Intent intent = new Intent("MsgBroadcaster");
        intent.putExtra("type", type);
        intent.putExtra("sticky", sticky);
        intent.putExtra("data", data);

        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);
    }

    public void setMsgListener(MsgListener listener) {
        if (listener == null) {
            LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(mMessageReceiver);

        } else if (mListener == null) {
            LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(mMessageReceiver, new IntentFilter("MsgBroadcaster"));
        }

        mListener = listener;
    }
}
