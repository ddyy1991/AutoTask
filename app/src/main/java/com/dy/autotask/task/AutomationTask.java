package com.dy.autotask.task;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.dy.autotask.AccessibilityServiceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 自动化任务对象
 * 支持链式调用的各种自动化操作
 */
public class AutomationTask implements Runnable {
    private static final String TAG = "AutomationTask";
    
    // 任务名称
    private String taskName;
    
    // 任务操作列表
    private final List<TaskAction> actions;
    
    // 任务状态
    private TaskStatus status = TaskStatus.PENDING;
    
    // 任务结果回调
    private TaskResultCallback resultCallback;
    
    // 任务超时时间（毫秒）
    private long timeoutMs = 30000; // 默认30秒
    
    // 任务管理器引用
    private AutomationTaskManager taskManager;
    
    // 是否被取消
    private volatile boolean isCancelled = false;
    
    // Handler用于主线程操作
    private final Handler mainHandler;
    
    // AccessibilityService引用
    private AccessibilityServiceUtil accessibilityService;
    
    /**
     * 构造函数
     * @param taskName 任务名称
     */
    public AutomationTask(String taskName) {
        this.taskName = taskName != null ? taskName : "UnknownTask";
        this.actions = new ArrayList<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 链式调用：点击某个按钮
     * @param elementId 元素ID
     * @return 当前任务实例
     */
    public AutomationTask click(String elementId) {
        actions.add(new TaskAction(TaskActionType.CLICK, elementId));
        return this;
    }
    
    /**
     * 链式调用：点击某个按钮（指定元素类型和超时时间）
     * @param elementId 元素ID/文本/坐标/描述
     * @param elementType 元素类型
     * @param timeoutMs 超时时间（毫秒）
     * @return 当前任务实例
     */
    public AutomationTask click(String elementId, ElementType elementType, long timeoutMs) {
        actions.add(new TaskAction(TaskActionType.CLICK, elementId, elementType, timeoutMs));
        return this;
    }
    
    /**
     * 链式调用：点击某个按钮（指定元素类型，使用默认超时时间）
     * @param elementId 元素ID/文本/坐标/描述
     * @param elementType 元素类型
     * @return 当前任务实例
     */
    public AutomationTask click(String elementId, ElementType elementType) {
        actions.add(new TaskAction(TaskActionType.CLICK, elementId, elementType, 1500));
        return this;
    }
    
    /**
     * 链式调用：查找页面元素
     * @param elementId 元素ID
     * @return 当前任务实例
     */
    public AutomationTask findElement(String elementId) {
        actions.add(new TaskAction(TaskActionType.FIND_ELEMENT, elementId));
        return this;
    }
    
    /**
     * 链式调用：查找页面元素（指定元素类型和超时时间）
     * @param elementId 元素ID/文本/坐标/描述
     * @param elementType 元素类型
     * @param timeoutMs 超时时间（毫秒）
     * @return 当前任务实例
     */
    public AutomationTask findElement(String elementId, ElementType elementType, long timeoutMs) {
        actions.add(new TaskAction(TaskActionType.FIND_ELEMENT, elementId, elementType, timeoutMs));
        return this;
    }
    
    /**
     * 链式调用：查找页面元素（指定元素类型，使用默认超时时间）
     * @param elementId 元素ID/文本/坐标/描述
     * @param elementType 元素类型
     * @return 当前任务实例
     */
    public AutomationTask findElement(String elementId, ElementType elementType) {
        actions.add(new TaskAction(TaskActionType.FIND_ELEMENT, elementId, elementType, 1500));
        return this;
    }
    
    /**
     * 链式调用：等待时间
     * @param milliseconds 等待时间（毫秒）
     * @return 当前任务实例
     */
    public AutomationTask waitFor(long milliseconds) {
        actions.add(new TaskAction(TaskActionType.WAIT, String.valueOf(milliseconds)));
        return this;
    }
    
    /**
     * 链式调用：输入内容到输入框
     * @param elementId 元素ID
     * @param text 输入内容
     * @return 当前任务实例
     */
    public AutomationTask inputText(String elementId, String text) {
        actions.add(new TaskAction(TaskActionType.INPUT_TEXT, elementId, text));
        return this;
    }
    
    /**
     * 链式调用：输入内容到输入框（指定元素类型和超时时间）
     * @param elementId 元素ID/文本/坐标/描述
     * @param text 输入内容
     * @param elementType 元素类型
     * @param timeoutMs 超时时间（毫秒）
     * @return 当前任务实例
     */
    public AutomationTask inputText(String elementId, String text, ElementType elementType, long timeoutMs) {
        actions.add(new TaskAction(TaskActionType.INPUT_TEXT, elementId, text, elementType, timeoutMs));
        return this;
    }
    
    /**
     * 链式调用：输入内容到输入框（指定元素类型，使用默认超时时间）
     * @param elementId 元素ID/文本/坐标/描述
     * @param text 输入内容
     * @param elementType 元素类型
     * @return 当前任务实例
     */
    public AutomationTask inputText(String elementId, String text, ElementType elementType) {
        actions.add(new TaskAction(TaskActionType.INPUT_TEXT, elementId, text, elementType, 1500));
        return this;
    }
    
    /**
     * 链式调用：模拟滑动
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param endX 结束X坐标
     * @param endY 结束Y坐标
     * @return 当前任务实例
     */
    public AutomationTask swipe(int startX, int startY, int endX, int endY) {
        String swipeData = startX + "," + startY + "," + endX + "," + endY;
        actions.add(new TaskAction(TaskActionType.SWIPE, swipeData));
        return this;
    }
    
    /**
     * 链式调用：模拟点击
     * @param x X坐标
     * @param y Y坐标
     * @return 当前任务实例
     */
    public AutomationTask tap(int x, int y) {
        String tapData = x + "," + y;
        actions.add(new TaskAction(TaskActionType.TAP, tapData));
        return this;
    }
    
    /**
     * 链式调用：模拟长按
     * @param x X坐标
     * @param y Y坐标
     * @param duration 长按时长（毫秒）
     * @return 当前任务实例
     */
    public AutomationTask longPress(int x, int y, long duration) {
        String longPressData = x + "," + y + "," + duration;
        actions.add(new TaskAction(TaskActionType.LONG_PRESS, longPressData));
        return this;
    }
    
    /**
     * 链式调用：点击菜单键
     * @return 当前任务实例
     */
    public AutomationTask pressMenu() {
        actions.add(new TaskAction(TaskActionType.PRESS_MENU, ""));
        return this;
    }
    
    /**
     * 链式调用：点击Home键
     * @return 当前任务实例
     */
    public AutomationTask pressHome() {
        actions.add(new TaskAction(TaskActionType.PRESS_HOME, ""));
        return this;
    }
    
    /**
     * 链式调用：点击返回键
     * @return 当前任务实例
     */
    public AutomationTask pressBack() {
        actions.add(new TaskAction(TaskActionType.PRESS_BACK, ""));
        return this;
    }
    
    /**
     * 链式调用：点击电源键
     * @return 当前任务实例
     */
    public AutomationTask pressPower() {
        actions.add(new TaskAction(TaskActionType.PRESS_POWER, ""));
        return this;
    }
    
    /**
     * 设置任务超时时间
     * @param timeoutMs 超时时间（毫秒）
     * @return 当前任务实例
     */
    public AutomationTask setTimeout(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }
    
    /**
     * 设置任务结果回调
     * @param callback 回调接口
     * @return 当前任务实例
     */
    public AutomationTask onResult(TaskResultCallback callback) {
        this.resultCallback = callback;
        return this;
    }
    
    /**
     * 执行任务
     */
    @Override
    public void run() {
        // 检查是否被取消
        if (isCancelled) {
            Log.d(TAG, "任务已被取消: " + taskName);
            notifyResult(TaskStatus.CANCELLED, "任务已被取消");
            return;
        }
        
        // 设置当前任务
        if (taskManager != null) {
            taskManager.setCurrentTask(this);
        }
        
        Log.d(TAG, "开始执行任务: " + taskName);
        status = TaskStatus.RUNNING;
        
        // 使用CountDownLatch实现超时控制
        CountDownLatch latch = new CountDownLatch(1);
        
        // 在后台线程执行任务操作
        Thread taskThread = new Thread(() -> {
            try {
                // 执行所有操作
                for (TaskAction action : actions) {
                    // 检查是否被取消
                    if (isCancelled) {
                        status = TaskStatus.CANCELLED;
                        latch.countDown();
                        return;
                    }
                    
                    // 执行单个操作
                    executeAction(action);
                }
                
                // 所有操作执行完成
                status = TaskStatus.SUCCESS;
                Log.d(TAG, "任务执行成功: " + taskName);
            } catch (Exception e) {
                status = TaskStatus.FAILED;
                Log.e(TAG, "任务执行失败: " + taskName, e);
            } finally {
                latch.countDown();
            }
        });
        
        taskThread.start();
        
        try {
            // 等待任务完成或超时
            if (latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                // 任务正常完成
                if (status == TaskStatus.RUNNING) {
                    status = TaskStatus.SUCCESS;
                }
            } else {
                // 任务超时
                status = TaskStatus.TIMEOUT;
                isCancelled = true;
                Log.w(TAG, "任务执行超时: " + taskName);
                
                // 中断任务线程
                taskThread.interrupt();
            }
        } catch (InterruptedException e) {
            status = TaskStatus.CANCELLED;
            Log.d(TAG, "任务被中断: " + taskName);
            Thread.currentThread().interrupt();
        }
        
        // 通知任务结果
        notifyResult(status, getStatusDescription(status));
    }
    
    /**
     * 执行单个操作
     * @param action 任务操作
     */
    private void executeAction(TaskAction action) throws Exception {
        Log.d(TAG, "执行操作: " + action.getType() + ", 参数: " + action.getData());
        
        // 添加到任务管理器日志
        if (taskManager != null) {
            mainHandler.post(() -> {
                taskManager.addLog("执行操作: " + action.getType() + ", 参数: " + action.getData());
            });
        }
        
        // 检查无障碍服务是否可用
        if (accessibilityService == null) {
            throw new IllegalStateException("无障碍服务未设置");
        }
        
        // 根据操作类型执行不同的逻辑
        switch (action.getType()) {
            case CLICK:
                executeClickAction(action);
                break;
            case FIND_ELEMENT:
                executeFindElementAction(action);
                break;
            case WAIT:
                executeWaitAction(action);
                break;
            case INPUT_TEXT:
                executeInputTextAction(action);
                break;
            case SWIPE:
                executeSwipeAction(action);
                break;
            case TAP:
                executeTapAction(action);
                break;
            case LONG_PRESS:
                executeLongPressAction(action);
                break;
            case PRESS_MENU:
                executePressMenuAction(action);
                break;
            case PRESS_HOME:
                executePressHomeAction(action);
                break;
            case PRESS_BACK:
                executePressBackAction(action);
                break;
            case PRESS_POWER:
                executePressPowerAction(action);
                break;
            default:
                throw new UnsupportedOperationException("不支持的操作类型: " + action.getType());
        }
    }
    
    /**
     * 执行点击操作
     */
    private void executeClickAction(TaskAction action) throws Exception {
        String elementId = action.getData();
        ElementType elementType = action.getElementType();
        long timeoutMs = action.getTimeoutMs();
        Log.d(TAG, "点击元素: " + elementId + ", 类型: " + elementType + ", 超时: " + timeoutMs);
        
        // 添加到任务管理器日志
        if (taskManager != null) {
            mainHandler.post(() -> {
                taskManager.addLog("点击元素: " + elementId + ", 类型: " + elementType + ", 超时: " + timeoutMs);
            });
        }
        
        // 根据元素类型查找并点击元素
        AccessibilityNodeInfo node = findNodeByType(elementId, elementType, timeoutMs);
        if (node != null) {
            boolean success = accessibilityService.clickNode(node);
            if (!success) {
                throw new RuntimeException("点击元素失败: " + elementId);
            } else {
                // 添加成功日志
                if (taskManager != null) {
                    mainHandler.post(() -> {
                        taskManager.addLog("点击元素成功: " + elementId);
                    });
                }
            }
        } else {
            throw new RuntimeException("未找到元素: " + elementId);
        }
    }
    
    /**
     * 执行查找元素操作
     */
    private void executeFindElementAction(TaskAction action) throws Exception {
        String elementId = action.getData();
        ElementType elementType = action.getElementType();
        long timeoutMs = action.getTimeoutMs();
        Log.d(TAG, "查找元素: " + elementId + ", 类型: " + elementType + ", 超时: " + timeoutMs);
        
        // 添加到任务管理器日志
        if (taskManager != null) {
            mainHandler.post(() -> {
                taskManager.addLog("查找元素: " + elementId + ", 类型: " + elementType + ", 超时: " + timeoutMs);
            });
        }
        
        // 根据元素类型查找元素
        AccessibilityNodeInfo node = findNodeByType(elementId, elementType, timeoutMs);
        if (node == null) {
            throw new RuntimeException("未找到元素: " + elementId);
        }
        // 元素找到了，添加成功日志
        if (taskManager != null) {
            mainHandler.post(() -> {
                taskManager.addLog("查找元素成功: " + elementId);
            });
        }
    }
    
    /**
     * 执行等待操作
     */
    private void executeWaitAction(TaskAction action) throws InterruptedException {
        long waitTime = Long.parseLong(action.getData());
        Log.d(TAG, "等待 " + waitTime + " 毫秒");
        
        // 添加到任务管理器日志
        if (taskManager != null) {
            mainHandler.post(() -> {
                taskManager.addLog("等待 " + waitTime + " 毫秒");
            });
        }
        
        Thread.sleep(waitTime);
    }
    
    /**
     * 执行输入文本操作
     */
    private void executeInputTextAction(TaskAction action) throws Exception {
        String elementId = action.getData();
        String text = action.getExtraData();
        ElementType elementType = action.getElementType();
        long timeoutMs = action.getTimeoutMs();
        Log.d(TAG, "向元素 " + elementId + " 输入文本: " + text + ", 类型: " + elementType + ", 超时: " + timeoutMs);
        
        // 添加到任务管理器日志
        if (taskManager != null) {
            mainHandler.post(() -> {
                taskManager.addLog("向元素 " + elementId + " 输入文本: " + text + ", 类型: " + elementType + ", 超时: " + timeoutMs);
            });
        }
        
        // 根据元素类型查找元素并输入文本
        AccessibilityNodeInfo node = findNodeByType(elementId, elementType, timeoutMs);
        if (node != null) {
            // 创建参数
            Bundle args = new Bundle();
            args.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            boolean success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            if (!success) {
                throw new RuntimeException("输入文本失败: " + elementId);
            } else {
                // 添加成功日志
                if (taskManager != null) {
                    mainHandler.post(() -> {
                        taskManager.addLog("向元素 " + elementId + " 输入文本成功");
                    });
                }
            }
        } else {
            throw new RuntimeException("未找到元素: " + elementId);
        }
    }
    
    /**
     * 执行滑动操作
     */
    private void executeSwipeAction(TaskAction action) throws Exception {
        String[] coords = action.getData().split(",");
        int startX = Integer.parseInt(coords[0]);
        int startY = Integer.parseInt(coords[1]);
        int endX = Integer.parseInt(coords[2]);
        int endY = Integer.parseInt(coords[3]);
        Log.d(TAG, "滑动操作: 从(" + startX + "," + startY + ")到(" + endX + "," + endY + ")");
        
        // 添加到任务管理器日志
        if (taskManager != null) {
            mainHandler.post(() -> {
                taskManager.addLog("滑动操作: 从(" + startX + "," + startY + ")到(" + endX + "," + endY + ")");
            });
        }
        
        // TODO: 实现滑动操作
        // 这里需要实现真正的滑动逻辑
        Thread.sleep(500); // 模拟滑动时间
    }
    
    /**
     * 执行点击坐标操作
     */
    private void executeTapAction(TaskAction action) throws Exception {
        String[] coords = action.getData().split(",");
        int x = Integer.parseInt(coords[0]);
        int y = Integer.parseInt(coords[1]);
        Log.d(TAG, "点击坐标: (" + x + "," + y + ")");
        
        // 添加到任务管理器日志
        if (taskManager != null) {
            mainHandler.post(() -> {
                taskManager.addLog("点击坐标: (" + x + "," + y + ")");
            });
        }
        
        // TODO: 实现点击坐标操作
        // 这里需要实现真正的点击坐标逻辑
        Thread.sleep(100); // 模拟点击时间
    }
    
    /**
     * 执行长按操作
     */
    private void executeLongPressAction(TaskAction action) throws Exception {
        String[] data = action.getData().split(",");
        int x = Integer.parseInt(data[0]);
        int y = Integer.parseInt(data[1]);
        long duration = Long.parseLong(data[2]);
        Log.d(TAG, "长按操作: 坐标(" + x + "," + y + "), 持续时间 " + duration + " 毫秒");
        
        // 添加到任务管理器日志
        if (taskManager != null) {
            mainHandler.post(() -> {
                taskManager.addLog("长按操作: 坐标(" + x + "," + y + "), 持续时间 " + duration + " 毫秒");
            });
        }
        
        // TODO: 实现长按操作
        // 这里需要实现真正的长按逻辑
        Thread.sleep(duration); // 模拟长按时间
    }
    
    /**
     * 执行点击菜单键操作
     */
    private void executePressMenuAction(TaskAction action) throws Exception {
        Log.d(TAG, "点击菜单键");
        
        // 添加到任务管理器日志
        if (taskManager != null) {
            mainHandler.post(() -> {
                taskManager.addLog("点击菜单键");
            });
        }
        
        boolean success = accessibilityService.performMenuAction();
        if (!success) {
            throw new RuntimeException("点击菜单键失败");
        } else {
            // 添加成功日志
            if (taskManager != null) {
                mainHandler.post(() -> {
                    taskManager.addLog("点击菜单键成功");
                });
            }
        }
    }
    
    /**
     * 执行点击Home键操作
     */
    private void executePressHomeAction(TaskAction action) throws Exception {
        Log.d(TAG, "点击Home键");
        
        // 添加到任务管理器日志
        if (taskManager != null) {
            mainHandler.post(() -> {
                taskManager.addLog("点击Home键");
            });
        }
        
        boolean success = accessibilityService.performHomeAction();
        if (!success) {
            throw new RuntimeException("点击Home键失败");
        } else {
            // 添加成功日志
            if (taskManager != null) {
                mainHandler.post(() -> {
                    taskManager.addLog("点击Home键成功");
                });
            }
        }
    }
    
    /**
     * 执行点击返回键操作
     */
    private void executePressBackAction(TaskAction action) throws Exception {
        Log.d(TAG, "点击返回键");
        
        // 添加到任务管理器日志
        if (taskManager != null) {
            mainHandler.post(() -> {
                taskManager.addLog("点击返回键");
            });
        }
        
        boolean success = accessibilityService.performBackAction();
        if (!success) {
            throw new RuntimeException("点击返回键失败");
        } else {
            // 添加成功日志
            if (taskManager != null) {
                mainHandler.post(() -> {
                    taskManager.addLog("点击返回键成功");
                });
            }
        }
    }
    
    /**
     * 执行点击电源键操作
     */
    private void executePressPowerAction(TaskAction action) throws Exception {
        Log.d(TAG, "点击电源键");
        
        // 添加到任务管理器日志
        if (taskManager != null) {
            mainHandler.post(() -> {
                taskManager.addLog("点击电源键");
            });
        }
        
        boolean success = accessibilityService.performPowerAction();
        if (!success) {
            throw new RuntimeException("点击电源键失败");
        } else {
            // 添加成功日志
            if (taskManager != null) {
                mainHandler.post(() -> {
                    taskManager.addLog("点击电源键成功");
                });
            }
        }
    }
    
    /**
     * 根据元素类型查找节点
     */
    private AccessibilityNodeInfo findNodeByType(String elementId, ElementType elementType, long timeoutMs) throws Exception {
        switch (elementType) {
            case ID:
                return accessibilityService.findNodeById(elementId, timeoutMs);
            case TEXT:
                return accessibilityService.findNodeByText(elementId, timeoutMs);
            case COORDINATES:
                // 坐标类型不需要查找节点，直接返回null
                return null;
            case DESCRIPTION:
                return accessibilityService.findNodeByDescription(elementId, timeoutMs);
            default:
                throw new IllegalArgumentException("不支持的元素类型: " + elementType);
        }
    }
    
    /**
     * 取消任务
     */
    public void cancel() {
        isCancelled = true;
        status = TaskStatus.CANCELLED;
        Log.d(TAG, "任务已取消: " + taskName);
    }
    
    /**
     * 获取任务名称
     * @return 任务名称
     */
    public String getTaskName() {
        return taskName;
    }
    
    /**
     * 获取任务状态
     * @return 任务状态
     */
    public TaskStatus getStatus() {
        return status;
    }
    
    /**
     * 设置任务管理器引用
     * @param taskManager 任务管理器
     */
    void setTaskManager(AutomationTaskManager taskManager) {
        this.taskManager = taskManager;
    }
    
    /**
     * 设置无障碍服务引用
     * @param service 无障碍服务
     */
    public void setAccessibilityService(AccessibilityServiceUtil service) {
        this.accessibilityService = service;
    }
    
    /**
     * 获取状态描述
     * @param status 任务状态
     * @return 状态描述
     */
    private String getStatusDescription(TaskStatus status) {
        switch (status) {
            case PENDING:
                return "任务待执行";
            case RUNNING:
                return "任务执行中";
            case SUCCESS:
                return "任务执行成功";
            case FAILED:
                return "任务执行失败";
            case TIMEOUT:
                return "任务执行超时";
            case CANCELLED:
                return "任务已取消";
            default:
                return "未知状态";
        }
    }
    
    /**
     * 通知任务结果
     * @param status 任务状态
     * @param message 状态消息
     */
    private void notifyResult(TaskStatus status, String message) {
        Log.d(TAG, "任务 " + taskName + " 状态: " + message);
        
        // 添加到任务管理器日志
        if (taskManager != null) {
            mainHandler.post(() -> {
                taskManager.addLog("任务 '" + taskName + "' 状态: " + message);
            });
        }
        
        // 对于失败或超时状态，显示Toast提示
        if (status == TaskStatus.FAILED || status == TaskStatus.TIMEOUT) {
            if (taskManager != null && taskManager.getContext() != null) {
                String toastMessage = "任务 '" + taskName + "' " + message;
                mainHandler.post(() -> {
                    Toast.makeText(taskManager.getContext(), toastMessage, Toast.LENGTH_LONG).show();
                });
            }
        }
        
        // 在主线程中回调结果
        mainHandler.post(() -> {
            if (resultCallback != null) {
                resultCallback.onTaskResult(this, status, message);
            }
        });
    }
    
    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING,    // 待执行
        RUNNING,    // 执行中
        SUCCESS,    // 执行成功
        FAILED,     // 执行失败
        TIMEOUT,    // 执行超时
        CANCELLED   // 已取消
    }
    
