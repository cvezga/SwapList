package com.github.cvezga.swaplist;

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
    private SwapListPage currentPage;
    private int currentPageIndex;

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
        this(new SwapListConfig(swapListFilePath,  itemsPerPage));
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
        this.currentPage = new SwapListPage(config);
    }

    public SwapListConfig getConfig() {
        return config;
    }

    /**
     * Sets the current page instance and its consecutive index.
     *
     * @param page      the page to set as current (may be null to clear)
     * @param pageIndex the consecutive page index (used for file naming)
     */
    private void setCurrentPage(SwapListPage page, int pageIndex) {
        this.currentPage = page;
        this.currentPageIndex = pageIndex;
    }

    private SwapListPage getCurrentPage() {
        return currentPage;
    }

    private int getCurrentPageIndex() {
        return currentPageIndex;
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
        String path = config.getSwapListFilePath();
        String fileName = path + "." + currentPageIndex;
        Path filePath = Paths.get(fileName);
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = new FileOutputStream(filePath.toFile());
             ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(currentPage.getList());
        }
    }

    public void add(Serializable item) throws IOException {
        if (this.currentPage.isFull()) {
            saveCurrentPage();
            createNewPageInstance();
        }
        this.currentPage.add(item);
    }

    private void createNewPageInstance() {
        this.currentPageIndex++;
        this.currentPage = new SwapListPage<Serializable>(config);
    }

    public Serializable get(int i) throws IOException {
        int pageIndex = i / config.getItemsPerPage();
        if (pageIndex != this.currentPageIndex) {
            saveCurrentPage();
            loadPage(pageIndex);
        }
        int pageItemIdex = i % config.getItemsPerPage();
        return this.currentPage.get(pageItemIdex);
    }

    private void loadPage(int pageIndex) {
        String path = config.getSwapListFilePath();
        String fileName = path + "." + pageIndex;
        Path filePath = Paths.get(fileName);
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            throw new IllegalStateException(String.format("path %s does not exist", fileName));
        }
        try (InputStream is = new FileInputStream(filePath.toFile());
             ObjectInputStream ois = new ObjectInputStream(is)) {
            List<Serializable> items = (List<Serializable>) ois.readObject();
            this.currentPage = new SwapListPage(items, config);
            this.currentPageIndex = pageIndex;

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}
