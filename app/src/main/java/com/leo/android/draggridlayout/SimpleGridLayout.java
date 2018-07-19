package com.leo.android.draggridlayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.math.MathUtils;
import android.support.v4.util.Pools;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created on 2018/5/30 上午9:45
 * weimain weimain@anve.com
 */
@SuppressWarnings("unused")
public class SimpleGridLayout extends ViewGroup {

    private int mColumnCount = DEFAULT_COLUMNCOUNT;
    private GridAdapter mGridAdapter;
    private boolean mSameSize = false;
    private int mVGap, mHGap;
    private GridItemClickListener mGridItemClickListener;

    private static final int DEFAULT_COLUMNCOUNT = 3;
    private int mRowCount;

    private final static Rect TEMP_RECT = new Rect();

    private final ViewDragHelper mDragHelper;

    private final static String TAG = "SimpleGridLayout";

    private final GestureDetector mGestureDetector;

    private View mCaptireView;

    private final SparseArray<Point> mLayoutPositionArray = new SparseArray<>();

    private static final Pools.Pool<Point> POINT_POOL = new Pools.SynchronizedPool<>(12);

    private final AtomicInteger mAnimatorCountDown = new AtomicInteger();

    private final List<View> mChildViews = new ArrayList<>();

    private int mTargetPosition = -1;

    public SimpleGridLayout(Context context) {
        this(context, null);
    }

