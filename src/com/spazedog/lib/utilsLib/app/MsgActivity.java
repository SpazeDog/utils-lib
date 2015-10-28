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

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.spazedog.lib.utilsLib.HashBundle;
import com.spazedog.lib.utilsLib.app.logic.ActivityConnector;
import com.spazedog.lib.utilsLib.app.logic.ActivityLogic;
import com.spazedog.lib.utilsLib.app.logic.FragmentConnector;

public class MsgActivity extends AppCompatActivity implements ActivityConnector<AppCompatActivity> {

    private ActivityLogic AL_Logic;

    @Override
    public MsgActivity getActivity() {
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AL_Logic = new ActivityLogic(this);
        AL_Logic.onCreate();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onRestart() {
        AL_Logic.onRestart();
        super.onRestart();
    }

    @Override
    protected void onStart() {
        AL_Logic.onStart();
        super.onStart();
    }

    @Override
    protected void onResume() {
        AL_Logic.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        AL_Logic.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        AL_Logic.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AL_Logic.onDestroy();
        AL_Logic = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        AL_Logic.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onReceiveMessage(int type, HashBundle data, boolean isSticky) {}

    @Override
    public final void onFragmentAttachment(FragmentConnector connector) {
        AL_Logic.onFragmentAttachment(connector);
    }

    @Override
    public final void onFragmentDetachment(FragmentConnector connector) {
        AL_Logic.onFragmentDetachment(connector);
    }

    @Override
    public final void sendMessage(int type, String key, Object value) {
        AL_Logic.sendMessage(type, new HashBundle(key, value), false, 0);
    }

    @Override
    public final void sendMessage(int type, String key, Object value, boolean sticky) {
        AL_Logic.sendMessage(type, new HashBundle(key, value), sticky, 0);
    }

    @Override
    public final void sendMessage(int type, String key, Object value, boolean sticky, int event) {
        AL_Logic.sendMessage(type, new HashBundle(key, value), sticky, event);
    }

    @Override
    public final void sendMessage(int type, HashBundle data, boolean sticky) {
        AL_Logic.sendMessage(type, data, false, 0);
    }

    @Override
    public final void sendMessage(int type, HashBundle data, boolean sticky, int event) {
        AL_Logic.sendMessage(type, data, false, event);
    }
}
