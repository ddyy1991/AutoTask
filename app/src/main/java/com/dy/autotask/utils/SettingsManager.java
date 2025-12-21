package com.dy.autotask.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 应用设置管理类
 * 用于管理用户的偏好设置
 */
public class SettingsManager {
    private static final String PREF_NAME = "auto_task_settings";
    private static final String KEY_LOG_WINDOW_ENABLED = "log_window_enabled";
    
    private static SettingsManager instance;
    private SharedPreferences sharedPreferences;
    
    private SettingsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context);
        }
        return instance;
    }
    
    /**
     * 设置是否启用任务日志悬浮窗
     * @param enabled 是否启用
     */
    public void setLogWindowEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_LOG_WINDOW_ENABLED, enabled).apply();
    }
    
    /**
     * 获取是否启用任务日志悬浮窗
     * @return 是否启用
     */
    public boolean isLogWindowEnabled() {
        return sharedPreferences.getBoolean(KEY_LOG_WINDOW_ENABLED, true); // 默认启用
    }
}