package com.example.applock.utils;

import android.content.Context;
import android.os.FileObserver;
import android.util.Log;

import com.example.applock.widget.LockPatternView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.R.attr.path;

/**
 * 图案解锁加密，解密工具类
 *
 * Created by 我的 on 2017/8/20.
 */

public class LockPatternUtils {
    private static final String TAG = "LockPatternUtils";
    private static final String LOCK_PATTERN_FILE = "gesture.key";
    public static final int MIN_LOCK_PATTERN_SIZE = 4;
    public static final int FAILED_ATTEMPTS_BEFORE_TIMEOUT = 5;
    public static final int MIN_PATTERN_REGISTER_FAIL = MIN_LOCK_PATTERN_SIZE;
    public static final long FAILED_ATTEMPT_TIMEOUT_MS = 30000L;

    private static File sLockPatternFilename;
    private static final AtomicBoolean sHaveNonZeroPatternFile = new AtomicBoolean( false );
    private static FileObserver sPasswordObserver;
    private static class LockPatternFileObserver extends FileObserver{

        public LockPatternFileObserver( String path, int mask ) {
            super( path, mask );
        }

        @Override
        public void onEvent(int i, String s) {
            Log.d( TAG, "lock pattern file changed" );
            if( LOCK_PATTERN_FILE.equals( path ) ){
                Log.d( TAG, "lock pattern file changed" );
                sHaveNonZeroPatternFile.set( sLockPatternFilename.length() > 0 );
            }
        }
    }

    public LockPatternUtils( Context context ){
        if( sLockPatternFilename == null ){
            String dataSystemDirectory = context.getFilesDir().getAbsolutePath();
            sLockPatternFilename = new File( dataSystemDirectory, LOCK_PATTERN_FILE );
            sHaveNonZeroPatternFile.set( sLockPatternFilename.length() > 0 );
            int fileObserverMask = FileObserver.CLOSE_WRITE | FileObserver.DELETE | FileObserver.MOVED_TO | FileObserver.CREATE;
            sPasswordObserver = new LockPatternFileObserver( dataSystemDirectory, fileObserverMask );
            sPasswordObserver.startWatching();
        }
    }

    public boolean savePatternExists(){
        return sHaveNonZeroPatternFile.get();
    }

    public void clearLock(){
        saveLockPattern( null );
    }

    public static List<LockPatternView.Cell> stringToPattern(String string ){
        List<LockPatternView.Cell> result = new ArrayList<LockPatternView.Cell>();

        final byte[] bytes = string.getBytes();
        for( int i = 0; i < bytes.length; i++ ){
            byte b = bytes[ i ];
            result.add( LockPatternView.Cell.of( b / 3, b % 3 ) );
        }
        return result;
    }

    /**
     * 加密
     */
    public static String patternToString( List<LockPatternView.Cell> pattern ){
        if( pattern == null ){
            return "";
        }
        final int patternSize = pattern.size();

        byte[] res = new byte[ patternSize ];
        for( int i = 0; i < patternSize; i++ ){
            LockPatternView.Cell cell = pattern.get( i );
            res[ i ] = ( byte )( cell.getRow() * 3 + cell.getColumn() );
        }
        return new String( res );
    }
}
