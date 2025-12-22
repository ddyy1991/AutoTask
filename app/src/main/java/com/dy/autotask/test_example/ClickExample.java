package com.dy.autotask.test_example;

import android.util.Log;
import com.dy.autotask.AccessibilityServiceUtil;
import com.dy.autotask.task.AutomationTask;
import com.dy.autotask.task.AutomationTaskManager;

/**
 * 点击功能使用示例
 */
public class ClickExample {
    private static final String TAG = "ClickExample";

    /**
     * 示例：使用增强的点击功能
     * @param accessibilityService 无障碍服务实例
     */
    public static void clickExample(AccessibilityServiceUtil accessibilityService) {
        // 创建一个任务
        AutomationTask task = new AutomationTask("点击示例任务")
                // 点击ID为"button_ok"的按钮
                .click("button_ok")
                // 点击文本为"确定"的按钮
                .click("确定", AutomationTask.ElementType.TEXT)
                // 点击描述为"关闭"的按钮
                .click("关闭", AutomationTask.ElementType.DESCRIPTION)
                // 点击坐标为(100, 200)的位置
                .tap(100, 200)
                .setTimeout(30000)  // 设置超时时间为30秒
                .onResult((task1, status, message, stepIndex) -> {  // 设置结果回调
                    switch (status) {
                        case SUCCESS:
                            Log.d(TAG, "点击任务执行成功！当前步骤: " + stepIndex);
                            break;
                        case FAILED:
                            Log.e(TAG, "点击任务执行失败: " + message + "，当前步骤: " + stepIndex);
                            break;
                        case TIMEOUT:
                            Log.w(TAG, "点击任务执行超时: " + message + "，当前步骤: " + stepIndex);
                            break;
                        case CANCELLED:
                            Log.d(TAG, "点击任务被取消: " + message + "，当前步骤: " + stepIndex);
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