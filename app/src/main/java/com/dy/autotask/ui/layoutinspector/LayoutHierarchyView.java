package com.dy.autotask.ui.layoutinspector;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dy.autotask.AccessibilityServiceUtil;
import com.dy.autotask.R;
import com.dy.autotask.model.NodeInfo;
import com.dy.autotask.ui.multilevel.ItemInfo;
import com.dy.autotask.ui.multilevel.MultiLevelListAdapter;
import com.dy.autotask.ui.multilevel.MultiLevelListView;
import com.dy.autotask.ui.widget.LevelBeamView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Layout hierarchy view for displaying accessibility node tree.
 */
public class LayoutHierarchyView extends MultiLevelListView {

    public interface OnItemLongClickListener {
        void onItemLongClick(View view, NodeInfo nodeInfo);
    }
    
    public interface OnItemClickHighlightListener {
        void onItemClickHighlight(Rect bounds);
    }

    private Adapter mAdapter;
    private OnItemLongClickListener mOnItemLongClickListener;
    private OnItemClickHighlightListener mOnItemClickHighlightListener;
    private NodeInfo mRootNode;
    private NodeInfo mCurrentSelectedNode; // 跟踪当前选中的节点
    
    // 搜索相关字段
    private List<NodeInfo> mSearchResults = new ArrayList<>(); // 搜索结果列表
    private int mCurrentSearchIndex = -1; // 当前搜索结果索引
    private String mSearchKeyword = ""; // 搜索关键字

    public LayoutHierarchyView(Context context) {
        super(context);
        init();
    }

