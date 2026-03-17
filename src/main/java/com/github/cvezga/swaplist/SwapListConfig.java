package com.github.cvezga.swaplist;

import java.io.Serializable;

/**
 * Configuration for a swap list (file path and paging).
 */
public final class SwapListConfig implements Serializable {

    private static final int DEFAULT_ITEMS_PER_PAGE = 1_000;
    private static final int MIN_ITEMS_PER_PAGE = 1;

    private final String swapListFilePath;
    private final int itemsPerPage;

    /**
     * Creates config with the given file path and default items per page.
     *
     * @param swapListFilePath path to the swap list file (must not be null or blank)
     * @throws IllegalArgumentException if path is null/blank or itemsPerPage &lt; 1
     */
    public SwapListConfig(String swapListFilePath) {
        this(swapListFilePath, DEFAULT_ITEMS_PER_PAGE);
    }

    /**
     * Creates config with the given file path and items per page.
     *
     * @param swapListFilePath path to the swap list file (must not be null or blank)
     * @param itemsPerPage     number of items per page (must be &gt;= 1)
     * @throws IllegalArgumentException if path is null/blank or itemsPerPage &lt; 1
     */
    public SwapListConfig(String swapListFilePath, int itemsPerPage) {
        if (swapListFilePath == null || swapListFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("swapListFilePath must not be null or blank");
        }
        if (itemsPerPage < MIN_ITEMS_PER_PAGE) {
            throw new IllegalArgumentException("itemsPerPage must be >= " + MIN_ITEMS_PER_PAGE + ", got " + itemsPerPage);
        }
        this.swapListFilePath = swapListFilePath;
        this.itemsPerPage = itemsPerPage;
    }

    public String getSwapListFilePath() {
        return swapListFilePath;
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    @Override
    public String toString() {
        return "SwapListConfig{" +
                "swapListFilePath='" + swapListFilePath + '\'' +
                ", itemsPerPage=" + itemsPerPage +
                '}';
    }
}
