package com.dy.autotask.utils;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;

/**
 * Base64 编码工具类
 * 用于将 Bitmap 转换为 Base64 编码的字符串
 */
public class Base64Util {

    private static final String TAG = "Base64Util";

    /**
     * 将 Bitmap 转换为 Base64 编码的字符串（JPEG 格式）
     *
     * @param bitmap 输入的 Bitmap 对象
     * @return Base64 编码字符串，失败返回 null
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        return bitmapToBase64(bitmap, 75);
    }

    /**
     * 将 Bitmap 转换为 Base64 编码的字符串（可指定质量）
     *
     * @param bitmap  输入的 Bitmap 对象
     * @param quality JPEG 质量（0-100）
     * @return Base64 编码字符串，失败返回 null
     */
    public static String bitmapToBase64(Bitmap bitmap, int quality) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap 为 null");
            return null;
        }

        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            byte[] imageBytes = baos.toByteArray();

            // 编码为 Base64，并添加 data:image/jpeg;base64 前缀
            String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
            String dataUrl = "data:image/jpeg;base64," + base64String;

            Log.d(TAG, "Bitmap 转换为 Base64 成功，长度: " + dataUrl.length());
            return dataUrl;

        } catch (Exception e) {
            Log.e(TAG, "Bitmap 转换为 Base64 失败: " + e.getMessage(), e);
            return null;
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (Exception e) {
                    Log.e(TAG, "关闭流失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 验证 Base64 字符串是否有效
     *
     * @param base64String Base64 编码字符串
     * @return 是否有效
     */
    public static boolean isValidBase64(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return false;
        }

        try {
            Base64.decode(base64String, Base64.DEFAULT);
            return true;
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Base64 字符串无效: " + e.getMessage());
            return false;
        }
    }
}
