package com.oubeichen.resourcemanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedHelpers.findClass;

public class XposedModReceiver extends BroadcastReceiver {

    //https://github.com/CyanogenMod/android_frameworks_base/blob/cm-11.0/services/java/com/android/server/pm/PackageManagerService.java

    public static final String CLASS_NAME = "com.android.server.pm.PackageManagerService";
    public static final String METHOD_NAME = "grantPermissionsLPw";

    private final Map<String, Object> mPackages;
    private final Object mSettings;
    private final Object pmSvc;

    public XposedModReceiver(final Object pmSvc) {
        super();
        this.pmSvc = pmSvc;
        this.mPackages = (Map<String, Object>) XposedHelpers.getObjectField(pmSvc, "mPackages");
        this.mSettings = XposedHelpers.getObjectField(pmSvc, "mSettings");
    }

    public static void initHooks(){
        Class<?> hookClass = null;
        try {
            hookClass = findClass(CLASS_NAME, XposedMod.class.getClassLoader());
        } catch (Throwable ex) {
            XposedBridge.log("Class or method not found");
            return;
        }
        for (Method hookMethod : hookClass.getDeclaredMethods()) {
            if (hookMethod.getName().equals("systemReady")) { // 系统启动后会调用　systemReady　这个函数，此时再注册监听器
                XposedBridge.hookMethod(hookMethod, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        ((Context) XposedHelpers.getObjectField(param.thisObject, "mContext"))
                                .registerReceiver((BroadcastReceiver) new XposedModReceiver(param.thisObject),
                                        new IntentFilter(Utils.MY_PACKAGE_NAME + ".UPDATE_PERMISSIONS"),
                                        Utils.MY_PACKAGE_NAME + ".BROADCAST_PERMISSION"
                                        , null);
                    }
                });
            }
            if (hookMethod.getName().equals(METHOD_NAME)) {
                XposedBridge.hookMethod(hookMethod, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String packageName = (String) XposedHelpers.getObjectField(param.args[0], "packageName");
                        Set<String> stringSet = XposedMod.permprefs.getStringSet(packageName + "/perm-restricted", null); //从配置文件里获取要限制的权限
                        if (stringSet != null && !stringSet.isEmpty()) {
                            final ArrayList<String> permList = (ArrayList) XposedHelpers.getObjectField(param.args[0], "requestedPermissions");//获取该应用现有权限
                            param.setObjectExtra("orig_requested_permissions", permList);
                            final ArrayList<String> newPermList = new ArrayList<String>(permList.size());
                            for (final String perm : permList) {　//一个个对比权限，不在列表里就加进去
                                if (!stringSet.contains(perm)) {
                                    newPermList.add(perm);
                                } else {
                                    XposedBridge.log("Not granting permission " + perm + " to package " + packageName
                                            + " because you think it should not have it");
                                }
                            }
                            XposedHelpers.setObjectField(param.args[0], "requestedPermissions", (Object) newPermList);//使用新的权限列表
                        }

                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param)
                            throws Throwable {
                        final ArrayList origPermList = (ArrayList) param.getObjectExtra("orig_requested_permissions");
                        if (origPermList != null) {
                            XposedHelpers.setObjectField(param.args[0], "requestedPermissions", origPermList); //将权限列表改回去，以免引起其他问题
                        }
                    }
                });
            }
        }
    }

    /**
    *   
    *　在系统中注册监听器，当某个应用被我们更改权限后立即杀死该应用（应用正在运行中是不会生效的，必须要重启）
    */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"update_permissions".equals(intent.getExtras().getString("action"))) {
            return;
        }
        XposedMod.permprefs.reload();
        String packageName = intent.getExtras().getString("Package");
        Object pkg = mPackages.get(packageName);
        ApplicationInfo appInfo = (ApplicationInfo)XposedHelpers.getObjectField(pkg, "applicationInfo");
        try {
            if (Build.VERSION.SDK_INT <= 18) { // JellyBean 以前的版本 killApplictaion只有两个参数
                XposedHelpers.callMethod(pmSvc, "killApplication", packageName, appInfo.uid);
            } else {
                XposedHelpers.callMethod(pmSvc, "killApplication", packageName, appInfo.uid, "update permission from " + Utils.MY_PACKAGE_NAME);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
        XposedHelpers.callMethod(pmSvc, "grantPermissionsLPw", pkg, true);
        XposedHelpers.callMethod(mSettings, "writeLPr");
    }

}
