package com.example.oubeichen.xposedmodsample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by oubeichen on 2015/12/11 0011.
 */
public class XposedReceiver extends BroadcastReceiver {

    private final Object clockSvc;

    public XposedReceiver(final Object object) {
        super();
        clockSvc = object;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        XposedBridge.log("on receive");
        if (Constant.UPDATE_CLOCK_ACTION.equals(intent.getExtras().getString(Constant.ACTION))) {
            XposedHelpers.callMethod(clockSvc, "updateClock");
        }
    }
}
