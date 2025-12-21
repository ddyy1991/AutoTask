package com.dy.autotask;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dy.autotask.utils.AutoTaskHelper;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_FLOAT_WINDOW_PERMISSION = 1001;
    private AutoTaskHelper autoTaskHelper;
    private TextView statusText;
    
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
        Button enableServiceBtn = findViewById(R.id.enable_service_btn);
        Button floatPermissionBtn = findViewById(R.id.float_permission_btn);
        
        enableServiceBtn.setOnClickListener(v -> {
            autoTaskHelper.openAccessibilitySettings(this);
        });
        
        floatPermissionBtn.setOnClickListener(v -> {
            requestFloatWindowPermission();
        });
        
        updateStatus();
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