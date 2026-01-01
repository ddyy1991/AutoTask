package com.dy.autotask.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Root权限检测和申请工具类
 */
public class RootUtil {
    private static final String TAG = "RootUtil";

    /**
     * 检测设备是否有root权限
     */
    public static boolean hasRootPermission() {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3();
    }

    /**
     * 方法1：检查su命令是否存在
     */
    private static boolean checkRootMethod1() {
        String[] paths = {"/system/bin/su", "/system/xbin/su", "/data/local/su", "/data/local/bin/su"};
        for (String path : paths) {
            if (new java.io.File(path).exists()) {
                Log.d(TAG, "检测到su命令: " + path);
                return true;
            }
        }
        return false;
    }

    /**
     * 方法2：尝试执行su命令
     */
    private static boolean checkRootMethod2() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.getOutputStream().write("exit\n".getBytes());
            process.getOutputStream().flush();
            int exitCode = process.waitFor();
            Log.d(TAG, "su命令执行结果: " + exitCode);
            return exitCode == 0;
        } catch (Exception e) {
            Log.d(TAG, "su命令执行异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 方法3：检查build属性
     */
    private static boolean checkRootMethod3() {
        try {
            String buildTags = android.os.Build.TAGS;
            if (buildTags != null && buildTags.contains("test-keys")) {
                Log.d(TAG, "检测到test-keys标签，可能有root权限");
                return true;
            }
        } catch (Exception e) {
            Log.d(TAG, "检查build属性异常: " + e.getMessage());
        }
        return false;
    }

    /**
     * 申请root权限（通过su命令）
     * 返回是否成功获得root权限
     */
    public static boolean requestRootPermission() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.getOutputStream().write("exit\n".getBytes());
            process.getOutputStream().flush();
            int exitCode = process.waitFor();
            Log.d(TAG, "申请root权限结果: " + exitCode);
            return exitCode == 0;
        } catch (Exception e) {
            Log.e(TAG, "申请root权限异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 使用root权限执行命令
     */
    public static boolean executeCommandAsRoot(String command) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.getOutputStream().write((command + "\n").getBytes());
            process.getOutputStream().write("exit\n".getBytes());
            process.getOutputStream().flush();
            int exitCode = process.waitFor();
            Log.d(TAG, "执行root命令结果: " + exitCode + ", 命令: " + command);
            return exitCode == 0;
        } catch (Exception e) {
            Log.e(TAG, "执行root命令异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 使用root权限授予权限
     */
    public static boolean grantPermissionWithRoot(String packageName, String permission) {
        String command = "pm grant " + packageName + " " + permission;
        return executeCommandAsRoot(command);
    }
}
