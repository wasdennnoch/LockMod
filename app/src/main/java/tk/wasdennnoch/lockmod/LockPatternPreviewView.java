/*
 * Copyright (C) 2007 The Android Open Source Project
 * Modifications (C) 2016 MrWasdennnoch@xda
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tk.wasdennnoch.lockmod;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Debug;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays and detects the user's unlock attempt, which is a drag of a finger
 * across regions of the screen.
 * <p/>
 * Is also capable of displaying a static pattern in "in progress", "wrong" or
 * "correct" states.
 */
public class LockPatternPreviewView extends View {

    // Aspect to use when rendering this view
    private static final int ASPECT_SQUARE = 0; // View will be the minimum of width/height
    private static final int ASPECT_LOCK_WIDTH = 1; // Fixed width; height will be minimum of (w,h)
    private static final int ASPECT_LOCK_HEIGHT = 2; // Fixed height; width will be minimum of (w,h)

    private static final boolean PROFILE_DRAWING = false;
    private CellState[][] mCellStates;

    private int mDotSize;
    private int mDotSizeActivated;
    private int mPathWidth;

    private boolean mDrawingProfilingStarted = false;

    private Paint mPaint = new Paint();
    private Paint mPathPaint = new Paint();

    /**
     * How many milliseconds we spend animating each circle of a lock pattern
     * if the animating mode is set.  The entire animation should take this
     * constant * the length of the pattern to complete.
     */
    private static final int MILLIS_PER_CIRCLE_ANIMATING = 700;

    private static byte mPatternSize = 3;

    /**
     * This can be used to avoid updating the display for very small motions or noisy panels.
     * It didn't seem to have much impact on the devices tested, so currently set to 0.
     */
    private static final float DRAG_THRESHHOLD = 0.0f;

    private OnPatternListener mOnPatternListener;
    private ArrayList<Cell> mPattern = new ArrayList<>(mPatternSize * mPatternSize);

    /**
     * Lookup table for the circles of the pattern we are currently drawing.
     * This will be the cells of the complete pattern unless we are animating,
     * in which case we use this to hold the cells we are drawing for the in
     * progress animation.
     */
    private boolean[][] mPatternDrawLookup = new boolean[mPatternSize][mPatternSize];

    /**
     * the in progress point:
     * - during interaction: where the user's finger is
     * - during animation: the current tip of the animating line
     */
    private float mInProgressX = -1;
    private float mInProgressY = -1;

    private long mAnimatingPeriodStart;

    private DisplayMode mPatternDisplayMode = DisplayMode.Correct;
    private boolean mInStealthMode = false;
    private boolean mPatternInProgress = false;

    private float mHitFactor = 0.6f;

    private float mSquareWidth;
    private float mSquareHeight;

    private final Path mCurrentPath = new Path();
    private final Rect mInvalidate = new Rect();
    private final Rect mTmpInvalidateRect = new Rect();

    private int mAspect;
    private int mRegularColor;
    private int mErrorColor;
    private int mSuccessColor;

    private Interpolator mFastOutSlowInInterpolator;
    private Interpolator mLinearOutSlowInInterpolator;


    private boolean mDisableLastSegmentAlpha = false;


    /**
     * Represents a cell in the matrix of the unlock pattern view.
     */
    public static class Cell {
        int row;
        int column;

        // keep # objects limited
        static Cell[][] sCells;

        static {
            updateSize(mPatternSize);
        }

        /**
         * @param row    The row of the cell.
         * @param column The column of the cell.
         */
        /* package */ Cell(int row, int column, byte size) {
            checkRange(row, column, size);
            this.row = row;
            this.column = column;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }

        /**
         * @param row    The row of the cell.
         * @param column The column of the cell.
         */
        public static synchronized Cell of(int row, int column, byte size) {
            return of(sCells, row, column, size);
        }

        public static Cell of(Cell[][] cells, int row, int column, byte size) {
            checkRange(row, column, size);
            return cells[row][column];
        }

