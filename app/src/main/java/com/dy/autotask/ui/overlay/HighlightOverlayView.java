package com.dy.autotask.ui.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

/**
 * 用于在屏幕上绘制高亮区域的覆盖视图
 */
public class HighlightOverlayView extends View {
    private Rect highlightRect;
    private Paint paint;

    public HighlightOverlayView(Context context) {
        super(context);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setAntiAlias(true);
        
        // 设置视图为透明背景
        setBackgroundColor(Color.TRANSPARENT);
    }

    /**
     * 设置要高亮显示的区域
     * @param rect 要高亮的矩形区域
     */
    public void setHighlightRect(Rect rect) {
        this.highlightRect = rect;
        invalidate(); // 重新绘制
    }

    /**
     * 清除高亮显示
     */
    public void clearHighlight() {
        this.highlightRect = null;
        invalidate(); // 重新绘制
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 如果有高亮区域，则绘制红色边框
        if (highlightRect != null) {
            canvas.drawRect(highlightRect, paint);
        }
    }
}