    public LayoutHierarchyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LayoutHierarchyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mAdapter = new Adapter();
        setAdapter(mAdapter);
        // 设置为非始终展开状态，以便显示折叠指示器
        setAlwaysExpanded(false);
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
                // Handle item click
                NodeInfo nodeInfo = (NodeInfo) item;
                if (mOnItemLongClickListener != null) {
                    mOnItemLongClickListener.onItemLongClick(view, nodeInfo);
                }
            }

            @Override
            public void onGroupItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo) {
                // Handle group item click
                // 这里不需要手动调用toggleExpansion，因为MultiLevelListView已经处理了
            }
        });
    }

    public void setRootNode(NodeInfo rootNodeInfo) {
        mRootNode = rootNodeInfo;
        // 检查根节点是否有子节点，如果有则传递所有子节点作为顶层节点
        if (rootNodeInfo != null && !rootNodeInfo.getChildren().isEmpty()) {
            mAdapter.setDataItems(new ArrayList<>(rootNodeInfo.getChildren()));
        } else {
            // 如果根节点没有子节点或者根节点为空，则只传递根节点本身
            mAdapter.setDataItems(Collections.singletonList(rootNodeInfo));
        }
    }
    
    /**
     * 设置搜索关键字并执行搜索
     * @param keyword 搜索关键字
     */
    public void setSearchKeyword(String keyword) {
        mSearchKeyword = keyword != null ? keyword : "";
        mSearchResults.clear();
        mCurrentSearchIndex = -1;
        
        // 如果关键字不为空，执行搜索
        if (!mSearchKeyword.isEmpty() && mRootNode != null) {
            searchNodes(mRootNode, mSearchKeyword);
        }
        
        // 通知适配器刷新
        mAdapter.reloadData();
    }
    
    /**
     * 在节点树中搜索匹配的节点
     * @param node 要搜索的节点
     * @param keyword 搜索关键字
     */
    private void searchNodes(NodeInfo node, String keyword) {
        // 检查当前节点是否匹配
        if (nodeMatchesKeyword(node, keyword)) {
            mSearchResults.add(node);
        }
        
        // 递归搜索子节点
        for (NodeInfo child : node.getChildren()) {
            searchNodes(child, keyword);
        }
    }
    
    /**
     * 检查节点是否匹配搜索关键字
     * @param node 节点
     * @param keyword 搜索关键字
     * @return 是否匹配
     */
    private boolean nodeMatchesKeyword(NodeInfo node, String keyword) {
        // 检查节点的文本、类名、ID等属性是否包含关键字
        if (node.getText() != null && node.getText().toLowerCase().contains(keyword.toLowerCase())) {
            return true;
        }
        
        if (node.getClassName() != null && node.getClassName().toLowerCase().contains(keyword.toLowerCase())) {
            return true;
        }
        
        if (node.getId() != null && node.getId().toLowerCase().contains(keyword.toLowerCase())) {
            return true;
        }
        
        if (node.getDesc() != null && node.getDesc().toLowerCase().contains(keyword.toLowerCase())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取搜索结果数量
     * @return 搜索结果数量
     */
    public int getSearchResultCount() {
        return mSearchResults.size();
    }
    
    /**
     * 获取当前搜索结果索引
     * @return 当前搜索结果索引
     */
    public int getCurrentSearchIndex() {
        return mCurrentSearchIndex;
    }
    
    /**
     * 清除搜索状态
     */
    public void clearSearchStatus() {
        mSearchResults.clear();
        mCurrentSearchIndex = -1;
        mSearchKeyword = "";
        
        // 取消所有搜索结果节点的选中状态
        for (NodeInfo node : mSearchResults) {
            node.setSelectedItem(false);
        }
        
        // 通知适配器刷新
        mAdapter.reloadData();
    }
    
    /**
     * 跳转到下一个搜索结果
     * @return 是否成功跳转
     */
    public boolean goToNextSearchResult() {
        if (mSearchResults.isEmpty()) {
            return false;
        }
        
        // 更新当前索引
        int previousIndex = mCurrentSearchIndex;
        mCurrentSearchIndex++;
        if (mCurrentSearchIndex >= mSearchResults.size()) {
            mCurrentSearchIndex = 0; // 循环到第一个
        }
        
        // 取消之前选中节点的选中状态
        if (previousIndex >= 0 && previousIndex < mSearchResults.size()) {
            NodeInfo previousNode = mSearchResults.get(previousIndex);
            previousNode.setSelectedItem(false);
        }
        
        // 设置新的选中节点
        NodeInfo currentNode = mSearchResults.get(mCurrentSearchIndex);
        currentNode.setSelectedItem(true);
        
        // 通知适配器数据改变以更新UI
        mAdapter.reloadData();
        
        // 通知外部显示高亮区域
        if (mOnItemClickHighlightListener != null) {
            mOnItemClickHighlightListener.onItemClickHighlight(currentNode.getBoundsInScreen());
        }
        
        return true;
    }

    /**
     * 设置选中的节点并高亮显示
     * @param selectedNode 选中的节点
     */
    public void setSelectedNode(AccessibilityNodeInfo selectedNode) {
        if (mRootNode != null && selectedNode != null) {
            NodeInfo matchedNode = findMatchingNode(mRootNode, selectedNode);
            if (matchedNode != null) {
                // 取消之前选中节点的选中状态
                if (mCurrentSelectedNode != null) {
                    mCurrentSelectedNode.setSelectedItem(false);
                }
                
                // 设置新的选中节点
                matchedNode.setSelectedItem(true);
                mCurrentSelectedNode = matchedNode;
                
                // 通知适配器数据改变以更新UI
                mAdapter.reloadData();
            }
        }
    }

    /**
     * 在NodeInfo树中查找与AccessibilityNodeInfo匹配的节点
     * @param nodeInfo NodeInfo节点
     * @param targetNode 目标AccessibilityNodeInfo节点
     * @return 匹配的NodeInfo节点，如果没有找到则返回null
     */
    private NodeInfo findMatchingNode(NodeInfo nodeInfo, AccessibilityNodeInfo targetNode) {
        if (nodeInfo == null || targetNode == null) {
            return null;
        }

        // 检查当前节点是否匹配
        if (nodeInfoMatches(nodeInfo, targetNode)) {
            return nodeInfo;
        }

        // 递归检查子节点
        for (NodeInfo child : nodeInfo.getChildren()) {
            NodeInfo matched = findMatchingNode(child, targetNode);
            if (matched != null) {
                return matched;
            }
        }

        return null;
    }

    /**
     * 检查NodeInfo是否与AccessibilityNodeInfo匹配
     * @param nodeInfo NodeInfo节点
     * @param targetNode 目标AccessibilityNodeInfo节点
     * @return 是否匹配
     */
    private boolean nodeInfoMatches(NodeInfo nodeInfo, AccessibilityNodeInfo targetNode) {
        // 这里可以根据具体需求实现匹配逻辑
        // 简单实现：比较类名和文本
        if (nodeInfo.getClassName() == null || targetNode.getClassName() == null) {
            return nodeInfo.getClassName() == targetNode.getClassName();
        }
        
        boolean classNameMatch = nodeInfo.getClassName().equals(targetNode.getClassName().toString());
        boolean textMatch = nodeInfo.getText() != null && targetNode.getText() != null && 
                          nodeInfo.getText().equals(targetNode.getText().toString());
        
        return classNameMatch && textMatch;
    }

    public void setOnItemLongClickListener(final OnItemLongClickListener onNodeInfoSelectListener) {
        mOnItemLongClickListener = onNodeInfoSelectListener;
    }
    
    public void setOnItemClickHighlightListener(OnItemClickHighlightListener listener) {
        mOnItemClickHighlightListener = listener;
    }

    private static class ViewHolder {
        TextView nameView;
        TextView infoView;
        ImageView arrowView;
        LevelBeamView levelBeamView;
        NodeInfo nodeInfo;

        ViewHolder(View view) {
            infoView = view.findViewById(R.id.dataItemInfo);
            nameView = view.findViewById(R.id.dataItemName);
            arrowView = view.findViewById(R.id.dataItemArrow);
            levelBeamView = view.findViewById(R.id.dataItemLevelBeam);
        }
    }

    private class Adapter extends MultiLevelListAdapter {

        @Override
        protected List<?> getSubObjects(Object object) {
            List<NodeInfo> allChildren = ((NodeInfo) object).getChildren();
            List<NodeInfo> visibleChildren = new ArrayList<>();
            
            // 过滤掉不可见的元素
            for (NodeInfo child : allChildren) {
                if (child.isVisibleToUser()) {
                    visibleChildren.add(child);
                }
            }
            
            return visibleChildren;
        }
        
        

        @Override
        protected boolean isExpandable(Object object) {
            return !((NodeInfo) object).getChildren().isEmpty();
        }

        @Override
        protected boolean isInitiallyExpanded(Object object) {
            // 默认展开所有节点
            return true;
        }

        @Override
        public View getViewForObject(Object object, View convertView, ItemInfo itemInfo) {
            NodeInfo nodeInfo = (NodeInfo) object;
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_hierarchy_view_item, null);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.nameView.setText(nodeInfo.toString());
            viewHolder.nodeInfo = nodeInfo;

            // 设置文字颜色和背景：根据节点的不同状态显示不同的颜色
            if (mSearchResults.contains(nodeInfo)) {
                // 如果是搜索结果节点
                if (mSearchResults.indexOf(nodeInfo) == mCurrentSearchIndex) {
                    // 当前搜索结果项 - 蓝色
                    viewHolder.nameView.setTextColor(0xFFFFFFFF); // 白色文字
                    convertView.setBackgroundColor(0xFF2196F3); // 蓝色背景
                } else {
                    // 其他搜索结果项 - 黄色
                    viewHolder.nameView.setTextColor(0xFF000000); // 黑色文字
                    convertView.setBackgroundColor(0xFFFFFF00); // 黄色背景
                }
            } else if (nodeInfo.isSelectedItem()) {
                // 普通选中的元素显示为紫色
                viewHolder.nameView.setTextColor(0xFF800080); // 紫色
                convertView.setBackgroundResource(R.drawable.selected_item_border);
            } else {
                // 非选中元素显示为黑色
                viewHolder.nameView.setTextColor(0xFF000000); // 黑色
                convertView.setBackground(null);
            }

            // 设置折叠指示器：只对有子元素的节点显示，颜色为黑色
            if (itemInfo.isExpandable() && !isAlwaysExpanded()) {
                viewHolder.arrowView.setVisibility(View.VISIBLE);
                viewHolder.arrowView.setImageResource(itemInfo.isExpanded() ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
                // 设置指示器颜色为黑色
                viewHolder.arrowView.setColorFilter(0xFF000000); // 黑色
            } else {
                // 对于没有子元素的节点，不显示任何图标
                viewHolder.arrowView.setVisibility(View.GONE);
            }

            viewHolder.levelBeamView.setLevel(itemInfo.getLevel());

            // 为整个item view添加点击和长按监听器
            convertView.setOnClickListener(v -> {
                // 只有在非搜索模式下才更新普通选中状态
                if (mSearchResults.isEmpty()) {
                    // 更新选中状态
                    if (mCurrentSelectedNode != null) {
                        mCurrentSelectedNode.setSelectedItem(false);
                    }
                    nodeInfo.setSelectedItem(true);
                    mCurrentSelectedNode = nodeInfo;
                    
                    // 通知外部显示高亮区域
                    if (mOnItemClickHighlightListener != null) {
                        mOnItemClickHighlightListener.onItemClickHighlight(nodeInfo.getBoundsInScreen());
                    }
                }
                
                // 通知适配器数据改变以更新UI
                mAdapter.reloadData();
                
                // 直接处理点击事件，展开或收起子元素
                if (itemInfo.isExpandable() && !isAlwaysExpanded()) {
                    // 获取item在列表中的位置
                    int position = mAdapter.getPosition(object);
                    if (position >= 0) {
                        mAdapter.toggleExpansion(object, itemInfo.getLevel(), position);
                    }
                }
            });
            
            convertView.setOnLongClickListener(v -> {
                showNodeOptionsDialog(nodeInfo);
                return true; // 返回true表示消费了长按事件
            });
            
            // 确保convertView可以处理点击事件
            convertView.setClickable(true);
            convertView.setFocusable(true);

            return convertView;
        }
        /**
     * 显示节点操作选项对话框
     * @param nodeInfo 选中的节点信息
     */
    private void showNodeOptionsDialog(NodeInfo nodeInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("请选择操作")
                .setItems(new String[]{"查看详情", "取消"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // 查看详情
                                showNodeDetails(nodeInfo);
                                break;
                            case 1: // 取消
                                dialog.dismiss();
                                break;
                        }
                    }
                })
                .setCancelable(true);
        
        AlertDialog dialog = builder.create();
        // 确保对话框在系统窗口上层显示
        if (dialog.getWindow() != null) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        }
        dialog.show();
    }

    /**
     * 显示节点详情
     * @param nodeInfo 节点信息
     */
    private void showNodeDetails(NodeInfo nodeInfo) {
        // 直接使用AccessibilityServiceUtil中的元素详情视图功能
        Context context = getContext();
        if (context instanceof AccessibilityServiceUtil) {
            AccessibilityServiceUtil serviceUtil = (AccessibilityServiceUtil) context;
            serviceUtil.showElementDetailsFromNode(nodeInfo);
        } else {
            // 如果无法获取AccessibilityServiceUtil实例，则使用简单的对话框显示基本信息
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("元素详情")
                    .setMessage(getBasicNodeInfo(nodeInfo))
                    .setPositiveButton("确定", (dialog, which) -> dialog.dismiss())
                    .setCancelable(true);
            
            AlertDialog dialog = builder.create();
            // 确保对话框在系统窗口上层显示
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            }
            dialog.show();
        }
    }
    
    /**
     * 获取节点基本信息
     * @param nodeInfo 节点信息
     * @return 基本信息字符串
     */
    private String getBasicNodeInfo(NodeInfo nodeInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("类名: ").append(nodeInfo.getClassName()).append("\n");
        sb.append("文本: ").append(nodeInfo.getText()).append("\n");
        sb.append("描述: ").append(nodeInfo.getDesc()).append("\n");
        sb.append("ID: ").append(nodeInfo.getId()).append("\n");
        sb.append("是否可点击: ").append(nodeInfo.getClickable() ? "是" : "否").append("\n");
        sb.append("是否可长按: ").append(nodeInfo.getLongClickable() ? "是" : "否").append("\n");
        sb.append("边界: ").append(nodeInfo.getBoundsInScreen().toShortString()).append("\n");
        return sb.toString();
    }


}
}