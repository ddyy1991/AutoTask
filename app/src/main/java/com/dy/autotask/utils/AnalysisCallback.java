package com.dy.autotask.utils;

/**
 * 图片分析异步回调接口
 * 用于处理异步分析结果
 */
public interface AnalysisCallback {

    /**
     * 分析成功时回调
     *
     * @param result 分析结果文本
     */
    void onSuccess(String result);

    /**
     * 分析失败时回调
     *
     * @param e 异常信息
     */
    void onError(Exception e);
}
