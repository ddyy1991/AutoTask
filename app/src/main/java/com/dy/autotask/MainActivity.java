package com.dy.autotask;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.widget.CheckBox;
import android.content.pm.PackageManager;
import android.Manifest;

import com.dy.autotask.task.AutomationTask;
import com.dy.autotask.task.AutomationTaskManager;
import com.dy.autotask.utils.AutoTaskHelper;
import com.dy.autotask.utils.ScreenshotUtil;
import com.dy.autotask.utils.SettingsManager;
import com.dy.autotask.utils.RootUtil;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_FLOAT_WINDOW_PERMISSION = 1001;
    private static final int REQUEST_MEDIA_PROJECTION = 1002;  // 新增：截图权限请求码
    private static final int REQUEST_FILE_PERMISSIONS = 1003;  // 新增：文件权限请求码
    private AutoTaskHelper autoTaskHelper;
    private TextView statusText;
    private View taskTest;
    private CheckBox cbLogWindow;
    private SettingsManager settingsManager;
    private String TAG = "MainActivity";
    private MediaProjectionManager mediaProjectionManager;  // 新增：MediaProjection管理器
    private boolean hasRootPermission = false;  // 新增：root权限标志

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化辅助类
        autoTaskHelper = AutoTaskHelper.getInstance();
        autoTaskHelper.initialize();
        
        // 检测root权限
        checkAndRequestRootPermission();
        
        // 初始化UI组件
        initUI();
        
        // 检查权限和服务状态
        checkPermissions();
    }
    
    private void initUI() {
        statusText = findViewById(R.id.status_text);
        taskTest = findViewById(R.id.task_test);
        cbLogWindow = findViewById(R.id.cb_log_window);
        Button enableServiceBtn = findViewById(R.id.enable_service_btn);
        Button floatPermissionBtn = findViewById(R.id.float_permission_btn);
        Button screenshotPermissionBtn = findViewById(R.id.screenshot_permission_btn);  // 新增
        Button imageAnalysisBtn = findViewById(R.id.image_analysis_btn);  // 新增：图片分析按钮

        // 初始化MediaProjectionManager
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        // 初始化设置管理器
        settingsManager = SettingsManager.getInstance(this);

        // 设置复选框初始状态
        cbLogWindow.setChecked(settingsManager.isLogWindowEnabled());

        // 设置复选框状态变化监听器
        cbLogWindow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsManager.setLogWindowEnabled(isChecked);
            Toast.makeText(this, isChecked ? "任务日志悬浮窗已启用" : "任务日志悬浮窗已禁用", Toast.LENGTH_SHORT).show();
        });

        enableServiceBtn.setOnClickListener(v -> {
            boolean isServiceEnabled = autoTaskHelper.isAccessibilityServiceEnabled(this);
            if(!isServiceEnabled){
                autoTaskHelper.openAccessibilitySettings(this);
            }else{
                Toast.makeText(this, "服务已开启", Toast.LENGTH_SHORT).show();
            }
        });

        taskTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createOpenAppTask();
            }
        });

        floatPermissionBtn.setOnClickListener(v -> {
            requestFloatWindowPermission();
        });

        // 新增：截图权限按钮点击监听
        screenshotPermissionBtn.setOnClickListener(v -> {
            requestScreenshotPermission();
        });

        // 新增：图片分析按钮点击监听
        imageAnalysisBtn.setOnClickListener(v -> {
            startImageAnalysis();
        });

        updateStatus();
    }
    
    private void createOpenAppTask() {
        boolean isServiceEnabled = autoTaskHelper.isAccessibilityServiceEnabled(this);
        if(!isServiceEnabled){
            autoTaskHelper.openAccessibilitySettings(this);
            return;
        }
        AutomationTaskManager taskManager=AutomationTaskManager.getInstance();
        taskManager.setContext(this);
        AutomationTask task = new AutomationTask("打开webApp自动测试")
 
        .pressHome()
                .launchApp("com.dy.webparseutil",6000)
                .waitFor(1000)
                .click("com.dy.webparseutil:id/btn_async_fetch", AutomationTask.ElementType.ID, 2000)  // 通过文本查找并点击“购物车”，超时2秒
                .waitFor(1000)
                .inputText("com.dy.webparseutil:id/et_url","https://youzisp.tv/vodtype/zongyi.html", AutomationTask.ElementType.ID, 1500)  // 通过文本查找“结算”按钮，超时3秒
                .waitFor(500)                                            // 等待500毫秒
                .click("com.dy.webparseutil:id/btn_webview_demo", AutomationTask.ElementType.ID, 2000)        // 点击“结算”按钮，超时2秒
                .waitFor(1500)                                           // 等待1秒
                .inputText("com.dy.webparseutil:id/et_search","<link", AutomationTask.ElementType.ID, 3000)  // 通过描述查找地址输入框，超时3秒
                .click("com.dy.webparseutil:id/btn_search")  // 通过文本查找地址输入框并输入地址，超时2.5秒
                .findText("doctype html",false,5000)
                .waitFor(1500)
                .click("com.dy.webparseutil:id/btn_next")  // 通过文本查找地址输入框并输入地址，超时2.5秒
                .waitFor(1500)
                .click("com.dy.webparseutil:id/btn_next")  // 通过文本查找地址输入框并输入地址，超时2.5秒
                .setTimeout(30000)                                       // 设置超时时间为60秒
                .onResult((task1, status, message, stepIndex) -> {                  // 设置结果回调
                    switch (status) {
                        case SUCCESS:
                            Log.d(TAG, "搜索最新字符串执行完成！当前步骤: " + stepIndex);
                            break;
                        case FAILED:
                            Log.e(TAG, "搜索最新字符串执行失败: " + message + "，当前步骤: " + stepIndex);
                            break;
                        case TIMEOUT:
                            Log.w(TAG, "搜索最新字符串执行超时: " + message + "，当前步骤: " + stepIndex);
                            break;
                        case CANCELLED:
                            Log.d(TAG, "搜索最新字符串任务取消: " + message + "，当前步骤: " + stepIndex);
                            break;
                    }
                });

        // 设置无障碍服务引用
        task.setAccessibilityService(autoTaskHelper.getService());

        // 添加任务到队列
        taskManager.addTask(task);

        // 执行任务队列
        taskManager.executeTasks();
    }

    private void checkPermissions() {
        // 检查无障碍服务是否启用
        boolean isServiceEnabled = autoTaskHelper.isAccessibilityServiceEnabled(this);
        if (!isServiceEnabled) {
            if (hasRootPermission) {
                Toast.makeText(this, "检测到Root权限，正在自动申请无障碍权限...", Toast.LENGTH_LONG).show();
                autoTaskHelper.openAccessibilitySettings(this);
            } else {
                Toast.makeText(this, "请启用无障碍服务", Toast.LENGTH_LONG).show();
            }
        } else {
            // 尝试初始化悬浮窗
            try {
                autoTaskHelper.initOrUpdateFloatingView();
            } catch (Exception e) {
                Log.e("MainActivity", "初始化悬浮窗失败: " + e.getMessage());
            }
        }
        
        updateStatus();
    }
    
    /**
     * 新增：检测并申请root权限
     */
    private void checkAndRequestRootPermission() {
        Log.d(TAG, "开始检测root权限...");
        hasRootPermission = RootUtil.hasRootPermission();
        Log.d(TAG, "root权限检测结果: " + hasRootPermission);
        
        if (hasRootPermission) {
            Toast.makeText(this, "检测到Root权限，正在自动申请相关权限...", Toast.LENGTH_LONG).show();
            // 自动申请文件读写权限
            requestFilePermissionsWithRoot();
            // 自动申请无障碍权限
            requestAccessibilityPermissionWithRoot();
            // 自动申请悬浮窗权限
            requestFloatWindowPermissionWithRoot();
        }
    }
    
    /**
     * 新增：使用root权限申请文件读写权限
     */
    private void requestFilePermissionsWithRoot() {
        Log.d(TAG, "使用root权限申请文件读写权限");
        String packageName = getPackageName();
        
        // 申请读权限
        RootUtil.grantPermissionWithRoot(packageName, Manifest.permission.READ_EXTERNAL_STORAGE);
        // 申请写权限
        RootUtil.grantPermissionWithRoot(packageName, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        
        Log.d(TAG, "文件读写权限申请完成");
    }
    
    /**
     * 新增：使用root权限申请无障碍权限
     */
    private void requestAccessibilityPermissionWithRoot() {
        Log.d(TAG, "使用root权限申请无障碍权限");
        // 无障碍权限需要用户手动在设置中启用，root权限无法直接授予
        // 这里只是打开设置页面供用户选择
        autoTaskHelper.openAccessibilitySettings(this);
    }
    
    /**
     * 新增：使用root权限申请悬浮窗权限
     */
    private void requestFloatWindowPermissionWithRoot() {
        Log.d(TAG, "使用root权限申请悬浮窗权限");
        String packageName = getPackageName();
        RootUtil.grantPermissionWithRoot(packageName, Manifest.permission.SYSTEM_ALERT_WINDOW);
        Log.d(TAG, "悬浮窗权限申请完成");
    }
    
    private void updateStatus() {
        boolean isServiceEnabled = autoTaskHelper.isAccessibilityServiceEnabled(this);
        boolean hasFloatPermission = checkFloatWindowPermission();
        boolean hasMediaProjection = ScreenshotUtil.getMediaProjection() != null;
        boolean hasReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean hasWritePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        StringBuilder status = new StringBuilder();
        status.append("服务状态:\n");
        status.append("Root权限: ").append(hasRootPermission ? "已获取" : "未获取").append("\n");
        status.append("无障碍服务: ").append(isServiceEnabled ? "已启用" : "未启用").append("\n");
        status.append("悬浮窗权限: ").append(hasFloatPermission ? "已授权" : "未授权").append("\n");
        status.append("文件读权限: ").append(hasReadPermission ? "已授权" : "未授权").append("\n");
        status.append("文件写权限: ").append(hasWritePermission ? "已授权" : "未授权").append("\n");
        status.append("截图权限: ").append(hasMediaProjection ? "已获取" : "未获取").append("\n");

        statusText.setText(status.toString());
    }
    
    private void requestFloatWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                if (hasRootPermission) {
                    Toast.makeText(this, "检测到Root权限，正在自动申请悬浮窗权限...", Toast.LENGTH_SHORT).show();
                    // 使用root权限自动授予悬浮窗权限
                    RootUtil.grantPermissionWithRoot(getPackageName(), "android.permission.SYSTEM_ALERT_WINDOW");
                } else {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_FLOAT_WINDOW_PERMISSION);
                }
            } else {
                Toast.makeText(this, "已拥有悬浮窗权限", Toast.LENGTH_SHORT).show();
            }
        }
        updateStatus();
    }

    /**
     * 新增：请求截图权限（MediaProjection）
     */
    private void requestScreenshotPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (ScreenshotUtil.getMediaProjection() != null) {
                Toast.makeText(this, "截图权限已获取", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "开始请求截图权限");

            // 创建MediaProjection的Intent
            Intent intent = mediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(intent, REQUEST_MEDIA_PROJECTION);
        } else {
            Toast.makeText(this, "设备不支持该功能（需要Android 5.0+）", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 新增：启动图片分析 Activity
     */
    private void startImageAnalysis() {
        Log.d(TAG, "启动图片分析 Activity");
        Intent intent = new Intent(this, ImageAnalysisActivity.class);
        startActivity(intent);
    }

    private boolean checkFloatWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true; // 低版本默认有权限
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FLOAT_WINDOW_PERMISSION) {
            updateStatus();
        } else if (requestCode == REQUEST_MEDIA_PROJECTION) {  // 新增：处理截图权限
            if (resultCode == RESULT_OK && data != null) {
                Log.d(TAG, "用户授予了截图权限");

                // 获取屏幕尺寸信息
                android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenWidth = displayMetrics.widthPixels;
                int screenHeight = displayMetrics.heightPixels;
                int screenDensity = displayMetrics.densityDpi;

                Log.d(TAG, "屏幕尺寸: " + screenWidth + "x" + screenHeight + ", 密度: " + screenDensity);

                // 创建MediaProjection实例
                android.media.projection.MediaProjection mediaProjection =
                        mediaProjectionManager.getMediaProjection(resultCode, data);

                if (mediaProjection != null) {
                    // 将MediaProjection设置到ScreenshotUtil
                    ScreenshotUtil.setMediaProjection(mediaProjection, screenWidth, screenHeight, screenDensity);
                    Log.d(TAG, "MediaProjection已初始化并保存");
                    Toast.makeText(this, "截图权限已获取，可以使用高效截图功能", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "创建MediaProjection失败");
                    Toast.makeText(this, "截图权限获取失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "用户拒绝了截图权限");
                Toast.makeText(this, "截图权限被拒绝，将使用无障碍服务截图", Toast.LENGTH_SHORT).show();
            }
            updateStatus();
        }
    }
}