package com.dy.autotask.test_example;

import android.util.Log;
import com.dy.autotask.AccessibilityServiceUtil;
import com.dy.autotask.task.AutomationTask;
import com.dy.autotask.task.AutomationTaskManager;

/**
 * 应用启动功能测试类
 */
public class AppLaunchTest {
    private static final String TAG = "AppLaunchTest";

    /**
     * 测试启动应用功能
     * @param accessibilityService 无障碍服务实例
     */
    public static void testLaunchApp(AccessibilityServiceUtil accessibilityService) {
        // 测试不同的应用启动方式
        
        // 1. 测试标准启动方式
        Log.d(TAG, "=== 测试1: 标准启动方式 ===");
        boolean result1 = accessibilityService.launchApp("com.android.chrome", 3000);
        Log.d(TAG, "Chrome启动结果: " + result1);
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 2. 测试另一个应用
        Log.d(TAG, "=== 测试2: 启动计算器 ===");
        boolean result2 = accessibilityService.launchApp("com.android.calculator2", 2000);
        Log.d(TAG, "计算器启动结果: " + result2);
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 3. 测试不存在的应用
        Log.d(TAG, "=== 测试3: 启动不存在的应用 ===");
        boolean result3 = accessibilityService.launchApp("com.nonexistent.app", 1000);
        Log.d(TAG, "不存在的应用启动结果: " + result3);
    }
    
    /**
     * 在任务中使用启动应用功能的示例
     * @param accessibilityService 无障碍服务实例
     */
    public static void launchAppInTaskExample(AccessibilityServiceUtil accessibilityService) {
        // 创建一个任务
        AutomationTask task = new AutomationTask("应用启动测试任务")
                // 启动Chrome浏览器，等待3秒
                .launchApp("com.android.chrome", 3000)
                // 等待1秒
                .waitFor(1000)
                // 启动计算器，等待2秒
                .launchApp("com.android.calculator2", 2000)
                .setTimeout(30000)  // 设置超时时间为30秒
                .onResult((task1, status, message, stepIndex) -> {  // 设置结果回调
                    switch (status) {
                        case SUCCESS:
                            Log.d(TAG, "应用启动任务执行成功！当前步骤: " + stepIndex);
                            break;
                        case FAILED:
                            Log.e(TAG, "应用启动任务执行失败: " + message + "，当前步骤: " + stepIndex);
                            break;
                        case TIMEOUT:
                            Log.w(TAG, "应用启动任务执行超时: " + message + "，当前步骤: " + stepIndex);
                            break;
                        case CANCELLED:
                            Log.d(TAG, "应用启动任务被取消: " + message + "，当前步骤: " + stepIndex);
                            break;
                    }
                });

        // 设置无障碍服务引用
        task.setAccessibilityService(accessibilityService);

        // 获取任务管理器实例
        AutomationTaskManager taskManager = AutomationTaskManager.getInstance();

        // 添加任务到队列
        taskManager.addTask(task);

        // 执行任务队列
        taskManager.executeTasks();
    }
}