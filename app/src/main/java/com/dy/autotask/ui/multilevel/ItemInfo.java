package com.dy.autotask.ui.multilevel;

/**
 * Object representing multi level list item information.
 */
public class ItemInfo {

    private int mLevel;
    private int mIdxInLevel;
    private int mLevelSize;
    private boolean mIsExpandable;
    private boolean mIsExpanded;

    /**
     * Class constructor.
     *
     * @param level Item level.
     * @param idxInLevel Item index in level.
     * @param levelSize Level size.
     * @param isExpandable Flag indicating if item is expandable.
     * @param isExpanded Flag indicating if item is expanded.
     */
    public ItemInfo(int level, int idxInLevel, int levelSize, boolean isExpandable, boolean isExpanded) {
        mLevel = level;
        mIdxInLevel = idxInLevel;
        mLevelSize = levelSize;
        mIsExpandable = isExpandable;
        mIsExpanded = isExpanded;
    }

    /**
     * Gets item level.
     *
     * @return Item level.
     */
    public int getLevel() {
        return mLevel;
    }

    /**
     * Gets item index in level.
     *
     * @return Item index in level.
     */
    public int getIdxInLevel() {
        return mIdxInLevel;
    }

    /**
     * Gets level size.
     *
     * @return Level size.
     */
    public int getLevelSize() {
        return mLevelSize;
    }

    /**
     * Checks if item is expandable.
     *
     * @return True if item is expandable. False otherwise.
     */
    public boolean isExpandable() {
        return mIsExpandable;
    }

    /**
     * Checks if item is expanded.
     *
     * @return True if item is expanded. False otherwise.
     */
    public boolean isExpanded() {
        return mIsExpanded;
    }
}