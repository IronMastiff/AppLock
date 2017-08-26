package com.example.applock.utils;

import android.content.Context;
import android.os.FileObserver;
import android.util.Log;

import com.example.applock.widget.LockPatternView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * 保存锁定图
     */
    public void saveLockPattern( List<LockPatternView.Cell> pattern ){
        //计算哈希值
        final byte[] hash = LockPatternUtils.patternToHash( pattern );
        try {
            //把哈希值写入文件
            RandomAccessFile randomAccessFile = new RandomAccessFile( sLockPatternFilename, "rwd" );
            //如果图案为空删除文件，并清空锁
            if( pattern == null ){
                randomAccessFile.setLength( 0 );
            }
            else{
                randomAccessFile.write( hash, 0, hash.length );
            }
            randomAccessFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch( IOException ioe ){
        }
    }

    private static byte[] patternToHash( List<LockPatternView.Cell> pattern ){
        if( pattern == null ){
            return null;
        }

        final int patternSize = pattern.size();
        byte[] res = new byte[ patternSize ];
        for( int i = 0; i < patternSize; i++ ){
            LockPatternView.Cell cell = pattern.get( i );
            res[ i ] = ( byte )( cell.getRow() * 3 + cell.getColumn() );
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance( "SHA-1" );
            byte[] hash = messageDigest.digest( res );
            return hash;
        } catch (NoSuchAlgorithmException e) {
            return res;
        }
    }

    public boolean checkPattern( List<LockPatternView.Cell> pattern ){
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile( sLockPatternFilename, "r" );
            final byte[] stored = new byte[ ( int )randomAccessFile.length() ];
            int got = randomAccessFile.read( stored, 0, stored.length );
            randomAccessFile.close();
            if( got <= 0 ){
                return true;
            }
            //输入的图的哈希值与文件中的图的哈希值对比
            return Arrays.equals( stored, LockPatternUtils.patternToHash( pattern ) );
        } catch (FileNotFoundException e) {
            return true;
        } catch (IOException e) {
            return true;
        }
    }

}
