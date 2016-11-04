package com.example.oubeichen.xposedmodsample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import java.lang.reflect.Method;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedHelpers.findClass;

/**
 * Created by oubeichen on 2016/11/4.
 */

public class XposedKillAppReceiver extends BroadcastReceiver {
    private final Object pmSvc;
    private final Map<String, Object> mPackages;

    public XposedKillAppReceiver(final Object pmSvc) {
        super();
        this.pmSvc = pmSvc;
        this.mPackages = (Map<String, Object>) XposedHelpers.getObjectField(pmSvc, "mPackages");
    }

    public static void initPmSvcHook() {
        Class<?> hookClass = null;
        try {
            hookClass = findClass("com.android.server.pm.PackageManagerService", XposedMod.class.getClassLoader());
        } catch (Throwable ex) {
            XposedBridge.log("Class or method not found");
            return;
        }
        for (Method hookMethod : hookClass.getDeclaredMethods()) {
            if (hookMethod.getName().equals("systemReady")) {
                XposedBridge.hookMethod(hookMethod, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        ((Context) XposedHelpers.getObjectField(param.thisObject, "mContext"))
                                .registerReceiver(new XposedKillAppReceiver(param.thisObject),
                                        new IntentFilter(Constant.KILL),
                                        Constant.PACKAGENAME + ".BROADCAST_PERMISSION"
                                        , null);
                    }
                });
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Constant.KILL_ACTION.equals(intent.getExtras().getString(Constant.ACTION, ""))) {
            return;
        }
        String packageName = intent.getExtras().getString(Constant.KILL_PACKAGE);
        Object pkg = mPackages.get(packageName);
        ApplicationInfo appInfo = (ApplicationInfo) XposedHelpers.getObjectField(pkg, "applicationInfo");
        try {
            if (Build.VERSION.SDK_INT <= 18) { // JellyBean 以前的版本 killApplictaion只有两个参数
                XposedHelpers.callMethod(pmSvc, "killApplication", packageName, appInfo.uid);
            } else {
                XposedHelpers.callMethod(pmSvc, "killApplication", packageName, appInfo.uid, "update view from " + Constant.PACKAGENAME);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
