package com.example.applock.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Debug;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import com.example.applock.R;
import com.example.applock.base.AppConstants;
import com.example.applock.utils.LockPatternUtils;
import com.example.applock.utils.SpUtil;

import java.util.ArrayList;
import java.util.IllegalFormatCodePointException;
import java.util.List;

/**
 * Created by 我的 on 2017/8/21.
 */

public class LockPatternView extends View {

    public int line_color_right = 0xff97C7F2; //正常时候路径颜色
    public int line_color_wrong = 0x66fe5479; //错误时候路径颜色

    private int res_gesture_pattern_item_bg = R.drawable.gesture_pattern_item_bg; //正常状况下的图片
    private int res_gesture_pattern_selected = R.drawable.gesture_pattern_selected; //选中时的图片
    private int res_gesture_pattern_selected_wrong = R.drawable.gesture_pattern_selected_wrong; //选这错误时的图片

    private static final String TAG = "LockPatternView";
    private static final int ASPECT_SQUARE = 0;
    private static final int ASPECT_LOCK_WIDTH = 1;
    private static final int ASPECT_LOCK_HEIGHT = 2;

    private static final boolean PROFILE_DRAWING = false;
    private boolean mDrawingProfilingStarted = false;

    private Paint mPaint = new Paint();
    private Paint mPathPaint = new Paint();

    static final int STATUS_BAR_HEIGHT = 25;

    private static final int MILLIS_PER_CIRCLE_ANIMATING = 700;

    private OnPatternListener mOnPatternListener;
    private ArrayList<Cell> mPattern = new ArrayList<Cell>( 9 );

    private boolean[][] mPatternDrawLookup = new boolean[ 3 ][ 3 ];

    private float mInProgressX = -1;
    private float mInProgressY = -1;

    private long mAnimatingPeriodStart;

    private DisplayMode mPatternDisplayMode = DisplayMode.Correct;
    private boolean mInputEnabled = true;
    private boolean mInStealthMode = false;
    private boolean mEnableHapticFeedback = true;
    private boolean mPatternInProgress = false;

    private float mDiameterFactor = 0.10f;
    private final int mStrockAlpha = 51;
    private float mHitFactor = 0.6f;

    private float mSquareWidth;
    private float mSquareHeight;

    private Bitmap mBitmapCircleDefault;
    private Bitmap mBitmapCircleGreen;
    private Bitmap mBitmapCircleRed;

    private final Path mCurrentPath = new Path();
    private final Rect mInvalidate = new Rect();

    private int mBitmapWidth;
    private int mBitmapHeight;

    private int mAspect;
    private final Matrix mCircleMatrix = new Matrix();

    private boolean defaultIsHideLine;

    /**
     * 设置线的颜色
     * @param color
     */
    public void setLineColorRight( int color ){
        line_color_right = color;
    }

    /**
     * 设置正常情况下的图片
     * @param resId
     */
    public void setGesturePatternItemBg( int resId ){
        this.res_gesture_pattern_item_bg = resId;
    }

    /**
     * 设置选中时候的图片
     * @param resId
     */
    public void setGesturePatternSelected( int resId ){
        this.res_gesture_pattern_selected = resId;
    }

    /**
     * 设置选择错误时候的图片
     * @param resId
     */
    public void setGesturePatternSelectedWrong( int resId ){
        this.res_gesture_pattern_selected_wrong = resId;
    }

    //绘画界面按钮个数
    public static class Cell {
        int row;
        int column;
        static Cell[][] sCells = new Cell[ 3 ][ 3 ];

        static{
            for( int i = 0; i < 3; i++ ){
                for( int j = 0; j < 3; j++ ){
                    sCells[ i ][ j ] = new Cell( i, j );
                }
            }
        }

        private Cell( int row, int column ){
            checkRange( row, column );
            this.row = row;
            this.column = column;
        }

        public int getRow(){
            return row;
        }

        public int getColumn(){
            return column;
        }

        public static synchronized Cell of( int row, int column ){
            checkRange( row, column );
            return sCells[row][column];
        }

        //排错
        private static void checkRange(int row, int column) {
            if( row < 0 || row > 2 ){
                throw new IllegalArgumentException( "row must be in range 0-2" );
            }
            if( column < 0 || column > 2 ){
                throw new IllegalArgumentException( "column must in range 0-2" );
            }
        }

