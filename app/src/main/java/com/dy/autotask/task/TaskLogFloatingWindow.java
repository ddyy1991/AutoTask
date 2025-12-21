package com.dy.autotask.task;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.dy.autotask.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 任务日志悬浮窗
 * 显示任务执行过程中的各种状态和步骤
 */
public class TaskLogFloatingWindow {
    private static final String TAG = "TaskLogFloatingWindow";
    
    private Context context;
    private WindowManager windowManager;
    private View floatingView;
    private TextView logTextView;

    private boolean isShowing = false;
    private boolean isEnabled = true; // 全局配置是否展示日志框
    
    // 用于拖动的变量
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private WindowManager.LayoutParams params;
    
    public TaskLogFloatingWindow(Context context) {
        this.context = context;
        initFloatingWindow();
    }
    
    /**
     * 初始化悬浮窗
     */
    private void initFloatingWindow() {
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        
        // 加载布局
        floatingView = LayoutInflater.from(context).inflate(R.layout.task_log_floating_window, null);
        logTextView = floatingView.findViewById(R.id.log_text_view);
        
        // 设置触摸监听器，使悬浮窗不接收触摸事件
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 不处理任何触摸事件，直接返回true表示消费该事件
                return true;
            }
        });
        
        // 设置窗口参数
        // 将dp转换为像素
        float density = context.getResources().getDisplayMetrics().density;
        int widthPx = (int) (250 * density);
        int heightPx = (int) (150 * density);
        
        params = new WindowManager.LayoutParams(
                widthPx, // 固定宽度250dp
                heightPx, // 固定高度150dp
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;
    }
    
    /**
     * 显示悬浮窗
     */
    public void show() {
        if (!isEnabled || isShowing) {
            return;
        }
        
        try {
            if (floatingView.getWindowToken() == null) {
                windowManager.addView(floatingView, params);
            } else {
                windowManager.updateViewLayout(floatingView, params);
            }
            isShowing = true;
            Log.d(TAG, "任务日志悬浮窗已显示");
        } catch (Exception e) {
            Log.e(TAG, "显示任务日志悬浮窗失败: " + e.getMessage());
        }
    }
    
    /**
     * 隐藏悬浮窗
     */
    public void hide() {
        if (!isShowing) {
            return;
        }
        
        try {
            if (floatingView.getWindowToken() != null) {
                windowManager.removeView(floatingView);
            }
            isShowing = false;
            Log.d(TAG, "任务日志悬浮窗已隐藏");
        } catch (Exception e) {
            Log.e(TAG, "隐藏任务日志悬浮窗失败: " + e.getMessage());
        }
    }
    
    /**
     * 添加日志信息
     * @param message 日志信息
     */
    public void addLog(String message) {
        addColoredLog(message, Color.WHITE);
    }
    
    /**
     * 添加带颜色的日志信息
     * @param message 日志信息
     * @param color 颜色
     */
    public void addColoredLog(String message, int color) {
        // 如果悬浮窗未启用，则不添加日志
        if (!isEnabled) {
            return;
        }
        
        // 如果悬浮窗未显示，尝试显示它
        if (!isShowing) {
            show();
        }
        
        try {
            String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String logEntry = "[" + timestamp + "] " + message + "\n";
            
            CharSequence currentText = logTextView.getText();
            String newText = logEntry + currentText;
            
            // 限制日志行数，避免过多占用内存
            String[] lines = newText.split("\n");
            if (lines.length > 100) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 100; i++) {
                    sb.append(lines[i]).append("\n");
                }
                newText = sb.toString();
            }
            
            // 创建带颜色的文本
            SpannableString spannableString = new SpannableString(newText);
            // 为新添加的日志行设置颜色
            spannableString.setSpan(new ForegroundColorSpan(color), 0, logEntry.length(), 0);
            
            logTextView.setText(spannableString);
            Log.d(TAG, "添加日志: " + message);
        } catch (Exception e) {
            Log.e(TAG, "添加日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 清空日志
     */
    public void clearLog() {
        if (!isEnabled || !isShowing) {
            return;
        }
        
        try {
            logTextView.setText("");
            Log.d(TAG, "日志已清空");
        } catch (Exception e) {
            Log.e(TAG, "清空日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置是否启用日志悬浮窗
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (!enabled && isShowing) {
            hide();
        }
    }
    
    /**
     * 获取是否启用状态
     * @return 是否启用
     */
    public boolean isEnabled() {
        return isEnabled;
    }
    
    /**
     * 获取悬浮窗是否正在显示
     * @return 是否正在显示
     */
    public boolean isShowing() {
        return isShowing;
    }
    
    /**
     * 销毁悬浮窗
     */
    public void destroy() {
        hide();
        context = null;
        windowManager = null;
        floatingView = null;
        logTextView = null;

    }
}