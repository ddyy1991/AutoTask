package com.dy.autotask.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 智谱 GLM API 响应数据模型
 * 支持两种格式：
 * 1. 标准响应格式（有 code 和 msg）
 * 2. 直接响应格式（choices 直接在根层级）
 */
public class ImageAnalysisResponse {

    @SerializedName("code")
    private int code;

    @SerializedName("msg")
    private String msg;

    @SerializedName("data")
    private Data data;

    // 直接在根层级的字段（新格式）
    @SerializedName("choices")
    private List<Choice> choices;

    @SerializedName("created")
    private long created;

    @SerializedName("id")
    private String id;

    @SerializedName("model")
    private String model;

    @SerializedName("object")
    private String object;

    @SerializedName("request_id")
    private String requestId;

    @SerializedName("usage")
    private Usage usage;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public List<Choice> getChoices() {
        // 支持两种格式：从根层级获取，或从 data 中获取
        if (choices != null) {
            return choices;  // 新格式：直接在根层级
        }
        if (data != null && data.getChoices() != null) {
            return data.getChoices();  // 旧格式：在 data 中
        }
        return null;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Usage getUsage() {
        // 支持两种格式
        if (usage != null) {
            return usage;  // 新格式：直接在根层级
        }
        if (data != null) {
            return data.getUsage();  // 旧格式：在 data 中
        }
        return null;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    /**
     * 数据体（旧格式）
     */
    public static class Data {

        @SerializedName("choices")
        private List<Choice> choices;

        @SerializedName("usage")
        private Usage usage;

        public List<Choice> getChoices() {
            return choices;
        }

        public void setChoices(List<Choice> choices) {
            this.choices = choices;
        }

        public Usage getUsage() {
            return usage;
        }

        public void setUsage(Usage usage) {
            this.usage = usage;
        }
    }

    /**
     * 选择项（包含分析结果）
     */
    public static class Choice {

        @SerializedName("index")
        private int index;

        @SerializedName("finish_reason")
        private String finishReason;

        @SerializedName("message")
        private Message message;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }
    }

    /**
     * 消息
     */
    public static class Message {

        @SerializedName("role")
        private String role;

        @SerializedName("content")
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * 使用统计
     */
    public static class Usage {

        @SerializedName("prompt_tokens")
        private int promptTokens;

        @SerializedName("completion_tokens")
        private int completionTokens;

        @SerializedName("total_tokens")
        private int totalTokens;

        public int getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
        }

        public int getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(int completionTokens) {
            this.completionTokens = completionTokens;
        }

        public int getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
        }
    }
}
