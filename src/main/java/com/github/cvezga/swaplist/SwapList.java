package com.github.cvezga.swaplist;

import com.github.cvezga.swaplist.exception.SwapListException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * Swap list instance backed by a configurable swap file.
 */
public class SwapList {

    private final SwapListConfig config;
    private SwapListPage<Serializable> currentPage;
    private int currentPageIndex;
    private int size;
    private int lastPageIndex;

    /**
     * Creates a swap list using the given file path and default config.
     *
     * @param swapListFilePath path to the swap list file (must not be null or blank)
     * @throws IllegalArgumentException if path is null or blank
     */
    public SwapList(String swapListFilePath) {
        this(new SwapListConfig(swapListFilePath));
    }

    public SwapList(String swapListFilePath, int itemsPerPage) {
        this(new SwapListConfig(swapListFilePath, itemsPerPage));
    }

    /**
     * Creates a swap list with the given config (allows testing with custom config).
     *
     * @param config swap list configuration (must not be null)
     * @throws NullPointerException if config is null
     */
    public SwapList(SwapListConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.currentPageIndex = 0;
        this.lastPageIndex = 0;
        this.currentPage = new SwapListPage<>(config);
    }

    public SwapListConfig getConfig() {
        return config;
    }


    public void add(Serializable item) throws IOException {
        if (this.currentPage.isFull() && !this.currentPage.isSaved()) {
            saveCurrentPage();
            createNewPageInstance();
        } else if (this.currentPageIndex != this.lastPageIndex) {
            saveCurrentPage();
            loadPage(this.lastPageIndex);
        }
        this.currentPage.add(item);
        this.currentPage.setIsSaved(false);
        this.size++;
    }

    private void createNewPageInstance() {
        this.lastPageIndex++;
        this.currentPage = new SwapListPage<>(config);
        this.currentPageIndex = this.lastPageIndex;
    }

    public Serializable get(int itemIndex) {

        try {
            swapPageForIndex(itemIndex);
        } catch (IOException e) {
            throw new SwapListException(e);
        }

        int pageItemIndex = itemIndex % config.getItemsPerPage();
        return this.currentPage.get(pageItemIndex);
    }

    private void swapPageForIndex(int itemIndex) throws IOException {
        int pageIndex = itemIndex / config.getItemsPerPage();
        if (pageIndex != this.currentPageIndex) {
            saveCurrentPage();
            loadPage(pageIndex);
        }
    }

    /**
     * Serializes the current page instance and state to a file. The file name is
     * the config {@link SwapListConfig#getSwapListFilePath() swap list file path}
     * as prefix with the consecutive current page index appended (e.g. {@code swap.data} + {@code .3} → {@code swap.data.3}).
     *
     * @throws IllegalStateException if there is no current page set
     * @throws IOException           if writing or creating the file fails
     */
    private void saveCurrentPage() throws IOException {
        if (currentPage == null) {
            throw new IllegalStateException("no current page set");
        }
        if (!this.currentPage.isSaved()) {
            String path = config.getSwapListFilePath();
            String fileName = path + "." + currentPageIndex;
            Path filePath = Paths.get(fileName);
            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(filePath);
                 ObjectOutputStream oos = new ObjectOutputStream(out)) {
                this.currentPage.setSaved(true);
                oos.writeObject(currentPage);
            }
        }
    }

    private void loadPage(int pageIndex) {
        String path = config.getSwapListFilePath();
        String fileName = path + "." + pageIndex;
        Path filePath = Paths.get(fileName);
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            throw new IllegalStateException(String.format("path %s does not exist", fileName));
        }
        try (InputStream is = Files.newInputStream(filePath.toFile().toPath());
             ObjectInputStream ois = new ObjectInputStream(is)) {
            this.currentPage = (SwapListPage<Serializable>) ois.readObject();
            //this.currentPage = new SwapListPage<>(items, config);
            this.currentPageIndex = pageIndex;

        } catch (IOException | ClassNotFoundException e) {
            throw new SwapListException(e);
        }
    }

    public int getSize() {
        return size;
    }
}
