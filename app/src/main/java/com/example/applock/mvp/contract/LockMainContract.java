package com.example.applock.mvp.contract;

import android.content.Context;

import com.example.applock.base.BasePresenter;
import com.example.applock.base.BaseView;
import com.example.applock.bean.CommLockInfo;
import com.example.applock.mvp.p.LockMainPresenter;

import java.util.List;

/**
 * Created by 我的 on 2017/8/19.
 */

public interface LockMainContract  {

    interface View extends BaseView<Presenter> {

        void loadAppInfoSuccess( List<CommLockInfo> list);
    }

    interface Presenter extends BasePresenter {
        void loadAppInfo( Context context );

        void searchAppInfo( String search, LockMainPresenter.ISearchResultListener listener );

        void onDestroy();
    }

}
