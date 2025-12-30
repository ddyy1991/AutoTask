package com.dy.autotask;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.KeyEvent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.lang.reflect.Method;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;

import java.util.List;

import com.dy.autotask.ui.overlay.HighlightOverlayView;

import com.dy.autotask.utils.AutoJsTool;
import com.dy.autotask.utils.AutoTaskHelper;  // 新增：导入AutoTaskHelper
import com.dy.autotask.model.NodeInfo;
import com.dy.autotask.task.AutomationTaskManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 无障碍服务工具类
 */
public class AccessibilityServiceUtil extends AccessibilityService {
    private static final String TAG = "AccessibilityService";
    private static AccessibilityServiceUtil instance;
    private WindowManager windowManager;
    private View floatingView;
    private TextView infoTextView;
    private boolean isFloatingViewAdded = false;

    // 悬浮窗状态管理
    private boolean isMenuExpanded = false;
    private Button btnMainFloat;
    private Button btnCaptureSkeleton;
    private Button btnCopyNodes;
    private Button btnCloseFloat;
    private Button btnToggleLog;
    private Button btnScreenshot;  // 新增：截图按钮
    private FrameLayout floatMenuContainer;
    
    // 骨架图相关
    private View skeletonOverlay;
    private boolean isSkeletonMode = false;
    private boolean shouldDrawRedHighlight = true; // 控制是否绘制红色高亮框
    private AccessibilityNodeInfo selectedNode;
    
    // 元素信息悬浮窗
    private View elementInfoView;
    private boolean isElementInfoShown = false;
    private boolean isFromLayoutHierarchy = false; // 标志：是否从布局层次跳转过来
    
    // 高亮覆盖视图
    private HighlightOverlayView highlightOverlayView;
    private boolean isHighlightShown = false;
    
    // 悬浮窗拖拽状态
    // private boolean isDraggingFloatingView = false; // 已移至OnTouchListener内部
    
