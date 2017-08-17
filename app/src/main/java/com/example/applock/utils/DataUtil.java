package com.example.applock.utils;

import com.example.applock.bean.CommLockInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by 我的 on 2017/8/12.
 */

public class DataUtil {

    public static List<CommLockInfo> clearRepeatCommLockInfo( List<CommLockInfo> lockInfos ){
        HashMap<String, CommLockInfo > hashMap = new HashMap<>();
        for ( CommLockInfo lockInfo : lockInfos ) {
            if ( !hashMap.containsKey( lockInfo.getPackageName() ) ){
                hashMap.put( lockInfo.getPackageName(), lockInfo );
            }
        }
        List<CommLockInfo> commLockInfos = new ArrayList<>();
        for ( HashMap.Entry<String, CommLockInfo> entry : hashMap.entrySet() ){
            commLockInfos.add( entry.getValue() );
        }
        return commLockInfos;
    }

}
