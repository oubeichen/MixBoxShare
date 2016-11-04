package com.example.oubeichen.xposedmodsample;

import java.io.File;

/**
 * Created by oubeichen on 12/10/15.
 */
public class Constant {
    public static final String TOGGLE1 = "toggle1";
    public static final String TOGGLE2 = "toggle2";
    public static final String TOGGLE3 = "toggle3";


    public static final String PREFS = "prefs";

    public static final String PACKAGENAME = "com.example.oubeichen.xposedmodsample";

    public static final String FILEDIRPATH = android.os.Environment
            .getExternalStorageDirectory().getAbsolutePath() + File.separator + "xposedsample";
    public static final String FILEPATH = FILEDIRPATH + File.separator + "hehe2.png";// 文件路径

    public static final String PERMISSION = PACKAGENAME + ".BROADCAST_PERMISSION";

    public static final String UPDATE = PACKAGENAME + ".UPDATE";
    public static final String UPDATE_CLOCK_ACTION = "update_clock";
    public static final String KILL = PACKAGENAME + ".KILL";
    public static final String KILL_PACKAGE = "kill_package";
    public static final String KILL_ACTION = "kill";
    public static final String ACTION = "action";
    public static final String NOTHING = "nothing";
}
