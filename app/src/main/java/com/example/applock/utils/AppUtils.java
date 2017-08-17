package com.example.applock.utils;

import android.view.Window;
import android.view.WindowManager;

/**
 * Created by 我的 on 2017/8/12.
 */

public class AppUtils {

    public static void hideStatusBar( Window window, boolean enable ){
        WindowManager.LayoutParams params = window.getAttributes();
        if( enable ){
            // |=，或等于，取其一
            params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        else{
            // &=，与等于，取其二同时满足。否则取反
            params.flags &= ( - WindowManager.LayoutParams.FLAG_FULLSCREEN );
        }

        window.setAttributes( params );
        window.addFlags( WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS );
    }
}
