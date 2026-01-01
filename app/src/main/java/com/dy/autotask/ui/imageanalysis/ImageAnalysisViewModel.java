package com.dy.autotask.ui.imageanalysis;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.dy.autotask.utils.AnalysisCallback;
import com.dy.autotask.utils.GLMImageAnalysisTool;

/**
 * 图片分析的 ViewModel
 * 管理 UI 数据和业务逻辑
 */
public class ImageAnalysisViewModel extends ViewModel {

    private static final String TAG = "ImageAnalysisViewModel";

    // UI 数据
    private final MutableLiveData<Bitmap> selectedImage = new MutableLiveData<>();
    private final MutableLiveData<String> analysisPrompt = new MutableLiveData<>();
    private final MutableLiveData<String> analysisResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<Bitmap> getSelectedImage() {
        return selectedImage;
    }

    public void setSelectedImage(Bitmap bitmap) {
        Log.d(TAG, "设置选中的图片");
        selectedImage.setValue(bitmap);
    }

    public LiveData<String> getAnalysisPrompt() {
        return analysisPrompt;
    }

    public void setAnalysisPrompt(String prompt) {
        analysisPrompt.setValue(prompt);
    }

    public LiveData<String> getAnalysisResult() {
        return analysisResult;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * 分析图片
     *
     * @param bitmap 输入的 Bitmap
     * @param prompt 分析提示词
     */
    public void analyzeImage(Bitmap bitmap, String prompt) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap 为 null");
            errorMessage.setValue("请先选择图片");
            return;
        }

        Log.d(TAG, "开始分析图片，提示词: " + (prompt != null ? prompt : "（默认）"));

        // 清空之前的结果和错误信息
        analysisResult.setValue(null);
        errorMessage.setValue(null);

        // 显示加载状态
        isLoading.setValue(true);

        // 使用异步方式调用 GLMImageAnalysisTool
        GLMImageAnalysisTool.analyzeImageAsync(bitmap, prompt, new AnalysisCallback() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "分析成功，结果长度: " + result.length());
                // 使用 postValue 在主线程中更新 LiveData（避免后台线程问题）
                analysisResult.postValue(result);
                isLoading.postValue(false);
                errorMessage.postValue(null);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "分析失败: " + e.getMessage(), e);
                // 使用 postValue 在主线程中更新 LiveData（避免后台线程问题）
                analysisResult.postValue(null);
                isLoading.postValue(false);
                errorMessage.postValue("分析失败: " + e.getMessage());
            }
        });
    }

    /**
     * 清空所有数据
     */
    public void clearAll() {
        Log.d(TAG, "清空所有数据");
        selectedImage.setValue(null);
        analysisPrompt.setValue(null);
        analysisResult.setValue(null);
        isLoading.setValue(false);
        errorMessage.setValue(null);
    }

    /**
     * 清空结果（保留图片和提示词）
     */
    public void clearResult() {
        Log.d(TAG, "清空分析结果");
        analysisResult.setValue(null);
        errorMessage.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel 已清除");
        // 关闭后台任务
        GLMImageAnalysisTool.shutdown();
    }
}
