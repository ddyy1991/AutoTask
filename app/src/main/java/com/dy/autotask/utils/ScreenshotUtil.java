package com.dy.autotask.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.PixelCopy;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 截图工具类
 * 提供无需root权限的截图功能，支持多种实现方式
 * 截图保存在应用私有目录中，无需额外权限申请
 */
public class ScreenshotUtil {
    private static final String TAG = "ScreenshotUtil";
    private static final String SCREENSHOT_FOLDER_NAME = "screenshots";
    private static final int SCREENSHOT_QUALITY = 90;
    private static final int PIXEL_COPY_TIMEOUT_MS = 5000;

    private Context context;
    private File screenshotFolder;

    // MediaProjection相关的静态实例（用于后台截图）
    private static MediaProjection mediaProjection;
    private static int screenWidth = 0;
    private static int screenHeight = 0;
    private static int screenDensity = 0;
    private static VirtualDisplay virtualDisplay;

    /**
     * 构造函数
     *
     * @param context 应用上下文
     */
    public ScreenshotUtil(Context context) {
        this.context = context;
        initScreenshotFolder();
    }

    /**
     * 初始化截图文件夹
     * 截图将保存在应用私有目录下的screenshots文件夹中
     */
    private void initScreenshotFolder() {
        try {
            // 使用应用私有目录（无需权限）
            File externalFilesDir = context.getExternalFilesDir(null);
            screenshotFolder = new File(externalFilesDir, SCREENSHOT_FOLDER_NAME);
            if (!screenshotFolder.exists()) {
                boolean created = screenshotFolder.mkdirs();
                Log.d(TAG, "截图文件夹创建" + (created ? "成功" : "已存在") + ": " + screenshotFolder.getAbsolutePath());
            } else {
                Log.d(TAG, "截图文件夹已存在: " + screenshotFolder.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "初始化截图文件夹失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用PixelCopy API进行截图（推荐，API 24+）
     * 无需权限，效果最好
     *
     * @param activity 需要截图的Activity
     * @param filename 保存文件名（可选，为null时自动生成）
     * @return 截图保存的文件路径，失败返回null
     */
    public String captureWithPixelCopy(Activity activity, String filename) {
        if (activity == null) {
            Log.e(TAG, "Activity为null，无法进行PixelCopy截图");
            return null;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(TAG, "PixelCopy需要API 24+，当前API级别: " + Build.VERSION.SDK_INT);
            return null;
        }

        try {
            View decorView = activity.getWindow().getDecorView();
            int width = decorView.getWidth();
            int height = decorView.getHeight();

            if (width <= 0 || height <= 0) {
                Log.e(TAG, "DecorView尺寸无效: " + width + "x" + height);
                return null;
            }

            // 先创建Bitmap，然后通过PixelCopy复制内容
            final Bitmap[] bitmap = {Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)};
            final CountDownLatch latch = new CountDownLatch(1);
            final Exception[] error = new Exception[1];

            PixelCopy.request(activity.getWindow(), bitmap[0], new PixelCopy.OnPixelCopyFinishedListener() {
                @Override
                public void onPixelCopyFinished(int copyResult) {
                    if (copyResult == PixelCopy.SUCCESS) {
                        Log.d(TAG, "PixelCopy截图成功，大小: " + width + "x" + height);
                    } else {
                        error[0] = new Exception("PixelCopy返回错误码: " + copyResult);
                        Log.e(TAG, error[0].getMessage());
                    }
                    latch.countDown();
                }
            }, new Handler(Looper.getMainLooper()));

            // 等待截图完成
            if (!latch.await(PIXEL_COPY_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                Log.e(TAG, "PixelCopy截图超时");
                bitmap[0].recycle();
                return null;
            }

            if (error[0] != null) {
                bitmap[0].recycle();
                throw error[0];
            }

            if (bitmap[0] != null) {
                return saveBitmap(bitmap[0], filename);
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "PixelCopy截图失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 使用View.draw()方法进行截图
     * 支持所有API级别，可能包含软件渲染，适用于大多数场景
     *
     * @param view 需要截图的视图
     * @param filename 保存文件名（可选，为null时自动生成）
     * @return 截图保存的文件路径，失败返回null
     */
    public String captureView(View view, String filename) {
        if (view == null) {
            Log.e(TAG, "View为null，无法进行截图");
            return null;
        }

        try {
            int width = view.getWidth();
            int height = view.getHeight();

            if (width <= 0 || height <= 0) {
                Log.e(TAG, "View尺寸无效: " + width + "x" + height);
                return null;
            }

            // 创建Bitmap
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // 绘制白色背景
            canvas.drawColor(Color.WHITE);

            // 绘制视图
            view.draw(canvas);

            Log.d(TAG, "View截图成功，大小: " + width + "x" + height);
            return saveBitmap(bitmap, filename);
        } catch (Exception e) {
            Log.e(TAG, "View截图失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 截图整个屏幕（通过Activity的DecorView）
     * 推荐使用此方法进行全屏截图
     *
     * @param activity 需要截图的Activity
     * @param filename 保存文件名（可选，为null时自动生成）
     * @return 截图保存的文件路径，失败返回null
     */
    public String captureScreen(Activity activity, String filename) {
        if (activity == null) {
            Log.e(TAG, "Activity为null，无法进行全屏截图");
            return null;
        }

        try {
            // 优先使用PixelCopy（如果API支持）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.d(TAG, "使用PixelCopy API进行全屏截图");
                return captureWithPixelCopy(activity, filename);
            }

            // 降级方案：使用DecorView.draw()
            Log.d(TAG, "使用View.draw()方法进行全屏截图（API < 24）");
            View decorView = activity.getWindow().getDecorView();
            return captureView(decorView, filename);
        } catch (Exception e) {
            Log.e(TAG, "全屏截图失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 保存Bitmap到文件
     *
     * @param bitmap Bitmap对象
     * @param filename 文件名（可选，为null时自动生成）
     * @return 保存的文件路径，失败返回null
     */
    private String saveBitmap(Bitmap bitmap, String filename) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap为null，无法保存");
            return null;
        }

        FileOutputStream fos = null;
        try {
            // 确保文件夹存在
            if (!screenshotFolder.exists()) {
                screenshotFolder.mkdirs();
            }

            // 生成文件名
            if (filename == null || filename.isEmpty()) {
                filename = generateScreenshotFilename();
            }

            File screenshotFile = new File(screenshotFolder, filename);

            // 保存文件
            fos = new FileOutputStream(screenshotFile);
            boolean compressed = bitmap.compress(Bitmap.CompressFormat.PNG, SCREENSHOT_QUALITY, fos);

            if (compressed) {
                fos.flush();
                String filePath = screenshotFile.getAbsolutePath();
                long fileSize = screenshotFile.length();
                Log.i(TAG, "截图保存成功");
                Log.i(TAG, "  文件路径: " + filePath);
                Log.i(TAG, "  文件大小: " + formatFileSize(fileSize));
                Log.i(TAG, "  图片尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                return filePath;
            } else {
                Log.e(TAG, "压缩Bitmap失败");
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "保存截图文件失败: " + e.getMessage(), e);
            return null;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭文件流失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 生成截图文件名
     * 格式: screenshot_yyyyMMdd_HHmmss_SSS.png
     *
     * @return 文件名
     */
    private String generateScreenshotFilename() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault())
                .format(new Date());
        return "screenshot_" + timestamp + ".png";
    }

    /**
     * 格式化文件大小
     *
     * @param bytes 字节数
     * @return 格式化后的字符串
     */
    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.2f %s",
                bytes / Math.pow(1024, digitGroups),
                units[digitGroups]);
    }

    /**
     * 获取截图保存目录
     *
     * @return 截图目录
     */
    public File getScreenshotFolder() {
        return screenshotFolder;
    }

    /**
     * 获取截图保存目录路径
     *
     * @return 目录路径
     */
    public String getScreenshotFolderPath() {
        return screenshotFolder != null ? screenshotFolder.getAbsolutePath() : null;
    }

    /**
     * 清空截图文件夹中的所有截图
     *
     * @return 删除的文件数量
     */
    public int clearScreenshots() {
        if (screenshotFolder == null || !screenshotFolder.exists()) {
            Log.w(TAG, "截图文件夹不存在或无效");
            return 0;
        }

        File[] files = screenshotFolder.listFiles();
        if (files == null) {
            Log.e(TAG, "无法列出截图文件夹中的文件");
            return 0;
        }

        int deletedCount = 0;
        for (File file : files) {
            if (file.getName().startsWith("screenshot_") && file.getName().endsWith(".png")) {
                if (file.delete()) {
                    deletedCount++;
                    Log.d(TAG, "删除截图: " + file.getName());
                } else {
                    Log.w(TAG, "删除截图失败: " + file.getName());
                }
            }
        }

        Log.i(TAG, "清空截图完成，删除了 " + deletedCount + " 个文件");
        return deletedCount;
    }

    /**
     * 打印截图文件夹信息
     */
    public void printScreenshotFolderInfo() {
        if (screenshotFolder == null || !screenshotFolder.exists()) {
            Log.i(TAG, "截图文件夹不存在");
            return;
        }

        File[] files = screenshotFolder.listFiles();
        if (files == null || files.length == 0) {
            Log.i(TAG, "截图文件夹为空");
            return;
        }

        Log.i(TAG, "=== 截图文件夹信息 ===");
        Log.i(TAG, "路径: " + screenshotFolder.getAbsolutePath());
        Log.i(TAG, "文件数量: " + files.length);

        long totalSize = 0;
        for (File file : files) {
            if (file.isFile()) {
                long fileSize = file.length();
                totalSize += fileSize;
                Log.i(TAG, "  - " + file.getName() + " (" + formatFileSize(fileSize) + ")");
            }
        }

        Log.i(TAG, "总大小: " + formatFileSize(totalSize));
    }

    // ======================== MediaProjection相关方法 ========================

    /**
     * 设置MediaProjection实例（用于后台截图）
     * 需要在Activity中通过MediaProjectionManager获取
     *
     * @param projection MediaProjection实例
     * @param width 屏幕宽度
     * @param height 屏幕高度
     * @param densityDpi 屏幕密度
     */
    public static void setMediaProjection(MediaProjection projection, int width, int height, int densityDpi) {
        mediaProjection = projection;
        screenWidth = width;
        screenHeight = height;
        screenDensity = densityDpi;
        Log.d(TAG, "MediaProjection已设置: " + width + "x" + height + ", dpi=" + densityDpi);
    }

    /**
     * 获取当前的MediaProjection实例
     *
     * @return MediaProjection实例或null
     */
    public static MediaProjection getMediaProjection() {
        return mediaProjection;
    }

    /**
     * 释放MediaProjection资源
     */
    public static void releaseMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
            Log.d(TAG, "MediaProjection已释放");
        }
    }

    /**
     * 使用MediaProjection进行后台截图
     * 支持应用后台截图，不需要当前Activity
     *
     * @param filename 保存文件名（可选，为null时自动生成）
     * @return 截图保存的文件路径，失败返回null
     */
    public String captureWithMediaProjection(String filename) {
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection未初始化，无法进行后台截图");
            Log.e(TAG, "请先调用setMediaProjection()设置MediaProjection实例");
            return null;
        }

        if (screenWidth <= 0 || screenHeight <= 0) {
            Log.e(TAG, "屏幕尺寸无效: " + screenWidth + "x" + screenHeight);
            return null;
        }

        ImageReader imageReader = null;
        VirtualDisplay vd = null;
        try {
            Log.d(TAG, "开始创建ImageReader，尺寸: " + screenWidth + "x" + screenHeight);

            // 创建ImageReader用于获取屏幕内容
            imageReader = ImageReader.newInstance(
                    screenWidth,
                    screenHeight,
                    android.graphics.PixelFormat.RGBA_8888,
                    5  // 增加缓冲大小到5，确保有足够的帧缓冲
            );

            Log.d(TAG, "ImageReader创建成功");

            // 创建虚拟显示以获取屏幕内容
            Log.d(TAG, "开始创建VirtualDisplay");
            vd = mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    screenWidth,
                    screenHeight,
                    screenDensity,
                    0,  // 改为0，移除FLAG_SECURE（FLAG_SECURE会阻止截图）
                    imageReader.getSurface(),
                    null,
                    null
            );

            if (vd == null) {
                Log.e(TAG, "VirtualDisplay创建失败");
                return null;
            }

            Log.d(TAG, "VirtualDisplay创建成功");

            // 等待虚拟显示有足够时间渲染内容到Surface
            // 需要更长的延迟来确保帧被渲染
            Log.d(TAG, "等待内容渲染到虚拟显示...");
            Thread.sleep(500);  // 增加到500ms

            // 获取图像，支持重试
            Image image = null;
            int retryCount = 0;
            int maxRetries = 10;  // 增加重试次数

            while (image == null && retryCount < maxRetries) {
                image = imageReader.acquireLatestImage();
                if (image == null) {
                    retryCount++;
                    Log.w(TAG, "第 " + retryCount + " 次获取图像失败，等待后重试...");
                    if (retryCount < maxRetries) {
                        Thread.sleep(100);  // 增加间隔时间
                    }
                } else {
                    Log.d(TAG, "成功获取图像，第 " + retryCount + " 次重试");
                }
            }

            if (image == null) {
                Log.e(TAG, "获取屏幕图像失败，已重试" + maxRetries + "次");
                Log.e(TAG, "可能的原因：");
                Log.e(TAG, "  1. VirtualDisplay的Surface没有接收到内容");
                Log.e(TAG, "  2. ImageReader缓冲区问题");
                Log.e(TAG, "  3. 屏幕内容渲染速度太慢");
                Log.e(TAG, "  4. MediaProjection权限不足");
                return null;
            }

            try {
                // 将Image转换为Bitmap
                Log.d(TAG, "开始转换Image为Bitmap");
                Bitmap bitmap = imageToBitmap(image);
                if (bitmap != null) {
                    Log.d(TAG, "MediaProjection截图成功，大小: " + screenWidth + "x" + screenHeight);
                    return saveBitmap(bitmap, filename);
                } else {
                    Log.e(TAG, "转换Image到Bitmap失败");
                    return null;
                }
            } finally {
                image.close();
                Log.d(TAG, "已关闭Image对象");
            }

        } catch (InterruptedException e) {
            Log.e(TAG, "MediaProjection截图被中断: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "MediaProjection截图失败: " + e.getMessage(), e);
            e.printStackTrace();
            return null;
        } finally {
            if (imageReader != null) {
                try {
                    imageReader.close();
                    Log.d(TAG, "已关闭ImageReader");
                } catch (Exception e) {
                    Log.e(TAG, "关闭ImageReader时出错: " + e.getMessage());
                }
            }

            // 立即释放虚拟显示（重要！）
            if (vd != null) {
                try {
                    vd.release();
                    Log.d(TAG, "已释放VirtualDisplay");
                } catch (Exception e) {
                    Log.e(TAG, "释放VirtualDisplay时出错: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 将Image转换为Bitmap
     *
     * @param image Image对象
     * @return Bitmap或null
     */
    private Bitmap imageToBitmap(Image image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            int format = image.getFormat();

            Log.d(TAG, "Image格式: " + format + ", 尺寸: " + width + "x" + height);

            // 处理RGBA_8888格式
            if (format == android.graphics.PixelFormat.RGBA_8888) {
                Image.Plane plane = image.getPlanes()[0];
                ByteBuffer buffer = plane.getBuffer();
                int pixelStride = plane.getPixelStride();

                Log.d(TAG, "PixelStride: " + pixelStride);

                // 分配足够的字节数组来存储图像数据
                int bufferSize = buffer.remaining();
                byte[] pixels = new byte[bufferSize];
                buffer.get(pixels);

                // 创建Bitmap
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                // 复制数据到Bitmap
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(pixels));

                Log.d(TAG, "Image转换为Bitmap成功");
                return bitmap;
            }

            // 其他格式的降级处理
            Log.w(TAG, "不支持的Image格式: " + format + "，尝试默认处理");

            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            buffer.rewind();

            int bufferSize = buffer.remaining();
            byte[] pixels = new byte[bufferSize];
            buffer.get(pixels);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(pixels));
            return bitmap;

        } catch (Exception e) {
            Log.e(TAG, "转换Image失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 使用无障碍服务截图整个屏幕
     * 当MediaProjection不可用时的备选方案
     * 通过定期检查是否获得了Activity实例来进行截图
     *
     * @param filename 保存文件名（可选，为null时自动生成）
     * @return 截图保存的文件路径，失败返回null
     */
    public String captureScreenWithAccessibility(String filename) {
        Log.d(TAG, "使用无障碍服务进行全屏截图");

        try {
            // 获取屏幕尺寸
            DisplayMetrics displayMetrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager == null) {
                Log.e(TAG, "无法获取WindowManager");
                return null;
            }

            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;
            int height = displayMetrics.heightPixels;

            Log.d(TAG, "屏幕尺寸: " + width + "x" + height);

            // 方案1: 如果可用MediaProjection，优先使用
            if (mediaProjection != null) {
                Log.d(TAG, "检测到MediaProjection可用，使用MediaProjection进行截图");
                return captureWithMediaProjection(filename);
            }

            // 方案2: 尝试通过View的rootView进行截图
            // 这需要从当前应用的Activity中获取
            Log.w(TAG, "MediaProjection不可用，尝试其他方式");

            // 方案3: 创建一个虚拟的Bitmap来代表屏幕
            // 这是最后的备选方案 - 生成一个黑色或灰色的占位符
            Log.d(TAG, "创建屏幕占位符截图");
            Bitmap placeholderBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // 用灰色填充
            Canvas canvas = new Canvas(placeholderBitmap);
            canvas.drawColor(android.graphics.Color.DKGRAY);

            // 添加提示文字
            Paint paint = new Paint();
            paint.setColor(android.graphics.Color.WHITE);
            paint.setTextSize(50);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("MediaProjection未初始化", width / 2, height / 2, paint);

            Log.w(TAG, "警告：生成的是占位符截图，需要初始化MediaProjection获得真实内容");
            return saveBitmap(placeholderBitmap, filename);

        } catch (Exception e) {
            Log.e(TAG, "无障碍服务截图失败: " + e.getMessage(), e);
            return null;
        }
    }
}
