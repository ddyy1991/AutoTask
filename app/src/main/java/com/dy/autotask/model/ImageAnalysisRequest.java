package com.dy.autotask.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * 智谱 GLM API 请求数据模型
 * 用于发送图片分析请求
 */
public class ImageAnalysisRequest {

    @SerializedName("model")
    private String model = "glm-4.6v-flash";

    @SerializedName("messages")
    private List<Message> messages;

    public ImageAnalysisRequest() {
        this.messages = new ArrayList<>();
    }

    public ImageAnalysisRequest(String model) {
        this.model = model;
        this.messages = new ArrayList<>();
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }

    /**
     * 消息体
     */
    public static class Message {

        @SerializedName("role")
        private String role = "user";

        @SerializedName("content")
        private List<Content> content;

        public Message() {
            this.content = new ArrayList<>();
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public List<Content> getContent() {
            return content;
        }

        public void setContent(List<Content> content) {
            this.content = content;
        }

        public void addContent(Content c) {
            this.content.add(c);
        }
    }

    /**
     * 内容项
     */
    public static class Content {

        @SerializedName("type")
        private String type;

        @SerializedName("text")
        private String text;

        @SerializedName("image_url")
        private ImageUrl imageUrl;

        // 文本内容构造
        public Content(String text) {
            this.type = "text";
            this.text = text;
        }

        // 图片内容构造
        public Content(ImageUrl imageUrl) {
            this.type = "image_url";
            this.imageUrl = imageUrl;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public ImageUrl getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(ImageUrl imageUrl) {
            this.imageUrl = imageUrl;
        }
    }

    /**
     * 图片 URL
     */
    public static class ImageUrl {

        @SerializedName("url")
        private String url;

        public ImageUrl(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
