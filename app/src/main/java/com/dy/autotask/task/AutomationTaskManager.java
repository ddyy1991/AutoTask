package com.dy.autotask.task;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.dy.autotask.task.TaskLogFileWriter;
import com.dy.autotask.utils.SettingsManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 自动化任务管理器
 * 负责管理任务队列、执行任务、暂停任务等操作
 */
public class AutomationTaskManager {
    private static final String TAG = "AutomationTaskManager";
    
    // 单例实例
    private static volatile AutomationTaskManager instance;
    
    // 任务队列
    private final BlockingQueue<Runnable> taskQueue;
    
    // 线程池执行器
    private final ThreadPoolExecutor executor;
    
    // 当前正在执行的任务
    private AutomationTask currentTask;
    
    // 任务管理器状态
    private boolean isRunning = false;
    
    // 应用上下文
    private Context context;
    
    // 任务日志悬浮窗
    private TaskLogFloatingWindow taskLogWindow;
    
    // 任务日志文件写入器
    private TaskLogFileWriter taskLogFileWriter;
    
    // 是否启用任务日志悬浮窗
    private boolean isLogWindowEnabled = true;
    
    // 设置管理器
    private SettingsManager settingsManager;
    
    /**
     * 私有构造函数
     */
    private AutomationTaskManager() {
        // 初始化任务队列
        taskQueue = new LinkedBlockingQueue<>();
        
        // 初始化线程池（核心线程数1，最大线程数1，确保任务按顺序执行）
        executor = new ThreadPoolExecutor(
                1, 1,
                60L, TimeUnit.SECONDS,
                taskQueue
        );
    }
    
    /**
     * 设置应用上下文
     * @param context 应用上下文
     */
    public void setContext(Context context) {
        this.context = context;
        if (context != null) {
            // 初始化设置管理器
            settingsManager = SettingsManager.getInstance(context);
            // 从设置中读取日志窗口启用状态
            isLogWindowEnabled = settingsManager.isLogWindowEnabled();
            
            if (taskLogWindow == null) {
                taskLogWindow = new TaskLogFloatingWindow(context);
            }
            taskLogWindow.setEnabled(isLogWindowEnabled);
            
            // 初始化任务日志文件写入器
            if (taskLogFileWriter == null) {
                taskLogFileWriter = new TaskLogFileWriter(context);
            }
        }
    }
    
    /**
     * 获取应用上下文
     * @return 应用上下文
     */
    public Context getContext() {
        return context;
    }
    
    /**
     * 设置是否启用任务日志悬浮窗
     * @param enabled 是否启用
     */
    public void setLogWindowEnabled(boolean enabled) {
        isLogWindowEnabled = enabled;
        if (taskLogWindow != null) {
            taskLogWindow.setEnabled(enabled);
        }
        
        // 保存设置
        if (settingsManager != null) {
            settingsManager.setLogWindowEnabled(enabled);
        }
    }
    
    /**
     * 获取是否启用任务日志悬浮窗
     * @return 是否启用
     */
    public boolean isLogWindowEnabled() {
        return isLogWindowEnabled;
    }
    
    /**
     * 获取任务日志悬浮窗是否正在显示
     * @return 是否正在显示
     */
    public boolean isLogWindowShowing() {
        return taskLogWindow != null && taskLogWindow.isShowing();
    }
    
    /**
     * 显示任务日志悬浮窗
     */
    public void showLogWindow() {
        if (taskLogWindow != null) {
            taskLogWindow.show();
        }
    }
    
    /**
     * 隐藏任务日志悬浮窗
     */
    public void hideLogWindow() {
        if (taskLogWindow != null) {
            taskLogWindow.hide();
        }
    }
    
    /**
     * 添加日志信息
     * @param message 日志信息
     */
    public void addLog(String message) {
        // 写入日志到文件
        if (taskLogFileWriter != null) {
            taskLogFileWriter.writeLog(message);
        }
        
        // 显示日志到悬浮窗
        if (taskLogWindow != null) {
            // 根据消息内容确定颜色
            int color = getColorForMessage(message);
            // 直接调用TaskLogFloatingWindow的addColoredLog方法
            taskLogWindow.addColoredLog(message, color);
        }
    }
    
