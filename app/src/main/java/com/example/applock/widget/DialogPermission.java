package com.example.applock.widget;

import android.animation.AnimatorSet;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.example.applock.R;

/**
 * Created by 我的 on 2017/8/17.
 */

public class DialogPermission extends BaseDialog {

    private TextView mButtonPermission;
    private onClickListener mOnClickListener;

    public DialogPermission( Context context) {
        super(context);
    }

    @Override
    protected float setWidthScale() {
        return 0;
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
        mButtonPermission = ( TextView )findViewById( R.id.button_permission );
        mButtonPermission.setOnClickListener( new View.OnClickListener(){

            @Override
            public void onClick(View view) {
                if( mOnClickListener != null ){
                    dismiss();
                    mOnClickListener.onClick();
                }
            }
        });

    }

    public void setOnClickListener( onClickListener onClickListener ){
        mOnClickListener = onClickListener;
    }

    public interface onClickListener{
        void onClick();
    }

    @Override
    protected int getContentViewId() {
        return R.layout.dialog_permission;
    }
}
