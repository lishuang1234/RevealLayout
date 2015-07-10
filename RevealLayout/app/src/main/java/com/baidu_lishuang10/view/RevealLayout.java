package com.baidu_lishuang10.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.baidu_lishuang10.reveallayout.R;
import com.baidu_lishuang10.util.MotionEventUtil;

import java.util.ArrayList;

/**
 * Created by baidu_lishuang10 on 15/7/9.
 *
 * 1.获取点击View
 * 2.绘制子元素完成时，在点击View之上绘制圆形
 * 3.拦截抬起手势，延迟一段时间进行程序点击调用
 *
 */
public class RevealLayout extends LinearLayout {

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[] mLocationInScreen = new int[2];
    private View mTouchTarget;

    private float mCenterX;
    private float mCenterY;
    private int mTargetWidth;
    private int mTargetHeight;
    private boolean mIsPressed;
    private boolean mShouldDoAnimation;
    private int mRevealRadius;
    private int mRevealRadiusGap;
    private int mMaxRevealRadius;
    private int mMaxBetweenWidthAndHeight;
    private int mMinBetweenWidthAndHeight;
    private final int INVALIDATE_DURATION = 40;

    private DispatchUpTouchEventRunnable runnable = new DispatchUpTouchEventRunnable();

    public RevealLayout(Context context) {
        this(context, null);
    }

    public RevealLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RevealLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);//不会绘制自身
        mPaint.setColor(getResources().getColor(R.color.reveal_color));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        getLocationOnScreen(mLocationInScreen);//获取该ViewGroup屏幕位置
    }

    /**
     * 绘制孩子View时调用，onDraw之后调用，防止子View遮盖动画：
     * 1.圆形半径自增
     * 2.获取被点击View在ViewGroup中的位置
     * 3.canvas剪切范围，绘制圆形，保存
     * 4.如果圆形半径未达到最大值依然请求重绘，如果没有被点击设置状态请求重绘即绘制到初始状态
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mTouchTarget == null || mTargetWidth < 0 || !mShouldDoAnimation)
            return;
        if (mRevealRadius > mMinBetweenWidthAndHeight / 4)
            mRevealRadius += mRevealRadiusGap * 4;
        else
            mRevealRadius += mRevealRadiusGap;

        getLocationOnScreen(mLocationInScreen);
        int[] viewLocation = new int[2];
        mTouchTarget.getLocationOnScreen(viewLocation);
        int left = viewLocation[0] - mLocationInScreen[0];
        int top = viewLocation[1] - mLocationInScreen[1];
        int right = mTouchTarget.getWidth() + left;
        int bottom = mTouchTarget.getMeasuredHeight() + top;

        canvas.save();
        canvas.clipRect(left, top, right, bottom);
        canvas.drawCircle(mCenterX, mCenterY, mRevealRadius, mPaint);
        canvas.restore();

        if (mRevealRadius <= mMaxBetweenWidthAndHeight) {//半径未达到最大绘制宽度
            postInvalidateDelayed(INVALIDATE_DURATION, left, top, right, bottom);
        } else if (!mIsPressed) {
            mShouldDoAnimation = false;
            postInvalidateDelayed(INVALIDATE_DURATION, left, top, right, bottom);
        }
    }

    /**
     * 处理手势事件：
     * 1.手指按下时，获取点击的View，初始化圆形动画参数,请求重绘
     * 2.手指抬起时，改变状态，拦截手势事件，延时调用点击函数，请求重绘
     * 3.手指取消，改变状态，重绘
     * */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int x = (int) ev.getRawX();
        int y = (int) ev.getRawY();
        int action = ev.getAction();
        Log.e("MotionEvent_TAG", "当前手势：" + MotionEventUtil.getMotionState(ev));
        if (action == MotionEvent.ACTION_DOWN) {//手指按下
            View touchTarget = getTouchTarget(this, x, y);
            if (touchTarget != null && touchTarget.isClickable() && touchTarget.isEnabled()) {
                mTouchTarget = touchTarget;
                initParametersForChild(ev, touchTarget);
                postInvalidateDelayed(INVALIDATE_DURATION);
            }
        } else if (action == MotionEvent.ACTION_UP) {//手指抬起,设置等动画完成在去执行点击事件
            mIsPressed = false;
            runnable.event = ev;
            postDelayed(runnable, 40);
            postInvalidateDelayed(INVALIDATE_DURATION);
            return true;//拦截事件不在向下传递给View，避免出现二次的Click事件
        } else if (action == MotionEvent.ACTION_CANCEL) {
            mIsPressed = false;
            postInvalidateDelayed(INVALIDATE_DURATION);
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 初始化动画坐标，动画最大半径，状态标志
     */
    private void initParametersForChild(MotionEvent ev, View touchTarget) {
        mCenterX = ev.getX();
        mCenterY = ev.getY();//注意：这是相对ViewGroup的点击坐标
        mTargetHeight = touchTarget.getMeasuredHeight();
        mTargetWidth = touchTarget.getMeasuredWidth();
        mMaxBetweenWidthAndHeight = Math.max(mTargetHeight, mTargetWidth);
        mMinBetweenWidthAndHeight = Math.min(mTargetHeight, mTargetWidth);
        mRevealRadius = 0;
        mRevealRadiusGap = mMinBetweenWidthAndHeight / 8;
        mIsPressed = true;
        mShouldDoAnimation = true;
        int[] viewLocation = new int[2];
        touchTarget.getLocationOnScreen(viewLocation);//获取targetView相对于屏幕原点坐标
        int leftOffset = viewLocation[0] - mLocationInScreen[0];
        int transformedCenterX = (int) (mCenterX - leftOffset);//获取点击位置相对于targetView左边的宽度
        mMaxRevealRadius = Math.max(transformedCenterX, mTargetWidth - transformedCenterX);//获取动画最大半径

    }

    /**
     * 获取被点击的View
     */

    private View getTouchTarget(View revealLayout, int x, int y) {
        View target = null;
        ArrayList<View> viewArrayList = revealLayout.getTouchables();
        for (View view : viewArrayList) {
            if (isTouchPointInPosition(view, x, y)) {//判断点击坐标是否在View内
                target = view;
                Log.e("TAG", "找到了点击元素！！！");
                break;
            }
        }
        return target;
    }

    /**
     * 判断触摸点是否位于View之中
     */
    private boolean isTouchPointInPosition(View view, int x, int y) {
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int left = viewLocation[0];
        int top = viewLocation[1];
        int right = view.getMeasuredWidth() + left;
        int bottom = view.getMeasuredHeight() + top;
        return (view.isClickable() && x >= left && x <= right && y >= top && y <= bottom);
    }

    private class DispatchUpTouchEventRunnable implements Runnable {

        private MotionEvent event;

        @Override
        public void run() {
            if (mTouchTarget == null || !mTouchTarget.isEnabled() || event == null)
                return;
            if (isTouchPointInPosition(mTouchTarget, (int) event.getRawX(), (int) event.getRawY()))
                performClick();

        }
    }
}
