package com.example.applock.widget;

import android.animation.AnimatorSet;
import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by 我的 on 2017/8/17.
 */

public class DialogSearch extends BaseDialog {

    public DialogSearch(@NonNull Context context) {
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

    }

    @Override
    protected int getContentViewId() {
        return 0;
    }
}
