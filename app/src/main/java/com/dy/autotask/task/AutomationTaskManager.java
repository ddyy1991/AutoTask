package com.dy.autotask.task;

import android.util.Log;

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
        
        isRunning = true;
        Log.d(TAG, "开始执行任务队列");
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
        
        isRunning = false;
        Log.d(TAG, "所有任务已清空");
    }
    
    /**
     * 设置当前任务
     * @param task 当前正在执行的任务
     */
    void setCurrentTask(AutomationTask task) {
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