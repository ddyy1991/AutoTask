package com.dy.autotask.utils;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.accessibility.AccessibilityNodeInfo;

import com.dy.autotask.AccessibilityServiceUtil;

import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * 自动化任务辅助类
 * 提供全局访问无障碍服务功能的接口
 */
public class AutoTaskHelper {
    private static AutoTaskHelper instance;
    private AccessibilityServiceUtil accessibilityService;

    private AutoTaskHelper() {}

    public static synchronized AutoTaskHelper getInstance() {
        if (instance == null) {
            instance = new AutoTaskHelper();
        }
        return instance;
    }

    /**
     * 初始化辅助类（应在应用启动时调用）
     */
    public void initialize() {
        // 实际上，AccessibilityServiceUtil会在服务连接时自动设置实例
        // 这里只是确保单例模式
    }

    /**
     * 检查无障碍服务是否已启用
     */
    public boolean isAccessibilityServiceEnabled(Context context) {
        accessibilityService = AccessibilityServiceUtil.getInstance();
        return accessibilityService != null;
    }

    /**
     * 打开无障碍服务设置页面
     */
    public void openAccessibilitySettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 获取无障碍服务实例
     */
    private AccessibilityServiceUtil getService() throws IllegalStateException {
        if (accessibilityService == null) {
            accessibilityService = AccessibilityServiceUtil.getInstance();
        }
        
        if (accessibilityService == null) {
            throw new IllegalStateException("无障碍服务未启用，请先启用服务");
        }
        
        return accessibilityService;
    }

    /**
     * 根据文本查找节点（带超时）
     */
    public AccessibilityNodeInfo findNodeByText(String text, long timeoutMs) 
            throws TimeoutException, InterruptedException, IllegalStateException {
        return getService().findNodeByText(text, timeoutMs);
    }

    /**
     * 根据ID查找节点（带超时）
     */
    public AccessibilityNodeInfo findNodeById(String viewId, long timeoutMs) 
            throws TimeoutException, InterruptedException, IllegalStateException {
        return getService().findNodeById(viewId, timeoutMs);
    }

    /**
     * 根据类名查找节点（带超时）
     */
    public List<AccessibilityNodeInfo> findNodesByClass(String className, long timeoutMs) 
            throws TimeoutException, InterruptedException, IllegalStateException {
        return getService().findNodesByClass(className, timeoutMs);
    }

    /**
     * 点击节点
     */
    public boolean clickNode(AccessibilityNodeInfo node) throws IllegalStateException {
        return getService().clickNode(node);
    }

    /**
     * 获取节点边界
     */
    public android.graphics.Rect getNodeBounds(AccessibilityNodeInfo node) throws IllegalStateException {
        return getService().getNodeBounds(node);
    }

    /**
     * 获取节点详细信息
     */
    public String getNodeInfo(AccessibilityNodeInfo node) throws IllegalStateException {
        return getService().getNodeInfo(node);
    }
    
    /**
     * 初始化或更新悬浮窗
     */
    public void initOrUpdateFloatingView() throws IllegalStateException {
        AccessibilityServiceUtil service = getService();
        service.initOrUpdateFloatingView();
    }
}