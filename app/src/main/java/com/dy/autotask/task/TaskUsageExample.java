package com.dy.autotask.task;

import android.content.Context;
import android.util.Log;

import com.dy.autotask.AccessibilityServiceUtil;

/**
 * 任务系统使用示例
 * 演示如何使用自动化任务管理系统
 */
public class TaskUsageExample {
    private static final String TAG = "TaskUsageExample";
    
    /**
     * 示例：创建并执行一个简单的自动化任务
     */
    public static void simpleTaskExample(AccessibilityServiceUtil accessibilityService, Context context) {
        // 获取任务管理器实例
        AutomationTaskManager taskManager = AutomationTaskManager.getInstance();
        
        // 设置上下文以启用Toast提示和日志悬浮窗
        taskManager.setContext(context);
        
        // 启用任务日志悬浮窗（默认启用）
        taskManager.setLogWindowEnabled(true);
        
        // 创建一个任务
        AutomationTask task = new AutomationTask("登录任务")
                .click("login_button")                           // 点击登录按钮（默认使用ID类型，超时1.5秒）
                .waitFor(1000)                                   // 等待1秒
                .findElement("username_input")                   // 查找用户名输入框（默认使用ID类型，超时1.5秒）
                .inputText("username_input", "test_user")       // 输入用户名（默认使用ID类型，超时1.5秒）
                .findElement("password_input")                   // 查找密码输入框（默认使用ID类型，超时1.5秒）
                .inputText("password_input", "test_password")   // 输入密码（默认使用ID类型，超时1.5秒）
                .click("submit_button")                          // 点击提交按钮（默认使用ID类型，超时1.5秒）
                .setTimeout(30000)                               // 设置任务超时时间为30秒
                .onResult((task1, status, message) -> {          // 设置结果回调
                    Log.d(TAG, "任务 '" + task1.getTaskName() + "' 执行结果: " + message);
                });
        
        // 创建一个使用新功能的任务
        AutomationTask advancedTask = new AutomationTask("高级登录任务")
                .click("登录", AutomationTask.ElementType.TEXT, 2000)  // 通过文本查找并点击“登录”按钮，超时2秒
                .waitFor(1000)                                         // 等待1秒
                .findElement("用户名", AutomationTask.ElementType.DESCRIPTION, 3000)  // 通过描述查找用户名输入框，超时3秒
                .inputText("用户名输入框", "test_user", AutomationTask.ElementType.DESCRIPTION, 2500)  // 通过描述查找输入框并输入用户名，超时2.5秒
                .click("100,200", AutomationTask.ElementType.COORDINATES, 1500)  // 点击坐标(100,200)，超时1.5秒
                .setTimeout(30000)                                     // 设置任务超时时间为30秒
                .onResult((task1, status, message) -> {                // 设置结果回调
                    Log.d(TAG, "任务 '" + task1.getTaskName() + "' 执行结果: " + message);
                });
        
        // 设置无障碍服务引用
        task.setAccessibilityService(accessibilityService);
        
        // 添加任务到队列
        taskManager.addTask(task);
        
        // 执行任务队列
        taskManager.executeTasks();
    }
    