    /**
     * 任务操作类型枚举
     */
    private enum TaskActionType {
        CLICK,          // 点击
        FIND_ELEMENT,   // 查找元素
        WAIT,           // 等待
        INPUT_TEXT,     // 输入文本
        SWIPE,          // 滑动
        TAP,            // 点击坐标
        LONG_PRESS,     // 长按
        PRESS_MENU,     // 点击菜单键
        PRESS_HOME,     // 点击Home键
        PRESS_BACK,     // 点击返回键
        PRESS_POWER     // 点击电源键
    }
    
    /**
     * 元素类型枚举
     */
    public enum ElementType {
        ID,             // 元素ID
        TEXT,           // 文本
        COORDINATES,    // 坐标
        DESCRIPTION     // 描述
    }
    
    /**
     * 任务操作类
     */
    private static class TaskAction {
        private final TaskActionType type;
        private final String data;
        private final String extraData;
        private final ElementType elementType;
        private final long timeoutMs;
        
        public TaskAction(TaskActionType type, String data) {
            this(type, data, null, ElementType.ID, 1500); // 默认类型为ID，超时时间为1.5秒
        }
        
        public TaskAction(TaskActionType type, String data, String extraData) {
            this(type, data, extraData, ElementType.ID, 1500); // 默认类型为ID，超时时间为1.5秒
        }
        
        public TaskAction(TaskActionType type, String data, ElementType elementType, long timeoutMs) {
            this(type, data, null, elementType, timeoutMs);
        }
        
        public TaskAction(TaskActionType type, String data, String extraData, ElementType elementType, long timeoutMs) {
            this.type = type;
            this.data = data;
            this.extraData = extraData;
            this.elementType = elementType;
            this.timeoutMs = timeoutMs;
        }
        
        public TaskActionType getType() {
            return type;
        }
        
        public String getData() {
            return data;
        }
        
        public String getExtraData() {
            return extraData;
        }
        
        public ElementType getElementType() {
            return elementType;
        }
        
        public long getTimeoutMs() {
            return timeoutMs;
        }
    }
    
    /**
     * 任务结果回调接口
     */
    public interface TaskResultCallback {
        /**
         * 任务执行结果回调
         * @param task 任务对象
         * @param status 任务状态
         * @param message 状态消息
         */
        void onTaskResult(AutomationTask task, TaskStatus status, String message);
    }
}