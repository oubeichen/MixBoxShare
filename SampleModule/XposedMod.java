package com.oubeichen.resourcemanager;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Message;

import com.oubeichen.resourcemanager.tools.RedirectionItem;
import com.oubeichen.resourcemanager.tools.RedirectionManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

/**
 * Created by oubeichen on 2015/3/29 0029.
 */
public class XposedMod implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    public static XSharedPreferences permprefs;
    public static XSharedPreferences toolsprefs;
    public static XSharedPreferences pathprefs;

    public static void loadPrefs() {
        (permprefs = new XSharedPreferences(Utils.MY_PACKAGE_NAME, Utils.PERM_PREFS_NAME)).makeWorldReadable();
        (toolsprefs = new XSharedPreferences(Utils.MY_PACKAGE_NAME, Utils.TOOLS_PREFS_NAME)).makeWorldReadable();
        (pathprefs = new XSharedPreferences(Utils.MY_PACKAGE_NAME, Utils.PATH_PREFS_NAME)).makeWorldReadable();
    }
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        loadPrefs();
        XposedModReceiver.initHooks();
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        // Image
        workforImage(lpparam);
        // Location
        workforLocation(lpparam);
        // IMEI
        workforIMEI(lpparam);
        // Redirections, doesn't work for now
        workforRedirections(lpparam);
    }

    // 修改相机拍得照片为指定图片
    private void workforImage(XC_LoadPackage.LoadPackageParam lpparam){
        // Image
        final int CAMERA_MSG_RAW_IMAGE        = 0x080;
        final int CAMERA_MSG_COMPRESSED_IMAGE = 0x100;
        final byte[] buffer = new byte[5000000];

        hook_method("android.hardware.Camera.EventHandler", lpparam.classLoader, "handleMessage", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param)
                    throws Throwable {
                toolsprefs.reload();
                if (!toolsprefs.getBoolean("imageEnabled", false)) { //未开启
                    return;
                }
                String imagepath = toolsprefs.getString("imagePath", "");
                if (imagepath.equals("")) {
                    return;
                }
                Context context = AndroidAppHelper.currentApplication();
                Message msg = (Message) param.args[0];
                switch (msg.what) {
                    case CAMERA_MSG_RAW_IMAGE:
                        XposedBridge.log("Raw image");
                        break;
                    case CAMERA_MSG_COMPRESSED_IMAGE:
                        XposedBridge.log("Compressed image");
                        try {
                            FileInputStream fin = new FileInputStream(imagepath);
                            fin.read(buffer);
                            fin.close();
                        } catch (Exception ex) {
                        }
                        msg.obj = buffer;
                        param.args[0] = msg;
                }
            }
        });
    }

    //修改定位信息，很有局限性
    private void workforLocation(XC_LoadPackage.LoadPackageParam lpparam) {
        hook_method("android.net.wifi.WifiManager", lpparam.classLoader, "getScanResults", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //return empty ap list, force apps using gps information
                toolsprefs.reload();
                if(!toolsprefs.getBoolean("locationEnabled", false)){ //未开启
                    return;
                }
                param.setResult(null);
            }
        });

        hook_method("android.telephony.TelephonyManager", lpparam.classLoader, "getCellLocation", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //return empty cell id list
                toolsprefs.reload();
                if(!toolsprefs.getBoolean("locationEnabled", false)){ //未开启
                    return;
                }
                param.setResult(null);
            }
        });

        hook_method("android.telephony.TelephonyManager", lpparam.classLoader, "getNeighboringCellInfo", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                // return empty neighboring cell info list
                toolsprefs.reload();
                if(!toolsprefs.getBoolean("locationEnabled", false)){ //未开启
                    return;
                }
                param.setResult(null);
            }
        });

        hook_methods("android.location.LocationManager", "requestLocationUpdates", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                toolsprefs.reload();
                if(!toolsprefs.getBoolean("locationEnabled", false)){ //未开启
                    return;
                }
                if (param.args.length == 4 && (param.args[0] instanceof String)) {

                    LocationListener ll = (LocationListener) param.args[3];

                    Class<?> clazz = LocationListener.class;
                    Method m = null;
                    for (Method method : clazz.getDeclaredMethods()) {
                        if (method.getName().equals("onLocationChanged")) {
                            m = method;
                            break;
                        }
                    }

                    try {
                        if (m != null) {
                            toolsprefs.reload();
                            Object[] args = new Object[1];
                            Location l = new Location(LocationManager.GPS_PROVIDER);

                            double la = Double.parseDouble(toolsprefs.getString("latitude", "-10001"));
                            double lo = Double.parseDouble(toolsprefs.getString("longitude", "-10001"));

                            l.setLatitude(la);
                            l.setLongitude(lo);

                            args[0] = l;

                            //invoke onLocationChanged directly to pass location infomation
                            m.invoke(ll, args);

                            XposedBridge.log("fake location: " + la + ", " + lo);
                        }
                    } catch (Exception e) {
                        XposedBridge.log(e);
                    }
                }
            }
        });

        hook_methods("android.location.LocationManager", "getGpsStatus", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                toolsprefs.reload();
                if (!toolsprefs.getBoolean("locationEnabled", false)) { //未开启
                    return;
                }
                GpsStatus gss = (GpsStatus) param.getResult();
                if (gss == null)
                    return;

                Class<?> clazz = GpsStatus.class;
                Method m = null;
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equals("setStatus")) {
                        if (method.getParameterTypes().length > 1) {
                            m = method;
                            break;
                        }
                    }
                }

                //access the private setStatus function of GpsStatus
                m.setAccessible(true);

                //make the apps belive GPS works fine now
                int svCount = 5;
                int[] prns = {1, 2, 3, 4, 5};
                float[] snrs = {0, 0, 0, 0, 0};
                float[] elevations = {0, 0, 0, 0, 0};
                float[] azimuths = {0, 0, 0, 0, 0};
                int ephemerisMask = 0x1f;
                int almanacMask = 0x1f;

                //5 satellites are fixed
                int usedInFixMask = 0x1f;

                try {
                    if (m != null) {
                        m.invoke(gss, svCount, prns, snrs, elevations, azimuths, ephemerisMask, almanacMask, usedInFixMask);
                        param.setResult(gss);
                    }
                } catch (Exception e) {
                    XposedBridge.log(e);
                }
            }
        });
    }
    //修改本机IMEI
    private void workforIMEI(XC_LoadPackage.LoadPackageParam lpparam) {
        hook_methods("android.telephony.TelephonyManager", "getDeviceId", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                toolsprefs.reload();
                if (!toolsprefs.getBoolean("IMEIEnabled", false)) { //未开启
                    return;
                }
                String newimei = toolsprefs.getString("IMEI", "");
                if (newimei.equals(""))
                    return;
                param.setResult(newimei);
            }
        });
    }

    //重定向内置存储
    private void workforRedirections(XC_LoadPackage.LoadPackageParam lpparam) {
        XC_MethodHook getExternalStorageDirectoryHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param)
                    throws Throwable {
                changeDirPath(param);
            }
        };

        XC_MethodHook getExternalFilesDirHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param)
                    throws Throwable {
                changeDirPath(param);

            }
        };

        XC_MethodHook getObbDirHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param)
                    throws Throwable {
                changeDirPath(param);
            }
        };

        XC_MethodHook getExternalStoragePublicDirectoryHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param)
                    throws Throwable {
                changeDirPath(param);
            }
        };
        hook_method(Environment.class.getName(), lpparam.classLoader,
                "getExternalStorageDirectory", getExternalStorageDirectoryHook);
        hook_method("android.app.ContextImpl", lpparam.classLoader,
                "getExternalFilesDir", getExternalFilesDirHook);
        hook_method("android.app.ContextImpl", lpparam.classLoader, "getObbDir",
                getObbDirHook);
        hook_method(Environment.class.getName(), lpparam.classLoader,
                "getExternalStoragePublicDirectory",
                getExternalStoragePublicDirectoryHook);
    }

    public void changeDirPath(XC_MethodHook.MethodHookParam param) {
        toolsprefs.reload();
        if(!toolsprefs.getBoolean("redirectionEnabled", false)){ //未开启
            return;
        }
        pathprefs.reload();
        List<RedirectionItem> redirections = new ArrayList<RedirectionItem>();
        Set<String> redirectionSet = pathprefs.getStringSet("redirections", null);
        if(redirectionSet != null) {
            for (String str : redirectionSet) {
                // 用#号隔开来源目录和目标目录
                String[] paths = str.split("#");
                RedirectionItem item = new RedirectionItem();
                item.setFrom(paths[0]);
                item.setTo(paths[1]);
                redirections.add(item);
            }
        }

        File oldDirPath = (File) param.getResult();
        XposedBridge.log(oldDirPath.getAbsolutePath());

        for(RedirectionItem redirection : redirections) {
            if(redirection.getFrom().equals(oldDirPath.getAbsolutePath())) {
                XposedBridge.log(" from " + redirection.getFrom() + " to " + redirection.getTo());
                File newDirPath = new File(redirection.getTo());
                if (!newDirPath.exists()) {
                    newDirPath.mkdirs();
                }
                param.setResult(newDirPath);
            }
        }

    }

    // idem
    private void hook_method(String className, ClassLoader classLoader, String methodName,
                             XC_MethodHook xmh)
    {
        Class<?> hookClass = null;
        try {
            hookClass = findClass(className, classLoader);
        } catch (Throwable ex) {
            XposedBridge.log("Class not found " + className);
        }
        for (Method hookMethod : hookClass.getDeclaredMethods()) {
            if (hookMethod.getName().equals(methodName)) {
                XposedBridge.hookMethod(hookMethod, xmh);
            }
        }
    }

    private void hook_methods(String className, String methodName, XC_MethodHook xmh)
    {
        try {
            Class<?> clazz = Class.forName(className);

            for (Method method : clazz.getDeclaredMethods())
                if (method.getName().equals(methodName)
                        && !Modifier.isAbstract(method.getModifiers())
                        && Modifier.isPublic(method.getModifiers())) {
                    XposedBridge.hookMethod(method, xmh);
                }
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

}
