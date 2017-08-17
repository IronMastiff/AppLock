package com.example.applock.bean;

import com.example.applock.R;

/**
 * Created by 我的 on 2017/7/24.
 */


/**
 * 图案锁的状态
 */
public enum LockStage {

    Intorduction( R.string.lock_recording_intro_header, -1, false ),

    HelpScreen( R.string.lock_setting_help_how_to_record, -1, false ),

    ChoiceTooShort( R.string.lock_recording_incorrect_too_short, -1, true ),

    FirstChoiceValid( R.string.lock_pattern_entered_header, -1, false ),

    NeedToConfirm( R.string.lock_need_to_confirm, -1, true ),

    confirmWrong( R.string.lock_need_to_unlock_wrong, -1, true ),

    ChoiceConfirmed( R.string.lock_pattern_confirmed_header, -1, false );


    private final boolean patternEnabled;
    private final int footerMessage;
    private final int headerMessage;

    LockStage(int headerMessage, int footerMessage, boolean patternEnabled ) {
        this.headerMessage = headerMessage;
        this.footerMessage = footerMessage;
        this.patternEnabled = patternEnabled;
    }

}
