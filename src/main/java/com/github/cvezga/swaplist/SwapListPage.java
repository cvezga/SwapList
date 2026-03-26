package com.github.cvezga.swaplist;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SwapListPage<T extends Serializable> implements Serializable {

    private transient SwapListConfig config;
    private final List<T> items;
    private boolean isSaved;
    private boolean updated;

    public SwapListPage() {
        this.config = new SwapListConfig();
        this.items = new ArrayList<>(this.config.getItemsPerPage());
    }

    public SwapListPage(SwapListConfig swapListConfig) {
        this.config = swapListConfig;
        this.items = new ArrayList<>(swapListConfig.getItemsPerPage());
    }

    public SwapListPage(List<T> items, SwapListConfig swapListConfig) {
        this.config = swapListConfig;
        this.items = items;
    }

    public void add(T item) {
        if (this.isFull()) {
            throw new IllegalStateException("Cannot add more items to the list");
        }
        this.items.add(item);
        this.updated = true;
    }

    public boolean isFull() {
        return this.items.size() >= config.getItemsPerPage();
    }

    public List<T> getList() {
        return this.items;
    }

    public T get(int i) {
        return this.items.get(i);
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean saved) {
        isSaved = saved;
    }

    public void setIsSaved(boolean isSaved) {
        this.isSaved = isSaved;
    }

    public void clear() {
        this.items.clear();
    }

    public void setConfig(SwapListConfig config) {
        this.config = config;
    }

    public SwapListConfig getConfig() {
        return config;
    }

    public List<T> getItems() {
        return items;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
}
