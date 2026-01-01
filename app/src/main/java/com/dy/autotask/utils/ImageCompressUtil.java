package com.dy.autotask.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.InputStream;

/**
 * 图片压缩工具类
 * 用于压缩和处理 Bitmap
 */
public class ImageCompressUtil {

    private static final String TAG = "ImageCompressUtil";

    // 最大宽度和高度（像素）
    private static final int MAX_WIDTH = 1024;
    private static final int MAX_HEIGHT = 1024;

    /**
     * 从 Content URI 加载并压缩图片
     *
     * @param context 应用上下文
     * @param uri     图片的 Content URI
     * @return 压缩后的 Bitmap，失败返回 null
     */
    public static Bitmap loadAndCompressImage(Context context, Uri uri) {
        if (context == null || uri == null) {
            Log.e(TAG, "Context 或 URI 为 null");
            return null;
        }

        InputStream inputStream = null;
        try {
            ContentResolver contentResolver = context.getContentResolver();
            inputStream = contentResolver.openInputStream(uri);

            if (inputStream == null) {
                Log.e(TAG, "无法打开 URI: " + uri);
                return null;
            }

            // 第一步：获取原始图片尺寸
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;

            Log.d(TAG, "原始图片尺寸: " + originalWidth + "x" + originalHeight);

            // 重新打开输入流（因为前面已经读取过）
            inputStream.close();
            inputStream = contentResolver.openInputStream(uri);

            // 第二步：计算采样率
            int sampleSize = calculateInSampleSize(originalWidth, originalHeight, MAX_WIDTH, MAX_HEIGHT);

            // 第三步：以计算的采样率加载压缩图片
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            Bitmap compressedBitmap = BitmapFactory.decodeStream(inputStream, null, options);

            if (compressedBitmap == null) {
                Log.e(TAG, "解码 Bitmap 失败");
                return null;
            }

            int compressedWidth = compressedBitmap.getWidth();
            int compressedHeight = compressedBitmap.getHeight();

            Log.d(TAG, "压缩后图片尺寸: " + compressedWidth + "x" + compressedHeight);
            Log.d(TAG, "采样率: " + sampleSize);

            return compressedBitmap;

        } catch (Exception e) {
            Log.e(TAG, "加载或压缩图片失败: " + e.getMessage(), e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "关闭输入流失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 计算 Bitmap 的采样率
     * 采样率决定了 Bitmap 的压缩程度
     *
     * @param width       原始宽度
     * @param height      原始高度
     * @param reqWidth    目标宽度
     * @param reqHeight   目标高度
     * @return 采样率（inSampleSize）
     */
    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int sampleSize = 1;

        // 如果原始尺寸超过目标尺寸，则进行压缩
        if (width > reqWidth || height > reqHeight) {
            // 计算宽度的采样率
            final int widthRatio = Math.round((float) width / reqWidth);
            // 计算高度的采样率
            final int heightRatio = Math.round((float) height / reqHeight);

            // 取较大的采样率
            sampleSize = Math.max(widthRatio, heightRatio);
        }

        return sampleSize;
    }

    /**
     * 直接压缩 Bitmap 对象
     *
     * @param bitmap 输入的 Bitmap
     * @return 压缩后的 Bitmap，失败返回输入的 Bitmap
     */
    public static Bitmap compressBitmap(Bitmap bitmap) {
        return compressBitmap(bitmap, MAX_WIDTH, MAX_HEIGHT);
    }

    /**
     * 按指定尺寸压缩 Bitmap
     *
     * @param bitmap   输入的 Bitmap
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @return 压缩后的 Bitmap
     */
    public static Bitmap compressBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap 为 null");
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // 如果已经在限制内，直接返回
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        try {
            // 计算缩放比例
            float scaleWidth = (float) maxWidth / width;
            float scaleHeight = (float) maxHeight / height;
            float scale = Math.min(scaleWidth, scaleHeight);

            int newWidth = (int) (width * scale);
            int newHeight = (int) (height * scale);

            Log.d(TAG, "缩放比例: " + scale + ", 新尺寸: " + newWidth + "x" + newHeight);

            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

        } catch (Exception e) {
            Log.e(TAG, "压缩 Bitmap 失败: " + e.getMessage(), e);
            return bitmap;
        }
    }

    /**
     * 获取 Bitmap 的内存大小（字节）
     *
     * @param bitmap Bitmap 对象
     * @return 字节数
     */
    public static long getBitmapSize(Bitmap bitmap) {
        if (bitmap == null) {
            return 0;
        }
        return bitmap.getByteCount();
    }

    /**
     * 格式化字节大小为可读字符串
     *
     * @param bytes 字节数
     * @return 格式化后的字符串
     */
    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
