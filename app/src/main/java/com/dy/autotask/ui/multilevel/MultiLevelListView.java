package com.dy.autotask.ui.multilevel;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

/**
 * MultiLevelListView.
 */
public class MultiLevelListView extends FrameLayout {

    private ListView mListView;
    private boolean mAlwaysExpanded;
    private MultiLevelListAdapter mAdapter;
    private OnItemClickListener mOnItemClickListener;

    /**
     * View constructor.
     */
    public MultiLevelListView(Context context) {
        super(context);
        init();
    }

    /**
     * View constructor.
     */
    public MultiLevelListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * View constructor.
     */
    public MultiLevelListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mListView = new ListView(getContext());
        // 启用ListView的触摸和滚动功能
        mListView.setSmoothScrollbarEnabled(true);
        mListView.setNestedScrollingEnabled(true);
        mListView.setVerticalScrollBarEnabled(true);
        addView(mListView);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object item = mAdapter.getItem(position);
                Object object = ((Object[]) item)[0];
                ItemInfo itemInfo = (ItemInfo) ((Object[]) item)[1];
                
                if (itemInfo.isExpandable() && !mAlwaysExpanded) {
                    mAdapter.toggleExpansion(object, itemInfo.getLevel(), position);
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onGroupItemClicked(MultiLevelListView.this, view, object, itemInfo);
                    }
                } else {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClicked(MultiLevelListView.this, view, object, itemInfo);
                    }
                }
            }
        });
    }

    /**
     * Sets adapter.
     *
     * @param adapter Adapter.
     */
    public void setAdapter(MultiLevelListAdapter adapter) {
        mAdapter = adapter;
        mListView.setAdapter(adapter);
    }

    /**
     * Sets whether list is always expanded.
     *
     * @param alwaysExpanded True if list should be always expanded. False otherwise.
     */
    public void setAlwaysExpanded(boolean alwaysExpanded) {
        mAlwaysExpanded = alwaysExpanded;
        if (mAdapter != null) {
            mAdapter.setAlwaysExpanded(alwaysExpanded);
        }
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
     * Sets item click listener.
     *
     * @param onItemClickListener Item click listener.
     */
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    /**
     * Interface for item click events.
     */
    public interface OnItemClickListener {
        void onItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo);
        void onGroupItemClicked(MultiLevelListView parent, View view, Object item, ItemInfo itemInfo);
    }
}