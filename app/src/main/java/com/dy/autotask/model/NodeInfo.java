package com.dy.autotask.model;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * NodeInfo class representing accessibility node information.
 */
public class NodeInfo {
    private String packageName;
    private String id;
    private String fullId;
    private String idHex;
    private String desc;
    private String text;
    private Rect boundsInScreen;
    private String className;
    private boolean clickable;
    private boolean longClickable;
    private boolean scrollable;
    private int indexInParent;
    private int childCount;
    private int depth;
    private boolean checked;
    private boolean enabled;
    private boolean editable;
    private boolean focusable;
    private boolean checkable;
    private boolean selected;
    private boolean visibleToUser;
    private boolean isSelectedItem; // 标记是否为上个窗口选中的元素
    private NodeInfo parent;
    private List<NodeInfo> children = new ArrayList<>();
    private AccessibilityNodeInfo accessibilityNodeInfo; // 保存原始的AccessibilityNodeInfo

    public NodeInfo(AccessibilityNodeInfo node, NodeInfo parent) {
        this.parent = parent;
        if (node != null) {
            this.accessibilityNodeInfo = node;
            this.packageName = node.getPackageName() != null ? node.getPackageName().toString() : null;
            this.id = getIdFromNode(node);
            this.fullId = getFullIdFromNode(node);
            this.desc = node.getContentDescription() != null ? node.getContentDescription().toString() : null;
            this.text = node.getText() != null ? node.getText().toString() : "";
            this.boundsInScreen = new Rect();
            node.getBoundsInScreen(this.boundsInScreen);
            this.className = node.getClassName() != null ? node.getClassName().toString() : null;
            this.clickable = node.isClickable();
            this.longClickable = node.isLongClickable();
            this.scrollable = node.isScrollable();
            this.indexInParent = node.getParent() != null ? getNodeIndex(node) : -1;
            this.childCount = node.getChildCount();
            this.depth = calculateDepth(node);
            this.checked = node.isChecked();
            this.enabled = node.isEnabled();
            this.editable = node.isEditable();
            this.focusable = node.isFocusable();
            this.checkable = node.isCheckable();
            this.selected = node.isSelected();
            this.visibleToUser = node.isVisibleToUser();
            this.isSelectedItem = false; // 默认不是选中的元素
        }
    }

    private String getIdFromNode(AccessibilityNodeInfo node) {
        // Simplified implementation
        return node.getViewIdResourceName() != null ? 
               node.getViewIdResourceName().substring(node.getViewIdResourceName().lastIndexOf("/") + 1) : 
               null;
    }

    private String getFullIdFromNode(AccessibilityNodeInfo node) {
        return node.getViewIdResourceName();
    }

    private int getNodeIndex(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                if (parent.getChild(i) == node) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int calculateDepth(AccessibilityNodeInfo node) {
        int depth = 0;
        AccessibilityNodeInfo parent = node.getParent();
        while (parent != null) {
            depth++;
            parent = parent.getParent();
        }
        return depth;
    }

    public static NodeInfo capture(AccessibilityNodeInfo root) {
        return capture(root, null);
    }

    private static NodeInfo capture(AccessibilityNodeInfo node, NodeInfo parent) {
        if (node == null) { // 不检查可见性，让调用者决定是否处理
            return null;
        }
        
        // 即使节点不可见，也要创建NodeInfo对象，因为子节点可能可见
        NodeInfo nodeInfo = new NodeInfo(node, parent);
        
        // Recursively capture children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                NodeInfo childInfo = capture(child, nodeInfo);
                if (childInfo != null) {
                    nodeInfo.addChild(childInfo);
                }
            }
        }
        
        return nodeInfo;
    }

    public void addChild(NodeInfo child) {
        // 检查是否已经存在相同的子节点，避免重复添加
        if (!children.contains(child)) {
            children.add(child);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        NodeInfo nodeInfo = (NodeInfo) obj;
        
        // 比较关键属性来判断是否为同一节点
        return Objects.equals(id, nodeInfo.id) &&
               Objects.equals(className, nodeInfo.className) &&
               Objects.equals(text, nodeInfo.text) &&
               Objects.equals(boundsInScreen, nodeInfo.boundsInScreen);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, className, text, boundsInScreen);
    }

    // Getters
    public String getPackageName() { return packageName; }
    public String getId() { return id; }
    public String getFullId() { return fullId; }
    public String getIdHex() { return idHex; }
    public String getDesc() { return desc; }
    public String getText() { return text; }
    public Rect getBoundsInScreen() { return boundsInScreen; }
    public String getClassName() { return className; }
    public boolean getClickable() { return clickable; }
    public boolean getLongClickable() { return longClickable; }
    public boolean getScrollable() { return scrollable; }
    public int getIndexInParent() { return indexInParent; }
    public int getChildCount() { return children != null ? children.size() : 0; }
    public int getDepth() { return depth; }
    public boolean getChecked() { return checked; }
    public boolean getEnabled() { return enabled; }
    public boolean getEditable() { return editable; }
    public boolean getFocusable() { return focusable; }
    public boolean getCheckable() { return checkable; }
    public boolean getSelected() { return selected; }
    public boolean getVisibleToUser() { return visibleToUser; }
    public boolean isSelectedItem() { return isSelectedItem; }
    public void setSelectedItem(boolean selectedItem) { isSelectedItem = selectedItem; }
    
    public AccessibilityNodeInfo getAccessibilityNodeInfo() { return accessibilityNodeInfo; }
    public NodeInfo getParent() { return parent; }
    public List<NodeInfo> getChildren() { return children; }
    public boolean isVisibleToUser() { return visibleToUser; }

    /**
     * Gets simplified class name (without package prefix).
     * @return Simplified class name.
     */
    public String getSimplifiedClassName() {
        if (className == null) {
            return null;
        }
        String simplified = className;
        
        // 移除常见的包名前缀
        String[] prefixes = {
            "android.widget.",
            "android.view.",
            "android.webkit.",
            "android.support.v7.widget.",
            "androidx.recyclerview.widget.",
            "androidx.appcompat.widget.",
            "androidx.viewpager.widget.",
            "androidx.swiperefreshlayout.widget.",
            "com.google.android.material."
        };
        
        for (String prefix : prefixes) {
            if (simplified.startsWith(prefix)) {
                simplified = simplified.substring(prefix.length());
                break;
            }
        }
        
        return simplified;
    }

    @Override
    public String toString() {
        String simplifiedName = getSimplifiedClassName();
        if (simplifiedName == null) {
            simplifiedName = "Unknown";
        }

        StringBuilder result = new StringBuilder(simplifiedName);

        // 添加文本或描述信息
        String contentInfo = null;

        // 优先显示文本内容
        if (text != null && !text.isEmpty()) {
            contentInfo = text;
        }
        // 如果没有文本，显示描述（ContentDescription）
        else if (desc != null && !desc.isEmpty()) {
            contentInfo = desc;
        }

        // 限制显示长度，超过10个字符则用...表示
        if (contentInfo != null) {
            if (contentInfo.length() > 10) {
                contentInfo = contentInfo.substring(0, 10) + "...";
            }
            result.append("[").append(contentInfo).append("]");
        }

        // 显示子View个数
        int childCount = getChildCount();
        if (childCount > 0) {
            result.append(" (").append(childCount).append(")");
        }

        return result.toString();
    }
}