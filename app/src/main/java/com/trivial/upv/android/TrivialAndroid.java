package com.trivial.upv.android;

import android.app.Application;

import com.trivial.upv.android.helper.singleton.VolleySingleton;

/**
 * Created by jvg63 on 01/08/2017.
 */

public class TrivialAndroid extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        VolleySingleton.getInstance(getApplicationContext());

    }
}
