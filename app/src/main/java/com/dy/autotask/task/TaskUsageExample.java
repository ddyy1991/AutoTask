package com.dy.autotask.task;

import android.util.Log;

/**
 * 任务系统使用示例
 * 演示如何使用自动化任务管理系统
 */
public class TaskUsageExample {
    private static final String TAG = "TaskUsageExample";
    
    /**
     * 示例：创建并执行一个简单的自动化任务
     */
    public static void simpleTaskExample() {
        // 获取任务管理器实例
        AutomationTaskManager taskManager = AutomationTaskManager.getInstance();
        
        // 创建一个任务
        AutomationTask task = new AutomationTask("登录任务")
                .click("login_button")                           // 点击登录按钮
                .waitFor(1000)                                   // 等待1秒
                .findElement("username_input")                   // 查找用户名输入框
                .inputText("username_input", "test_user")       // 输入用户名
                .findElement("password_input")                   // 查找密码输入框
                .inputText("password_input", "test_password")   // 输入密码
                .click("submit_button")                          // 点击提交按钮
                .setTimeout(30000)                               // 设置超时时间为30秒
                .onResult((task1, status, message) -> {          // 设置结果回调
                    Log.d(TAG, "任务 '" + task1.getTaskName() + "' 执行结果: " + message);
                });
        
        // 添加任务到队列
        taskManager.addTask(task);
        
        // 执行任务队列
        taskManager.executeTasks();
    }
    
    /**
     * 示例：创建并执行一个复杂的自动化任务
     */
    public static void complexTaskExample() {
        // 获取任务管理器实例
        AutomationTaskManager taskManager = AutomationTaskManager.getInstance();
        
        // 创建一个复杂的任务
        AutomationTask task = new AutomationTask("购物车结算任务")
                .click("cart_icon")                              // 点击购物车图标
                .waitFor(500)                                    // 等待500毫秒
                .findElement("checkout_button")                  // 查找结算按钮
                .click("checkout_button")                        // 点击结算按钮
                .waitFor(1000)                                   // 等待1秒
                .findElement("address_input")                    // 查找地址输入框
                .inputText("address_input", "北京市朝阳区")      // 输入地址
                .swipe(100, 500, 100, 200)                      // 滑动选择配送方式
                .tap(200, 300)                                  // 点击某个坐标
                .longPress(150, 250, 2000)                      // 长按2秒
                .click("confirm_order_button")                   // 点击确认订单按钮
                .setTimeout(60000)                               // 设置超时时间为60秒
                .onResult((task1, status, message) -> {          // 设置结果回调
                    switch (status) {
                        case SUCCESS:
                            Log.d(TAG, "购物车结算任务执行成功！");
                            break;
                        case FAILED:
                            Log.e(TAG, "购物车结算任务执行失败: " + message);
                            break;
                        case TIMEOUT:
                            Log.w(TAG, "购物车结算任务执行超时: " + message);
                            break;
                        case CANCELLED:
                            Log.d(TAG, "购物车结算任务被取消: " + message);
                            break;
                    }
                });
        
        // 添加任务到队列
        taskManager.addTask(task);
        
        // 执行任务队列
        taskManager.executeTasks();
    }
    
    /**
     * 示例：任务管理操作
     */
    public static void taskManagementExample() {
        // 获取任务管理器实例
        AutomationTaskManager taskManager = AutomationTaskManager.getInstance();
        
        // 创建几个任务
        AutomationTask task1 = new AutomationTask("任务1")
                .waitFor(2000)
                .onResult((task, status, message) -> 
                    Log.d(TAG, "任务1完成: " + message));
        
        AutomationTask task2 = new AutomationTask("任务2")
                .waitFor(3000)
                .onResult((task, status, message) -> 
                    Log.d(TAG, "任务2完成: " + message));
        
        AutomationTask task3 = new AutomationTask("任务3")
                .waitFor(1000)
                .onResult((task, status, message) -> 
                    Log.d(TAG, "任务3完成: " + message));
        
        // 添加任务到队列
        taskManager.addTask(task1);
        taskManager.addTask(task2);
        taskManager.addTask(task3);
        
        // 执行任务队列
        taskManager.executeTasks();
        
        // 暂停当前任务（将当前任务移到队列末尾）
        // taskManager.pauseCurrentTask();
        
        // 停止并移除当前任务
        // taskManager.stopAndRemoveCurrentTask();
        
        // 停止并清空所有任务
        // taskManager.stopAndClearAllTasks();
    }
}