    public SimpleGridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleGridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);


        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SimpleGridLayout);
        setColumnCount(typedArray.getInt(R.styleable.SimpleGridLayout_sgl_columnCount, mColumnCount));
        setVGap(typedArray.getDimensionPixelOffset(R.styleable.SimpleGridLayout_sgl_vgap, mVGap));
        setHGap(typedArray.getDimensionPixelOffset(R.styleable.SimpleGridLayout_sgl_hgap, mHGap));
        setSameSize(typedArray.getBoolean(R.styleable.SimpleGridLayout_sgl_sameSize, mSameSize));
        typedArray.recycle();

        mDragHelper = ViewDragHelper.create(this, new ViewDragHelper.Callback() {
            @Override
            public boolean tryCaptureView(@NonNull View child, int pointerId) {
                return mCaptireView != null && mCaptireView == child;
            }

            @Override
            public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
                return MathUtils.clamp(top, getPaddingTop(), (getHeight() - getPaddingBottom()) - child.getHeight());
            }

            @Override
            public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
                return MathUtils.clamp(left, getPaddingLeft(), (getWidth() - getPaddingRight()) - child.getWidth());
            }

            @Override
            public int getViewVerticalDragRange(@NonNull View child) {
                if (getHeight() == 0) {
                    Log.w(TAG, "getHeight == 0");
                }
                return getHeight() - getPaddingTop() - getPaddingBottom() - child.getHeight();
            }

            @Override
            public int getViewHorizontalDragRange(@NonNull View child) {
                if (getWidth() == 0) {
                    Log.w(TAG, "getWidth == 0");
                }
                return getWidth() - getPaddingLeft() - getPaddingRight() - child.getWidth();
            }

            @Override
            public void onViewReleased(@NonNull final View releasedChild, float xvel, float yvel) {
                super.onViewReleased(releasedChild, xvel, yvel);
                mCaptireView = null;
                if (mTargetPosition > -1) {
                    moveAnimation(releasedChild, mLayoutPositionArray.get(mTargetPosition), mTargetPosition, new Runnable() {
                        @Override
                        public void run() {
                            releasedChild.setAlpha(1f);
                            ViewCompat.setZ(releasedChild, 0f);
                        }
                    });
                    mTargetPosition = -1;
                }
            }

            @Override
            public void onViewCaptured(@NonNull View capturedChild, int activePointerId) {
                super.onViewCaptured(capturedChild, activePointerId);
                mTargetPosition = indexOfChild(capturedChild);
                mCaptireView = capturedChild;
                capturedChild.setPivotX(capturedChild.getWidth() / 2f);
                capturedChild.setPivotY(capturedChild.getHeight() / 2f);
                capturedChild.setAlpha(0.8f);
                ViewCompat.setZ(capturedChild, 100f);
                postInvalidate();
            }

            @Override
            public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
                super.onViewPositionChanged(changedView, left, top, dx, dy);
                Log.d(TAG,"dx: " + dx + "，dy: " + dy);
                int targetPosition = findFirstContactView(left, top, left + changedView.getWidth(), top + changedView.getHeight(), changedView);
                int sourcePosition = indexOfChild(changedView);
                if (targetPosition == -1 || sourcePosition == targetPosition) {
                    if (targetPosition == -1) {
                        Log.d(TAG, "targetPosition == -1");
                    }
                    if (sourcePosition == targetPosition) {
                        Log.w(TAG, "sourcePosition == targetPosition");
                    }
                    return;
                }
                if (mAnimatorCountDown.get() > 0) {
                    return;
                }
                Log.d(TAG, "sourcePositon: " + sourcePosition + " ,targetPosition: " + targetPosition);
                // 移动位置
                if (targetPosition < sourcePosition && (dx < 0 || dy < 0)) {
                    mTargetPosition = targetPosition;
                    for (int i = targetPosition; i < sourcePosition; i++) {
                        moveAnimation(getChildAt(i), mLayoutPositionArray.get(i + 1), i + 1, null);
                    }
                } else if (targetPosition > sourcePosition && (dx > 0 || dy > 0)) {
                    mTargetPosition = targetPosition;
                    for (int i = sourcePosition + 1; i < (targetPosition + 1); i++) {
                        moveAnimation(getChildAt(i), mLayoutPositionArray.get(i - 1), i - 1, null);
                    }
                }
            }
        });

        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                mCaptireView = mDragHelper.findTopChildUnder((int) e.getX(), (int) e.getY());
                if (mCaptireView != null) {
                    mDragHelper.captureChildView(mCaptireView, e.getPointerId(0));
                }
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                final View targetView = mDragHelper.findTopChildUnder((int) e.getX(), (int) e.getY());
                if (targetView == null) {
                    return performClick();
                } else if (mGridItemClickListener != null) {
                    mGridItemClickListener.onClickItem(indexOfChild(targetView), targetView);
                    return true;
                }
                return false;
            }

        });

    }

    public void setColumnCount(int columnCount) {

        if (columnCount <= 0) {
            throw new IllegalArgumentException("columncount > 0");
        }

        if (mColumnCount == columnCount) {
            return;
        }
        mColumnCount = columnCount;
        requestLayout();
    }

    public void setGridAdapter(GridAdapter gridAdapter) {
        if (mGridAdapter == gridAdapter) {
            return;
        }
        mGridAdapter = gridAdapter;
        buildAdapter();
    }

    public void notifyDataChange() {
        if (mGridAdapter == null) {
            return;
        }
        buildAdapter();
    }

    public void setGridItemClickListener(GridItemClickListener gridItemClickListener) {
        mGridItemClickListener = gridItemClickListener;
    }

    public void setSameSize(boolean sameSize) {
        if (mSameSize == sameSize) {
            return;
        }
        mSameSize = sameSize;
        requestLayout();
    }

    public void setVGap(int VGap) {
        mVGap = VGap;
        requestLayout();
    }

    public void setHGap(int HGap) {
        mHGap = HGap;
        requestLayout();
    }

    public int getColumnCount() {
        return mColumnCount;
    }

    public boolean isSameSize() {
        return mSameSize;
    }

    public int getVGap() {
        return mVGap;
    }

    public int getHGap() {
        return mHGap;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (isNotEffecti()) {
            return;
        }

        mRowCount = getChildCount() / mColumnCount;
        if (getChildCount() % mColumnCount > 0) {
            mRowCount++;
        }


        final int canUserWidth = resolveSize(DisplayUtils.getScreenWdith(getContext()), widthMeasureSpec) -
                mHGap * (mColumnCount - 1) - getPaddingLeft() - getPaddingRight();
        final int wantItemWidth = canUserWidth / mColumnCount;
        int wantItemHeight = wantItemWidth;
        if (!mSameSize) {
            wantItemHeight = 0;
            measureChildren(MeasureSpec.makeMeasureSpec(wantItemWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            for (int i = 0; i < getChildCount(); i++) {
                final View childView = getChildAt(i);
                wantItemHeight = Math.max(wantItemHeight, childView.getMeasuredHeight());
            }
        } else {
            final int childMeasureWdith = MeasureSpec.makeMeasureSpec(wantItemWidth, MeasureSpec.EXACTLY);
            final int childMeasureHeight = MeasureSpec.makeMeasureSpec(wantItemHeight, MeasureSpec.EXACTLY);
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).measure(childMeasureWdith, childMeasureHeight);
            }
        }

        int resultWidth = wantItemWidth * mColumnCount + mHGap * (mColumnCount - 1);
        resultWidth += getPaddingLeft() + getPaddingRight();

        int resultHeight = wantItemHeight * mRowCount + mVGap * (mRowCount - 1);
        resultHeight += getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(resultWidth, resultHeight);

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        if (isNotEffecti() || isDragProcess()) {
            return;
        }

        final int itemWidth = (getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - (mColumnCount - 1) * mHGap) /
                mColumnCount;
        final int itemHeight = (getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - (mRowCount - 1) * mVGap) /
                mRowCount;


        int childTop = getPaddingTop();
        int childLeft = getPaddingLeft();

        for (int i = 0; i < getChildCount(); i++) {
            final View childView = getChildAt(i);
            Point point = mLayoutPositionArray.get(i);

            if(point == null){
                point = acquireTempPoint();
            }

            point.x = childLeft;
            point.y = childTop;
            mLayoutPositionArray.put(i, point);

            childView.layout(childLeft, childTop, childLeft + itemWidth, childTop + itemHeight);

            if (mGridAdapter != null) {
                mGridAdapter.handleItemView(childView, i);
            }
            childLeft += itemWidth + mHGap;
            if ((i + 1) % mColumnCount == 0) {
                childTop += itemHeight + mVGap;
                childLeft = getPaddingLeft();
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = mDragHelper.shouldInterceptTouchEvent(ev);
        if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_DRAGGING) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }
        return intercept;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDragHelper.processTouchEvent(event);
        if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_DRAGGING) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }
        // 手势判断
        mGestureDetector.onTouchEvent(event);
        return true;
    }


    @Override
    public int indexOfChild(View child) {
        return mChildViews.indexOf(child);
    }

    @Override
    public View getChildAt(int index) {
        return mChildViews.get(index);
    }


    private boolean isDragProcess() {
        return mDragHelper.getViewDragState() == ViewDragHelper.STATE_DRAGGING ||
                mDragHelper.getViewDragState() == ViewDragHelper.STATE_SETTLING;
    }

    public void buildAdapter() {
        removeAllViews();
        mChildViews.clear();
        // 清除保存的位置信息
        for (int i = 0; i < mLayoutPositionArray.size(); i++) {
            releaseTempPoint(mLayoutPositionArray.valueAt(i));
        }
        mLayoutPositionArray.clear();
        if (isNotEffecti()) {
            return;
        }

        for (int i = 0; i < mGridAdapter.getCount(); i++) {
            final View itemView = mGridAdapter.getView(i, this);
            addView(itemView);
            mChildViews.add(itemView);
        }

        requestLayout();
        invalidate();
    }

    private boolean isNotEffecti() {
        return mGridAdapter == null || mGridAdapter.getCount() <= 0;
    }

    private int findFirstContactView(int left, int top, int right, int bottom, View excludeView) {
        final int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            if (child == excludeView) {
                continue;
            }
            if (left < child.getRight() && child.getLeft() < right && top < child.getBottom() && child.getTop() < bottom) {
                // 相交
                int calculateLeft = left;
                int calculateRight = right;
                int caluculateTop = top;
                int caluculateBottom = bottom;
                if (child.getLeft() > calculateLeft){
                    calculateLeft = child.getLeft();
                }
                if (child.getRight() < calculateRight){
                    calculateRight = child.getRight();
                }
                if (child.getTop() > caluculateTop){
                    caluculateTop = child.getTop();
                }
                if (child.getBottom() < caluculateBottom){
                    caluculateBottom = child.getBottom();
                }
                int unionArea = (calculateRight - calculateLeft) * (caluculateBottom - caluculateTop);
                if (unionArea > (child.getWidth() * child.getHeight() * 0.5f)){
                    return i;
                }
            }
        }
        return -1;
    }


    private static final long DURATION = 300;

    private void moveAnimation(final View targetView, Point targetPoint, final int targetPosition, final @Nullable Runnable onAnimationEnd) {
        if (targetPoint == null || targetView == null) {
            Log.e(TAG, "动画的 view 和 point 不能为空，请检查");
            return;
        }

        ValueAnimator leftAnimator = ValueAnimator.ofInt(targetView.getLeft(), targetPoint.x);
        leftAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                targetView.offsetLeftAndRight((int) animation.getAnimatedValue() - targetView.getLeft());
            }
        });

        ValueAnimator topAnimator = ValueAnimator.ofInt(targetView.getTop(), targetPoint.y);
        topAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                targetView.offsetTopAndBottom((int) animation.getAnimatedValue() - targetView.getTop());
            }
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(DURATION);
        animatorSet.playTogether(leftAnimator, topAnimator);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mChildViews.set(targetPosition, targetView);
                if (onAnimationEnd != null) {
                    onAnimationEnd.run();
                }
                if (mAnimatorCountDown.decrementAndGet() == 0) {
                    // 全部动画结束
                    if (mTargetPosition > -1 && mCaptireView != null) {
                        mChildViews.set(mTargetPosition, mCaptireView);
                    }
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mAnimatorCountDown.incrementAndGet();
            }
        });
        animatorSet.start();
    }

    @NonNull
    private static Point acquireTempPoint() {
        Point point = POINT_POOL.acquire();
        if (point == null) {
            point = new Point();
        }
        return point;
    }

    private static void releaseTempPoint(@NonNull Point point) {
        point.x = 0;
        point.y = 0;
        POINT_POOL.release(point);
    }

}
