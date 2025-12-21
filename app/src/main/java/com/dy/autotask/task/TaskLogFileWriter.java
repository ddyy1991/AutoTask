package com.dy.autotask.task;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 任务日志文件写入器
 * 负责将任务日志写入应用私有文件夹，并按时间分割日志文件
 */
public class TaskLogFileWriter {
    private static final String TAG = "TaskLogFileWriter";
    private static final String LOG_FOLDER_NAME = "task_logs";
    
    private Context context;
    private File logFolder;
    private FileOutputStream currentOutputStream;
    private String currentLogFileName;
    
    public TaskLogFileWriter(Context context) {
        this.context = context;
        initLogFolder();
    }
    
    /**
     * 初始化日志文件夹
     */
    private void initLogFolder() {
        try {
            // 获取外部存储的应用私有目录下的task_logs目录
            File externalFilesDir = context.getExternalFilesDir(null);
            logFolder = new File(externalFilesDir, LOG_FOLDER_NAME);
            if (!logFolder.exists()) {
                logFolder.mkdirs();
            }
            Log.d(TAG, "日志文件夹路径: " + logFolder.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "初始化日志文件夹失败: " + e.getMessage());
        }
    }
    
    /**
     * 写入日志到文件
     * @param message 日志消息
     */
    public void writeLog(String message) {
        try {
            // 获取当前时间对应的日志文件名
            String logFileName = getLogFileNameForCurrentTime();
            
            // 如果文件名发生变化，需要关闭当前流并打开新的流
            if (!logFileName.equals(currentLogFileName)) {
                closeCurrentStream();
                currentLogFileName = logFileName;
                openNewStream(logFileName);
            }
            
            // 写入日志
            if (currentOutputStream != null) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
                String logEntry = "[" + timestamp + "] " + message + "\n";
                currentOutputStream.write(logEntry.getBytes());
                currentOutputStream.flush();
            }
        } catch (Exception e) {
            Log.e(TAG, "写入日志文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据当前时间获取日志文件名
     * @return 日志文件名 (格式: yyyyMMdd_HH.txt)
     */
    private String getLogFileNameForCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH", Locale.getDefault());
        return "task_log_" + sdf.format(new Date()) + ".txt";
    }
    
    /**
     * 打开新的文件输出流
     * @param fileName 文件名
     */
    private void openNewStream(String fileName) {
        try {
            File logFile = new File(logFolder, fileName);
            currentOutputStream = new FileOutputStream(logFile, true); // 追加模式
        } catch (Exception e) {
            Log.e(TAG, "打开日志文件流失败: " + e.getMessage());
            currentOutputStream = null;
        }
    }
    
    /**
     * 关闭当前文件输出流
     */
    private void closeCurrentStream() {
        if (currentOutputStream != null) {
            try {
                currentOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "关闭日志文件流失败: " + e.getMessage());
            } finally {
                currentOutputStream = null;
            }
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        closeCurrentStream();
    }
    
    /**
     * 获取日志文件夹路径
     * @return 日志文件夹路径
     */
    public String getLogFolderPath() {
        return logFolder != null ? logFolder.getAbsolutePath() : null;
    }
}