package com.dy.autotask.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.dy.autotask.R;
import com.dy.autotask.model.NodeInfo;

import java.util.List;

/**
 * 布局层次适配器，用于ExpandableListView的数据绑定
 */
public class LayoutHierarchyAdapter extends BaseExpandableListAdapter {
    private static final int INDENT_PER_LEVEL = 42; // 每级缩进的像素值，等于指示器宽度
    
    private Context context;
    private List<NodeInfo> nodeList;
    private LayoutInflater inflater;
    private OnNodeDetailsClickListener onNodeDetailsClickListener;
    
    public interface OnNodeDetailsClickListener {
        void onNodeDetailsClick(NodeInfo nodeInfo);
    }
    
    public LayoutHierarchyAdapter(Context context, List<NodeInfo> nodeList) {
        this.context = context;
        this.nodeList = nodeList;
        this.inflater = LayoutInflater.from(context);
    }
    
    public void setOnNodeDetailsClickListener(OnNodeDetailsClickListener listener) {
        this.onNodeDetailsClickListener = listener;
    }

    @Override
    public int getGroupCount() {
        return nodeList != null ? nodeList.size() : 0;
    }
    
    @Override
    public int getChildrenCount(int groupPosition) {
        NodeInfo groupNode = (NodeInfo) getGroup(groupPosition);
        return groupNode != null ? groupNode.getChildren().size() : 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return nodeList != null ? nodeList.get(groupPosition) : null;
    }
    
    @Override
    public Object getChild(int groupPosition, int childPosition) {
        NodeInfo groupNode = (NodeInfo) getGroup(groupPosition);
        return groupNode != null ? groupNode.getChildren().get(childPosition) : null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }
    
    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }
    
    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.layout_hierarchy_group_item, parent, false);
            holder = new GroupViewHolder();
            holder.textView = convertView.findViewById(R.id.node_info_text);
            holder.detailsButton = convertView.findViewById(R.id.btn_node_details);
            holder.indicator = convertView.findViewById(R.id.indicator);
            convertView.setTag(holder);
        } else {
            holder = (GroupViewHolder) convertView.getTag();
        }
        
        NodeInfo node = (NodeInfo) getGroup(groupPosition);
        if (node != null) {
            holder.textView.setText(node.toString());
            
            // 只对有子节点的组项显示指示器
            if (holder.indicator != null) {
                if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                    holder.indicator.setVisibility(View.VISIBLE);
                    holder.indicator.setText(isExpanded ? "▼" : "▶");
                } else {
                    holder.indicator.setVisibility(View.GONE);
                }
            }
            
            // 设置详情按钮点击事件
            holder.detailsButton.setOnClickListener(v -> {
                if (onNodeDetailsClickListener != null) {
                    onNodeDetailsClickListener.onNodeDetailsClick(node);
                }
            });
        }
        
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ChildViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.layout_hierarchy_child_item, parent, false);
            holder = new ChildViewHolder();
            holder.textView = convertView.findViewById(R.id.node_info_text);
            holder.detailsButton = convertView.findViewById(R.id.btn_node_details);
            convertView.setTag(holder);
        } else {
            holder = (ChildViewHolder) convertView.getTag();
        }
        
        NodeInfo node = (NodeInfo) getChild(groupPosition, childPosition);
        if (node != null) {
            holder.textView.setText(node.toString());
            
            // 添加动态缩进，根据节点深度设置左边距
            int indentLevel = getNodeDepth(node);
            int leftMargin = INDENT_PER_LEVEL * indentLevel;
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.textView.getLayoutParams();
            params.leftMargin = leftMargin;
            holder.textView.setLayoutParams(params);
            
            // 设置详情按钮点击事件
            holder.detailsButton.setOnClickListener(v -> {
                if (onNodeDetailsClickListener != null) {
                    onNodeDetailsClickListener.onNodeDetailsClick(node);
                }
            });
        }
        
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
    
    static class GroupViewHolder {
        TextView textView;
        Button detailsButton;
        TextView indicator;
    }
    
    static class ChildViewHolder {
        TextView textView;
        Button detailsButton;
    }
    
    /**
     * 计算节点在树中的深度
     * @param node 节点信息
     * @return 节点深度
     */
    private int getNodeDepth(NodeInfo node) {
        int depth = 0;
        NodeInfo currentNode = node;
        
        // 向上遍历父节点来计算深度
        while (currentNode != null && currentNode.getParent() != null) {
            depth++;
            currentNode = currentNode.getParent();
        }
        
        return depth;
    }
    
    /**
     * 清理所有节点资源
     */
    public void recycleNodes() {
        if (nodeList != null) {
            nodeList.clear();
        }
    }
}