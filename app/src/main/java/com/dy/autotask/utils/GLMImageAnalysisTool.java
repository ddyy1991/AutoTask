package com.dy.autotask.utils;

import android.graphics.Bitmap;
import android.util.Log;

import com.dy.autotask.BuildConfig;
import com.dy.autotask.model.ImageAnalysisRequest;
import com.dy.autotask.model.ImageAnalysisResponse;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 智谱 GLM 图片分析工具类（核心工具）
 *
 * 这是整个项目的 AI 分析接口，提供给其他模块使用
 * 支持同步和异步两种调用方式
 *
 * 使用方式：
 * // 同步调用
 * String result = GLMImageAnalysisTool.analyzeImage(bitmap, "分析这张图片");
 *
 * // 异步调用
 * GLMImageAnalysisTool.analyzeImageAsync(bitmap, "分析这张图片",
 *     new AnalysisCallback() {
 *         public void onSuccess(String result) { ... }
 *         public void onError(Exception e) { ... }
 *     });
 */
public class GLMImageAnalysisTool {

    private static final String TAG = "GLMImageAnalysisTool";

    // 智谱 API 端点
    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    // 模型名称
    private static final String MODEL = "glm-4.6v-flash";

    // 超时时间（秒）
    private static final int TIMEOUT_SECONDS = 30;

    // OkHttp 客户端（单例）
    private static OkHttpClient okHttpClient;

    // 异步执行器
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Gson 实例
    private static final Gson gson = new Gson();

    static {
        // 初始化 OkHttp 客户端
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 同步分析图片（阻塞调用）
     *
     * @param bitmap 输入的 Bitmap 图片
     * @param prompt 分析提示词（可选，为 null 时使用默认提示词）
     * @return 分析结果字符串，失败返回 null
     * @throws IOException 网络错误
     */
    public static String analyzeImage(Bitmap bitmap, String prompt) throws IOException {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap 为 null");
            throw new IllegalArgumentException("Bitmap 不能为 null");
        }

        Log.d(TAG, "开始同步分析图片...");

        try {
            // 1. 压缩图片
            Log.d(TAG, "压缩图片...");
            Bitmap compressedBitmap = ImageCompressUtil.compressBitmap(bitmap);
            if (compressedBitmap == null) {
                throw new IOException("图片压缩失败");
            }
            Log.d(TAG, "压缩完成，尺寸: " + compressedBitmap.getWidth() + "x" + compressedBitmap.getHeight());

            // 2. 转换为 Base64
            Log.d(TAG, "转换为 Base64...");
            String base64Image = Base64Util.bitmapToBase64(compressedBitmap);
            if (base64Image == null) {
                throw new IOException("Base64 编码失败");
            }
            Log.d(TAG, "Base64 编码完成，长度: " + base64Image.length());

            // 3. 构建请求
            Log.d(TAG, "构建 API 请求...");
            String requestJson = buildRequestJson(base64Image, prompt);
            Log.d(TAG, "请求体大小: " + requestJson.length() + " 字节");

            // 4. 发送 HTTP 请求
            Log.d(TAG, "发送请求到 GLM API...");
            String responseJson = sendRequest(requestJson);
            if (responseJson == null) {
                throw new IOException("API 响应为空");
            }

            // 5. 解析响应
            Log.d(TAG, "解析 API 响应...");
            String result = parseResponse(responseJson);
            if (result == null) {
                throw new IOException("无法解析 API 响应");
            }

            Log.d(TAG, "分析完成，结果长度: " + result.length());
            return result;

        } catch (Exception e) {
            Log.e(TAG, "分析失败: " + e.getMessage(), e);
            throw new IOException("分析图片失败: " + e.getMessage(), e);
        }
    }

