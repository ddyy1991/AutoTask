package com.dy.autotask.task;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
     * 链式调用：查找页面元素
     * @param elementId 元素ID
     * @return 当前任务实例
     */
    public AutomationTask findElement(String elementId) {
        actions.add(new TaskAction(TaskActionType.FIND_ELEMENT, elementId));
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
    private void executeAction(TaskAction action) {
        Log.d(TAG, "执行操作: " + action.getType() + ", 参数: " + action.getData());
        
        // 这里应该实现具体的自动化操作逻辑
        // 由于这是一个示例，我们只是模拟操作执行
        try {
            // 模拟操作执行时间
            Thread.sleep(100);
            
            // 根据操作类型执行不同的逻辑
            switch (action.getType()) {
                case CLICK:
                    // 模拟点击操作
                    Log.d(TAG, "点击元素: " + action.getData());
                    break;
                case FIND_ELEMENT:
                    // 模拟查找元素操作
                    Log.d(TAG, "查找元素: " + action.getData());
                    break;
                case WAIT:
                    // 模拟等待操作
                    long waitTime = Long.parseLong(action.getData());
                    Log.d(TAG, "等待 " + waitTime + " 毫秒");
                    Thread.sleep(waitTime);
                    break;
                case INPUT_TEXT:
                    // 模拟输入文本操作
                    Log.d(TAG, "向元素 " + action.getData() + " 输入文本: " + action.getExtraData());
                    break;
                case SWIPE:
                    // 模拟滑动操作
                    Log.d(TAG, "滑动操作: " + action.getData());
                    break;
                case TAP:
                    // 模拟点击操作
                    Log.d(TAG, "点击坐标: " + action.getData());
                    break;
                case LONG_PRESS:
                    // 模拟长按操作
                    Log.d(TAG, "长按操作: " + action.getData());
                    break;
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "操作被中断");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Log.e(TAG, "执行操作失败: " + action.getType(), e);
            throw new RuntimeException("操作执行失败", e);
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
        LONG_PRESS      // 长按
    }
    
    /**
     * 任务操作类
     */
    private static class TaskAction {
        private final TaskActionType type;
        private final String data;
        private final String extraData;
        
        public TaskAction(TaskActionType type, String data) {
            this(type, data, null);
        }
        
        public TaskAction(TaskActionType type, String data, String extraData) {
            this.type = type;
            this.data = data;
            this.extraData = extraData;
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