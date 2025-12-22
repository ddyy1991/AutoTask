package com.dy.autotask.test_example;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import com.dy.autotask.AccessibilityServiceUtil;
import com.dy.autotask.task.AutomationTask;
import com.dy.autotask.task.AutomationTaskManager;

/**
 * 点击功能兼容性测试类
 */
public class ClickCompatibilityTest {
    private static final String TAG = "ClickCompatibilityTest";

    /**
     * 测试不同Android版本上的点击兼容性
     * @param accessibilityService 无障碍服务实例
     */
    public static void testClickCompatibility(AccessibilityServiceUtil accessibilityService) {
        Log.d(TAG, "=== 开始点击兼容性测试 ===");
        Log.d(TAG, "当前Android版本: " + android.os.Build.VERSION.SDK_INT);
        
        // 模拟一个节点点击测试
        testNodeClick(accessibilityService, null, "空节点测试");
        
        Log.d(TAG, "=== 点击兼容性测试结束 ===");
    }
    
    /**
     * 测试节点点击
     * @param service 无障碍服务
     * @param node 节点信息
     * @param testName 测试名称
     */
    private static void testNodeClick(AccessibilityServiceUtil service, AccessibilityNodeInfo node, String testName) {
        Log.d(TAG, "--- " + testName + " ---");
        
        try {
            boolean result = service.clickNode(node);
            Log.d(TAG, testName + " 结果: " + (result ? "成功" : "失败"));
        } catch (Exception e) {
            Log.e(TAG, testName + " 发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 在任务中使用点击功能的示例
     * @param accessibilityService 无障碍服务实例
     */
    public static void clickInTaskExample(AccessibilityServiceUtil accessibilityService) {
        // 创建一个任务
        AutomationTask task = new AutomationTask("点击兼容性测试任务")
                // 等待1秒
                .waitFor(1000)
                // 点击某个元素（这里使用示例ID）
                .click("com.example.app:id/sample_button")
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