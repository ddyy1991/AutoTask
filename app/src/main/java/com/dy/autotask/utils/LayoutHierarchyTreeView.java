package com.dy.autotask.utils;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dy.autotask.R;

/**
 * 布局层次树形视图
 */
public class LayoutHierarchyTreeView {
    private static final String TAG = "LayoutHierarchyTreeView";
    private Context context;
    private LinearLayout container;
    private AccessibilityService accessibilityService;

    public LayoutHierarchyTreeView(Context context, LinearLayout container, AccessibilityService service) {
        this.context = context;
        this.container = container;
        this.accessibilityService = service;
    }

    /**
     * 显示布局层次树
     */
    public void showLayoutHierarchy() {
        AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
        if (rootNode == null) {
            Toast.makeText(context, "无法获取根节点", Toast.LENGTH_SHORT).show();
            return;
        }

        // 清空容器
        container.removeAllViews();

        // 添加根节点
        addNodeToTree(rootNode, container, 0);
    }

    /**
     * 递归添加节点到树形结构
     */
    private void addNodeToTree(AccessibilityNodeInfo node, LinearLayout parentLayout, int depth) {
        if (node == null) {
            return;
        }

        // 创建节点视图
        View nodeView = LayoutInflater.from(context).inflate(R.layout.layout_hierarchy_node_item, null);
        TextView nodeInfoText = nodeView.findViewById(R.id.node_info_text);

        // 设置节点信息
        String nodeInfo = buildNodeInfo(node, depth);
        nodeInfoText.setText(nodeInfo);

        // 设置缩进
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) nodeInfoText.getLayoutParams();
        params.leftMargin = depth * 40; // 每级缩进40dp
        nodeInfoText.setLayoutParams(params);

        // 添加节点点击事件
        nodeView.setOnClickListener(v -> {
            // 显示节点详情
            showNodeDetails(node);
        });

        // 添加到父布局
        parentLayout.addView(nodeView);

        // 递归添加子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = node.getChild(i);
            if (childNode != null) {
                addNodeToTree(childNode, parentLayout, depth + 1);
            }
        }
    }

    /**
     * 构建节点信息文本
     */
    private String buildNodeInfo(AccessibilityNodeInfo node, int depth) {
        StringBuilder info = new StringBuilder();

        // 添加缩进符号
        for (int i = 0; i < depth; i++) {
            info.append("  └─ ");
        }

        // 添加类名
        CharSequence className = node.getClassName();
        if (className != null) {
            String simpleClassName = className.toString();
            int lastDotIndex = simpleClassName.lastIndexOf('.');
            if (lastDotIndex != -1) {
                simpleClassName = simpleClassName.substring(lastDotIndex + 1);
            }
            info.append(simpleClassName);
        } else {
            info.append("Unknown");
        }

        // 添加文本内容
        CharSequence text = node.getText();
        if (!TextUtils.isEmpty(text)) {
            info.append(" [")
                .append(text.toString().replace("\n", " "))
                .append("]");
        }

        return info.toString();
    }

    /**
     * 显示节点详情
     */
    private void showNodeDetails(AccessibilityNodeInfo node) {
        // 这里可以实现显示节点详情的逻辑
        Toast.makeText(context, "节点详情: " + node.getClassName(), Toast.LENGTH_SHORT).show();
    }
}