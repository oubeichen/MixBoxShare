package com.example.oubeichen.xposedmodsample;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.XResources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

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


    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals("com.android.systemui") && !lpparam.packageName.equals("im.mixbox.magnet"))
            return;
        XposedBridge.log(lpparam.packageName);
        try {
            Class<?> clazz = findClass("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader);
            XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    AndroidAppHelper.currentApplication()
                            .registerReceiver(new XposedReceiver(param.thisObject),
                                    new IntentFilter(Constant.UPDATE));
                }
            });
        } catch (Throwable ex) {
            XposedBridge.log("cannot find class");
        }
        //https://github.com/MoKee/android_frameworks_base/blob/kk_mkt/packages/SystemUI/src/com/android/systemui/statusbar/policy/Clock.java
        try {
            findAndHookMethod("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader, "updateClock", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    mPrefs.reload();
                    TextView tv = (TextView) param.thisObject;
                    if (mPrefs.getBoolean(Constant.TOGGLE1, false)) {
                        String text = tv.getText().toString();
                        tv.setText(text + " :)");
                        tv.setTextColor(Color.RED);
                    } else {
                        tv.setTextColor(Color.WHITE);
                    }
                }
            });
        } catch (Throwable ex) {
        }
        try {
            findAndHookMethod("im.mixbox.magnet.activities.LoadActivity", lpparam.classLoader, "wechatLogin", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    if (mPrefs.getBoolean(Constant.TOGGLE3, false)) {
                        Toast.makeText(AndroidAppHelper.currentApplication(), "粑粑不想让你登录", Toast.LENGTH_SHORT).show();
                        methodHookParam.setResult(null);
                    }
                }
            });
            findAndHookMethod("im.mixbox.magnet.activities.LoadActivity", lpparam.classLoader, "emailLogin", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    if (mPrefs.getBoolean(Constant.TOGGLE3, false)) {
                        Toast.makeText(AndroidAppHelper.currentApplication(), "麻麻不想让你登录", Toast.LENGTH_SHORT).show();
                        methodHookParam.setResult(null);
                    }
                }
            });
            findAndHookMethod("im.mixbox.magnet.activities.LoadActivity", lpparam.classLoader, "register", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    if (mPrefs.getBoolean(Constant.TOGGLE3, false)) {
                        Toast.makeText(AndroidAppHelper.currentApplication(), "宝宝不想让你注册", Toast.LENGTH_SHORT).show();
                        methodHookParam.setResult(null);
                    }
                }
            });
        } catch (Exception ex) {

        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        Context context = AndroidAppHelper.currentApplication();
        if (!resparam.packageName.equals("im.mixbox.magnet"))
            return;

        mPrefs.reload();
        if (mPrefs.getBoolean(Constant.TOGGLE2, false)) {
            resparam.res.setReplacement("im.mixbox.magnet", "string", "wechat_login", "哈哈,你被Hack了!!");
            resparam.res.setReplacement("im.mixbox.magnet", "drawable", "bg_splash", new XResources.DrawableLoader() {
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