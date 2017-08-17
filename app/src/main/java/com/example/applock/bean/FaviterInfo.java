package com.example.applock.bean;

import org.litepal.crud.DataSupport;

/**
 * Created by 我的 on 2017/8/11.
 */

public class FaviterInfo extends DataSupport {
    private String packageName;

    public FaviterInfo() {
    }

    public FaviterInfo( String packageName ){
        this.packageName = packageName;
    }

    public String getPackageName(){
        return packageName;
    }

    public void setPackageName( String packageName ){
        this.packageName = packageName;
    }
}