    /**
     * 显示高亮覆盖视图
     * @param bounds 要高亮的区域
     */
    private void showHighlightOverlay(Rect bounds) {
        Log.d(TAG, "显示高亮覆盖视图: " + bounds);
        
        // 确保窗口管理器已初始化
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager == null) {
                Log.e(TAG, "无法获取WindowManager服务");
                return;
            }
        }
        
        // 先移除已存在的高亮覆盖视图
        if (highlightOverlayView != null) {
            try {
                windowManager.removeView(highlightOverlayView);
                highlightOverlayView = null;
                isHighlightShown = false;
            } catch (Exception e) {
                Log.e(TAG, "移除旧高亮覆盖视图失败: " + e.getMessage());
            }
        }
        
        // 创建高亮覆盖视图
        highlightOverlayView = new HighlightOverlayView(this);
        highlightOverlayView.setHighlightRect(bounds);
        
        // 设置悬浮窗参数
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = 0;
        layoutParams.y = 0;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                           WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                           WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        
        // 添加悬浮窗到窗口
        try {
            windowManager.addView(highlightOverlayView, layoutParams);
            isHighlightShown = true;
            Log.d(TAG, "高亮覆盖视图添加成功");
        } catch (Exception e) {
            Log.e(TAG, "添加高亮覆盖视图失败: " + e.getMessage());
            highlightOverlayView = null;
            isHighlightShown = false;
        }
    }
    
    /**
     * 隐藏高亮覆盖视图
     */
    private void hideHighlightOverlay() {
        Log.d(TAG, "隐藏高亮覆盖视图");
        
        if (highlightOverlayView != null && windowManager != null) {
            try {
                windowManager.removeView(highlightOverlayView);
                highlightOverlayView = null;
                isHighlightShown = false;
                Log.d(TAG, "高亮覆盖视图移除成功");
            } catch (Exception e) {
                Log.e(TAG, "移除高亮覆盖视图失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 获取简化类名，移除包名前缀
     * @param fullClassName 完整类名
     * @return 简化类名
     */
    private String getSimplifiedClassName(String fullClassName) {
        if (fullClassName == null) {
            return "未知类名";
        }
        
        String simplified = fullClassName;
        
        // 移除常见的包名前缀
        String[] prefixes = {
            "android.widget.",
            "android.view.",
            "android.webkit.",
            "android.support.v7.widget.",
            "androidx.recyclerview.widget.",
            "androidx.appcompat.widget.",
            "androidx.viewpager.widget.",
            "androidx.swiperefreshlayout.widget.",
            "com.google.android.material."
        };
        
        for (String prefix : prefixes) {
            if (simplified.startsWith(prefix)) {
                simplified = simplified.substring(prefix.length());
                break;
            }
        }
        
        return simplified;
    }
    
    /**
     * 检查是否具有悬浮窗权限
     */
    private boolean hasFloatWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true; // 低版本默认有权限
    }

    // 单例模式，确保全局唯一实例
    public static synchronized AccessibilityServiceUtil getInstance() {
        return instance;
    }
    
    /**
     * 初始化或重新初始化悬浮窗
     */
    public void initOrUpdateFloatingView() {
        // 如果悬浮窗已存在，先移除
        if (isFloatingViewAdded && floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
                isFloatingViewAdded = false;
            } catch (Exception e) {
                Log.e(TAG, "移除旧悬浮窗失败: " + e.getMessage());
            }
        }
        
        // 重新初始化悬浮窗
        initFloatingView();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "无障碍服务已连接");
        instance = this;
        
        // 配置无障碍服务信息
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                     AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                     AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.notificationTimeout = 100;
        setServiceInfo(info);
        
        // 初始化AutoJs6工具
        AutoJsTool.getInstance().init(this);
        
        // 初始化悬浮窗
        initOrUpdateFloatingView();
    }

    /**
     * 初始化悬浮窗
     */
    private void initFloatingView() {
        Log.d(TAG, "开始初始化悬浮窗");
        
        // 检查是否已有悬浮窗
        if (isFloatingViewAdded && floatingView != null) {
            Log.d(TAG, "悬浮窗已存在，无需重复初始化");
            return;
        }
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) {
            Log.e(TAG, "无法获取WindowManager服务");
            return;
        }
        
        try {
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_view_layout, null);
            Log.d(TAG, "悬浮窗布局加载成功");
        } catch (Exception e) {
            Log.e(TAG, "加载悬浮窗布局失败: " + e.getMessage());
            return;
        }
        
        // 检查悬浮窗权限
        if (!hasFloatWindowPermission()) {
            Log.w(TAG, "没有悬浮窗权限，无法显示悬浮窗");
            Toast.makeText(this, "请授予悬浮窗权限以使用功能", Toast.LENGTH_LONG).show();
            return;
        } else {
            Log.d(TAG, "已获得悬浮窗权限");
        }
        
        // 设置悬浮窗参数
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        
        // 计算屏幕中间位置的y坐标
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        // 悬浮窗高度约为50dp，设置y坐标为屏幕中间位置
        params.y = screenHeight / 2 - (int)(25 * displayMetrics.density);
        
        // 设置初始尺寸为主按钮的尺寸
        float density = getResources().getDisplayMetrics().density;
        int buttonWidthPx = (int) (35 * density + 0.5f); // 35dp
        int buttonHeightPx = (int) (35 * density + 0.5f); // 35dp
        params.width = buttonWidthPx;
        params.height = buttonHeightPx;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
                       WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                       WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                       WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        params.format = PixelFormat.TRANSLUCENT;
        
        // 添加悬浮窗到窗口
        try {
            windowManager.addView(floatingView, params);
            isFloatingViewAdded = true;
            Log.d(TAG, "悬浮窗添加成功");
            // 使用通知替代Toast，避免在AccessibilityService中使用Toast的问题
            showNotification("AutoTask", "悬浮窗已显示，点击蓝色按钮使用功能");
        } catch (Exception e) {
            Log.e(TAG, "添加悬浮窗失败: " + e.getMessage());
            isFloatingViewAdded = false;
            Toast.makeText(this, "悬浮窗显示失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        
        // 初始化悬浮窗控件
        initFloatingControls();
        
        // 设置按钮点击事件
        setFloatingButtonListeners();
        
        Log.d(TAG, "悬浮窗初始化完成");
    }
    
    /**
     * 初始化悬浮窗控件
     */
    private void initFloatingControls() {
        Log.d(TAG, "开始初始化悬浮窗控件");
        
        if (floatingView == null) {
            Log.e(TAG, "悬浮窗视图为null，无法初始化控件");
            return;
        }
        
        try {
            btnMainFloat = floatingView.findViewById(R.id.btn_main_float);
            btnCaptureSkeleton = floatingView.findViewById(R.id.btn_capture_skeleton);
            btnCopyNodes = floatingView.findViewById(R.id.btn_copy_nodes);
            btnCloseFloat = floatingView.findViewById(R.id.btn_close_float);
            btnToggleLog = floatingView.findViewById(R.id.btn_toggle_log);
            btnScreenshot = floatingView.findViewById(R.id.btn_screenshot);  // 新增：找到截图按钮
            floatMenuContainer = floatingView.findViewById(R.id.float_menu_container);

            Log.d(TAG, "控件查找结果: btnMainFloat=" + (btnMainFloat != null) + ", btnCaptureSkeleton=" + (btnCaptureSkeleton != null) + ", btnCopyNodes=" + (btnCopyNodes != null) + ", btnCloseFloat=" + (btnCloseFloat != null) + ", btnScreenshot=" + (btnScreenshot != null) + ", floatMenuContainer=" + (floatMenuContainer != null));

            if (btnMainFloat == null || btnCaptureSkeleton == null || btnCopyNodes == null || btnCloseFloat == null || btnScreenshot == null || floatMenuContainer == null) {
                Log.e(TAG, "无法找到悬浮窗中的控件");
                return;
            }
            
            // 设置悬浮窗拖拽功能
            setFloatingViewDraggable();
            
            Log.d(TAG, "悬浮窗控件初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "初始化悬浮窗控件失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置悬浮窗可拖拽
     */
    private void setFloatingViewDraggable() {
        if (floatingView == null || btnMainFloat == null) {
            return;
        }
        
        // 移除长按阈值限制，允许直接拖动
        final int DRAG_THRESHOLD = 5; // 降低拖拽阈值，提高灵敏度
        
        // 为蓝色主按钮设置触摸监听器，实现直接拖动
        btnMainFloat.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isDragging = false;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 记录初始位置
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        // 计算移动距离
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;
                        
                        // 判断是否开始拖拽
                        if (!isDragging && (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD)) {
                            isDragging = true;
                        }
                        
                        // 计算新位置
                        int newX = initialX + (int) deltaX;
                        int newY = initialY + (int) deltaY;
                        
                        // 添加边界检测，限制悬浮窗不能拖出屏幕
                        if (windowManager != null) {
                            try {
                                // 获取屏幕尺寸
                                Point screenSize = new Point();
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    // Android 11及以上版本
                                    getDisplay().getRealSize(screenSize);
                                } else {
                                    // 更早的版本
                                    windowManager.getDefaultDisplay().getRealSize(screenSize);
                                }
                                
                                // 使用固定的悬浮窗尺寸（60dp）
                                int floatingViewWidth = 60; // dp
                                int floatingViewHeight = 60; // dp
                                
                                // 将dp转换为像素
                                float density = getResources().getDisplayMetrics().density;
                                int floatingViewWidthPx = (int) (floatingViewWidth * density + 0.5f);
                                int floatingViewHeightPx = (int) (floatingViewHeight * density + 0.5f);
                                
                                // 限制X轴边界
                                newX = Math.max(0, Math.min(newX, screenSize.x - floatingViewWidthPx));
                                
                                // 限制Y轴边界
                                newY = Math.max(0, Math.min(newY, screenSize.y - floatingViewHeightPx));
                            } catch (Exception e) {
                                Log.e(TAG, "计算边界时出错: " + e.getMessage());
                                // 出错时不限制边界
                            }
                        }
                        
                        // 更新悬浮窗位置
                        params.x = newX;
                        params.y = newY;
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        // 检查是否为点击事件
                        if (!isDragging) {
                            btnMainFloat.performClick();
                        }
                        isDragging = false;
                        return true;
                }
                return false;
            }
        });
    }
    
    /**
     * 设置悬浮窗按钮监听事件
     */
    private void setFloatingButtonListeners() {
        // 主按钮点击事件：展开/收起扇形菜单
        btnMainFloat.setOnClickListener(v -> {
            toggleMenuExpansion();
        });
        
        // 功能按钮1：捕获元素骨架图
        btnCaptureSkeleton.setOnClickListener(v -> {
            captureSkeleton();
            toggleMenuExpansion(); // 收起菜单
        });

        // 功能按钮2：复制节点层次数据
        btnCopyNodes.setOnClickListener(v -> {
            copyNodeHierarchy();
            toggleMenuExpansion(); // 收起菜单
        });

        // 功能按钮3：截图（新增）
        btnScreenshot.setOnClickListener(v -> {
            takeScreenshot();
            toggleMenuExpansion(); // 收起菜单
        });

        // 功能按钮4：关闭悬浮窗
        btnCloseFloat.setOnClickListener(v -> {
            closeFloatingView();
        });

        // 功能按钮5：任务日志控制
        btnToggleLog.setOnClickListener(v -> {
            toggleTaskLogWindow();
            toggleMenuExpansion(); // 收起菜单
        });
    }
    
    /**
     * 切换菜单展开/收起状态
     */
    private void toggleMenuExpansion() {
        if (isMenuExpanded) {
            collapseMenu();
        } else {
            expandMenu();
        }
        isMenuExpanded = !isMenuExpanded;
    }
    
    /**
     * 展开菜单（右侧水平排列）
     */
    private void expandMenu() {
        if (btnCaptureSkeleton == null || btnCopyNodes == null || btnCloseFloat == null || btnToggleLog == null || btnScreenshot == null) {
            return;
        }

        // 设置按钮可见性
        btnToggleLog.setVisibility(View.VISIBLE);
        btnScreenshot.setVisibility(View.VISIBLE);  // 新增
        btnCaptureSkeleton.setVisibility(View.VISIBLE);
        btnCopyNodes.setVisibility(View.VISIBLE);
        btnCloseFloat.setVisibility(View.VISIBLE);

        // 动态调整悬浮窗触摸区域以容纳展开的菜单
        adjustFloatingViewSizeForExpandedMenu();

        // 获取屏幕密度，用于dp转px
        float density = getResources().getDisplayMetrics().density;
        // 按钮大小为35dp，间距为3dp
        int buttonWidthPx = (int) (35 * density);
        int spacingPx = (int) (3 * density);

        // 水平排列，从主按钮右侧开始，间距3dp
        // 按钮0：最右侧
        int x0 = buttonWidthPx * 4 + spacingPx * 4;
        int y0 = 0;

        // 按钮1：第二右
        int x1 = buttonWidthPx * 3 + spacingPx * 3;
        int y1 = 0;

        // 按钮2：中间偏右
        int x2 = buttonWidthPx * 2 + spacingPx * 2;
        int y2 = 0;

        // 按钮3：中间偏左
        int x3 = buttonWidthPx + spacingPx;
        int y3 = 0;

        // 按钮4：最左侧（靠近主按钮）
        int x4 = 0;
        int y4 = 0;

        // 应用展开动画
        animateButton(btnToggleLog, x4, y4, 0);      // 最左侧按钮
        animateButton(btnScreenshot, x3, y3, 0);     // 新增：截图按钮在第二个位置
        animateButton(btnCaptureSkeleton, x1, y1, 0);
        animateButton(btnCopyNodes, x2, y2, 150);
        animateButton(btnCloseFloat, x0, y0, 300);   // 最右侧按钮

        // 主按钮旋转动画
        btnMainFloat.animate()
                .rotation(45)
                .setDuration(300)
                .start();
    }

    /**
     * 收起扇形菜单
     */
    private void collapseMenu() {
        if (btnCaptureSkeleton == null || btnCopyNodes == null || btnCloseFloat == null || btnToggleLog == null || btnScreenshot == null) {
            return;
        }

        // 应用收起动画
        animateButtonBack(btnToggleLog, 0);
        animateButtonBack(btnScreenshot, 0);         // 新增
        animateButtonBack(btnCaptureSkeleton, 300);
        animateButtonBack(btnCopyNodes, 150);
        animateButtonBack(btnCloseFloat, 0);

        // 在动画结束后调整悬浮窗触摸区域
        btnCloseFloat.postDelayed(() -> {
            adjustFloatingViewSizeForCollapsedMenu();
        }, 300);

        // 主按钮旋转动画
        btnMainFloat.animate()
                .rotation(0)
                .setDuration(300)
                .start();
    }
    
    /**
     * 按钮展开动画
     */
    private void animateButton(View button, int x, int y, long delay) {
        button.animate()
                .translationX(x)
                .translationY(y)
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(delay)
                .start();
    }
    
    /**
     * 按钮收起动画
     */
    private void animateButtonBack(View button, long delay) {
        button.animate()
                .translationX(0)
                .translationY(0)
                .alpha(0f)
                .setDuration(300)
                .setStartDelay(delay)
                .withEndAction(() -> {
                    button.setVisibility(View.GONE);
                })
                .start();
    }
    
    /**
     * 动态调整悬浮窗大小以适应展开的菜单
     */
    private void adjustFloatingViewSizeForExpandedMenu() {
        if (floatingView == null || windowManager == null) {
            return;
        }

        try {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();
            if (params != null) {
                // 获取屏幕密度
                float density = getResources().getDisplayMetrics().density;

                // 计算展开菜单所需的尺寸
                // 按钮容器左边距: 40dp, 5个子按钮: 35dp * 5, 按钮间间距: 3dp * 4, 右边距: 10dp
                int totalWidthDp = 40 + 35 * 5 + 3 * 4 + 10; // 左边距 + 5个子按钮 + 4个间距 + 右边距
                int totalHeightDp = 40; // 高度稍大于主按钮以容纳所有内容

                // 转换为像素
                int totalWidthPx = (int) (totalWidthDp * density + 0.5f);
                int totalHeightPx = (int) (totalHeightDp * density + 0.5f);

                // 更新布局参数
                params.width = totalWidthPx;
                params.height = totalHeightPx;

                Log.d(TAG, "展开菜单窗口大小: " + totalWidthDp + "dp x " + totalHeightDp + "dp = " + totalWidthPx + "px x " + totalHeightPx + "px");

                // 更新悬浮窗
                windowManager.updateViewLayout(floatingView, params);
            }
        } catch (Exception e) {
            Log.e(TAG, "调整悬浮窗大小失败: " + e.getMessage());
        }
    }
    
    /**
     * 动态调整悬浮窗大小以适应收起的菜单
     */
    private void adjustFloatingViewSizeForCollapsedMenu() {
        if (floatingView == null || windowManager == null) {
            return;
        }
        
        try {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) floatingView.getLayoutParams();
            if (params != null) {
                // 获取屏幕密度
                float density = getResources().getDisplayMetrics().density;
                
                // 收起时只需要主按钮的尺寸
                int widthDp = 35; // 主按钮宽度
                int heightDp = 35; // 主按钮高度
                
                // 转换为像素
                int widthPx = (int) (widthDp * density + 0.5f);
                int heightPx = (int) (heightDp * density + 0.5f);
                
                // 更新布局参数
                params.width = widthPx;
                params.height = heightPx;
                
                // 更新悬浮窗
                windowManager.updateViewLayout(floatingView, params);
            }
        } catch (Exception e) {
            Log.e(TAG, "调整悬浮窗大小失败: " + e.getMessage());
        }
    }
    
    /**
     * 捕获元素骨架图
     */
    private void captureSkeleton() {
        Log.d(TAG, "捕获元素骨架图");
        if (isSkeletonMode) {
            removeSkeletonOverlay();
        } else {
            drawSkeletonOverlay();
        }
        isSkeletonMode = !isSkeletonMode;
    }
    
    /**
     * 复制节点层次数据
     */
    private void copyNodeHierarchy() {
        Log.d(TAG, "复制节点层次数据");
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            StringBuilder info = new StringBuilder();
            traverseNode(rootNode, info, 0);
            
            // 复制到剪贴板
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("节点层次数据", info.toString());
            clipboard.setPrimaryClip(clip);
            
            // 显示提示
            showToast("节点层次数据已复制到剪贴板");
        }
    }
    
    /**
     * 关闭悬浮窗
     */
    private void closeFloatingView() {
        Log.d(TAG, "关闭悬浮窗");
        if (isFloatingViewAdded && floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
                isFloatingViewAdded = false;
                Log.d(TAG, "悬浮窗已关闭");
            } catch (Exception e) {
                Log.e(TAG, "关闭悬浮窗失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 切换任务日志窗口显示状态
     */
    private void toggleTaskLogWindow() {
        Log.d(TAG, "切换任务日志窗口显示状态");

        // 获取任务管理器实例
        AutomationTaskManager taskManager = AutomationTaskManager.getInstance();
        if (taskManager != null) {
            if (taskManager.isLogWindowShowing()) {
                taskManager.hideLogWindow();
            } else {
                taskManager.showLogWindow();
            }
        }
    }

    /**
     * 截图整个屏幕（新增）
     * 优先使用MediaProjection方式，否则使用无障碍方式
     */
    private void takeScreenshot() {
        Log.d(TAG, "用户点击了截图按钮");

        try {
            String filePath = null;
            com.dy.autotask.utils.ScreenshotUtil screenshotUtil =
                    new com.dy.autotask.utils.ScreenshotUtil(getApplicationContext());

            // 检查是否已获取MediaProjection权限
            if (com.dy.autotask.utils.ScreenshotUtil.getMediaProjection() != null) {
                Log.d(TAG, "检测到MediaProjection可用，使用高效截图方式");
                // 优先使用MediaProjection方式（高效）
                filePath = screenshotUtil.captureWithMediaProjection(null);
                if (filePath != null) {
                    Log.i(TAG, "MediaProjection截图成功: " + filePath);
                }
            } else {
                Log.d(TAG, "MediaProjection不可用，使用无障碍服务截图方式");
                // 降级为无障碍方式
                filePath = screenshotUtil.captureScreenWithAccessibility(null);
                if (filePath != null) {
                    Log.i(TAG, "无障碍服务截图成功: " + filePath);
                }
            }

            if (filePath != null) {
                Log.i(TAG, "截图成功: " + filePath);
                Toast.makeText(this, "截图已保存", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "截图失败");
                Toast.makeText(this, "截图失败，请检查日志", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "截图异常: " + e.getMessage(), e);
            Toast.makeText(this, "截图异常: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 绘制骨架图覆盖层
     */
    private void drawSkeletonOverlay() {
        Log.d(TAG, "绘制骨架图覆盖层");
        
        // 确保窗口管理器已初始化
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager == null) {
                Log.e(TAG, "无法获取WindowManager服务");
                return;
            }
        }
        
        // 创建透明覆盖层
        skeletonOverlay = new View(this) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                drawElementSkeletons(canvas);
            }
        };
        
        // 设置覆盖层参数
        WindowManager.LayoutParams overlayParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            overlayParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        overlayParams.gravity = Gravity.TOP | Gravity.START;
        overlayParams.x = 0;
        overlayParams.y = 0;
        overlayParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        overlayParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        overlayParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                               WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                               WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        overlayParams.format = PixelFormat.TRANSLUCENT;
        
        // 添加覆盖层到窗口
        try {
            windowManager.addView(skeletonOverlay, overlayParams);
            Log.d(TAG, "骨架图覆盖层添加成功");
        } catch (Exception e) {
            Log.e(TAG, "添加骨架图覆盖层失败: " + e.getMessage());
            skeletonOverlay = null;
            return;
        }
        
        // 设置覆盖层点击事件
        skeletonOverlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int x = (int) event.getX();
                    int y = (int) event.getY();
                    handleElementClick(x, y);
                    return true;
                }
                return false;
            }
        });
    }
    

    
    /**
     * 遍历元素并绘制边框
     */
    private void traverseAndDrawElements(AccessibilityNodeInfo node, Canvas canvas, Paint paint) {
        if (node == null || canvas == null || paint == null) {
            return;
        }
        
        // 绘制当前元素边框
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        canvas.drawRect(bounds, paint);
        
        // 递归遍历子元素
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = node.getChild(i);
            if (childNode != null) {
                traverseAndDrawElements(childNode, canvas, paint);
                childNode.recycle();
            }
        }
    }
    
    /**
     * 处理元素点击事件
     */
    private void handleElementClick(int x, int y) {
        Log.d(TAG, "元素点击位置: x=" + x + ", y=" + y);
        
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.w(TAG, "无法获取根节点");
            return;
        }
        
        // 查找点击位置的元素
        AccessibilityNodeInfo clickedNode = findNodeAtPosition(rootNode, x, y);
        if (clickedNode != null) {
            selectedNode = clickedNode;
            
            // 重绘骨架图，高亮选中元素
            if (skeletonOverlay != null) {
                skeletonOverlay.invalidate();
            }
            
            // 显示元素操作菜单
            showElementActionMenu(x, y);
        }
        
        // 回收根节点
        rootNode.recycle();
    }
    
    /**
     * 绘制元素骨架
     */
    private void drawElementSkeletons(Canvas canvas) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.w(TAG, "无法获取根节点");
            return;
        }
        
        // 创建绿色画笔
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        
        // 遍历所有元素并绘制边框
        traverseAndDrawElements(rootNode, canvas, paint);
        
        // 高亮绘制选中元素
        if (selectedNode != null && shouldDrawRedHighlight) {
            Paint highlightPaint = new Paint();
            highlightPaint.setColor(Color.RED);
            highlightPaint.setStyle(Paint.Style.STROKE);
            highlightPaint.setStrokeWidth(4f);
            highlightPaint.setStrokeCap(Paint.Cap.ROUND);
            highlightPaint.setStrokeJoin(Paint.Join.ROUND);
            
            Rect bounds = new Rect();
            selectedNode.getBoundsInScreen(bounds);
            canvas.drawRect(bounds, highlightPaint);
        }
        
        // 回收根节点
        rootNode.recycle();
    }
    
    /**
     * 查找指定位置的元素
     */
    private AccessibilityNodeInfo findNodeAtPosition(AccessibilityNodeInfo node, int x, int y) {
        if (node == null) {
            return null;
        }
        
        // 检查当前节点是否包含点击位置
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.contains(x, y)) {
            // 递归检查子节点，返回最内层的元素
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo childNode = node.getChild(i);
                if (childNode != null) {
                    AccessibilityNodeInfo foundNode = findNodeAtPosition(childNode, x, y);
                    childNode.recycle();
                    if (foundNode != null) {
                        return foundNode;
                    }
                }
            }
            // 如果没有子节点包含点击位置，返回当前节点
            return AccessibilityNodeInfo.obtain(node);
        }
        
        return null;
    }
    
    /**
     * 显示元素操作菜单
     */
    private void showElementActionMenu(int x, int y) {
        Log.d(TAG, "显示元素操作菜单");
        
        // 确保窗口管理器已初始化
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager == null) {
                Log.e(TAG, "无法获取WindowManager服务");
                return;
            }
        }
        
        // 先移除已存在的菜单
        if (elementInfoView != null) {
            try {
                windowManager.removeView(elementInfoView);
                elementInfoView = null;
                isElementInfoShown = false;
            } catch (Exception e) {
                Log.e(TAG, "移除旧菜单失败: " + e.getMessage());
            }
        }
        
        // 创建元素操作菜单
        elementInfoView = LayoutInflater.from(this).inflate(R.layout.element_action_menu, null);
        
        // 设置菜单参数
        WindowManager.LayoutParams menuParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            menuParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            menuParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        menuParams.gravity = Gravity.TOP | Gravity.START;
        menuParams.x = x;
        menuParams.y = y;
        menuParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        menuParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        menuParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                           WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                           WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        menuParams.format = PixelFormat.TRANSLUCENT;
        
        // 添加菜单到窗口
        try {
            windowManager.addView(elementInfoView, menuParams);
            isElementInfoShown = true;
            Log.d(TAG, "元素操作菜单添加成功");
        } catch (Exception e) {
            Log.e(TAG, "添加元素操作菜单失败: " + e.getMessage());
            elementInfoView = null;
            isElementInfoShown = false;
            return;
        }
        
        // 设置菜单按钮点击事件
        setElementActionMenuListeners();
    }
    
    /**
     * 设置元素操作菜单按钮监听器
     */
    private void setElementActionMenuListeners() {
        if (elementInfoView == null) {
            return;
        }
        
        // 查看选中元素信息按钮
        Button btnViewElementInfo = elementInfoView.findViewById(R.id.btn_view_element_info);
        // 在布局层次中查看按钮
        Button btnViewInHierarchy = elementInfoView.findViewById(R.id.btn_view_in_hierarchy);
        // 关闭悬浮窗按钮
        Button btnCloseSkeleton = elementInfoView.findViewById(R.id.btn_close_skeleton);
        
        if (btnViewElementInfo != null) {
            btnViewElementInfo.setOnClickListener(v -> {
                showElementDetails();
            });
        }
        
        if (btnViewInHierarchy != null) {
            btnViewInHierarchy.setOnClickListener(v -> {
                showLayoutHierarchyWithAnalyzer();
            });
        }
        
        if (btnCloseSkeleton != null) {
            btnCloseSkeleton.setOnClickListener(v -> {
                removeSkeletonOverlay();
                isSkeletonMode = false;
            });
        }
    }
    
    /**
     * 显示元素详细信息
     */
    private void showElementDetails() {
        Log.d(TAG, "显示元素详细信息");
        
        if (selectedNode == null) {
            Log.e(TAG, "没有选中的元素");
            return;
        }
        
        // 确保窗口管理器已初始化
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager == null) {
                Log.e(TAG, "无法获取WindowManager服务");
                return;
            }
        }
        
        // 先移除已存在的元素信息窗口
        if (elementInfoView != null) {
            try {
                windowManager.removeView(elementInfoView);
                elementInfoView = null;
                isElementInfoShown = false;
            } catch (Exception e) {
                Log.e(TAG, "移除旧元素信息窗口失败: " + e.getMessage());
            }
        }
        
        // 如果不是从布局层次跳转过来的，重置标志
        if (!isFromLayoutHierarchy) {
            isFromLayoutHierarchy = false;
        }
        
        // 创建元素信息窗口
        elementInfoView = LayoutInflater.from(this).inflate(R.layout.element_details_layout, null);
        
        // 设置窗口参数
        WindowManager.LayoutParams detailsParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            detailsParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            detailsParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        detailsParams.gravity = Gravity.TOP | Gravity.START;
        detailsParams.x = 0;
        detailsParams.y = 0;
        detailsParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        detailsParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        detailsParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                           WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        detailsParams.format = PixelFormat.TRANSLUCENT;
        detailsParams.alpha = 1.0f; // 设置为不透明背景
        
        // 添加窗口到屏幕
        try {
            windowManager.addView(elementInfoView, detailsParams);
            isElementInfoShown = true;
            Log.d(TAG, "元素信息窗口添加成功");
        } catch (Exception e) {
            Log.e(TAG, "添加元素信息窗口失败: " + e.getMessage());
            elementInfoView = null;
            isElementInfoShown = false;
            return;
        }
        
        // 填充元素信息
        fillElementDetails();
        
        // 设置关闭按钮事件
        Button btnCloseDetails = elementInfoView.findViewById(R.id.btn_close_details);
        if (btnCloseDetails != null) {
            btnCloseDetails.setOnClickListener(v -> {
                try {
                    if (windowManager != null && elementInfoView != null) {
                        windowManager.removeView(elementInfoView);
                        elementInfoView = null;
                        isElementInfoShown = false;
                        
                        // 如果是从布局层次跳转过来的，不需要关闭布局层次界面
                        if (isFromLayoutHierarchy) {
                            // 重置标志
                            isFromLayoutHierarchy = false;
                            
                            // 恢复标志位，重新绘制红色高亮框
                            shouldDrawRedHighlight = true;
                            
                            // 重绘骨架图
                            if (skeletonOverlay != null) {
                                skeletonOverlay.invalidate();
                            }
                            
                            // 隐藏高亮覆盖视图
                            hideHighlightOverlay();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "关闭元素信息窗口失败: " + e.getMessage());
                }
            });
        }
    }
    
    /**
     * 使用原生API显示布局层次
     */
    private void showLayoutHierarchyWithAnalyzer() {
        Log.d(TAG, "使用原生API显示布局层次");
        
        // 设置标志位，不再绘制红色高亮框
        shouldDrawRedHighlight = false;
        
        // 重绘骨架图
        if (skeletonOverlay != null) {
            skeletonOverlay.invalidate();
        }
        
        // 创建全屏悬浮窗来显示布局层次
        showFullscreenLayoutHierarchy();
    }
    
    /**
     * 从NodeInfo显示元素详情
     * @param nodeInfo 节点信息
     */
    public void showElementDetailsFromNode(com.dy.autotask.model.NodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return;
        }
        
        // 将NodeInfo转换为AccessibilityNodeInfo
        selectedNode = nodeInfo.getAccessibilityNodeInfo();
        
        // 设置标志，表示是从布局层次跳转过来的
        isFromLayoutHierarchy = true;
        
        // 显示元素详情
        showElementDetails();
    }
    
    /**
     * 填充元素详细信息
     */
    private void fillElementDetails() {
        if (elementInfoView == null || selectedNode == null) {
            return;
        }
        
        LinearLayout container = elementInfoView.findViewById(R.id.element_info_container);
        if (container == null) {
            Log.e(TAG, "无法找到元素信息容器");
            return;
        }
        
        // 清空容器
        container.removeAllViews();
        
        // 获取元素属性
        Map<String, String> elementProperties = getElementProperties(selectedNode);
        
        // 动态创建属性项
        for (Map.Entry<String, String> entry : elementProperties.entrySet()) {
            String propertyName = entry.getKey();
            String propertyValue = entry.getValue();
            
            // 创建属性项布局
            LinearLayout propertyItem = new LinearLayout(this);
            propertyItem.setOrientation(LinearLayout.HORIZONTAL);
            propertyItem.setPadding(0, 8, 0, 8);
            propertyItem.setGravity(Gravity.CENTER_VERTICAL);
            
            // 属性名
            TextView nameTextView = new TextView(this);
            nameTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            nameTextView.setText(propertyName);
            nameTextView.setTextColor(Color.BLACK);
            nameTextView.setTextSize(16); // 增大字体大小
            nameTextView.setPadding(0, 8, 0, 8);
            
            // 属性值
            TextView valueTextView = new TextView(this);
            valueTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
            valueTextView.setText(propertyValue);
            valueTextView.setTextColor(Color.BLUE);
            valueTextView.setTextSize(14); // 比属性名小一号字体
            valueTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); // 居中展示
            valueTextView.setGravity(Gravity.CENTER);
            valueTextView.setEllipsize(TextUtils.TruncateAt.END);
            valueTextView.setSingleLine(false);
            valueTextView.setClickable(true);
            valueTextView.setFocusable(true);
            valueTextView.setTag(propertyValue);
            
            // 优化按钮样式
            valueTextView.setBackgroundResource(R.drawable.attribute_value_button);
            valueTextView.setPadding(16, 12, 16, 12);
            valueTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            valueTextView.setTypeface(null, Typeface.BOLD_ITALIC);
            
            // 添加点击复制功能
            valueTextView.setOnClickListener(v -> {
                String value = (String) v.getTag();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("元素属性值", value);
                clipboard.setPrimaryClip(clip);
                
                // 显示弹窗内提示
                showCopySuccessTip();
            });
            
            // 添加到属性项
            propertyItem.addView(nameTextView);
            propertyItem.addView(valueTextView);
            
            // 添加到容器
            container.addView(propertyItem);
        }
    }
    
    /**
     * 显示复制成功提示
     */
    private void showCopySuccessTip() {
        if (elementInfoView == null) {
            return;
        }
        
        TextView copySuccessTip = elementInfoView.findViewById(R.id.copy_success_tip);
        if (copySuccessTip != null) {
            copySuccessTip.setVisibility(View.VISIBLE);
            
            // 1秒后自动隐藏
            copySuccessTip.postDelayed(() -> {
                if (copySuccessTip != null && copySuccessTip.isAttachedToWindow()) {
                    copySuccessTip.setVisibility(View.GONE);
                }
            }, 1000);
        }
    }
    
    /**
     * 获取元素属性
     */
    private Map<String, String> getElementProperties(AccessibilityNodeInfo node) {
        Map<String, String> properties = new LinkedHashMap<>();
        
        if (node == null) {
            return properties;
        }
        
        // 获取资源ID相关信息
        String resourceName = node.getViewIdResourceName() != null ? node.getViewIdResourceName() : "null";
        String id = resourceName != null ? resourceName.contains("/") ? resourceName.substring(resourceName.lastIndexOf("/") + 1) : resourceName : "null";
        String fullId = resourceName;
        
        // 添加新的属性字段，按照要求的顺序排列
        // 前三个位置：id, fullId, text
        properties.put("id", id);
        properties.put("fullId", fullId);
        properties.put("text", node.getText() != null ? node.getText().toString() : "null");
        
        // 基本属性
        properties.put("className", node.getClassName() != null ? node.getClassName().toString() : "null");
        properties.put("contentDescription", node.getContentDescription() != null ? node.getContentDescription().toString() : "null");
        
        // 位置和大小
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        properties.put("bounds", bounds.toString());
        
        // 交互属性
        properties.put("editable", String.valueOf(node.isEditable()));
        properties.put("enabled", String.valueOf(node.isEnabled()));
        properties.put("focusable", String.valueOf(node.isFocusable()));
        properties.put("focused", String.valueOf(node.isFocused()));
        properties.put("longClickable", String.valueOf(node.isLongClickable()));
        properties.put("scrollable", String.valueOf(node.isScrollable()));
        properties.put("selected", String.valueOf(node.isSelected()));
        properties.put("clickable", String.valueOf(node.isClickable()));
        properties.put("checkable", String.valueOf(node.isCheckable()));
        properties.put("checked", String.valueOf(node.isChecked()));
        
        // 其他属性
        properties.put("idHex", "0x" + Integer.toHexString(node.getViewIdResourceName() != null ? node.getViewIdResourceName().hashCode() : 0));
        properties.put("packageName", node.getPackageName() != null ? node.getPackageName().toString() : "null");
        properties.put("row", String.valueOf(-1)); // Android AccessibilityNodeInfo没有直接提供row信息
        properties.put("rowCount", String.valueOf(0)); // Android AccessibilityNodeInfo没有直接提供rowCount信息
        properties.put("rowSpan", String.valueOf(-1)); // Android AccessibilityNodeInfo没有直接提供rowSpan信息
        properties.put("childCount", String.valueOf(node.getChildCount()));
        properties.put("depth", String.valueOf(getNodeDepth(node)));
        
        return properties;
    }
    
    /**
     * 获取节点深度
     */
    private int getNodeDepth(AccessibilityNodeInfo node) {
        int depth = 0;
        AccessibilityNodeInfo parent = node.getParent();
        while (parent != null) {
            depth++;
            AccessibilityNodeInfo temp = parent.getParent();
            parent.recycle();
            parent = temp;
        }
        return depth;
    }
    
    /**
     * 显示布局层次
     */
    
    /**
     * 移除骨架图覆盖层
     */
    private void removeSkeletonOverlay() {
        Log.d(TAG, "移除骨架图覆盖层");
        
        if (skeletonOverlay != null && windowManager != null) {
            try {
                windowManager.removeView(skeletonOverlay);
                skeletonOverlay = null;
                Log.d(TAG, "骨架图覆盖层已移除");
            } catch (Exception e) {
                Log.e(TAG, "移除骨架图覆盖层失败: " + e.getMessage());
            }
        }
        
        // 移除元素信息悬浮窗
        if (elementInfoView != null && windowManager != null) {
            try {
                windowManager.removeView(elementInfoView);
                elementInfoView = null;
                isElementInfoShown = false;
            } catch (Exception e) {
                Log.e(TAG, "移除元素信息悬浮窗失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 显示全屏布局层次悬浮窗
     */
    private void showFullscreenLayoutHierarchy() {
        Log.d(TAG, "显示全屏布局层次悬浮窗");
        
        // 确保窗口管理器已初始化
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager == null) {
                Log.e(TAG, "无法获取WindowManager服务");
                return;
            }
        }
        
        // 先移除已存在的悬浮窗
        if (elementInfoView != null) {
            try {
                windowManager.removeView(elementInfoView);
                elementInfoView = null;
                isElementInfoShown = false;
            } catch (Exception e) {
                Log.e(TAG, "移除旧悬浮窗失败: " + e.getMessage());
            }
        }
        
        // 创建布局层次悬浮窗
        elementInfoView = LayoutInflater.from(this).inflate(R.layout.layout_hierarchy_display, null);
        
        // 设置悬浮窗参数
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = 0;
        layoutParams.y = 0;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                           WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.alpha = 1.0f;
        
        // 添加悬浮窗到窗口
        try {
            windowManager.addView(elementInfoView, layoutParams);
            isElementInfoShown = true;
            Log.d(TAG, "全屏布局层次悬浮窗添加成功");
        } catch (Exception e) {
            Log.e(TAG, "添加全屏布局层次悬浮窗失败: " + e.getMessage());
            elementInfoView = null;
            isElementInfoShown = false;
            return;
        }
        
        // 显示布局层次树
        com.dy.autotask.ui.layoutinspector.LayoutHierarchyView hierarchyView = elementInfoView.findViewById(R.id.layout_hierarchy_view);
        if (hierarchyView != null) {
            // 设置点击高亮回调
            hierarchyView.setOnItemClickHighlightListener(bounds -> {
                showHighlightOverlay(bounds);
            });
            
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                // 检查根节点是否可见，只有根节点可见时才显示布局层次
                if (rootNode.isVisibleToUser()) {
                    NodeInfo nodeInfo = NodeInfo.capture(rootNode);
                    hierarchyView.setRootNode(nodeInfo);
                    // 如果有选中的节点，设置并高亮显示
                    if (selectedNode != null) {
                        hierarchyView.setSelectedNode(selectedNode);
                        
                        // 获取选中节点的边界并显示红色线框
                        Rect bounds = new Rect();
                        selectedNode.getBoundsInScreen(bounds);
                        showHighlightOverlay(bounds);
                    }
                } else {
                    // 根节点不可见时，显示提示信息
                    Toast.makeText(this, "当前窗口根节点不可见，无法显示布局层次", Toast.LENGTH_SHORT).show();
                }
            }
        }
        
        // 设置关闭按钮事件
        Button btnCloseHierarchy = elementInfoView.findViewById(R.id.btn_close_hierarchy);
        if (btnCloseHierarchy != null) {
            btnCloseHierarchy.setOnClickListener(v -> {
                try {
                    if (windowManager != null && elementInfoView != null) {
                        windowManager.removeView(elementInfoView);
                        elementInfoView = null;
                        isElementInfoShown = false;
                        
                        // 恢复标志位，重新绘制红色高亮框
                        shouldDrawRedHighlight = true;
                        
                        // 重绘骨架图
                        if (skeletonOverlay != null) {
                            skeletonOverlay.invalidate();
                        }
                        
                        // 隐藏高亮覆盖视图
                        hideHighlightOverlay();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "关闭布局层次悬浮窗失败: " + e.getMessage());
                }
            });
        }
    }
    
    /**
     * 显示Toast提示
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示当前窗口信息
     */
    private void showCurrentWindowInfo() {
        Log.d(TAG, "开始获取当前窗口信息");
        
        if (infoTextView == null) {
            Log.e(TAG, "infoTextView为空");
            return;
        }
        
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            Log.d(TAG, "获取到根节点，开始构建信息");
            StringBuilder info = new StringBuilder();
            info.append("当前窗口信息:\n");
            info.append("窗口类名: ").append(rootNode.getClassName() != null ? rootNode.getClassName() : "未知").append("\n");
            info.append("窗口包名: ").append(rootNode.getPackageName() != null ? rootNode.getPackageName() : "未知").append("\n");
            info.append("子节点数: ").append(rootNode.getChildCount()).append("\n\n");
            
            // 获取布局层次结构
            info.append("布局层次结构:\n");
            traverseNode(rootNode, info, 0);
            
            String infoText = info.toString();
            Log.d(TAG, "准备显示信息，长度: " + infoText.length());
            infoTextView.setText(infoText);
            
            // 确保滚动到顶部
            infoTextView.post(() -> {
                ScrollView scrollView = (ScrollView) infoTextView.getParent();
                if (scrollView != null) {
                    scrollView.scrollTo(0, 0);
                }
            });
        } else {
            Log.w(TAG, "无法获取根节点");
            infoTextView.setText("无法获取当前窗口信息，请确保已启用无障碍服务并打开了应用界面。");
        }
    }

    /**
     * 遍历节点并构建层次结构信息
     */
    private void traverseNode(AccessibilityNodeInfo node, StringBuilder info, int depth) {
        if (node == null || info == null) return;
        
        // 不复制隐藏状态的元素
        if (!node.isVisibleToUser()) {
            return;
        }
        
        // 限制递归深度，防止栈溢出
        if (depth > 50) {
            info.append("... (递归深度超过限制)\n");
            return;
        }
        
        // 添加缩进表示层级
        for (int i = 0; i < depth && i < 20; i++) { // 限制缩进层数
            info.append("  ");
        }
        
        // 获取简化类名
        String simplifiedClassName = "未知类名";
        CharSequence className = node.getClassName();
        if (className != null) {
            simplifiedClassName = getSimplifiedClassName(className.toString());
        }
        
        // 获取元素完整ID
        String fullId = "无ID";
        if (node.getViewIdResourceName() != null) {
            fullId = node.getViewIdResourceName();
        }
        
        // 获取区域坐标
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        String coordinates = "[" + bounds.left + "," + bounds.top + "," + bounds.right + "," + bounds.bottom + "]";
        
        // 添加节点信息：简化类名、完整ID、区域坐标、文本内容（按指定顺序）
        info.append(simplifiedClassName)
            .append("|")
            .append(fullId)
            .append("|")
            .append(coordinates)
            .append("|");
            
        // 处理文本内容
        CharSequence text = node.getText();
        if (text != null && text.length() > 0) {
            String textStr = text.toString();
            // 限制文本长度以防止信息过长
            if (textStr.length() > 50) {
                textStr = textStr.substring(0, 50) + "...";
            }
            info.append(textStr);
        } else {
            info.append(""); // 如果没有文本，添加空字符串以保持格式
        }
        
        info.append("\n");
        
        // 递归遍历子节点，限制子节点数量以防止信息过长
        int childCount = Math.min(node.getChildCount(), 100); // 最多处理100个子节点
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                traverseNode(child, info, depth + 1);
            }
        }
        
        // 如果有更多子节点未处理，添加提示
        if (node.getChildCount() > childCount) {
            for (int i = 0; i < depth + 1 && i < 20; i++) {
                info.append("  ");
            }
            info.append("... (还有 ").append(node.getChildCount() - childCount).append(" 个子节点未显示)\n");
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 处理无障碍事件
        Log.d(TAG, "收到无障碍事件: " + event.toString());
        
        // 监听窗口内容变化事件
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || 
            event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "窗口内容发生变化");
            Log.d(TAG, "事件包名: " + event.getPackageName());
            Log.d(TAG, "事件类名: " + event.getClassName());
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "无障碍服务被中断");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // 移除悬浮窗
        if (isFloatingViewAdded && floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
                isFloatingViewAdded = false;
            } catch (Exception e) {
                Log.e(TAG, "移除悬浮窗失败: " + e.getMessage());
            }
        }
        
        // 清空实例引用
        instance = null;
        
        return super.onUnbind(intent);
    }

    // ==================== 元素查找方法 ====================

    /**
     * 根据文本内容查找节点（带超时）
     */
    public AccessibilityNodeInfo findNodeByText(String text, long timeoutMs) throws TimeoutException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final AccessibilityNodeInfo[] result = {null};
        
        Thread thread = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null) {
                    result[0] = findNodeByTextRecursive(root, text);
                    if (result[0] != null) {
                        break;
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
            latch.countDown();
        });
        
        thread.start();
        
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            thread.interrupt();
            throw new TimeoutException("查找节点超时: " + text);
        }
        
        if (result[0] == null) {
            throw new RuntimeException("未找到节点: " + text);
        }
        
        return result[0];
    }

    /**
     * 递归查找文本节点
     */
    private AccessibilityNodeInfo findNodeByTextRecursive(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;
        
        // 检查当前节点
        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.toString().equals(text)) {
            return node;
        }
        
        // 递归检查子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findNodeByTextRecursive(child, text);
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }

    /**
     * 根据ID查找节点（带超时）
     */
    public AccessibilityNodeInfo findNodeById(String viewId, long timeoutMs) throws TimeoutException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final AccessibilityNodeInfo[] result = {null};
        
        Thread thread = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null) {
                    List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(viewId);
                    if (!nodes.isEmpty()) {
                        result[0] = nodes.get(0);
                        if (result[0] != null) {
                            break;
                        }
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
            latch.countDown();
        });
        
        thread.start();
        
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            thread.interrupt();
            throw new TimeoutException("查找节点超时: " + viewId);
        }
        
        if (result[0] == null) {
            throw new RuntimeException("未找到节点: " + viewId);
        }
        
        return result[0];
    }

    /**
     * 根据描述查找节点（带超时）
     */
    public AccessibilityNodeInfo findNodeByDescription(String description, long timeoutMs) throws TimeoutException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final AccessibilityNodeInfo[] result = {null};
        
        Thread thread = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null) {
                    result[0] = findNodeByDescriptionRecursive(root, description);
                    if (result[0] != null) {
                        break;
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
            latch.countDown();
        });
        
        thread.start();
        
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            thread.interrupt();
            throw new TimeoutException("查找节点超时: " + description);
        }
        
        if (result[0] == null) {
            throw new RuntimeException("未找到节点: " + description);
        }
        
        return result[0];
    }
    
    /**
     * 递归查找描述节点
     */
    private AccessibilityNodeInfo findNodeByDescriptionRecursive(AccessibilityNodeInfo node, String description) {
        if (node == null) return null;
        
        // 检查当前节点
        CharSequence nodeDescription = node.getContentDescription();
        if (nodeDescription != null && nodeDescription.toString().equals(description)) {
            return node;
        }
        
        // 递归检查子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findNodeByDescriptionRecursive(child, description);
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 根据类名查找节点（带超时）
     */
    public List<AccessibilityNodeInfo> findNodesByClass(String className, long timeoutMs) throws TimeoutException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final List<AccessibilityNodeInfo>[] result = new List[]{new ArrayList<>()};
        
        Thread thread = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null) {
                    result[0] = findNodesByClassRecursive(root, className);
                    if (!result[0].isEmpty()) {
                        break;
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
            latch.countDown();
        });
        
        thread.start();
        
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            thread.interrupt();
            throw new TimeoutException("查找节点超时: " + className);
        }
        
        return result[0];
    }

    /**
     * 递归查找指定类名的节点
     */
    private List<AccessibilityNodeInfo> findNodesByClassRecursive(AccessibilityNodeInfo node, String className) {
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        if (node == null) return result;
        
        // 检查当前节点
        if (className.equals(node.getClassName())) {
            result.add(node);
        }
        
        // 递归检查子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                result.addAll(findNodesByClassRecursive(child, className));
            }
        }
        
        return result;
    }

    /**
     * 点击节点
     */
    public boolean clickNode(AccessibilityNodeInfo node) {
        if (node == null) return false;
        
        // 在Android 7.0及以上版本使用Gesture方式点击
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            boolean gestureResult = performGestureClick(node);
            if (gestureResult) {
                return true;
            } else {
                Log.e(TAG, "使用Gesture方式点击失败");
            }
        }
        
        // 在低版本Android或Gesture点击失败时，回退到传统点击方式
        Log.d(TAG, "尝试使用传统方式点击");
        
        // 方法1: 使用performAction点击
        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            Log.d(TAG, "使用performAction方式点击成功");
            return true;
        } else {
            Log.w(TAG, "使用performAction方式点击失败");
        }
        
        // 方法2: 使用ACTION_FOCUS + ACTION_CLICK组合
        if (node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
            Log.d(TAG, "焦点设置成功");
            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                Log.d(TAG, "使用焦点+点击方式点击成功");
                return true;
            } else {
                Log.w(TAG, "使用焦点+点击方式点击失败");
            }
        } else {
            Log.w(TAG, "焦点设置失败");
        }
        
        // 方法3: 查找可点击的父节点
        AccessibilityNodeInfo parentNode = node.getParent();
        while (parentNode != null) {
            if (parentNode.isClickable()) {
                if (parentNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    Log.d(TAG, "使用父节点点击方式点击成功");
                    return true;
                } else {
                    Log.w(TAG, "使用父节点点击方式点击失败");
                }
                break;
            }
            parentNode = parentNode.getParent();
        }
        
        Log.e(TAG, "所有点击方式都失败");
        return false;
    }
        
    /**
     * 使用Gesture方式点击节点
     */
    private boolean performGestureClick(AccessibilityNodeInfo node) {
        try {
            // 使用反射调用Gesture API，避免编译时依赖
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            int centerX = bounds.centerX();
            int centerY = bounds.centerY();
                
            if (centerX <= 0 || centerY <= 0) {
                return false;
            }
                
            // 获取GestureDescription类
            Class<?> gestureDescriptionClass = Class.forName("android.view.accessibility.GestureDescription");
            Class<?> strokeDescriptionClass = Class.forName("android.view.accessibility.GestureDescription$StrokeDescription");
            Class<?> pathClass = Class.forName("android.graphics.Path");
            Class<?> builderClass = Class.forName("android.view.accessibility.GestureDescription$Builder");
                
            // 创建Path对象
            Object path = pathClass.newInstance();
            Method moveToMethod = pathClass.getMethod("moveTo", float.class, float.class);
            moveToMethod.invoke(path, (float) centerX, (float) centerY);
                
            // 创建StrokeDescription对象
            Object strokeDescription = strokeDescriptionClass.getConstructor(pathClass, long.class, long.class)
                .newInstance(path, 0L, 100L);
                
            // 创建Builder对象
            Object builder = builderClass.newInstance();
            Method addStrokeMethod = builderClass.getMethod("addStroke", strokeDescriptionClass);
            addStrokeMethod.invoke(builder, strokeDescription);
                
            // 构建GestureDescription对象
            Method buildMethod = builderClass.getMethod("build");
            Object gesture = buildMethod.invoke(builder);
                
            // 调用dispatchGesture方法
            Method dispatchGestureMethod = AccessibilityService.class.getMethod(
                "dispatchGesture", gestureDescriptionClass, 
                Class.forName("android.accessibilityservice.AccessibilityService$GestureResultCallback"), 
                android.os.Handler.class);
                
            Object result = dispatchGestureMethod.invoke(this, gesture, null, null);
            return result != null ? (Boolean) result : false;
        } catch (Exception e) {
            Log.e(TAG, "使用Gesture方式点击失败: " + e.getMessage());
            return false;
        }
    }
        
    /**
     * 获取节点的边界矩形
     */
    public Rect getNodeBounds(AccessibilityNodeInfo node) {
        if (node == null) return null;
        
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        return bounds;
    }

    /**
     * 获取节点详细信息
     */
    public String getNodeInfo(AccessibilityNodeInfo node) {
        if (node == null) return "节点为空";
        
        StringBuilder info = new StringBuilder();
        info.append("类名: ").append(node.getClassName()).append("\n");
        info.append("文本: ").append(node.getText()).append("\n");
        info.append("内容描述: ").append(node.getContentDescription()).append("\n");
        info.append("是否可点击: ").append(node.isClickable()).append("\n");
        info.append("是否可聚焦: ").append(node.isFocusable()).append("\n");
        info.append("是否可见: ").append(node.isVisibleToUser()).append("\n");
        info.append("是否已选中: ").append(node.isSelected()).append("\n");
        info.append("是否已勾选: ").append(node.isChecked()).append("\n");
        
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        info.append("屏幕坐标: ").append(bounds.toShortString()).append("\n");
        
        return info.toString();
    }
    
    /**
     * 执行全局菜单键操作
     * @return 是否执行成功
     */
    public boolean performMenuAction() {
        return performGlobalAction(GLOBAL_ACTION_RECENTS);
    }
    
    /**
     * 执行全局Home键操作
     * @return 是否执行成功
     */
    public boolean performHomeAction() {
        return performGlobalAction(GLOBAL_ACTION_HOME);
    }
    
    /**
     * 执行全局返回键操作
     * @return 是否执行成功
     */
    public boolean performBackAction() {
        return performGlobalAction(GLOBAL_ACTION_BACK);
    }
    
    /**
     * 执行全局电源键操作
     * @return 是否执行成功
     */
    public boolean performPowerAction() {
        // 注意：AccessibilityService不直接支持电源键操作
        // 这里提供一个替代方案，可以通过发送KeyEvent来实现
        return false; // 暂时不实现
    }
    
    /**
     * 启动指定包名的应用程序
     * @param packageName 应用程序包名
     * @return 是否启动成功
     */
    public boolean launchApp(String packageName) {
        return launchApp(packageName, 2000); // 默认等待2秒
    }
    
    /**
     * 启动指定包名的应用程序
     * @param packageName 应用程序包名
     * @param waitTimeMs 启动后等待时间（毫秒）
     * @return 是否启动成功
     */
    public boolean launchApp(String packageName, long waitTimeMs) {
        if (packageName == null || packageName.isEmpty()) {
            Log.e(TAG, "包名不能为空");
            return false;
        }

        // 增强版启动逻辑，更稳定可靠
        return launchAppWithRetry(packageName, waitTimeMs, 1);
    }

    /**
     * 带重试机制的应用启动方法（增强版）
     * @param packageName 应用包名
     * @param waitTimeMs 等待时间
     * @param retryCount 重试次数（1表示只尝试一次）
     * @return 是否启动成功
     */
    private boolean launchAppWithRetry(String packageName, long waitTimeMs, int retryCount) {
        int maxRetries = retryCount;
        int currentRetry = 0;

        while (currentRetry < maxRetries) {
            try {
                // 第一步：检查应用是否已安装
                PackageManager pm = getPackageManager();
                try {
                    pm.getPackageInfo(packageName, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "应用未安装: " + packageName);
                    return false;
                }

                Log.d(TAG, "[尝试 " + (currentRetry + 1) + "/" + maxRetries + "] 启动应用: " + packageName);

                // 第二步：初始延迟，确保系统处于稳定状态（特别是在pressHome后）
                if (currentRetry == 0) {
                    Thread.sleep(500); // 初始延迟500ms
                }

                // 第三步：获取启动Intent并启动应用
                boolean launchSuccess = attemptLaunchApp(packageName);
                if (!launchSuccess) {
                    Log.w(TAG, "第 " + (currentRetry + 1) + " 次启动失败，准备重试...");
                    currentRetry++;
                    if (currentRetry < maxRetries) {
                        Thread.sleep(1000); // 重试前等待1秒
                        continue;
                    } else {
                        return false;
                    }
                }

                Log.d(TAG, "应用启动Intent已发送: " + packageName);

                // 第四步：等待应用真正启动并进入前台
                if (waitTimeMs > 0) {
                    boolean launched = waitForAppLaunchWithValidation(packageName, waitTimeMs);
                    if (launched) {
                        Log.d(TAG, "✓ 应用成功启动并进入前台: " + packageName);
                        return true;
                    } else {
                        Log.w(TAG, "✗ 等待应用启动超时或失败: " + packageName);
                        currentRetry++;
                        if (currentRetry < maxRetries) {
                            // 清理并重试
                            Thread.sleep(1000);
                            continue;
                        } else {
                            // 最后一次重试也失败了，但应用可能在后台，返回true
                            return true;
                        }
                    }
                } else {
                    // 不等待启动完成，直接返回（不推荐）
                    Log.d(TAG, "已发送启动Intent，不等待启动完成");
                    Thread.sleep(1000); // 最少等待1秒
                    return true;
                }

            } catch (InterruptedException e) {
                Log.e(TAG, "启动应用被中断: " + packageName);
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                Log.e(TAG, "启动应用异常: " + packageName + ", 错误: " + e.getMessage());
                e.printStackTrace();
                currentRetry++;
                if (currentRetry < maxRetries) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }

        Log.e(TAG, "重试次数已达上限，最终启动失败: " + packageName);
        return false;
    }

    /**
     * 尝试启动应用（单次尝试）
     * @param packageName 应用包名
     * @return 是否成功发送启动Intent
     */
    private boolean attemptLaunchApp(String packageName) {
        try {
            PackageManager pm = getPackageManager();

            // 方法1: 使用标准启动Intent（推荐）
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // 清除栈顶，避免重复
                Log.d(TAG, "使用标准启动Intent启动: " + packageName);
                startActivity(intent);
                return true;
            }

            // 方法2: 使用隐式Intent（备用）
            Log.w(TAG, "标准Intent获取失败，使用隐式Intent");
            Intent launchIntent = new Intent(Intent.ACTION_MAIN);
            launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            launchIntent.setPackage(packageName);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            List<ResolveInfo> activities = pm.queryIntentActivities(launchIntent, 0);
            if (activities != null && !activities.isEmpty()) {
                ResolveInfo resolveInfo = activities.get(0);
                launchIntent.setComponent(new ComponentName(packageName, resolveInfo.activityInfo.name));
                Log.d(TAG, "使用隐式Intent启动: " + packageName + ", Activity: " + resolveInfo.activityInfo.name);
                startActivity(launchIntent);
                return true;
            }

            // 方法3: 最后尝试获取包信息（诊断）
            try {
                pm.getPackageInfo(packageName, 0);
                Log.e(TAG, "应用已安装但无法启动: " + packageName + ", 可能被禁用或系统限制");
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "应用未安装: " + packageName);
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "启动应用异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 改进的应用启动等待验证（支持降级方案）
     * @param packageName 应用包名
     * @param timeoutMs 超时时间
     * @return 是否启动完成
     */
    private boolean waitForAppLaunchWithValidation(String packageName, long timeoutMs) {
        try {
            long startTime = System.currentTimeMillis();
            int checkInterval = 300; // 检查间隔300ms
            int maxAttempts = (int) (timeoutMs / checkInterval);
            int attempts = 0;

            while (attempts < maxAttempts && System.currentTimeMillis() - startTime < timeoutMs) {
                attempts++;

                // 方法1：使用UsageStatsManager（Android 5.0+，推荐）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (checkForegroundAppWithUsageStats(packageName)) {
                        Log.d(TAG, "✓ UsageStats检测: 应用已进入前台");
                        return true;
                    }
                }

                // 方法2：使用AccessibilityService获取窗口包名（可靠）
                if (checkForegroundAppWithAccessibility(packageName)) {
                    Log.d(TAG, "✓ AccessibilityService检测: 应用已进入前台");
                    return true;
                }

                // 方法3：使用ActivityManager（备用，API 21+已弃用）
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    if (checkForegroundAppWithActivityManager(packageName)) {
                        Log.d(TAG, "✓ ActivityManager检测: 应用已进入前台");
                        return true;
                    }
                }

                // 等待后重试
                Thread.sleep(checkInterval);
            }

            // 超时后再次尝试（可能刚好启动）
            Log.d(TAG, "启动等待超时，执行最后一次检查...");
            if (checkForegroundAppWithAccessibility(packageName) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && checkForegroundAppWithUsageStats(packageName))) {
                Log.d(TAG, "✓ 最后检查成功: 应用已启动");
                return true;
            }

            Log.w(TAG, "✗ 启动等待超时: " + packageName + ", 但应用可能在后台");
            return false;
        } catch (InterruptedException e) {
            Log.e(TAG, "等待被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "等待应用启动异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 使用AccessibilityService检查前台应用（最可靠）
     */
    private boolean checkForegroundAppWithAccessibility(String packageName) {
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                String currentPackage = rootNode.getPackageName().toString();
                if (packageName.equals(currentPackage)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "AccessibilityService检查失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 使用UsageStatsManager检查前台应用
     */
    private boolean checkForegroundAppWithUsageStats(String packageName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
                if (usageStatsManager == null) {
                    return false;
                }

                long currentTime = System.currentTimeMillis();
                List<UsageStats> stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    currentTime - 1000 * 15,  // 查询最近15秒的数据
                    currentTime
                );

                if (stats != null && !stats.isEmpty()) {
                    // 找到最后使用的应用
                    UsageStats recentUsage = null;
                    for (UsageStats stat : stats) {
                        if (recentUsage == null || stat.getLastTimeUsed() > recentUsage.getLastTimeUsed()) {
                            recentUsage = stat;
                        }
                    }

                    if (recentUsage != null && packageName.equals(recentUsage.getPackageName())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "UsageStats检查失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 使用ActivityManager检查前台应用（已弃用，仅作备用）
     */
    @SuppressWarnings("deprecation")
    private boolean checkForegroundAppWithActivityManager(String packageName) {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
                if (!taskInfo.isEmpty() && taskInfo.get(0).topActivity != null) {
                    String currentPackage = taskInfo.get(0).topActivity.getPackageName();
                    if (packageName.equals(currentPackage)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "ActivityManager检查失败: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * 等待应用程序启动完成
     * @param packageName 应用程序包名
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否启动完成
     */
    private boolean waitForAppLaunch(String packageName, long timeoutMs) {
        try {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                // 获取当前前台应用的包名
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // 检查是否具有USAGE_STATS权限
                    UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
                    long currentTime = System.currentTimeMillis();
                    List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 1000 * 10, currentTime);
                    if (stats != null) {
                        SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
                        for (UsageStats usageStats : stats) {
                            mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                        }
                        if (!mySortedMap.isEmpty()) {
                            String currentPackage = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                            Log.d(TAG, "当前前台应用: " + currentPackage + ", 目标应用: " + packageName);
                            if (packageName.equals(currentPackage)) {
                                Log.d(TAG, "应用程序启动完成: " + packageName);
                                return true;
                            }
                        } else {
                            Log.d(TAG, "未获取到应用使用统计信息");
                        }
                    } else {
                        Log.d(TAG, "无法获取应用使用统计信息，可能缺少USAGE_STATS权限");
                    }
                } else {
                    // 对于较低版本的Android，使用ActivityManager
                    ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                    List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
                    if (!taskInfo.isEmpty()) {
                        ComponentName componentInfo = taskInfo.get(0).topActivity;
                        if (componentInfo != null) {
                            String currentPackage = componentInfo.getPackageName();
                            Log.d(TAG, "当前运行任务应用: " + currentPackage + ", 目标应用: " + packageName);
                            if (packageName.equals(currentPackage)) {
                                Log.d(TAG, "应用程序启动完成: " + packageName);
                                return true;
                            }
                        }
                    }
                }
                
                // 等待一小段时间后重试
                Thread.sleep(500);
            }
            
            Log.w(TAG, "等待应用程序启动超时: " + packageName);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "等待应用程序启动时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 清理后台应用程序（不清理自身）
     * @return 是否清理成功
     */
    public boolean clearRecentApps() {
        try {
            // 先执行Home键操作，回到主屏幕
            boolean homeSuccess = performGlobalAction(GLOBAL_ACTION_HOME);
            if (homeSuccess) {
                Log.d(TAG, "已回到主屏幕");
                // 等待一段时间确保回到主屏幕
                Thread.sleep(500);
                
                // 执行全局动作：显示最近任务
                boolean recentSuccess = performGlobalAction(GLOBAL_ACTION_RECENTS);
                if (recentSuccess) {
                    Log.d(TAG, "已打开最近任务列表");
                    // 等待一段时间让最近任务列表显示
                    Thread.sleep(500);
                    
                    // 执行全局动作：按下返回键，关闭最近任务列表
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    Log.d(TAG, "已清理后台应用程序");
                    return true;
                } else {
                    Log.e(TAG, "无法执行显示最近任务操作");
                    return false;
                }
            } else {
                Log.e(TAG, "无法执行Home键操作");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "清理后台应用程序失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 查找文本（支持单个文本）
     * @param text 要查找的文本
     * @param exactMatch 是否精确匹配
     * @return 是否找到文本
     */
    public boolean findText(String text, boolean exactMatch) {
        return findText(new String[]{text}, exactMatch, 5000); // 默认超时5秒
    }
    
    /**
     * 查找文本（支持单个文本）
     * @param text 要查找的文本
     * @param exactMatch 是否精确匹配
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否找到文本
     */
    public boolean findText(String text, boolean exactMatch, long timeoutMs) {
        return findText(new String[]{text}, exactMatch, timeoutMs);
    }
    
    /**
     * 查找文本（支持多个文本）
     * @param texts 要查找的文本数组
     * @param exactMatch 是否精确匹配
     * @return 是否找到所有文本
     */
    public boolean findText(String[] texts, boolean exactMatch) {
        return findText(texts, exactMatch, 5000); // 默认超时5秒
    }
    
    /**
     * 查找文本（支持多个文本）
     * @param texts 要查找的文本数组
     * @param exactMatch 是否精确匹配
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否找到所有文本
     */
    public boolean findText(String[] texts, boolean exactMatch, long timeoutMs) {
        try {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root == null) {
                    Log.e(TAG, "无法获取根节点");
                    Thread.sleep(100); // 等待100毫秒后重试
                    continue;
                }
                
                if (findTextInNode(root, texts, exactMatch)) {
                    return true;
                }
                
                Thread.sleep(100); // 等待100毫秒后重试
            }
            
            Log.w(TAG, "查找文本超时: " + timeoutMs + "ms");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "查找文本失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 在节点中递归查找文本
     * @param node 节点
     * @param texts 要查找的文本数组
     * @param exactMatch 是否精确匹配
     * @return 是否找到所有文本
     */
    private boolean findTextInNode(AccessibilityNodeInfo node, String[] texts, boolean exactMatch) {
        if (node == null) {
            return false;
        }
        
        // 创建一个布尔数组来跟踪每个文本是否已找到
        boolean[] foundFlags = new boolean[texts.length];
        
        // 检查当前节点的文本
        CharSequence nodeText = node.getText();
        if (nodeText != null) {
            String textStr = nodeText.toString();
            checkTextMatches(textStr, texts, exactMatch, foundFlags);
        }
        
        // 检查当前节点的内容描述
        CharSequence contentDescription = node.getContentDescription();
        if (contentDescription != null) {
            String descStr = contentDescription.toString();
            checkTextMatches(descStr, texts, exactMatch, foundFlags);
        }
        
        // 检查是否所有文本都已找到
        boolean allFound = true;
        for (boolean found : foundFlags) {
            if (!found) {
                allFound = false;
                break;
            }
        }
        
        if (allFound) {
            return true;
        }
        
        // 递归检查子节点
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null && findTextInNode(child, texts, exactMatch)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查文本是否匹配
     * @param sourceText 源文本
     * @param targetTexts 目标文本数组
     * @param exactMatch 是否精确匹配
     * @param foundFlags 匹配标志数组
     */
    private void checkTextMatches(String sourceText, String[] targetTexts, boolean exactMatch, boolean[] foundFlags) {
        for (int i = 0; i < targetTexts.length; i++) {
            if (!foundFlags[i]) { // 如果还没有找到，则检查
                if (exactMatch) {
                    if (sourceText.equals(targetTexts[i])) {
                        foundFlags[i] = true;
                    }
                } else {
                    if (sourceText.contains(targetTexts[i])) {
                        foundFlags[i] = true;
                    }
                }
            }
        }
    }
    
    /**
     * 显示通知
     * @param title 通知标题
     * @param content 通知内容
     */
    public void showNotification(String title, String content) {
        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            
            // 创建通知渠道（Android 8.0及以上）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    "autotask_channel",
                    "AutoTask Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("AutoTask service notifications");
                notificationManager.createNotificationChannel(channel);
            }
            
            // 创建通知
            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, "autotask_channel");
            } else {
                builder = new Notification.Builder(this);
            }
            
            builder.setContentTitle(title)
                   .setContentText(content)
                   .setSmallIcon(android.R.drawable.ic_dialog_info)
                   .setAutoCancel(true);
            
            // 添加点击意图
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            builder.setContentIntent(pendingIntent);
            
            // 显示通知
            notificationManager.notify(1, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "显示通知失败: " + e.getMessage());
            // 如果通知失败，回退到日志记录
            Log.d(TAG, "通知内容: " + title + " - " + content);
        }
    }
}