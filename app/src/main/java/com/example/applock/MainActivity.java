package com.example.applock;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.applock.base.BaseActivity;

import org.litepal.LitePalApplication;

import java.util.List;

public class MainActivity extends LitePalApplication {

    private static MainActivity application;
    private static List<BaseActivity> activityList; //activity管理

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
//        SpUtil.getInstance().init( application );


    }

}
