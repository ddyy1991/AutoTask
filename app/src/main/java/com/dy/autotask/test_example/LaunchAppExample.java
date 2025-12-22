package com.dy.autotask.test_example;

import android.util.Log;
import com.dy.autotask.AccessibilityServiceUtil;
import com.dy.autotask.task.AutomationTask;
import com.dy.autotask.task.AutomationTaskManager;

/**
 * 启动应用程序功能使用示例
 */
public class LaunchAppExample {
    private static final String TAG = "LaunchAppExample";

    /**
     * 示例：使用启动应用程序功能
     * @param accessibilityService 无障碍服务实例
     */
    public static void launchAppExample(AccessibilityServiceUtil accessibilityService) {
        // 创建一个任务
        AutomationTask task = new AutomationTask("启动应用示例任务")
                // 启动微信应用，等待3秒
                .launchApp("com.tencent.mm", 3000)
                // 等待2秒
                .waitFor(2000)
                // 点击微信中的某个按钮
                .click("com.tencent.mm:id/some_button")
                .setTimeout(30000)  // 设置超时时间为30秒
                .onResult((task1, status, message, stepIndex) -> {  // 设置结果回调
                    switch (status) {
                        case SUCCESS:
                            Log.d(TAG, "启动应用任务执行成功！当前步骤: " + stepIndex);
                            break;
                        case FAILED:
                            Log.e(TAG, "启动应用任务执行失败: " + message + "，当前步骤: " + stepIndex);
                            break;
                        case TIMEOUT:
                            Log.w(TAG, "启动应用任务执行超时: " + message + "，当前步骤: " + stepIndex);
                            break;
                        case CANCELLED:
                            Log.d(TAG, "启动应用任务被取消: " + message + "，当前步骤: " + stepIndex);
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