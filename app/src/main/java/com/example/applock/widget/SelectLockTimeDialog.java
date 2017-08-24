package com.example.applock.widget;

import android.animation.AnimatorSet;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import com.example.applock.R;
import com.example.applock.bean.LockAutoTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 我的 on 2017/8/24.
 */

public class SelectLockTmeDialog extends BaseDialog {

    private RecyclerView mRecyclerView;
    private List<LockAutoTime> mTimeList;
    private SelectTimeAdapter mSelectTimeAdapter;
    private Context context;
    private String title;

    public SelectLockTmeDialog(@NonNull Context context) {
        super(context);
        this.context = context;
        this.title = title;
    }

    public void setTitle( String title ){
        this.title = title;
    }

    @Override
    protected float setWidthScale() {
        return 0.9f;
    }

    @Override
    protected AnimatorSet setEnterAnim() {
        return null;
    }

    @Override
    protected AnimatorSet setExitAnim() {
        return null;
    }

    @Override
    protected void init() {
        setCanceledOnTouchOutside( false );
        mRecyclerView = ( RecyclerView )findViewById( R.id.recycler_view );
        mRecyclerView.setLayoutmanager( new LinearLayoutmanager( context ) );
        String titleArray[] = context.getResources().getStringArray( R.array.lock_time_array );
        Long timeArray[] = { 15000L, 30000L, 60000L, 100000L, 300000L, 600000L, 1000000L, 0L };
        mTimeList = new ArrayList<>();
    }

    @Override
    protected int getContentViewId() {
        return R.layout.dialog_lock_select_time;
    }
}
