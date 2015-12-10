package com.example.oubeichen.xposedmodsample;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity implements CompoundButton.OnCheckedChangeListener {

    ToggleButton mToggle1;
    ToggleButton mToggle2;

    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToggle1 = (ToggleButton) findViewById(R.id.toggle1);
        mToggle2 = (ToggleButton) findViewById(R.id.toggle2);

        mPrefs = getSharedPreferences(Constant.PREFS, Activity.MODE_WORLD_READABLE);

        mToggle1.setChecked(mPrefs.getBoolean(Constant.TOGGLE1, false));
        mToggle2.setChecked(mPrefs.getBoolean(Constant.TOGGLE2, false));

        mToggle1.setOnCheckedChangeListener(this);
        mToggle2.setOnCheckedChangeListener(this);

        createFile();
    }

    @Override
    public void onCheckedChanged(CompoundButton button, boolean isChecked) {

        SharedPreferences.Editor editor = mPrefs.edit();
        Intent intent = new Intent(Constant.UPDATE);
        if (button == mToggle1) {
            editor.putBoolean(Constant.TOGGLE1, button.isChecked());
            intent.putExtra(Constant.ACTION, Constant.UPDATE_CLOCK_ACTION);
        } else if (button == mToggle2) {
            editor.putBoolean(Constant.TOGGLE2, button.isChecked());
        }
        editor.commit();
        sendBroadcast(intent);
    }


    private void createFile() {
        InputStream ins = null;
        FileOutputStream fos = null;
        try {
            File dir = new File(Constant.FILEDIRPATH);// 目录路径
            if (!dir.exists()) {// 如果不存在，则创建路径名
                System.out.println("要存储的目录不存在");
                dir.mkdirs();
            }
            // 目录存在，则将apk中raw中的需要的文档复制到该目录下
            File file = new File(Constant.FILEPATH);
            if (!file.exists()) {// 文件不存在
                ins = getResources().openRawResource(R.raw.header);// 通过raw得到数据资源
                fos = new FileOutputStream(file);
                byte[] buffer = new byte[8192];
                int count = 0;// 循环写出
                while ((count = ins.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();// 关闭流
                ins.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
