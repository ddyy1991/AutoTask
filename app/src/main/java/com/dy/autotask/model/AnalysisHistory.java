package com.dy.autotask.model;

/**
 * 分析历史记录数据模型
 * 用于保存分析结果的历史记录
 */
public class AnalysisHistory {

    private long timestamp;          // 时间戳
    private String prompt;           // 分析提示词
    private String result;           // 分析结果
    private String imagePath;        // 图片路径（可选）
    private String imageBase64;      // 图片 Base64（用于显示缩略图）

    public AnalysisHistory() {
    }

    public AnalysisHistory(long timestamp, String prompt, String result) {
        this.timestamp = timestamp;
        this.prompt = prompt;
        this.result = result;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    /**
     * 获取格式化的时间显示文本
     *
     * @return 时间文本，如 "14:30:45"
     */
    public String getTimeText() {
        long seconds = timestamp / 1000;
        long hours = (seconds / 3600) % 24;
        long minutes = (seconds / 60) % 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * 获取提示词的摘要（最多 50 字）
     *
     * @return 提示词摘要
     */
    public String getPromptSummary() {
        if (prompt == null || prompt.isEmpty()) {
            return "（无提示词）";
        }
        return prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt;
    }

    /**
     * 获取结果的摘要（最多 100 字）
     *
     * @return 结果摘要
     */
    public String getResultSummary() {
        if (result == null || result.isEmpty()) {
            return "（无结果）";
        }
        return result.length() > 100 ? result.substring(0, 100) + "..." : result;
    }

    @Override
    public String toString() {
        return "AnalysisHistory{" +
                "timestamp=" + timestamp +
                ", prompt='" + prompt + '\'' +
                ", resultLength=" + (result != null ? result.length() : 0) +
                '}';
    }
}
