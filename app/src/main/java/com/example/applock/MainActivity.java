package com.example.applock;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.applock.base.BaseActivity;
import com.example.applock.utils.SpUtil;

import org.litepal.LitePalApplication;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends LitePalApplication {

    private static MainActivity application;
    private static List<BaseActivity> activityList; //activity管理

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        SpUtil.getInstance().init( application );
        activityList = new ArrayList<>();
    }

    public static MainActivity getInstance() {
        return application;
    }

    public void doForCreate( BaseActivity activity ){
        activityList.add( activity );
    }

    public void doForFinish( BaseActivity activity ){
        activityList.remove( activity );
    }

    public void clearAllActivity(){
        for( BaseActivity activity : activityList ){
            if( activity != null && !clearAllWhiteList( activity ) ){
                activity.clear();
            }
        }
        activityList.clear();
    }

    private boolean clearAllWhiteList( BaseActivity activity ){
        return activity instanceof GeatureUnlockActivity;
    }
}