    /**
     * 示例：创建并执行一个复杂的自动化任务
     */
    public static void complexTaskExample(AccessibilityServiceUtil accessibilityService, Context context) {
        // 获取任务管理器实例
        AutomationTaskManager taskManager = AutomationTaskManager.getInstance();
        
        // 设置上下文以启用Toast提示和日志悬浮窗
        taskManager.setContext(context);
        
        // 启用任务日志悬浮窗（默认启用）
        taskManager.setLogWindowEnabled(true);
        
        // 创建一个复杂的任务
        AutomationTask task = new AutomationTask("购物车结算任务")
                .click("cart_icon")                              // 点击购物车图标（默认使用ID类型，超时1.5秒）
                .waitFor(500)                                    // 等待500毫秒
                .findElement("checkout_button")                  // 查找结算按钮（默认使用ID类型，超时1.5秒）
                .click("checkout_button")                        // 点击结算按钮（默认使用ID类型，超时1.5秒）
                .waitFor(1000)                                   // 等待1秒
                .findElement("address_input")                    // 查找地址输入框（默认使用ID类型，超时1.5秒）
                .inputText("address_input", "北京市朝阳区")      // 输入地址（默认使用ID类型，超时1.5秒）
                .swipe(100, 500, 100, 200)                      // 滑动选择配送方式
                .tap(200, 300)                                  // 点击某个坐标
                .longPress(150, 250, 2000)                      // 长按2秒
                .click("confirm_order_button")                   // 点击确认订单按钮（默认使用ID类型，超时1.5秒）
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
        
        // 创建一个使用新功能的复杂任务
        AutomationTask advancedTask = new AutomationTask("高级购物车结算任务")
                .click("购物车", AutomationTask.ElementType.TEXT, 2000)  // 通过文本查找并点击“购物车”，超时2秒
                .waitFor(500)                                            // 等待500毫秒
                .findElement("结算", AutomationTask.ElementType.TEXT, 3000)  // 通过文本查找“结算”按钮，超时3秒
                .click("结算", AutomationTask.ElementType.TEXT, 2000)        // 点击“结算”按钮，超时2秒
                .waitFor(1000)                                           // 等待1秒
                .findElement("地址", AutomationTask.ElementType.DESCRIPTION, 3000)  // 通过描述查找地址输入框，超时3秒
                .inputText("地址", "北京市朝阳区", AutomationTask.ElementType.TEXT, 2500)  // 通过文本查找地址输入框并输入地址，超时2.5秒
                .swipe(100, 500, 100, 200)                              // 滑动选择配送方式
                .tap(200, 300)                                          // 点击某个坐标
                .longPress(150, 250, 2000)                              // 长按2秒
                .click("确认订单", AutomationTask.ElementType.TEXT, 2000)     // 通过文本查找并点击“确认订单”按钮，超时2秒
                .setTimeout(60000)                                       // 设置超时时间为60秒
                .onResult((task1, status, message) -> {                  // 设置结果回调
                    switch (status) {
                        case SUCCESS:
                            Log.d(TAG, "高级购物车结算任务执行成功！");
                            break;
                        case FAILED:
                            Log.e(TAG, "高级购物车结算任务执行失败: " + message);
                            break;
                        case TIMEOUT:
                            Log.w(TAG, "高级购物车结算任务执行超时: " + message);
                            break;
                        case CANCELLED:
                            Log.d(TAG, "高级购物车结算任务被取消: " + message);
                            break;
                    }
                });
        
        // 设置无障碍服务引用
        task.setAccessibilityService(accessibilityService);
        
        // 添加任务到队列
        taskManager.addTask(task);
        
        // 执行任务队列
        taskManager.executeTasks();
    }
    
    /**
     * 示例：任务管理操作
     */
    public static void taskManagementExample(AccessibilityServiceUtil accessibilityService, Context context) {
        // 获取任务管理器实例
        AutomationTaskManager taskManager = AutomationTaskManager.getInstance();
        
        // 设置上下文以启用Toast提示和日志悬浮窗
        taskManager.setContext(context);
        
        // 启用任务日志悬浮窗（默认启用）
        taskManager.setLogWindowEnabled(true);
        
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
        
        // 创建使用新功能的任务
        AutomationTask advancedTask1 = new AutomationTask("高级任务1")
                .click("按钮1", AutomationTask.ElementType.TEXT, 2000)  // 通过文本查找并点击按钮，超时2秒
                .waitFor(1500)
                .onResult((task, status, message) -> 
                    Log.d(TAG, "高级任务1完成: " + message));
        
        AutomationTask advancedTask2 = new AutomationTask("高级任务2")
                .findElement("输入框1", AutomationTask.ElementType.DESCRIPTION, 3000)  // 通过描述查找输入框，超时3秒
                .inputText("输入框1", "测试文本", AutomationTask.ElementType.DESCRIPTION, 2500)  // 通过描述查找输入框并输入文本，超时2.5秒
                .onResult((task, status, message) -> 
                    Log.d(TAG, "高级任务2完成: " + message));
        
        // 创建包含按键操作的任务
        AutomationTask keyTask = new AutomationTask("按键操作任务")
                .click("某个按钮")                           // 点击某个按钮
                .waitFor(1000)                               // 等待1秒
                .pressMenu()                                 // 点击菜单键
                .waitFor(1000)                               // 等待1秒
                .pressBack()                                 // 点击返回键
                .waitFor(1000)                               // 等待1秒
                .pressHome()                                 // 点击Home键
                .onResult((task, status, message) -> 
                    Log.d(TAG, "按键操作任务完成: " + message));
        
        // 设置无障碍服务引用
        task1.setAccessibilityService(accessibilityService);
        task2.setAccessibilityService(accessibilityService);
        task3.setAccessibilityService(accessibilityService);
        advancedTask1.setAccessibilityService(accessibilityService);
        advancedTask2.setAccessibilityService(accessibilityService);
        keyTask.setAccessibilityService(accessibilityService);
        
        // 添加任务到队列
        taskManager.addTask(task1);
        taskManager.addTask(task2);
        taskManager.addTask(task3);
        taskManager.addTask(keyTask);
        
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