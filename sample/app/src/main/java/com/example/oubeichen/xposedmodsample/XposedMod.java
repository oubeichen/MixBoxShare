package com.example.oubeichen.xposedmodsample;

import android.content.res.XResources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

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

        findAndHookMethod("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader, "updateClock", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                mPrefs.reload();
                if(mPrefs.getBoolean(Constant.TOGGLE1, false)){
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
        if (!resparam.packageName.equals("im.mixbox.magnet"))
            return;

        mPrefs.reload();
        if(mPrefs.getBoolean(Constant.TOGGLE2, false)) {
            resparam.res.setReplacement("im.mixbox.magnet", "string", "wechat_login", "哈哈,你被Hack了!!");
            resparam.res.setReplacement("im.mixbox.magnet", "drawable", "login_img_bg", new XResources.DrawableLoader() {
                @Override
                public Drawable newDrawable(XResources res, int id) throws Throwable {
                    return new ColorDrawable(Color.WHITE);
                }
            });
        }
    }
}