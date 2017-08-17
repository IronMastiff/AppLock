package com.example.applock.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by 我的 on 2017/8/12.
 */

public abstract class BaseFragment extends Fragment {

    private View view;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        if( getOptionsMenuId() != -1 ){
            setHasOptionsMenu( true );
        }

        initBefore( inflater, container, savedInstanceState );
        view = inflater.inflate( getContentViewId(), container, false );
        init( view );
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if( getOptionsMenuId() != -1 ){
            inflater.inflate( getOptionsMenuId(), menu );
        }
        super.onCreateOptionsMenu( menu, inflater );
    }

    /**
     * 初始化之前
     * @param inflater
     * @param container
     * @param savedInstanceState
     */
    private void initBefore(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    }

    //    设置当前菜单的资源
    public int getOptionsMenuId() {
        return -1;
    }

    /**
     * 初始化
     */
    protected abstract void init( View rootView );

    /**
     * 当前布局文件
     * @return
     */
    public int getContentViewId() {
        return -1;
    }

}
