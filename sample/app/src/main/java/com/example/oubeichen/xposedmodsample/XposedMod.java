package com.example.oubeichen.xposedmodsample;

import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

/**
 * Created by oubeichen on 12/10/15.
 */
public class XposedMod implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {


    public static XSharedPreferences mPrefs;

    public static void loadPrefs() {
        (mPrefs = new XSharedPreferences(Constant.PACKAGENAME, Constant.PREFS)).makeWorldReadable();
    }

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        loadPrefs();
    }

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals("com.android.systemui"))
            return;
        try {
            Class<?> clazz = findClass("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader);
            XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    AndroidAppHelper.currentApplication()
                            .registerReceiver(new XposedReceiver(param.thisObject),
                            new IntentFilter(Constant.UPDATE));
                    XposedBridge.log("registerd");
                }
            });
        } catch (Throwable ex) {
            XposedBridge.log("cannot find class");
        }
        //https://github.com/MoKee/android_frameworks_base/blob/kk_mkt/packages/SystemUI/src/com/android/systemui/statusbar/policy/Clock.java
        findAndHookMethod("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader, "updateClock", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                mPrefs.reload();
                if (mPrefs.getBoolean(Constant.TOGGLE1, false)) {
                    TextView tv = (TextView) param.thisObject;
                    String text = tv.getText().toString();
                    tv.setText(text + " :)");
                    tv.setTextColor(Color.RED);
                }
            }
        });

    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        Context context = AndroidAppHelper.currentApplication();
        if (!resparam.packageName.equals("im.mixbox.magnet"))
            return;

        mPrefs.reload();
        if (mPrefs.getBoolean(Constant.TOGGLE2, false)) {
            resparam.res.setReplacement("im.mixbox.magnet", "string", "wechat_login", "哈哈,你被Hack了!!");
            resparam.res.setReplacement("im.mixbox.magnet", "drawable", "login_img_bg", new XResources.DrawableLoader() {
                @Override
                public Drawable newDrawable(XResources res, int id) throws Throwable {
                    Bitmap bitmap = null;
                    try {
                        File file = new File(Constant.FILEPATH);
                        if (file.exists()) {
                            bitmap = BitmapFactory.decodeFile(Constant.FILEPATH);
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                    return new BitmapDrawable(bitmap);
                }
            });
        }
    }

}