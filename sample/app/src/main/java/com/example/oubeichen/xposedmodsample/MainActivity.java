package com.example.oubeichen.xposedmodsample;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

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
    }

    @Override
    public void onCheckedChanged(CompoundButton button, boolean isChecked) {

        SharedPreferences.Editor editor = mPrefs.edit();
        if (button == mToggle1) {
            editor.putBoolean(Constant.TOGGLE1, button.isChecked());
        } else if (button == mToggle2) {
            editor.putBoolean(Constant.TOGGLE2, button.isChecked());
        }
        editor.commit();
    }
}
