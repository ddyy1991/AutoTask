package com.dy.autotask.ui.multilevel;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base adapter for multi level list view.
 */
public abstract class MultiLevelListAdapter extends BaseAdapter {

    private List<Object> mItems = new ArrayList<>();
    private boolean mAlwaysExpanded = false;

    /**
     * Sets whether list is always expanded.
     *
     * @param alwaysExpanded True if list should be always expanded. False otherwise.
     */
    public void setAlwaysExpanded(boolean alwaysExpanded) {
        mAlwaysExpanded = alwaysExpanded;
    }

    /**
     * Checks if list is always expanded.
     *
     * @return True if list is always expanded. False otherwise.
     */
    public boolean isAlwaysExpanded() {
        return mAlwaysExpanded;
    }

    /**
     * Gets sub items for given object.
     *
     * @param object Object for which sub items should be returned.
     * @return Sub items list.
     */
    protected abstract List<?> getSubObjects(Object object);

    /**
     * Checks if object is expandable.
     *
     * @param object Object to check.
     * @return True if object is expandable. False otherwise.
     */
    protected abstract boolean isExpandable(Object object);

    /**
     * Checks if object should be initially expanded.
     *
     * @param object Object to check.
     * @return True if object should be initially expanded. False otherwise.
     */
    protected boolean isInitiallyExpanded(Object object) {
        return false;
    }

    /**
     * Gets view for given object.
     *
     * @param object Object for which view should be created.
     * @param convertView Convert view.
     * @param itemInfo Item information.
     * @return View for given object.
     */
    public abstract View getViewForObject(Object object, View convertView, ItemInfo itemInfo);

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Gets the position of the specified object in the adapter.
     *
     * @param object The object to find.
     * @return The position of the object, or -1 if not found.
     */
    public int getPosition(Object object) {
        for (int i = 0; i < mItems.size(); i++) {
            Object item = getItem(i);
            if (item instanceof Object[]) {
                Object[] itemArray = (Object[]) item;
                if (itemArray.length > 0 && itemArray[0] == object) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Object item = getItem(position);
        ItemInfo itemInfo = (ItemInfo) ((Object[]) item)[1];
        return getViewForObject(((Object[]) item)[0], convertView, itemInfo);
    }

    /**
     * Sets data items.
     *
     * @param dataItems Data items.
     */
    public void setDataItems(List<?> dataItems) {
        mItems.clear();
        int currentIndex = 0;
        for (Object object : dataItems) {
            mItems.add(new Object[]{object, new ItemInfo(0, currentIndex, dataItems.size(), isExpandable(object), isInitiallyExpanded(object))});
            currentIndex++;
            if (isInitiallyExpanded(object) || isAlwaysExpanded()) {
                expand(object, 0, currentIndex);
                // 更新currentIndex为当前mItems的大小
                currentIndex = mItems.size();
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Reloads data.
     */
    public void reloadData() {
        notifyDataSetChanged();
    }

    /**
     * Expands given object.
     *
     * @param object Object to expand.
     * @param level Level.
     * @param index Index.
     */
    private void expand(Object object, int level, int index) {
        List<?> subObjects = getSubObjects(object);
        int size = subObjects.size();
        
        // 从index开始逐个插入子元素
        for (int i = 0; i < size; i++) {
            Object obj = subObjects.get(i);
            // 计算正确的插入位置
            int insertPosition = index + i;
            
            // 确保插入位置不超出列表范围
            if (insertPosition > mItems.size()) {
                insertPosition = mItems.size();
            }
            
            mItems.add(insertPosition, new Object[]{obj, new ItemInfo(level + 1, i, size, isExpandable(obj), isInitiallyExpanded(obj))});
            
            // 如果子元素需要展开，递归展开并在正确位置插入其子元素
            if (isInitiallyExpanded(obj) || isAlwaysExpanded()) {
                // 递归展开子元素，插入位置为当前插入位置+1
                expand(obj, level + 1, insertPosition + 1);
            }
        }
    }

    /**
     * Collapses given object.
     *
     * @param object Object to collapse.
     * @param level Level.
     * @param index Index.
     */
    private void collapse(Object object, int level, int index) {
        List<?> subObjects = getSubObjects(object);
        // 从后往前处理子对象，避免索引变化导致的问题
        for (int i = subObjects.size() - 1; i >= 0; i--) {
            Object obj = subObjects.get(i);
            // 先递归处理展开的子节点
            if (isInitiallyExpanded(obj) || isAlwaysExpanded()) {
                // 递归调用时使用当前索引+i，因为在移除之前所有元素都在正确位置
                collapse(obj, level + 1, index + i);
            }
            // 然后移除当前元素
            // 检查索引是否有效
            if (index + i < mItems.size()) {
                mItems.remove(index + i);
            }
        }
    }

    /**
     * Toggles expansion state of given object.
     *
     * @param object Object to toggle.
     * @param level Level.
     * @param index Index.
     */
    public void toggleExpansion(Object object, int level, int index) {
        ItemInfo itemInfo = (ItemInfo) ((Object[]) mItems.get(index))[1];
        if (itemInfo.isExpanded()) {
            collapse(object, level, index + 1);
            ((Object[]) mItems.get(index))[1] = new ItemInfo(level, itemInfo.getIdxInLevel(), itemInfo.getLevelSize(), itemInfo.isExpandable(), false);
        } else {
            expand(object, level, index + 1);
            ((Object[]) mItems.get(index))[1] = new ItemInfo(level, itemInfo.getIdxInLevel(), itemInfo.getLevelSize(), itemInfo.isExpandable(), true);
        }
        notifyDataSetChanged();
    }
}