    /**
     * 异步分析图片（非阻塞调用，推荐）
     *
     * @param bitmap 输入的 Bitmap 图片
     * @param prompt 分析提示词
     * @param callback 结果回调
     */
    public static void analyzeImageAsync(Bitmap bitmap, String prompt, AnalysisCallback callback) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap 为 null");
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Bitmap 不能为 null"));
            }
            return;
        }

        if (callback == null) {
            Log.w(TAG, "回调为 null");
            return;
        }

        Log.d(TAG, "启动异步分析任务...");

        // 在后台线程执行分析
        executorService.submit(() -> {
            try {
                String result = analyzeImage(bitmap, prompt);
                callback.onSuccess(result);
            } catch (Exception e) {
                Log.e(TAG, "异步分析失败: " + e.getMessage(), e);
                callback.onError(e);
            }
        });
    }

    /**
     * 构建 API 请求 JSON
     *
     * @param base64Image Base64 编码的图片
     * @param prompt 分析提示词
     * @return JSON 字符串
     */
    private static String buildRequestJson(String base64Image, String prompt) {
        // 使用默认提示词
        if (prompt == null || prompt.isEmpty()) {
            prompt = "请分析这张图片的内容";
        }

        // 构建请求对象
        ImageAnalysisRequest request = new ImageAnalysisRequest(MODEL);

        ImageAnalysisRequest.Message message = new ImageAnalysisRequest.Message();

        // 添加文本内容
        message.addContent(new ImageAnalysisRequest.Content(prompt));

        // 添加图片内容
        ImageAnalysisRequest.ImageUrl imageUrl = new ImageAnalysisRequest.ImageUrl(base64Image);
        message.addContent(new ImageAnalysisRequest.Content(imageUrl));

        request.addMessage(message);

        // 转换为 JSON
        String jsonRequest = gson.toJson(request);

        Log.d(TAG, "请求结构: model=" + request.getModel() + ", messages count=" + request.getMessages().size());
        Log.d(TAG, "第一条消息: role=" + message.getRole() + ", content items=" + message.getContent().size());
        for (int i = 0; i < message.getContent().size(); i++) {
            ImageAnalysisRequest.Content content = message.getContent().get(i);
            Log.d(TAG, "  - Content " + i + ": type=" + content.getType() + ", has text=" + (content.getText() != null) + ", has image=" + (content.getImageUrl() != null));
        }

        return jsonRequest;
    }

    /**
     * 发送 HTTP 请求
     *
     * @param requestJson 请求 JSON
     * @return 响应 JSON 字符串
     * @throws IOException 网络错误
     */
    private static String sendRequest(String requestJson) throws IOException {
        String apiKey = BuildConfig.GLM_API_KEY;
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("default_key")) {
            Log.e(TAG, "API Key 未配置或无效");
            throw new IOException("API Key 未配置");
        }

        Log.d(TAG, "API Key 已读取（长度: " + apiKey.length() + "）");
        Log.d(TAG, "API Key 格式: " + (apiKey.contains(".") ? "✓ 正确格式（包含点号）" : "✗ 可能格式错误"));

        // 构建请求
        RequestBody body = RequestBody.create(requestJson, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        // 执行请求
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                Log.e(TAG, "API 返回错误: " + response.code() + " - " + response.message());
                Log.e(TAG, "错误体: " + errorBody);
                throw new IOException("API 错误: " + response.code() + " " + response.message());
            }

            String responseBody = response.body() != null ? response.body().string() : null;
            if (responseBody == null) {
                throw new IOException("响应体为空");
            }

            Log.d(TAG, "API 请求成功，响应体长度: " + responseBody.length());
            Log.d(TAG, "完整响应内容: " + responseBody);
            return responseBody;
        }
    }

    /**
     * 解析 API 响应
     *
     * @param responseJson API 响应 JSON
     * @return 分析结果文本
     * @throws IOException 解析失败
     */
    private static String parseResponse(String responseJson) throws IOException {
        try {
            Log.d(TAG, "开始解析响应，内容长度: " + responseJson.length());
            Log.d(TAG, "响应内容摘要: " + responseJson.substring(0, Math.min(200, responseJson.length())));

            ImageAnalysisResponse response = gson.fromJson(responseJson, ImageAnalysisResponse.class);

            // 检查响应码
            if (response == null) {
                Log.e(TAG, "响应对象为空");
                throw new IOException("响应对象为空");
            }

            // 新格式的 API 响应没有 code 和 msg，直接包含 choices
            // 旧格式的响应有 code 和 msg，内容在 data 中

            // 检查是否有 code 字段（如果有表示旧格式）
            if (response.getCode() != 0) {
                Log.d(TAG, "旧格式响应 - 响应码: " + response.getCode());
                Log.d(TAG, "旧格式响应 - 响应消息: " + response.getMsg());

                if (response.getCode() != 200) {
                    Log.e(TAG, "API 返回非成功码: " + response.getCode() + " - " + response.getMsg());
                    throw new IOException("API 错误 (" + response.getCode() + "): " + response.getMsg());
                }
            } else {
                Log.d(TAG, "新格式响应 - 直接包含 choices");
            }

            // 提取分析结果（getChoices() 会自动处理两种格式）
            List<ImageAnalysisResponse.Choice> choicesList = response.getChoices();
            if (choicesList == null || choicesList.isEmpty()) {
                Log.e(TAG, "API 响应中没有 choices 数据");
                throw new IOException("API 响应中没有结果");
            }

            Log.d(TAG, "choices 数量: " + choicesList.size());

            ImageAnalysisResponse.Choice choice = choicesList.get(0);
            if (choice.getMessage() == null) {
                Log.e(TAG, "消息对象为空");
                throw new IOException("消息对象为空");
            }

            String content = choice.getMessage().getContent();
            if (content == null || content.isEmpty()) {
                Log.e(TAG, "分析结果为空");
                throw new IOException("分析结果为空");
            }

            Log.d(TAG, "成功解析 API 响应，内容长度: " + content.length());
            return content;

        } catch (com.google.gson.JsonSyntaxException e) {
            Log.e(TAG, "JSON 解析异常: " + e.getMessage());
            Log.e(TAG, "原始响应: " + responseJson);
            throw new IOException("JSON 解析失败: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "解析响应失败: " + e.getMessage(), e);
            throw new IOException("解析响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 关闭执行器（应用退出时调用）
     */
    public static void shutdown() {
        Log.d(TAG, "关闭异步执行器...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
