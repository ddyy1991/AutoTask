package com.dy.autotask.utils;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * AutoJs工具类，封装布局分析功能
 */
public class AutoJsTool {
    private static final String TAG = "AutoJs6Tool";
    private static AutoJsTool instance;
    private AccessibilityService accessibilityService;

    private AutoJsTool() {
    }

    public static synchronized AutoJsTool getInstance() {
        if (instance == null) {
            instance = new AutoJsTool();
        }
        return instance;
    }

    /**
     * 初始化AutoJs6工具
     * @param service 无障碍服务实例
     */
    public void init(AccessibilityService service) {
        this.accessibilityService = service;
        Log.d(TAG, "AutoJs6工具初始化成功");
    }

    /**
     * 获取根节点信息
     * @return 根节点
     */
    public AccessibilityNodeInfo getRootNode() {
        try {
            if (accessibilityService == null) {
                Log.e(TAG, "无障碍服务未初始化");
                return null;
            }
            return accessibilityService.getRootInActiveWindow();
        } catch (Exception e) {
            Log.e(TAG, "获取根节点失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "成功释放AutoJs6工具资源");
    }
}