        public String toString(){
            return "( row=" + row + ",column" + column + " )";
        }

    }

    public enum DisplayMode{
        Correct, Animate, Wrong
    }

    public static interface OnPatternListener{
        void onPatternStart();
        void onPatternCleared();
        void onPatternCellAdded( List<Cell> pattern );
        void onPatternDetected( List<Cell> pattern );
    }


    public LockPatternView(Context context) {
        this( context, null );
    }

    public LockPatternView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LockPatternView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes( attrs, R.styleable.LockPatternView );

        final String aspect = a.getString( R.styleable.LockPatternView_aspect );

        if( "square".equals( aspect ) ){
            mAspect = ASPECT_SQUARE;
        }
        else if( "lock_width".equals( aspect ) ){
            mAspect = ASPECT_LOCK_WIDTH;
        }
        else if( "lock_height".equals( aspect ) ){
            mAspect = ASPECT_LOCK_HEIGHT;
        }
        else{
            mAspect = ASPECT_SQUARE;
        }

        setClickable( true );

        /**
         * 设置画笔值
         */
        mPathPaint.setAntiAlias( true );
        mPathPaint.setDither( true );
        mPathPaint.setColor( line_color_right );
        mPathPaint.setAlpha( mStrockAlpha );
        mPathPaint.setStyle( Paint.Style.STROKE );
        mPathPaint.setStrokeJoin( Paint.Join.ROUND );
        mPathPaint.setStrokeCap( Paint.Cap.ROUND );