    /**
     * 根据消息内容确定颜色
     * @param message 消息内容
     * @return 颜色值
     */
    private int getColorForMessage(String message) {
        if (message == null) return 0xFFFFFFFF; // 白色
        
        // 任务失败 - 红色
        if (message.contains("失败") || message.contains("FAILED") || message.contains("错误")) {
            return 0xFFFF0000; // 红色
        }
        
        // 任务超时 - 黄色
        if (message.contains("超时") || message.contains("TIMEOUT")) {
            return 0xFFFFFF00; // 黄色
        }
        
        // 任务成功 - 绿色
        if (message.contains("成功") || message.contains("SUCCESS") || message.contains("完成")) {
            return 0xFF00FF00; // 绿色
        }
        
        // 任务开始 - 橙色
        if (message.contains("开始执行任务") || message.contains("启动应用程序")) {
            return 0xFFFFA500; // 橙色
        }
        
        // 默认颜色 - 白色
        return 0xFFFFFFFF; // 白色
    }
    
    /**
     * 清空日志
     */
    public void clearLog() {
        if (taskLogWindow != null) {
            taskLogWindow.clearLog();
        }
    }
    
    /**
     * 获取单例实例
     * @return AutomationTaskManager实例
     */
    public static AutomationTaskManager getInstance() {
        if (instance == null) {
            synchronized (AutomationTaskManager.class) {
                if (instance == null) {
                    instance = new AutomationTaskManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 添加任务到队列
     * @param task 要添加的任务
     */
    public void addTask(AutomationTask task) {
        if (task == null) {
            Log.w(TAG, "尝试添加空任务");
            return;
        }
        
        // 设置任务管理器引用
        task.setTaskManager(this);
        
        // 添加到执行队列
        executor.execute(task);
        Log.d(TAG, "任务已添加到队列: " + task.getTaskName());
    }
    
    /**
     * 执行任务队列
     */
    public void executeTasks() {
        if (isRunning) {
            Log.w(TAG, "任务管理器已在运行中");
            return;
        }
        
        // 显示任务日志悬浮窗
        if (isLogWindowEnabled) {
            showLogWindow();
        }
        
        isRunning = true;
        Log.d(TAG, "开始执行任务队列");
        addLog("开始执行任务队列");
    }
    
    /**
     * 暂停当前任务（将当前任务移到队列末尾）
     */
    public void pauseCurrentTask() {
        if (currentTask != null) {
            Log.d(TAG, "暂停当前任务: " + currentTask.getTaskName());
            
            // 取消当前任务
            currentTask.cancel();
            
            // 将任务重新添加到队列末尾
            addTask(currentTask);
            
            // 清空当前任务引用
            currentTask = null;
        } else {
            Log.w(TAG, "没有正在执行的任务可暂停");
        }
    }
    
    /**
     * 停止并移除当前任务
     */
    public void stopAndRemoveCurrentTask() {
        if (currentTask != null) {
            Log.d(TAG, "停止并移除当前任务: " + currentTask.getTaskName());
            
            // 取消当前任务
            currentTask.cancel();
            
            // 清空当前任务引用
            currentTask = null;
        } else {
            Log.w(TAG, "没有正在执行的任务可停止");
        }
    }
    
    /**
     * 停止并清空所有任务
     */
    public void stopAndClearAllTasks() {
        Log.d(TAG, "停止并清空所有任务");
        
        // 停止当前任务
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
        
        // 清空任务队列
        taskQueue.clear();
        
        // 关闭线程池
        executor.shutdownNow();
        
        // 重新初始化线程池
        // 初始化线程池（核心线程数1，最大线程数1，确保任务按顺序执行）
        // 注意：这里需要重新创建线程池，因为shutdownNow()后不能再提交任务
        // 在实际应用中可能需要更好的处理方式
        
        // 清理日志文件写入器
        if (taskLogFileWriter != null) {
            taskLogFileWriter.cleanup();
        }
        
        isRunning = false;
        Log.d(TAG, "所有任务已清空");
    }
    
    /**
     * 设置当前任务
     * @param task 当前正在执行的任务
     */
    void setCurrentTask(AutomationTask task) {
        if (this.currentTask != null && !this.currentTask.equals(task)) {
            addLog("切换任务: 从 '" + this.currentTask.getTaskName() + "' 切换到 '" + task.getTaskName() + "'");
        } else if (this.currentTask == null) {
            addLog("开始执行任务: '" + task.getTaskName() + "'");
        }
        this.currentTask = task;
    }
    
    /**
     * 获取当前任务
     * @return 当前正在执行的任务
     */
    public AutomationTask getCurrentTask() {
        return currentTask;
    }
    
    /**
     * 检查任务管理器是否正在运行
     * @return 是否正在运行
     */
    public boolean isRunning() {
        return isRunning;
    }
}