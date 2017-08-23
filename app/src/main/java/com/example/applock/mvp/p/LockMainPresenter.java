package com.example.applock.mvp.p;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import com.example.applock.base.AppConstants;
import com.example.applock.bean.CommLockInfo;
import com.example.applock.db.CommLockInfoManager;
import com.example.applock.mvp.contract.LockMainContract;
import com.example.applock.utils.SpUtil;

import java.util.Iterator;
import java.util.List;


/**
 * Created by 我的 on 2017/8/20.
 */

public class LockMainPresenter implements LockMainContract.Presenter {

    private LockMainContract.View mView;
    private PackageManager mPackageManager;
    private CommLockInfoManager mLockInfoManager;
    private Context mContext;
    private LoadAppInfo mLoadAppInfo;

    public LockMainPresenter(LockMainContract.View view, Context mContext){
        mView = view;
        this.mContext = mContext;
        mPackageManager = mContext.getPackageManager();
        mLockInfoManager = new CommLockInfoManager( mContext );
    }

    /**
     * 加载所有app
     * @param context
     */

    @Override
    public void loadAppInfo(Context context) {
        mLoadAppInfo = new LoadAppInfo();
        mLoadAppInfo.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR );
    }

    @Override
    public void searchAppInfo(String search, ISearchResultListener listener) {
        new SearchInfoAsyncTask( listener ).executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, search );
    }

    @Override
    public void onDestroy() {
        if( mLoadAppInfo != null && mLoadAppInfo.getStatus() != AsyncTask.Status.FINISHED ){
            mLoadAppInfo.cancel( true );
        }
    }

    private class LoadAppInfo extends AsyncTask<Void, String, List<CommLockInfo>> {

        @Override
        protected void onPostExecute(List<CommLockInfo> commLockInfos) {
            super.onPostExecute(commLockInfos);
            mView.loadAppInfoSuccess( commLockInfos );
        }

        @Override
        protected List<CommLockInfo> doInBackground(Void... params) {
            List<CommLockInfo> commLockInfos = mLockInfoManager.getAllCommLockInfo();
            Iterator<CommLockInfo> infoIterator = commLockInfos.iterator();
            int favoriteNum = 0;
            while( infoIterator.hasNext() ) {
                CommLockInfo info = infoIterator.next();
                try {
                    ApplicationInfo appInfo = mPackageManager.getApplicationInfo( info.getPackageName(), PackageManager.GET_UNINSTALLED_PACKAGES );
                    if( appInfo == null || mPackageManager.getApplicationIcon( appInfo ) == null ){
                        infoIterator.remove(); //将有错的app删除
                        continue;
                    }
                    else{
                        info.setAppInfo( appInfo ); //给列表Application赋值
                        if( ( appInfo.flags & ApplicationInfo.FLAG_SYSTEM ) != 0 ){ //判断是否是系统应用
                            info.setSysApp( true );
                            info.setTopTitle( "系统应用" );
                        }
                        else{
                            info.setSysApp( false );
                            info.setTopTitle( "用户应用" );
                        }
                    }
                    //获取推荐应用总数
                    if( info.isLocked() ){
                        favoriteNum++;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    infoIterator.remove();
                }
            }
            SpUtil.getInstance().putInt( AppConstants.LOCK_FAVITER_NUM, favoriteNum );
            return commLockInfos;
        }
    }

    private class SearchInfoAsyncTask extends AsyncTask< String, Void, List<CommLockInfo>> {
        private ISearchResultListener mSearchResultListener;

        public SearchInfoAsyncTask(ISearchResultListener searchResultListener) {
            mSearchResultListener = searchResultListener;
        }

        @Override
        protected List<CommLockInfo> doInBackground(String... params) {
            List<CommLockInfo> commLockInfos = mLockInfoManager.queryBlurryList( params[ 0 ] );
            Iterator<CommLockInfo> infoIterator = commLockInfos.iterator();
            while ( infoIterator.hasNext() ){
                CommLockInfo info = infoIterator.next();
                try {
                    ApplicationInfo appInfo = mPackageManager.getApplicationInfo( info.getPackageName(),PackageManager.GET_UNINSTALLED_PACKAGES );
                    if ( appInfo == null || mPackageManager.getApplicationIcon( appInfo ) == null ){
                        infoIterator.remove(); //将有错的app删除
                    }
                    else{
                        info.setAppInfo( appInfo ); //给列表ApplicationInfo赋值
                        if( ( appInfo.flags & ApplicationInfo.FLAG_SYSTEM ) != 0 ){ //判断是否是系统应用
                            info.setSysApp( true );
                            info.setTopTitle( "系统应用" );
                        }
                        else{
                            info.setSysApp( false );
                            info.setTopTitle( "用户应用" );
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    infoIterator.remove();
                }
            }
            return commLockInfos;
        }
    }

    public interface ISearchResultListener{
        void onSearchResult( List<CommLockInfo> commLockOInfos );
    }
}
