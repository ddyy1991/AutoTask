package com.dy.autotask.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Stardust on Mar 10, 2017.
 * Modified by SuperMonster003 as of May 3, 2023.
 * Transformed by SuperMonster003 on May 3, 2023.
 */
public class LevelBeamView extends View {

    private int mLevel = 0;
    private float mPaddingLeft = 0f;
    private float mPaddingRight = 0f;
    private float mLinesWidth = 0f;
    private float mLinesOffset = 0f;
    private Paint mLinePaint;
    private int[] mColors;

    public LevelBeamView(Context context) {
        super(context);
        init();
    }

    public LevelBeamView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LevelBeamView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setLevel(int level) {
        mLevel = level;
        requestLayout();
    }

    private void init() {
        setWillNotDraw(false);
        mPaddingLeft = dpToPx(5); // 默认左边距
        mPaddingRight = dpToPx(5); // 默认右边距
        mLinesWidth = dpToPx(2); // 线条宽度
        mLinesOffset = dpToPx(3); // 线条偏移
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStyle(Paint.Style.FILL);
        mLinePaint.setStrokeWidth(mLinesWidth);
        mColors = new int[]{
                Color.parseColor("#FF9800"), // 橙色
                Color.parseColor("#4CAF50"), // 绿色
                Color.parseColor("#2196F3"), // 蓝色
                Color.parseColor("#9C27B0"), // 紫色
                Color.parseColor("#E91E63")  // 粉色
        };
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = (int) (mPaddingLeft + mPaddingRight + (mLevel + 1) * (mLinesWidth + mLinesOffset));
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int lvl = 0; lvl <= mLevel; lvl++) {
            float lineX = mPaddingLeft + lvl * mLinesWidth;
            if (lvl >= 1) {
                lineX += (lvl * mLinesOffset);
            }
            mLinePaint.setColor(getColorForLevel(lvl));
            canvas.drawLine(lineX, 0f, lineX, getHeight(), mLinePaint);
        }
    }

    private int getColorForLevel(int level) {
        return mColors[level % mColors.length];
    }

    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }
}