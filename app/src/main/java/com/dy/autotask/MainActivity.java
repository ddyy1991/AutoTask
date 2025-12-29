package com.dy.autotask;

import android.content.Intent;
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

import android.widget.CheckBox;

import com.dy.autotask.task.AutomationTask;
import com.dy.autotask.task.AutomationTaskManager;
import com.dy.autotask.utils.AutoTaskHelper;
import com.dy.autotask.utils.SettingsManager;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_FLOAT_WINDOW_PERMISSION = 1001;
    private AutoTaskHelper autoTaskHelper;
    private TextView statusText;
    private View taskTest;
    private CheckBox cbLogWindow;
    private SettingsManager settingsManager;
    private String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化辅助类
        autoTaskHelper = AutoTaskHelper.getInstance();
        autoTaskHelper.initialize();
        
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
            Toast.makeText(this, "请启用无障碍服务", Toast.LENGTH_LONG).show();
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
    
    private void updateStatus() {
        boolean isServiceEnabled = autoTaskHelper.isAccessibilityServiceEnabled(this);
        boolean hasFloatPermission = checkFloatWindowPermission();
        
        StringBuilder status = new StringBuilder();
        status.append("服务状态:\n");
        status.append("无障碍服务: ").append(isServiceEnabled ? "已启用" : "未启用").append("\n");
        status.append("悬浮窗权限: ").append(hasFloatPermission ? "已授权" : "未授权").append("\n");
        
        statusText.setText(status.toString());
    }
    
    private void requestFloatWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_FLOAT_WINDOW_PERMISSION);
            } else {
                Toast.makeText(this, "已拥有悬浮窗权限", Toast.LENGTH_SHORT).show();
            }
        }
        updateStatus();
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
        }
    }
}