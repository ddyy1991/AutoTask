package com.dy.autotask;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dy.autotask.ui.imageanalysis.ImageAnalysisViewModel;
import com.dy.autotask.utils.ImageCompressUtil;

/**
 * 图片分析 Activity
 * 用户在此页面选择图片、输入提示词、查看分析结果
 */
public class ImageAnalysisActivity extends AppCompatActivity {

    private static final String TAG = "ImageAnalysisActivity";

    // UI 组件
    private Button btnUploadImage;
    private ImageView ivPreview;
    private EditText etPrompt;
    private Button btnSubmit;
    private Button btnBack;
    private Button btnCopyResult;
    private TextView tvResult;
    private TextView tvError;
    private View loadingContainer;
    private View imagePrevContainer;

    // ViewModel
    private ImageAnalysisViewModel viewModel;

    // 图片选择器
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // 当前选中的 Bitmap
    private Bitmap selectedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_analysis);

        Log.d(TAG, "Activity 已创建");

        // 初始化 UI 组件
        initViews();

        // 初始化 ViewModel
        viewModel = new ViewModelProvider(this).get(ImageAnalysisViewModel.class);

        // 初始化图片选择器
        initImagePicker();

        // 设置事件监听
        setupEventListeners();

        // 观察 ViewModel 数据变化
        observeViewModel();
    }

    /**
     * 初始化 UI 组件
     */
    private void initViews() {
        btnUploadImage = findViewById(R.id.btn_upload_image);
        ivPreview = findViewById(R.id.iv_preview);
        etPrompt = findViewById(R.id.et_prompt);
        btnSubmit = findViewById(R.id.btn_submit);
        btnBack = findViewById(R.id.btn_back);
        btnCopyResult = findViewById(R.id.btn_copy_result);
        tvResult = findViewById(R.id.tv_result);
        tvError = findViewById(R.id.tv_error);
        loadingContainer = findViewById(R.id.loading_container);
        imagePrevContainer = findViewById(R.id.image_preview_container);
    }

    /**
     * 初始化图片选择器
     */
    private void initImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            Log.d(TAG, "选择的图片 URI: " + imageUri);
                            loadImage(imageUri);
                        }
                    }
                }
        );
    }

    /**
     * 从 Content URI 加载图片
     *
     * @param uri 图片的 Content URI
     */
    private void loadImage(Uri uri) {
        Log.d(TAG, "开始加载图片...");

        // 在后台线程加载和压缩图片
        new Thread(() -> {
            Bitmap bitmap = ImageCompressUtil.loadAndCompressImage(this, uri);

            if (bitmap == null) {
                Log.e(TAG, "加载图片失败");
                runOnUiThread(() -> {
                    Toast.makeText(ImageAnalysisActivity.this, "加载图片失败", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            // 保存到成员变量
            selectedBitmap = bitmap;

            // 更新 UI（必须在主线程）
            runOnUiThread(() -> {
                // 在主线程中更新 ViewModel
                viewModel.setSelectedImage(bitmap);
                // 隐藏上传按钮
                btnUploadImage.setVisibility(View.GONE);
                // 显示预览容器
                imagePrevContainer.setVisibility(View.VISIBLE);
                // 设置图片到预览
                ivPreview.setImageBitmap(bitmap);
                Log.d(TAG, "图片已加载并显示");
            });
        }).start();
    }

    /**
     * 打开图片选择器
     */
    private void openImagePicker() {
        Log.d(TAG, "打开图片选择器");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    /**
     * 设置事件监听
     */
    private void setupEventListeners() {
        // 返回按钮
        btnBack.setOnClickListener(v -> {
            Log.d(TAG, "用户点击了返回按钮");
            finish();
        });

        // 上传图片按钮点击（第一次选择图片）
        btnUploadImage.setOnClickListener(v -> {
            Log.d(TAG, "用户点击了上传图片按钮");
            openImagePicker();
        });

        // 图片预览容器点击（重新选择图片）
        imagePrevContainer.setOnClickListener(v -> {
            Log.d(TAG, "用户点击了图片预览，重新选择");
            openImagePicker();
        });

        // 提交分析按钮
        btnSubmit.setOnClickListener(v -> {
            Log.d(TAG, "用户点击了提交按钮");
            submitAnalysis();
        });

        // 复制结果按钮
        btnCopyResult.setOnClickListener(v -> {
            Log.d(TAG, "用户点击了复制按钮");
            copyResultToClipboard();
        });
    }

    /**
     * 提交分析
     */
    private void submitAnalysis() {
        if (selectedBitmap == null) {
            Log.w(TAG, "未选择图片");
            Toast.makeText(this, "请先选择图片", Toast.LENGTH_SHORT).show();
            return;
        }

        String prompt = etPrompt.getText().toString().trim();
        Log.d(TAG, "提交分析，提示词: " + (prompt.isEmpty() ? "（默认）" : prompt));

        // 调用 ViewModel 进行分析
        viewModel.analyzeImage(selectedBitmap, prompt.isEmpty() ? null : prompt);
    }

    /**
     * 复制结果到剪贴板
     */
    private void copyResultToClipboard() {
        String result = tvResult.getText().toString();

        if (result.isEmpty() || result.equals("分析结果将显示在这里")) {
            Log.w(TAG, "没有结果可复制");
            Toast.makeText(this, "没有分析结果", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                android.content.ClipData clip = android.content.ClipData.newPlainText("分析结果", result);
                clipboard.setPrimaryClip(clip);
                Log.d(TAG, "结果已复制到剪贴板");
                Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "复制失败: " + e.getMessage(), e);
            Toast.makeText(this, "复制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 观察 ViewModel 中的数据变化
     */
    private void observeViewModel() {
        // 观察选中的图片
        viewModel.getSelectedImage().observe(this, bitmap -> {
            if (bitmap != null) {
                Log.d(TAG, "ViewModel 中的图片已更新");
                // 图片加载时已经更新 UI，这里可以做额外处理
            }
        });

        // 观察分析结果
        viewModel.getAnalysisResult().observe(this, result -> {
            if (result != null && !result.isEmpty()) {
                Log.d(TAG, "收到分析结果，长度: " + result.length());
                tvResult.setText(result);
                tvResult.setVisibility(View.VISIBLE);
                tvError.setVisibility(View.GONE);
            }
        });

        // 观察加载状态
        viewModel.getIsLoading().observe(this, isLoading -> {
            Log.d(TAG, "加载状态: " + isLoading);
            if (isLoading) {
                loadingContainer.setVisibility(View.VISIBLE);
                btnSubmit.setEnabled(false);
                tvResult.setVisibility(View.GONE);
                tvError.setVisibility(View.GONE);
            } else {
                loadingContainer.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
            }
        });

        // 观察错误信息
        viewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Log.e(TAG, "分析错误: " + errorMsg);
                tvError.setText(errorMsg);
                tvError.setVisibility(View.VISIBLE);
                tvResult.setVisibility(View.GONE);
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            } else {
                tvError.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity 已销毁");

        // 回收 Bitmap
        if (selectedBitmap != null && !selectedBitmap.isRecycled()) {
            selectedBitmap.recycle();
            selectedBitmap = null;
        }
    }
}