        public static void updateSize(byte size) {
            sCells = new Cell[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    sCells[i][j] = new Cell(i, j, size);
                }
            }
        }

        private static void checkRange(int row, int column, byte size) {
            if (row < 0 || row > size - 1) {
                throw new IllegalArgumentException("row must be in range 0-" + (size - 1));
            }
            if (column < 0 || column > size - 1) {
                throw new IllegalArgumentException("column must be in range 0-" + (size - 1));
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Cell) {
                final Cell other = (Cell) o;
                return other.getColumn() == getColumn()
                        && other.getRow() == getRow();
            }
            return false;
        }

        public String toString() {
            return "(row=" + row + ",clmn=" + column + ")";
        }
    }

    public static class CellState {
        public float scale = 1.0f;
        public float translateY = 0.0f;
        public float alpha = 1.0f;
        public float size;
        public float lineEndX = Float.MIN_VALUE;
        public float lineEndY = Float.MIN_VALUE;
        public ValueAnimator lineAnimator;
    }

    /**
     * How to display the current pattern.
     */
    public enum DisplayMode {

        /**
         * The pattern drawn is correct (i.e draw it in a friendly color)
         */
        Correct,

        /**
         * Animate the pattern (for demo, and help).
         */
        Animate,

        /**
         * The pattern is wrong (i.e draw a foreboding color)
         */
        Wrong
    }

    /**
     * The call back interface for detecting patterns entered by the user.
     */
    public interface OnPatternListener {

        /**
         * A new pattern has begun.
         */
        void onPatternStart();

        /**
         * The pattern was cleared.
         */
        void onPatternCleared();

        /**
         * The user extended the pattern currently being drawn by one cell.
         *
         * @param pattern The pattern with newly added cell.
         */
        void onPatternCellAdded(List<Cell> pattern);

        /**
         * A pattern was detected from the user.
         *
         * @param pattern The pattern.
         */
        void onPatternDetected(List<Cell> pattern);
    }

    public LockPatternPreviewView(Context context) {
        this(context, null);
    }

    @SuppressWarnings("deprecation")
    public LockPatternPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mAspect = ASPECT_SQUARE;

        setClickable(true);


        mPathPaint.setAntiAlias(true);
        mPathPaint.setDither(true);

        mRegularColor = getResources().getColor(R.color.regular_color_default);
        mErrorColor = getResources().getColor(R.color.error_color_default);
        mSuccessColor = getResources().getColor(R.color.success_color_default);

        int pathColor = mRegularColor;
        mPathPaint.setColor(pathColor);

        mPathPaint.setStyle(Paint.Style.STROKE);
        mPathPaint.setStrokeJoin(Paint.Join.ROUND);
        mPathPaint.setStrokeCap(Paint.Cap.ROUND);

        mPathWidth = getResources().getDimensionPixelSize(R.dimen.line_width_default);
        mPathPaint.setStrokeWidth(mPathWidth);

        mDotSize = getResources().getDimensionPixelSize(R.dimen.dot_size_default);
        mDotSizeActivated = getResources().getDimensionPixelSize(R.dimen.dot_size_activated_default);

        mPaint.setAntiAlias(true);
        mPaint.setDither(true);

        mCellStates = new CellState[mPatternSize][mPatternSize];
        for (int i = 0; i < mPatternSize; i++) {
            for (int j = 0; j < mPatternSize; j++) {
                mCellStates[i][j] = new CellState();
                mCellStates[i][j].size = mDotSize;
            }
        }

        mFastOutSlowInInterpolator =
                AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in);
        mLinearOutSlowInInterpolator =
                AnimationUtils.loadInterpolator(context, android.R.interpolator.linear_out_slow_in);
    }


    @SuppressWarnings("deprecation")
    public void initValues(SharedPreferences prefs) {
        mRegularColor = prefs.getInt("regular_color", getResources().getColor(R.color.regular_color_default));
        mErrorColor = prefs.getInt("error_color", getResources().getColor(R.color.error_color_default));
        mSuccessColor = prefs.getInt("success_color", getResources().getColor(R.color.success_color_default));
        mDisableLastSegmentAlpha = prefs.getBoolean("disable_last_segment_alpha", false);

        setLineWidth(prefs.getInt("line_width", getResources().getDimensionPixelSize(R.dimen.line_width_default)));
        setDotSize(prefs.getInt("dot_size", getResources().getDimensionPixelSize(R.dimen.dot_size_default)));
        mDotSizeActivated = prefs.getInt("dot_size_activated", getResources().getDimensionPixelSize(R.dimen.dot_size_activated_default));

        if (prefs.getBoolean("stroke_dots", false)) {
            setStrokeDots(true);
            int strokeWidth = prefs.getInt("stroke_dots_width", 6);
            mPaint.setStrokeWidth(strokeWidth);
            if (prefs.getBoolean("dash_dots", false)) {
                PathEffect dash = new DashPathEffect(
                        new float[]{strokeWidth * prefs.getFloat("dash_dots_on_multiplier", 1),
                                strokeWidth * prefs.getFloat("dash_dots_off_multiplier", 1)}
                        , 0);
                PathEffect effect = new ComposePathEffect(dash, new CornerPathEffect(strokeWidth));
                mPaint.setPathEffect(effect);
            } else {
                mPaint.setPathEffect(null);
            }
        } else {
            setStrokeDots(false);
        }
        if (prefs.getBoolean("dash_line", false)) {
            PathEffect dash = new DashPathEffect(
                    new float[]{mPathWidth * prefs.getFloat("dash_line_on_multiplier", 1),
                            mPathWidth * prefs.getFloat("dash_line_off_multiplier", 1)}
                    , 0);
            PathEffect effect = new ComposePathEffect(dash, new CornerPathEffect(mPathWidth));
            mPathPaint.setPathEffect(effect);
        } else {
            mPathPaint.setPathEffect(null);
        }

        if (prefs.getBoolean("blur_dot", false) || prefs.getBoolean("blur_line", false)) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        } else {
            setLayerType(View.LAYER_TYPE_NONE, null);
        }
        if (prefs.getBoolean("blur_dot", false)) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            BlurMaskFilter blurMaskFilter = new BlurMaskFilter(prefs.getInt("blur_dot_radius", 5),
                    BlurMaskFilter.Blur.valueOf(prefs.getString("blur_dot_mode", "NORMAL")));
            mPaint.setMaskFilter(blurMaskFilter);
        } else {
            mPaint.setMaskFilter(null);
        }
        if (prefs.getBoolean("blur_line", false)) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            BlurMaskFilter blurMaskFilter = new BlurMaskFilter(prefs.getInt("blur_line_radius", 5),
                    BlurMaskFilter.Blur.valueOf(prefs.getString("blur_line_mode", "NORMAL")));
            mPathPaint.setMaskFilter(blurMaskFilter);
        } else {
            mPathPaint.setMaskFilter(null);
        }

        setShaderEnabled(prefs.getBoolean("rainbow_shader", false), prefs.getString("rainbow_shader_type", ""));

        invalidate();
    }

    public void setLineWidth(int lineWidth) {
        mPathWidth = lineWidth;
        mPathPaint.setStrokeWidth(lineWidth);
    }

    public void setDotSize(int dotSize) {
        mDotSize = dotSize;
        for (CellState[] c : mCellStates) {
            for (CellState state : c) {
                state.size = dotSize;
            }
        }
    }

    public void setStrokeDots(boolean strokeDots) {
        mPaint.setStyle(strokeDots ? Paint.Style.STROKE : Paint.Style.FILL);
    }

    public void setShaderEnabled(boolean shaderEnabled, final String type) {
        if (shaderEnabled) {
            post(new Runnable() {
                @Override
                public void run() {
                    int[] colors = new int[]{0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
                    Shader shader;
                    switch (type) {
                        case "linear":
                            shader = new LinearGradient(0, 0, 0, getHeight(), colors, null, Shader.TileMode.REPEAT);
                            break;
                        case "radial":
                            shader = new RadialGradient(getWidth() / 2, getHeight() / 2, getWidth() / 2, colors, null, Shader.TileMode.REPEAT);
                            break;
                        case "sweep":
                            shader = new SweepGradient(getWidth() / 2, getHeight() / 2, colors, null);
                            break;
                        default:
                            shader = new LinearGradient(0, 0, 0, getHeight(), colors, null, Shader.TileMode.REPEAT);
                            break;
                    }
                    mPaint.setShader(shader);
                    mPathPaint.setShader(shader);
                    invalidate();
                }
            });
        } else {
            mPaint.setShader(null);
            mPathPaint.setShader(null);
        }
    }


    /**
     * Set the pattern size of the lockscreen
     *
     * @param size The pattern size.
     */
    public void setLockPatternSize(byte size) {
        mPatternSize = size;
        Cell.updateSize(size);
        mCellStates = new CellState[mPatternSize][mPatternSize];
        for (int i = 0; i < mPatternSize; i++) {
            for (int j = 0; j < mPatternSize; j++) {
                mCellStates[i][j] = new CellState();
                mCellStates[i][j].size = mDotSize;
            }
        }
        mPattern = new ArrayList<>(size * size);
        mPatternDrawLookup = new boolean[size][size];
    }

    @SuppressWarnings("unused")
    public byte getLockPatternSize() {
        return mPatternSize;
    }

    /**
     * Set the call back for pattern detection.
     *
     * @param onPatternListener The call back.
     */
    public void setOnPatternListener(
            OnPatternListener onPatternListener) {
        mOnPatternListener = onPatternListener;
    }

    /**
     * Set the display mode of the current pattern.  This can be useful, for
     * instance, after detecting a pattern to tell this view whether change the
     * in progress result to correct or wrong.
     *
     * @param displayMode The display mode.
     */
    public void setDisplayMode(DisplayMode displayMode) {
        mPatternDisplayMode = displayMode;
        if (displayMode == DisplayMode.Animate) {
            if (mPattern.size() == 0) {
                throw new IllegalStateException("you must have a pattern to "
                        + "animate if you want to set the display mode to animate");
            }
            mAnimatingPeriodStart = SystemClock.elapsedRealtime();
            final Cell first = mPattern.get(0);
            mInProgressX = getCenterXForColumn(first.getColumn());
            mInProgressY = getCenterYForRow(first.getRow());
            clearPatternDrawLookup();
        }
        invalidate();
    }


    private void notifyCellAdded() {
        if (mOnPatternListener != null) {
            mOnPatternListener.onPatternCellAdded(mPattern);
        }
    }

    private void notifyPatternStarted() {
        if (mOnPatternListener != null) {
            mOnPatternListener.onPatternStart();
        }
    }

    private void notifyPatternDetected() {
        if (mOnPatternListener != null) {
            mOnPatternListener.onPatternDetected(mPattern);
        }
    }

    private void notifyPatternCleared() {
        if (mOnPatternListener != null) {
            mOnPatternListener.onPatternCleared();
        }
    }

    public void clearPattern() {
        resetPattern();
    }

    /**
     * Reset all pattern state.
     */
    private void resetPattern() {
        mPattern.clear();
        clearPatternDrawLookup();
        mPatternDisplayMode = DisplayMode.Correct;
        invalidate();
    }

    /**
     * Clear the pattern lookup table.
     */
    private void clearPatternDrawLookup() {
        for (int i = 0; i < mPatternSize; i++) {
            for (int j = 0; j < mPatternSize; j++) {
                mPatternDrawLookup[i][j] = false;
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        final int width = w - getPaddingLeft() - getPaddingRight();
        mSquareWidth = width / (float) mPatternSize;

        final int height = h - getPaddingTop() - getPaddingBottom();
        mSquareHeight = height / (float) mPatternSize;
    }

    private int resolveMeasured(int measureSpec, int desired) {
        int result;
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (MeasureSpec.getMode(measureSpec)) {
            case MeasureSpec.UNSPECIFIED:
                result = desired;
                break;
            case MeasureSpec.AT_MOST:
                result = Math.max(specSize, desired);
                break;
            case MeasureSpec.EXACTLY:
            default:
                result = specSize;
        }
        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int minimumWidth = getSuggestedMinimumWidth();
        final int minimumHeight = getSuggestedMinimumHeight();
        int viewWidth = resolveMeasured(widthMeasureSpec, minimumWidth);
        int viewHeight = resolveMeasured(heightMeasureSpec, minimumHeight);

        switch (mAspect) {
            case ASPECT_SQUARE:
                viewWidth = viewHeight = Math.min(viewWidth, viewHeight);
                break;
            case ASPECT_LOCK_WIDTH:
                viewHeight = Math.min(viewWidth, viewHeight);
                break;
            case ASPECT_LOCK_HEIGHT:
                viewWidth = Math.min(viewWidth, viewHeight);
                break;
        }
        // Log.v(TAG, "LockPatternView dimensions: " + viewWidth + "x" + viewHeight);
        setMeasuredDimension(viewWidth, viewHeight);
    }

    /**
     * Determines whether the point x, y will add a new point to the current
     * pattern (in addition to finding the cell, also makes heuristic choices
     * such as filling in gaps based on current pattern).
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    private Cell detectAndAddHit(float x, float y) {
        final Cell cell = checkForNewHit(x, y);
        if (cell != null) {

            // check for gaps in existing pattern
            final ArrayList<Cell> pattern = mPattern;
            if (!pattern.isEmpty()) {
                final Cell lastCell = pattern.get(pattern.size() - 1);
                int dRow = cell.row - lastCell.row;
                int dColumn = cell.column - lastCell.column;

                int fillInRow = lastCell.row;
                int fillInColumn = lastCell.column;

                if (dRow == 0 || dColumn == 0 || Math.abs(dRow) == Math.abs(dColumn)) {
                    while (true) {
                        fillInRow += Integer.signum(dRow);
                        fillInColumn += Integer.signum(dColumn);
                        if (fillInRow == cell.row && fillInColumn == cell.column) break;
                        Cell fillInGapCell = Cell.of(fillInRow, fillInColumn, mPatternSize);
                        if (!mPatternDrawLookup[fillInGapCell.row][fillInGapCell.column]) {
                            addCellToPattern(fillInGapCell);
                        }
                    }
                }
            }

            addCellToPattern(cell);
            return cell;
        }
        return null;
    }

    private void addCellToPattern(Cell newCell) {
        mPatternDrawLookup[newCell.getRow()][newCell.getColumn()] = true;
        mPattern.add(newCell);
        if (!mInStealthMode) {
            startCellActivatedAnimation(newCell);
        }
        notifyCellAdded();
    }

    private void startCellActivatedAnimation(Cell cell) {
        final CellState cellState = mCellStates[cell.row][cell.column];
        startSizeAnimation(mDotSize, mDotSizeActivated, 96, mLinearOutSlowInInterpolator,
                cellState, new Runnable() {
                    @Override
                    public void run() {
                        startSizeAnimation(mDotSizeActivated, mDotSize, 192, mFastOutSlowInInterpolator,
                                cellState, null);
                    }
                });
        startLineEndAnimation(cellState, mInProgressX, mInProgressY,
                getCenterXForColumn(cell.column), getCenterYForRow(cell.row));
    }

    private void startLineEndAnimation(final CellState state,
                                       final float startX, final float startY, final float targetX, final float targetY) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = (float) animation.getAnimatedValue();
                state.lineEndX = (1 - t) * startX + t * targetX;
                state.lineEndY = (1 - t) * startY + t * targetY;
                invalidate();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                state.lineAnimator = null;
            }
        });
        valueAnimator.setInterpolator(mFastOutSlowInInterpolator);
        valueAnimator.setDuration(100);
        valueAnimator.start();
        state.lineAnimator = valueAnimator;
    }

    private void startSizeAnimation(float start, float end, long duration, Interpolator interpolator,
                                    final CellState state, final Runnable endRunnable) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(start, end);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                state.size = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        if (endRunnable != null) {
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    endRunnable.run();
                }
            });
        }
        valueAnimator.setInterpolator(interpolator);
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    // helper method to find which cell a point maps to
    private Cell checkForNewHit(float x, float y) {

        final int rowHit = getRowHit(y);
        if (rowHit < 0) {
            return null;
        }
        final int columnHit = getColumnHit(x);
        if (columnHit < 0) {
            return null;
        }

        if (mPatternDrawLookup[rowHit][columnHit]) {
            return null;
        }
        return Cell.of(rowHit, columnHit, mPatternSize);
    }

    /**
     * Helper method to find the row that y falls into.
     *
     * @param y The y coordinate
     * @return The row that y falls in, or -1 if it falls in no row.
     */
    private int getRowHit(float y) {

        final float squareHeight = mSquareHeight;
        float hitSize = squareHeight * mHitFactor;

        float offset = getPaddingTop() + (squareHeight - hitSize) / 2f;
        for (int i = 0; i < mPatternSize; i++) {

            final float hitTop = offset + squareHeight * i;
            if (y >= hitTop && y <= hitTop + hitSize) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Helper method to find the column x fallis into.
     *
     * @param x The x coordinate.
     * @return The column that x falls in, or -1 if it falls in no column.
     */
    private int getColumnHit(float x) {
        final float squareWidth = mSquareWidth;
        float hitSize = squareWidth * mHitFactor;

        float offset = getPaddingLeft() + (squareWidth - hitSize) / 2f;
        for (int i = 0; i < mPatternSize; i++) {

            final float hitLeft = offset + squareWidth * i;
            if (x >= hitLeft && x <= hitLeft + hitSize) {
                return i;
            }
        }
        return -1;
    }

    /*@Override
    public boolean onHoverEvent(MotionEvent event) {
        if (AccessibilityManager.getInstance(mContext).isTouchExplorationEnabled()) {
            final int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    event.setAction(MotionEvent.ACTION_DOWN);
                    break;
                case MotionEvent.ACTION_HOVER_MOVE:
                    event.setAction(MotionEvent.ACTION_MOVE);
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    event.setAction(MotionEvent.ACTION_UP);
                    break;
            }
            onTouchEvent(event);
            event.setAction(action);
        }
        return super.onHoverEvent(event);
    }*/

    @Override
    public boolean onTouchEvent(@SuppressWarnings("NullableProblems") MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handleActionDown(event);
                return true;
            case MotionEvent.ACTION_UP:
                handleActionUp();
                return true;
            case MotionEvent.ACTION_MOVE:
                handleActionMove(event);
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (mPatternInProgress) {
                    mPatternInProgress = false;
                    resetPattern();
                    notifyPatternCleared();
                }
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
        // Handle all recent motion events so we don't skip any cells even when the device
        // is busy...
        final float radius = mPathWidth;
        final int historySize = event.getHistorySize();
        mTmpInvalidateRect.setEmpty();
        boolean invalidateNow = false;
        for (int i = 0; i < historySize + 1; i++) {
            final float x = i < historySize ? event.getHistoricalX(i) : event.getX();
            final float y = i < historySize ? event.getHistoricalY(i) : event.getY();
            Cell hitCell = detectAndAddHit(x, y);
            final int patternSize = mPattern.size();
            if (hitCell != null && patternSize == 1) {
                mPatternInProgress = true;
                notifyPatternStarted();
            }
            // note current x and y for rubber banding of in progress patterns
            final float dx = Math.abs(x - mInProgressX);
            final float dy = Math.abs(y - mInProgressY);
            if (dx > DRAG_THRESHHOLD || dy > DRAG_THRESHHOLD) {
                invalidateNow = true;
            }

            if (mPatternInProgress && patternSize > 0) {
                final ArrayList<Cell> pattern = mPattern;
                final Cell lastCell = pattern.get(patternSize - 1);
                float lastCellCenterX = getCenterXForColumn(lastCell.column);
                float lastCellCenterY = getCenterYForRow(lastCell.row);

                // Adjust for drawn segment from last cell to (x,y). Radius accounts for line width.
                float left = Math.min(lastCellCenterX, x) - radius;
                float right = Math.max(lastCellCenterX, x) + radius;
                float top = Math.min(lastCellCenterY, y) - radius;
                float bottom = Math.max(lastCellCenterY, y) + radius;

                // Invalidate between the pattern's new cell and the pattern's previous cell
                if (hitCell != null) {
                    final float width = mSquareWidth * 0.5f;
                    final float height = mSquareHeight * 0.5f;
                    final float hitCellCenterX = getCenterXForColumn(hitCell.column);
                    final float hitCellCenterY = getCenterYForRow(hitCell.row);

                    left = Math.min(hitCellCenterX - width, left);
                    right = Math.max(hitCellCenterX + width, right);
                    top = Math.min(hitCellCenterY - height, top);
                    bottom = Math.max(hitCellCenterY + height, bottom);
                }

                // Invalidate between the pattern's last cell and the previous location
                mTmpInvalidateRect.union(Math.round(left), Math.round(top),
                        Math.round(right), Math.round(bottom));
            }
        }
        mInProgressX = event.getX();
        mInProgressY = event.getY();

        // To save updates, we only invalidate if the user moved beyond a certain amount.
        if (invalidateNow) {
            mInvalidate.union(mTmpInvalidateRect);
            invalidate(mInvalidate);
            mInvalidate.set(mTmpInvalidateRect);
        }
    }

    private void handleActionUp() {
        // report pattern detected
        if (!mPattern.isEmpty()) {
            mPatternInProgress = false;
            cancelLineAnimations();
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

    private void cancelLineAnimations() {
        for (int i = 0; i < mPatternSize; i++) {
            for (int j = 0; j < mPatternSize; j++) {
                CellState state = mCellStates[i][j];
                if (state.lineAnimator != null) {
                    state.lineAnimator.cancel();
                    state.lineEndX = Float.MIN_VALUE;
                    state.lineEndY = Float.MIN_VALUE;
                }
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
        } else if (mPatternInProgress) {
            mPatternInProgress = false;
            notifyPatternCleared();
        }
        if (hitCell != null) {
            final float startX = getCenterXForColumn(hitCell.column);
            final float startY = getCenterYForRow(hitCell.row);

            final float widthOffset = mSquareWidth / 2f;
            final float heightOffset = mSquareHeight / 2f;

            invalidate((int) (startX - widthOffset), (int) (startY - heightOffset),
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
            final int spotInCycle = (int) (SystemClock.elapsedRealtime() -
                    mAnimatingPeriodStart) % oneCycle;
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
                final float percentageOfNextCircle =
                        ((float) (spotInCycle % MILLIS_PER_CIRCLE_ANIMATING)) /
                                MILLIS_PER_CIRCLE_ANIMATING;

                final Cell currentCell = pattern.get(numCircles - 1);
                final float centerX = getCenterXForColumn(currentCell.column);
                final float centerY = getCenterYForRow(currentCell.row);

                final Cell nextCell = pattern.get(numCircles);
                final float dx = percentageOfNextCircle *
                        (getCenterXForColumn(nextCell.column) - centerX);
                final float dy = percentageOfNextCircle *
                        (getCenterYForRow(nextCell.row) - centerY);
                mInProgressX = centerX + dx;
                mInProgressY = centerY + dy;
            }
            // TODO: Infinite loop here...
            invalidate();
        }

        final Path currentPath = mCurrentPath;
        currentPath.rewind();

        // draw the circles
        for (int i = 0; i < mPatternSize; i++) {
            float centerY = getCenterYForRow(i);
            for (int j = 0; j < mPatternSize; j++) {
                CellState cellState = mCellStates[i][j];
                float centerX = getCenterXForColumn(j);
                float size = cellState.size * cellState.scale;
                float translationY = cellState.translateY;
                drawCircle(canvas, (int) centerX, (int) centerY + translationY,
                        size, drawLookup[i][j], cellState.alpha);
            }
        }

        // TODO: the path should be created and cached every time we hit-detect a cell
        // only the last segment of the path should be computed here
        // draw the path of the pattern (unless we are in stealth mode)
        final boolean drawPath = ((!mInStealthMode && mPatternDisplayMode != DisplayMode.Wrong)
                || mPatternDisplayMode == DisplayMode.Wrong);
        if (drawPath) {
            mPathPaint.setColor(getCurrentColor(true /* partOfPattern */));

            boolean anyCircles = false;
            float lastX = 0f;
            float lastY = 0f;
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
                if (i != 0) {
                    CellState state = mCellStates[cell.row][cell.column];
                    currentPath.rewind();
                    currentPath.moveTo(lastX, lastY);
                    if (state.lineEndX != Float.MIN_VALUE && state.lineEndY != Float.MIN_VALUE) {
                        currentPath.lineTo(state.lineEndX, state.lineEndY);
                    } else {
                        currentPath.lineTo(centerX, centerY);
                    }
                    canvas.drawPath(currentPath, mPathPaint);
                }
                lastX = centerX;
                lastY = centerY;
            }

            // draw last in progress section
            if ((mPatternInProgress || mPatternDisplayMode == DisplayMode.Animate)
                    && anyCircles) {
                currentPath.rewind();
                currentPath.moveTo(lastX, lastY);
                currentPath.lineTo(mInProgressX, mInProgressY);

                mPathPaint.setAlpha((int) (calculateLastSegmentAlpha(
                        mInProgressX, mInProgressY, lastX, lastY) * 255f));
                canvas.drawPath(currentPath, mPathPaint);
            }
        }
    }

    private float calculateLastSegmentAlpha(float x, float y, float lastX, float lastY) {
        if (mDisableLastSegmentAlpha)
            return Color.alpha(mRegularColor) / 255;

        float diffX = x - lastX;
        float diffY = y - lastY;
        float dist = (float) Math.sqrt(diffX * diffX + diffY * diffY);
        float frac = dist / mSquareWidth;
        return Math.min(1f, Math.max(0f, (frac - 0.3f) * 4f));
    }

    private int getCurrentColor(boolean partOfPattern) {
        if (!partOfPattern || mInStealthMode || mPatternInProgress) {
            // unselected circle
            return mRegularColor;
        } else if (mPatternDisplayMode == DisplayMode.Wrong) {
            // the pattern is wrong
            return mErrorColor;
        } else if (mPatternDisplayMode == DisplayMode.Correct ||
                mPatternDisplayMode == DisplayMode.Animate) {
            return mSuccessColor;
        } else {
            throw new IllegalStateException("unknown display mode " + mPatternDisplayMode);
        }
    }

    /**
     * @param partOfPattern Whether this circle is part of the pattern.
     */
    private void drawCircle(Canvas canvas, float centerX, float centerY, float size,
                            boolean partOfPattern, float alpha) {
        mPaint.setColor(getCurrentColor(partOfPattern));
        mPaint.setAlpha((int) (alpha * 255));
        canvas.drawCircle(centerX, centerY, size / 2, mPaint);
    }

}
