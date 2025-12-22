package com.dy.autotask.test_example;

import android.util.Log;
import com.dy.autotask.AccessibilityServiceUtil;
import com.dy.autotask.task.AutomationTask;
import com.dy.autotask.task.AutomationTaskManager;

/**
 * 查找文本功能使用示例
 */
public class FindTextExample {
    private static final String TAG = "FindTextExample";

    /**
     * 示例：使用查找文本功能
     * @param accessibilityService 无障碍服务实例
     */
    public static void findTextExample(AccessibilityServiceUtil accessibilityService) {
        // 创建一个任务
        AutomationTask task = new AutomationTask("查找文本示例任务")
                // 查找单个文本（精确匹配，带超时参数）
                .findText("确定", true, 3000)
                // 查找多个文本（模糊匹配，带超时参数）
                .findText(new String[]{"取消", "关闭", "退出"}, false, 5000)
                // 查找单个文本（模糊匹配，使用默认超时）
                .findText("设置", false)
                .setTimeout(30000)  // 设置超时时间为30秒
                .onResult((task1, status, message, stepIndex) -> {  // 设置结果回调
                    switch (status) {
                        case SUCCESS:
                            Log.d(TAG, "查找文本任务执行成功！当前步骤: " + stepIndex);
                            break;
                        case FAILED:
                            Log.e(TAG, "查找文本任务执行失败: " + message + "，当前步骤: " + stepIndex);
                            break;
                        case TIMEOUT:
                            Log.w(TAG, "查找文本任务执行超时: " + message + "，当前步骤: " + stepIndex);
                            break;
                        case CANCELLED:
                            Log.d(TAG, "查找文本任务被取消: " + message + "，当前步骤: " + stepIndex);
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