        defaultIsHideLine = SpUtil.getInstance().getBoolean( AppConstants.LOCK_IS_HIDE_LINE, false );
        a.recycle();
    }

    private Bitmap getBitmapFor( int resId ){
        return BitmapFactory.decodeResource( getContext().getResources(), resId );
    }

    public boolean isInStealthMode() {
        return mInStealthMode;
    }

    public boolean isTactileFeedbackEnabled(){
        return mEnableHapticFeedback;
    }

    public void setInStralthMode( Boolean inStealthMode ){
        mInStealthMode = inStealthMode;
    }

    public void setTactileFeedbackEnabled( boolean tactileFeedbackEnabled ){
        mEnableHapticFeedback = tactileFeedbackEnabled;
    }

    public void setOnPatternListener( OnPatternListener onPatternListener ){
        mOnPatternListener = onPatternListener;
    }

    public void setPattern( DisplayMode displayMode, List<Cell> pattern ){
        mPattern.clear();
        mPattern.addAll( pattern );
        clearPatternDrawLookup();
        for( Cell cell : pattern ){
            mPatternDrawLookup [ cell.getRow() ][ cell.getColumn() ] = true;
        }
        setDisplayMode( displayMode );
    }

    public void setDisplayMode( DisplayMode displayMode ){
        mPatternDisplayMode = displayMode;
        if( displayMode == DisplayMode.Animate ){
            if( mPattern.size() == 0 ){
                throw new IllegalStateException( "you must have a pattern to "
                        + "animate if you want to set the display mode to animate" );
            }
            mAnimatingPeriodStart = SystemClock.elapsedRealtime();
            final Cell first = mPattern.get( 0 );
            mInProgressX = getCenterXForColumn( first.getColumn() );
            mInProgressY = getCenterYForRow( first.getRow() );
            clearPatternDrawLookup();
        }
        invalidate();
    }

    private void notifyCellAdded(){
        if( mOnPatternListener != null ){
            mOnPatternListener.onPatternCellAdded( mPattern );
        }
        sendAccessEvent( R.string.lockscreen_access_pattern_cell_added );
    }

    private void notifyPatternStarted() {
        if( mOnPatternListener != null ){
            mOnPatternListener.onPatternStart();
        }
        sendAccessEvent( R.string.lockscreen_access_pattern_start );
    }

    private void notifyPatternDetected(){
        if( mOnPatternListener != null ){
            mOnPatternListener.onPatternDetected( mPattern );
        }
        sendAccessEvent( R.string.lockscreen_access_pattern_detected );
    }

    private void notifyPatternCleared(){
        if( mOnPatternListener != null ){
            mOnPatternListener.onPatternCleared();
        }
        sendAccessEvent( R.string.lockscreen_access_pattern_cleared );
    }

    public void clearPattern(){
        resetPattern();
    }

    private void resetPattern(){
        mPattern.clear();
        clearPatternDrawLookup();
        mPatternDisplayMode = DisplayMode.Correct;
        invalidate();
    }

    private void clearPatternDrawLookup(){
        for( int i = 0; i < 3; i++ ){
            for( int j = 0; j < 3; j++ ){
                mPatternDrawLookup[i][j] = false;
            }
        }
    }

    public void disableInput(){
        mInputEnabled = false;
    }

    public void enableInput(){
        mInputEnabled = true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        initRes();

        final int width = w - getPaddingLeft() - getPaddingRight();
        mSquareWidth = width / 2.0f;

        final int height = h - getPaddingTop() - getPaddingBottom();
        mSquareHeight = height / 3.0f;
    }

    /**
     * 初始化资源
     */
    public void initRes(){
        defaultIsHideLine = SpUtil.getInstance().getBoolean( AppConstants.LOCK_IS_HIDE_LINE, false );
        mBitmapCircleDefault = getBitmapFor( res_gesture_pattern_item_bg );
        if( defaultIsHideLine ){
            mBitmapCircleGreen = mBitmapCircleDefault;
        }
        else{
            mBitmapCircleGreen = getBitmapFor( res_gesture_pattern_selected );
        }
        mBitmapCircleRed = getBitmapFor( res_gesture_pattern_selected_wrong );
        final Bitmap bitmaps[] = { mBitmapCircleDefault, mBitmapCircleGreen, mBitmapCircleRed };

        for( Bitmap bitmap : bitmaps ){
            mBitmapWidth = Math.max( mBitmapWidth, bitmap.getWidth() );
            mBitmapHeight = Math.max( mBitmapHeight, bitmap.getHeight() );
        }
    }

    private int resolveMeasured( int measureSpec, int desired ){
        int result = 0;
        int specSize = MeasureSpec.getSize( measureSpec );
        switch( MeasureSpec.getSize( measureSpec ) ){
            case MeasureSpec.UNSPECIFIED:
                result = desired;
                break;
            case MeasureSpec.AT_MOST:
                result = Math.max( specSize, desired );
                break;
            case MeasureSpec.EXACTLY:
            default:
                result = specSize;
        }
        return result;
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return 3 * mBitmapHeight;
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return 3 * mBitmapWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int miniumWidth = getSuggestedMinimumWidth();
        final int miniunHeight = getSuggestedMinimumHeight();
        int viewWidth = resolveMeasured( widthMeasureSpec, miniumWidth );
        int viewHeight = resolveMeasured( heightMeasureSpec, miniunHeight );

        switch( mAspect ){
            case ASPECT_SQUARE:
                viewWidth = viewHeight =  Math.min( viewWidth, viewHeight );
                break;
            case ASPECT_LOCK_WIDTH:
                viewHeight = Math.min( viewWidth, viewHeight );
                break;
            case ASPECT_LOCK_HEIGHT:
                viewWidth = Math.min( viewWidth, viewHeight );
                break;
        }
        setMeasuredDimension( viewWidth, viewHeight );

    }

    private Cell detectAndAddHit( float x, float y ){
        final Cell cell = checkForNewHit( x, y );
        if( cell != null ){
            Cell fillInGapCell = null;
            final ArrayList<Cell> pattern = mPattern;
            if( !pattern.isEmpty() ){
                final Cell lastCell = pattern.get( pattern.size() - 1 );
                int dRow = cell.row - lastCell.row;
                int dColumn = cell.column - lastCell.column;

                int fillInRow = lastCell.row;
                int fillInColumn = lastCell.column;

                if( Math.abs( dRow ) == 2 && Math.abs( dColumn ) != 1 ){
                    fillInRow = lastCell.row + ( ( dRow > 0 ) ? 1 : -1 );
                }

                if( Math.abs( dRow ) == 2 && Math.abs( dRow ) != 1 ){
                    fillInColumn = lastCell.column + ( ( dColumn > 0 ) ? 1 : -1 );
                }

                fillInGapCell = Cell.of( fillInRow, fillInColumn );
            }

            if (fillInGapCell != null
                    && !mPatternDrawLookup[ fillInGapCell.row ][ fillInGapCell.column ] ){
                addCellToPattern( fillInGapCell );
            }
            addCellToPattern( cell );
            if( mEnableHapticFeedback ){
                performHapticFeedback(
                        HapticFeedbackConstants.VIRTUAL_KEY,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                        | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                );
            }
            return cell;
        }
        return null;
    }

    private void addCellToPattern(Cell newCell) {
        mPatternDrawLookup[ newCell.getRow() ][ newCell.getColumn() ] = true;
        mPattern.add( newCell );
        notifyCellAdded();
    }

    private Cell checkForNewHit( float x, float y ){
        final int rowHit = getRowHit( y );
        if( rowHit < 0 ){
            return null;
        }
        final int columnHit = getColumnHit( x );
        if( columnHit < 0 ){
            return null;
        }

        if( mPatternDrawLookup[ rowHit ][ columnHit ] ){
            return null;
        }
        return Cell.of( rowHit, columnHit );
    }

    private int getRowHit( float y ){
        final float squareHeight = mSquareHeight;
        float hitSize = squareHeight * mHitFactor;

        float offset = getPaddingTop() + ( squareHeight - hitSize ) / 2f;
        for( int i = 0; i < 3; i++ ){

            final float hitTop = offset + squareHeight * 1;
            if( y >= hitTop && y <= hitTop + hitSize ){
                return i;
            }
        }
        return -1;
    }

    private int getColumnHit( float x ){
        final float squareWidth = mSquareWidth;
        float hitSize = squareWidth * mHitFactor;

        float offset = getPaddingLeft() + ( squareWidth - hitSize ) / 2f;
        for( int i = 0; i < 3; i++ ){
            final float hitLeft = offset + squareWidth * 1;
            if( x >= hitLeft && x <= hitLeft * hitSize ){
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mInputEnabled || !isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleActionDown(event);
                return true;
            case MotionEvent.ACTION_UP:
                handleActionUp(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(event);
                return true;
            case MotionEvent.ACTION_CANCEL:
                resetPattern();
                mPatternInProgress = false;
                notifyPatternCleared();
                if (PROFILE_DRAWING) {
                    if (mDrawingProfilingStarted) {
                        Debug.stopMethodTracing();
                        mDrawingProfilingStarted = false;
                    }
                }
                return true;
        }
        return false;
    }

    private void handleActionMove(MotionEvent event) {
        // Handle all recent motion events so we don't skip any cells even when
        // the device
        // is busy...
        final int historySize = event.getHistorySize();
        for (int i = 0; i < historySize + 1; i++) {
            final float x = i < historySize ? event.getHistoricalX(i) : event
                    .getX();
            final float y = i < historySize ? event.getHistoricalY(i) : event
                    .getY();
            final int patternSizePreHitDetect = mPattern.size();
            Cell hitCell = detectAndAddHit(x, y);
            final int patternSize = mPattern.size();
            if (hitCell != null && patternSize == 1) {
                mPatternInProgress = true;
                notifyPatternStarted();
            }
            // note current x and y for rubber banding of in progress patterns
            final float dx = Math.abs(x - mInProgressX);
            final float dy = Math.abs(y - mInProgressY);
            if (dx + dy > mSquareWidth * 0.01f) {
                float oldX = mInProgressX;
                float oldY = mInProgressY;

                mInProgressX = x;
                mInProgressY = y;

                if (mPatternInProgress && patternSize > 0) {
                    final ArrayList<Cell> pattern = mPattern;
                    final float radius = mSquareWidth * mDiameterFactor * 0.5f;

                    final Cell lastCell = pattern.get(patternSize - 1);

                    float startX = getCenterXForColumn(lastCell.column);
                    float startY = getCenterYForRow(lastCell.row);

                    float left;
                    float top;
                    float right;
                    float bottom;

                    final Rect invalidateRect = mInvalidate;

                    if (startX < x) {
                        left = startX;
                        right = x;
                    } else {
                        left = x;
                        right = startX;
                    }

                    if (startY < y) {
                        top = startY;
                        bottom = y;
                    } else {
                        top = y;
                        bottom = startY;
                    }

                    // Invalidate between the pattern's last cell and the
                    // current location
                    invalidateRect.set((int) (left - radius),
                            (int) (top - radius), (int) (right + radius),
                            (int) (bottom + radius));

                    if (startX < oldX) {
                        left = startX;
                        right = oldX;
                    } else {
                        left = oldX;
                        right = startX;
                    }

                    if (startY < oldY) {
                        top = startY;
                        bottom = oldY;
                    } else {
                        top = oldY;
                        bottom = startY;
                    }

                    // Invalidate between the pattern's last cell and the
                    // previous location
                    invalidateRect.union((int) (left - radius),
                            (int) (top - radius), (int) (right + radius),
                            (int) (bottom + radius));

                    // Invalidate between the pattern's new cell and the
                    // pattern's previous cell
                    if (hitCell != null) {
                        startX = getCenterXForColumn(hitCell.column);
                        startY = getCenterYForRow(hitCell.row);

                        if (patternSize >= 2) {
                            // (re-using hitcell for old cell)
                            hitCell = pattern.get(patternSize - 1
                                    - (patternSize - patternSizePreHitDetect));
                            oldX = getCenterXForColumn(hitCell.column);
                            oldY = getCenterYForRow(hitCell.row);

                            if (startX < oldX) {
                                left = startX;
                                right = oldX;
                            } else {
                                left = oldX;
                                right = startX;
                            }

                            if (startY < oldY) {
                                top = startY;
                                bottom = oldY;
                            } else {
                                top = oldY;
                                bottom = startY;
                            }
                        } else {
                            left = right = startX;
                            top = bottom = startY;
                        }

                        final float widthOffset = mSquareWidth / 2f;
                        final float heightOffset = mSquareHeight / 2f;

                        invalidateRect.set((int) (left - widthOffset),
                                (int) (top - heightOffset),
                                (int) (right + widthOffset),
                                (int) (bottom + heightOffset));
                    }

                    invalidate(invalidateRect);
                } else {
                    invalidate();
                }
            }
        }
    }

    private void sendAccessEvent(int resId) {
        setContentDescription(getContext().getString(resId));
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        setContentDescription(null);
    }

    private void handleActionUp(MotionEvent event) {
        // report pattern detected
        if (!mPattern.isEmpty()) {
            mPatternInProgress = false;
            notifyPatternDetected();
            invalidate();
        }
        if (PROFILE_DRAWING) {
            if (mDrawingProfilingStarted) {
                Debug.stopMethodTracing();
                mDrawingProfilingStarted = false;
            }
        }
    }

    private void handleActionDown(MotionEvent event) {
        resetPattern();
        final float x = event.getX();
        final float y = event.getY();
        final Cell hitCell = detectAndAddHit(x, y);
        if (hitCell != null) {
            mPatternInProgress = true;
            mPatternDisplayMode = DisplayMode.Correct;
            notifyPatternStarted();
        } else {
            mPatternInProgress = false;
            notifyPatternCleared();
        }
        if (hitCell != null) {
            final float startX = getCenterXForColumn(hitCell.column);
            final float startY = getCenterYForRow(hitCell.row);

            final float widthOffset = mSquareWidth / 2f;
            final float heightOffset = mSquareHeight / 2f;

            invalidate((int) (startX - widthOffset),
                    (int) (startY - heightOffset),
                    (int) (startX + widthOffset), (int) (startY + heightOffset));
        }
        mInProgressX = x;
        mInProgressY = y;
        if (PROFILE_DRAWING) {
            if (!mDrawingProfilingStarted) {
                Debug.startMethodTracing("LockPatternDrawing");
                mDrawingProfilingStarted = true;
            }
        }
    }

    private float getCenterXForColumn(int column) {
        return getPaddingLeft() + column * mSquareWidth + mSquareWidth / 2f;
    }

    private float getCenterYForRow(int row) {
        return getPaddingTop() + row * mSquareHeight + mSquareHeight / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final ArrayList<Cell> pattern = mPattern;
        final int count = pattern.size();
        final boolean[][] drawLookup = mPatternDrawLookup;

        if (mPatternDisplayMode == DisplayMode.Animate) {

            // figure out which circles to draw

            // + 1 so we pause on complete pattern
            final int oneCycle = (count + 1) * MILLIS_PER_CIRCLE_ANIMATING;
            final int spotInCycle = (int) (SystemClock.elapsedRealtime() - mAnimatingPeriodStart)
                    % oneCycle;
            final int numCircles = spotInCycle / MILLIS_PER_CIRCLE_ANIMATING;

            clearPatternDrawLookup();
            for (int i = 0; i < numCircles; i++) {
                final Cell cell = pattern.get(i);
                drawLookup[cell.getRow()][cell.getColumn()] = true;
            }

            // figure out in progress portion of ghosting line

            final boolean needToUpdateInProgressPoint = numCircles > 0
                    && numCircles < count;

            if (needToUpdateInProgressPoint) {
                final float percentageOfNextCircle = ((float) (spotInCycle % MILLIS_PER_CIRCLE_ANIMATING))
                        / MILLIS_PER_CIRCLE_ANIMATING;

                final Cell currentCell = pattern.get(numCircles - 1);
                final float centerX = getCenterXForColumn(currentCell.column);
                final float centerY = getCenterYForRow(currentCell.row);

                final Cell nextCell = pattern.get(numCircles);
                final float dx = percentageOfNextCircle
                        * (getCenterXForColumn(nextCell.column) - centerX);
                final float dy = percentageOfNextCircle
                        * (getCenterYForRow(nextCell.row) - centerY);
                mInProgressX = centerX + dx;
                mInProgressY = centerY + dy;
            }
            // TODO: Infinite loop here...
            invalidate();
        }

        final float squareWidth = mSquareWidth;
        final float squareHeight = mSquareHeight;

        float radius = mBitmapWidth * 0.1f;
        mPathPaint.setStrokeWidth(radius);

        final Path currentPath = mCurrentPath;
        currentPath.rewind();

        // TODO: the path should be created and cached every time we hit-detect
        // a cell
        // only the last segment of the path should be computed here
        // draw the path of the pattern (unless the user is in progress, and
        // we are in stealth mode)
        final boolean drawPath = (!mInStealthMode || mPatternDisplayMode == DisplayMode.Wrong);

        // draw the arrows associated with the path (unless the user is in
        // progress, and
        // we are in stealth mode)
        boolean oldFlag = (mPaint.getFlags() & Paint.FILTER_BITMAP_FLAG) != 0;
        mPaint.setFilterBitmap(true); // draw with higher quality since we
        // render with transforms
        // draw the lines
        if (drawPath) {
            boolean anyCircles = false;
            for (int i = 0; i < count; i++) {
                Cell cell = pattern.get(i);

                // only draw the part of the pattern stored in
                // the lookup table (this is only different in the case
                // of animation).
                if (!drawLookup[cell.row][cell.column]) {
                    break;
                }
                anyCircles = true;

                float centerX = getCenterXForColumn(cell.column);
                float centerY = getCenterYForRow(cell.row);
                if (i == 0) {
                    currentPath.moveTo(centerX, centerY);
                } else {
                    currentPath.lineTo(centerX, centerY);
                }
            }

            // add last in progress section
            if ((mPatternInProgress || mPatternDisplayMode == DisplayMode.Animate)
                    && anyCircles) {
                currentPath.lineTo(mInProgressX, mInProgressY);
            }
            // chang the line color in different DisplayMode
            if (mPatternDisplayMode == DisplayMode.Wrong)
                mPathPaint.setColor(line_color_wrong);
            else
                mPathPaint.setColor(line_color_right);

            defaultIsHideLine = SpUtil.getInstance().getBoolean(AppConstants.LOCK_IS_HIDE_LINE, false);
            if (!defaultIsHideLine) {
                canvas.drawPath(currentPath, mPathPaint);
            }
        }

        // draw the circles
        final int paddingTop = getPaddingTop();
        final int paddingLeft = getPaddingLeft();

        for (int i = 0; i < 3; i++) {
            float topY = paddingTop + i * squareHeight;
            // float centerY = mPaddingTop + i * mSquareHeight + (mSquareHeight
            // / 2);
            for (int j = 0; j < 3; j++) {
                float leftX = paddingLeft + j * squareWidth;
                drawCircle(canvas, (int) leftX, (int) topY, drawLookup[i][j]);
            }
        }

        mPaint.setFilterBitmap(oldFlag); // restore default flag
    }

    /**
     * @param canvas
     * @param leftX
     * @param topY
     * @param partOfPattern Whether this circle is part of the pattern.
     */
    private void drawCircle(Canvas canvas, int leftX, int topY,
                            boolean partOfPattern) {
        Bitmap outerCircle;
        Bitmap innerCircle = null;

        if (!partOfPattern
                || (mInStealthMode && mPatternDisplayMode != DisplayMode.Wrong)) {
            // unselected circle
            outerCircle = mBitmapCircleDefault;
            innerCircle = null;
        } else if (mPatternInProgress) {
            // user is in middle of drawing a pattern
            outerCircle = mBitmapCircleDefault;
            innerCircle = mBitmapCircleGreen;
        } else if (mPatternDisplayMode == DisplayMode.Wrong) {
            // the pattern is wrong
            outerCircle = mBitmapCircleDefault;
            innerCircle = mBitmapCircleRed;
        } else if (mPatternDisplayMode == DisplayMode.Correct
                || mPatternDisplayMode == DisplayMode.Animate) {
            // the pattern is correct
            outerCircle = mBitmapCircleDefault;
            innerCircle = mBitmapCircleGreen;
        } else {
            throw new IllegalStateException("unknown display mode "
                    + mPatternDisplayMode);
        }

        final int width = mBitmapWidth;
        final int height = mBitmapHeight;

        final float squareWidth = mSquareWidth;
        final float squareHeight = mSquareHeight;

        int offsetX = (int) ((squareWidth - width) / 2f);
        int offsetY = (int) ((squareHeight - height) / 2f);

        // Allow circles to shrink if the view is too small to hold them.
        float sx = Math.min(mSquareWidth / mBitmapWidth, 10.0f) * 4 / 5;
        float sy = Math.min(mSquareHeight / mBitmapHeight, 10.0f) * 4 / 5;

        mCircleMatrix.setTranslate(leftX + offsetX, topY + offsetY);
        mCircleMatrix.preTranslate(mBitmapWidth / 2, mBitmapHeight / 2);
        mCircleMatrix.preScale(sx, sy);
        mCircleMatrix.preTranslate(-mBitmapWidth / 2, -mBitmapHeight / 2);

        canvas.drawBitmap(outerCircle, mCircleMatrix, mPaint);
        if (innerCircle != null)
            canvas.drawBitmap(innerCircle, mCircleMatrix, mPaint);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState,
                LockPatternUtils.patternToString(mPattern),
                mPatternDisplayMode.ordinal(), mInputEnabled, mInStealthMode,
                mEnableHapticFeedback);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        final SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setPattern(DisplayMode.Correct,
                LockPatternUtils.stringToPattern(ss.getSerializedPattern() ) );
        mPatternDisplayMode = DisplayMode.values()[ss.getDisplayMode()];
        mInputEnabled = ss.isInputEnabled();
        mInStealthMode = ss.isInStealthMode();
        mEnableHapticFeedback = ss.isTactileFeedbackEnabled();
    }

    /**
     * The parecelable for saving and restoring a lock pattern view.
     */
    private static class SavedState extends BaseSavedState {

        private final String mSerializedPattern;
        private final int mDisplayMode;
        private final boolean mInputEnabled;
        private final boolean mInStealthMode;
        private final boolean mTactileFeedbackEnabled;

        /**
         * Constructor called from {@link LockPatternView#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, String serializedPattern,
                           int displayMode, boolean inputEnabled, boolean inStealthMode,
                           boolean tactileFeedbackEnabled) {
            super(superState);
            mSerializedPattern = serializedPattern;
            mDisplayMode = displayMode;
            mInputEnabled = inputEnabled;
            mInStealthMode = inStealthMode;
            mTactileFeedbackEnabled = tactileFeedbackEnabled;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            mSerializedPattern = in.readString();
            mDisplayMode = in.readInt();
            mInputEnabled = (Boolean) in.readValue(null);
            mInStealthMode = (Boolean) in.readValue(null);
            mTactileFeedbackEnabled = (Boolean) in.readValue(null);
        }

        public String getSerializedPattern() {
            return mSerializedPattern;
        }

        public int getDisplayMode() {
            return mDisplayMode;
        }

        public boolean isInputEnabled() {
            return mInputEnabled;
        }

        public boolean isInStealthMode() {
            return mInStealthMode;
        }

        public boolean isTactileFeedbackEnabled() {
            return mTactileFeedbackEnabled;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(mSerializedPattern);
            dest.writeInt(mDisplayMode);
            dest.writeValue(mInputEnabled);
            dest.writeValue(mInStealthMode);
            dest.writeValue(mTactileFeedbackEnabled);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
