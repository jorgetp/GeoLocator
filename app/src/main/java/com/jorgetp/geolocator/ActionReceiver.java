package com.jorgetp.geolocator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ActionReceiver extends BroadcastReceiver {
    public static final String ACTION_GET_AND_NOTIFY =
            "com.jorgetp.geolocator.ACTION_GET_AND_NOTIFY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_GET_AND_NOTIFY.equals(intent.getAction())) {
            LocationWorker.handleLocation(context, true, null);   // notify = true
        }
    }
}
