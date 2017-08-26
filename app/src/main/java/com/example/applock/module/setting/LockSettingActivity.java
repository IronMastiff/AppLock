package com.example.applock.module.setting;

import com.example.applock.R;
import com.example.applock.base.AppConstants;
import com.example.applock.base.BaseActivity;
import com.example.applock.bean.LockAutoTime;
import com.example.applock.utils.SpUtil;
import com.example.applock.widget.SelectLockTimeDialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by 我的 on 2017/8/25.
 */

public class LockSettingActivity extends BaseActivity implements View.OnClickListener, DialogInterface.OnDismissListener {

    private TextView mButtonAbout, mLockTime, mButtonChangePassword, mIsShowPath, mLockTip, mLockScreenSwitch, mLockTakePictureSwitch;
    private CheckBox mLockSwitch;
    private RelativeLayout mLockWhen, mLockScreen, mLockTakePicture;
    private LockSettingReceiver mLockSettingReceiver;
    public static final String ON_ITEM_CLICK_ACTION = "on_item_click_action";
    private SelectLockTimeDialog dialog;
    private static final int REQUEST_CHANGE_PASSWORDE = 3;
    private RelativeLayout mTopLayout;



    @Override
    public void onClick(View view) {
        switch( view.getId() ){
            case R.id.button_change_password:
                Intent intent = new Intent( LockSettingActivity.this, GestureCreateActivity.class );
                startActivityForResult( intent, REQUEST_CHANGE_PASSWORDE );
                overridePendingTransition( android.R.anim.fade_in, android.R.anim.fade_out );
                break;
            case R.id.about_me:
                intent = new Intent( LockSettingActivity.this, AboutMeActivity.class );
                startActivity( intent );
                break;
            case R.id.lock_when:
                String title = SpUtil.getInstance().getString( AppConstants.LOCK_APART_TITLE, "" );
                dialog.setTitle( title );
                dialog.show();
                break;
            case R.id.is_show_path:
                boolean ishideline = SpUtil.getInstance().getBoolean( AppConstants.LOCK_IS_HIDE_LINE, false );
                if( ishideline ){
                    SpUtil.getInstance().putBoolean( AppConstants.LOCK_IS_HIDE_LINE, false );
                    ToastUtil.showToast( "路径已显示" );
                }
                else{
                    SpUtil.getInstance().putBoolean( AppConstants.LOCK_IS_HIDE_LINE, true );
                    ToastUtil.showToast( "路径已隐藏" );
                }
                break;
            case R.id.lock_screen:
                boolean isLockAutoScreen = SpUtil.getInstance().getBoolean( AppConstants.LOCK_AUTO_SCREEN, false );
                if( isLockAutoScreen ){
                    SpUtil.getInstance().putBoolean( AppConstants.LOCK_AUTO_SCREEN, false );
                    mLockScreenSwitch.setText( "OFF" );
                }
                else{
                    SpUtil.getInstance().putBoolean( AppConstants.LOCK_AUTO_SCREEN, true );
                    mLockScreenSwitch.setText( "ON" );
                }
                break;
            case R.id.lock_take_pic:
                boolean isTakePicture = SpUtil.getInstance().getBoolean( AppConstants.LOCK_AUTO_RECORD_PIC, false );
                if( isTakePicture ){
                    SpUtil.getInstance().putBoolean( AppConstants.LOCK_AUTO_RECORD_PIC, false );
                    mLockTakePictureSwitch.setText( "OFF" );
                }
                else{
                    SpUtil.getInstance().putBoolean( AppConstants.LOCK_AUTO_RECORD_PIC, true );
                    mLockScreenSwitch.setText( "ON" );
                }
                break;
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_setting;
    }

    @Override
    protected void initViews(Bundle saveInstanceState) {
        mButtonChangePassword = ( TextView )findViewById( R.id.button_change_password );
        mLockTime = ( TextView )findViewById( R.id.lock_time );
        mButtonAbout = ( TextView )findViewById( R.id.about_me );
        mLockSwitch = ( CheckBox )findViewById( R.id.switch_compat );
        mLockWhen = ( RelativeLayout )findViewById( R.id.lock_when );
        mLockTakePicture = ( RelativeLayout )findViewById( R.id.lock_take_pic );
        mIsShowPath = ( TextView )findViewById( R.id.is_show_path );
        mLockTip = ( TextView ) findViewById(R.id.lock_tip);
        mLockScreenSwitch = ( TextView ) findViewById(R.id.lock_screen_switch);
        mLockTakePictureSwitch = ( TextView ) findViewById(R.id.lock_take_pic_switch);
        mTopLayout = ( RelativeLayout ) findViewById(R.id.top_layout);
        mTopLayout.setPadding( 0, SystemBarHelper.getStatusBarHeight( this ), 0, 0 );
    }

    @Override
    protected void initData() {
        mLockSettingReceiver = new LockSettingReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction( ON_ITEM_CLICK_ACTION );
        registerReceiver( mLockSettingReceiver, filter );
        dialog = new SelectLockTimeDialog( this, "" );
        dialog.setOnDismissListener( this );
        boolean isLockOpen = SpUtil.getInstance().getBoolean( AppConstants.LOCK_STATE );
        mLockSwitch.setChecked( isLockOpen );

        boolean isLockAutoScreen = SpUtil.getInstance().getBoolean( AppConstants.LOCK_AUTO_SCREEN, false );
        mLockScreenSwitch.setText( isLockAutoScreen ? "ON" : "OFF" );

        boolean isTakePicture = SpUtil.getInstance().getBoolean( AppConstants.LOCK_AUTO_RECORD_PIC, false );
        mLockTakePictureSwitch.setText( isTakePicture ? "ON" : "OFF" );

        mLockTime.setText( SpUtil.getInstance().getString( AppConstants.LOCK_APART_TITLE, "IMMEDIATELY" ) );

    }

    @Override
    protected void initAction() {
        mButtonChangePassword.setOnClickListener( this );
        mButtonAbout.setOnClickListener( this );
        mLockWhen.setOnClickListener( this );
        mLockScreen.setOnClickListener( this );
        mIsShowPath.setOnClickListener( this );
        mLockScreenSwitch.setOnClickListener( this );
        mLockTakePicture.setOnClickListener( this );
        mLockSwitch.setOnClickListener( this );
        mLockSwitch.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SpUtil.getInstance().putBoolean( AppConstants.LOCK_STATE,  b );
                Intent intent = new Intent( LockSettingActivity.this, LockService.class );
                if( b ){
                    mLockTip.setText( "已加锁， 加锁应用打开时需要输入密码" );
                    startService( intent );
                }
                else{
                    mLockTip.setText( "未加锁，未加锁应用打开时不需要密码" );
                    stopService( intent );
                }
            }
        });
    }

    private class LockSettingReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if( action.equals( ON_ITEM_CLICK_ACTION ) ){
                LockAutoTime info = intent.getParcelableExtra( "info" );
                boolean isLast = intent.getBooleanExtra( "isLock", true );
                if( isLast ){
                    mLockTime.setText( info.getTitle() );
                    SpUtil.getInstance().putString( AppConstants.LOCK_APART_TITLE, info.getTitle() );
                    SpUtil.getInstance().putLong( AppConstants.LOCK_APART_MILLISECONS, 0L );
                    SpUtil.getInstance().putBoolean( AppConstants.LOCK_AUTO_SCREEN_TIME, true );
                }
                dialog.dismiss();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( resultCode == RESULT_OK ){
            switch( requestCode ){
                case REQUEST_CHANGE_PASSWORDE:
                    ToastUtil.showToast( "密码设置成功" );
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver( mLockSettingReceiver );
    }
}

