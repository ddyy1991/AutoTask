package com.dy.autotask.utils;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;
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
        if (accessibilityService == null) {
            return false;
        }
        
        // 进一步检查服务是否真的在运行
        try {
            // 尝试获取根节点，如果能成功说明服务正在运行
            AccessibilityNodeInfo root = accessibilityService.getRootInActiveWindow();
            return root != null;
        } catch (Exception e) {
            // 如果出现异常，说明服务可能没有正确启用
            return false;
        }
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
    public AccessibilityServiceUtil getService() throws IllegalStateException {
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

    // ===================== 截图功能 =====================

    /**
     * 全屏截图（推荐方法）
     * 无需root权限，会自动选择最佳的截图方式
     *
     * @param context 应用上下文
     * @param activity 需要截图的Activity
     * @return 截图保存的文件路径，失败返回null
     */
    public String captureScreen(Context context, Activity activity) {
        ScreenshotUtil screenshotUtil = new ScreenshotUtil(context);
        return screenshotUtil.captureScreen(activity, null);
    }

    /**
     * 全屏截图（指定文件名）
     *
     * @param context 应用上下文
     * @param activity 需要截图的Activity
     * @param filename 保存文件名
     * @return 截图保存的文件路径，失败返回null
     */
    public String captureScreen(Context context, Activity activity, String filename) {
        ScreenshotUtil screenshotUtil = new ScreenshotUtil(context);
        return screenshotUtil.captureScreen(activity, filename);
    }

    /**
     * 截图指定视图
     *
     * @param context 应用上下文
     * @param view 需要截图的视图
     * @return 截图保存的文件路径，失败返回null
     */
    public String captureView(Context context, View view) {
        ScreenshotUtil screenshotUtil = new ScreenshotUtil(context);
        return screenshotUtil.captureView(view, null);
    }

    /**
     * 截图指定视图（指定文件名）
     *
     * @param context 应用上下文
     * @param view 需要截图的视图
     * @param filename 保存文件名
     * @return 截图保存的文件路径，失败返回null
     */
    public String captureView(Context context, View view, String filename) {
        ScreenshotUtil screenshotUtil = new ScreenshotUtil(context);
        return screenshotUtil.captureView(view, filename);
    }

    /**
     * 使用PixelCopy API截图（API 24+）
     *
     * @param context 应用上下文
     * @param activity 需要截图的Activity
     * @return 截图保存的文件路径，失败返回null
     */
    public String captureWithPixelCopy(Context context, Activity activity) {
        ScreenshotUtil screenshotUtil = new ScreenshotUtil(context);
        return screenshotUtil.captureWithPixelCopy(activity, null);
    }

    /**
     * 获取截图保存目录
     *
     * @param context 应用上下文
     * @return 截图目录路径
     */
    public String getScreenshotFolderPath(Context context) {
        ScreenshotUtil screenshotUtil = new ScreenshotUtil(context);
        return screenshotUtil.getScreenshotFolderPath();
    }

    /**
     * 清空所有截图
     *
     * @param context 应用上下文
     * @return 删除的文件数量
     */
    public int clearScreenshots(Context context) {
        ScreenshotUtil screenshotUtil = new ScreenshotUtil(context);
        return screenshotUtil.clearScreenshots();
    }

    /**
     * 打印截图文件夹信息
     *
     * @param context 应用上下文
     */
    public void printScreenshotFolderInfo(Context context) {
        ScreenshotUtil screenshotUtil = new ScreenshotUtil(context);
        screenshotUtil.printScreenshotFolderInfo();
    }

    // ===================== 后台截图功能 =====================

    /**
     * 使用无障碍服务进行全屏截图
     * 支持应用后台运行时的截图
     *
     * @param context 应用上下文
     * @return 截图保存的文件路径，失败返回null
     */
    public String captureScreenWithAccessibility(Context context) {
        ScreenshotUtil screenshotUtil = new ScreenshotUtil(context);
        return screenshotUtil.captureScreenWithAccessibility(null);
    }

    /**
     * 使用无障碍服务进行全屏截图（指定文件名）
     *
     * @param context 应用上下文
     * @param filename 保存文件名
     * @return 截图保存的文件路径，失败返回null
     */
    public String captureScreenWithAccessibility(Context context, String filename) {
        ScreenshotUtil screenshotUtil = new ScreenshotUtil(context);
        return screenshotUtil.captureScreenWithAccessibility(filename);
    }

    /**
     * 使用MediaProjection进行后台截图
     * 需要先通过setMediaProjection()初始化
     *
     * @param context 应用上下文
     * @return 截图保存的文件路径，失败返回null
     */
    public String captureScreenWithMediaProjection(Context context) {
        ScreenshotUtil screenshotUtil = new ScreenshotUtil(context);
        return screenshotUtil.captureWithMediaProjection(null);
    }

    /**
     * 使用MediaProjection进行后台截图（指定文件名）
     *
     * @param context 应用上下文
     * @param filename 保存文件名
     * @return 截图保存的文件路径，失败返回null
     */
    public String captureScreenWithMediaProjection(Context context, String filename) {
        ScreenshotUtil screenshotUtil = new ScreenshotUtil(context);
        return screenshotUtil.captureWithMediaProjection(filename);
    }
}