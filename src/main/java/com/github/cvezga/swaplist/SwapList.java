package com.github.cvezga.swaplist;

import com.esotericsoftware.kryo.io.Input;
import com.github.cvezga.swaplist.exception.SwapListException;
import com.github.cvezga.swaplist.serializer.KryoSerializer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static java.nio.file.Files.readAllBytes;

/**
 * Swap list instance backed by a configurable swap file.
 */
public class SwapList {

    private final SwapListConfig config;
    private final KryoSerializer serializer;
    private SwapListPage<Serializable> currentPage;
    private int currentPageIndex;
    private int size;
    private int lastPageIndex;
    private static final String PAGE_PREFIX = "page.";
    private static final String CONFIG_FILENAME = "config";

    /**
     * Creates a swap list using the given file path and default config.
     *
     * @param swapListPath path to the swap list file (must not be null or blank)
     * @throws IllegalArgumentException if path is null or blank
     */
    public SwapList(String swapListPath) throws IOException {
        this(new SwapListConfig(swapListPath));
    }

    public SwapList(String swapListFilePath, int itemsPerPage) throws IOException {
        this(new SwapListConfig(swapListFilePath, itemsPerPage));
    }

    /**
     * Creates a swap list with the given config (allows testing with custom config).
     *
     * @param config swap list configuration (must not be null)
     * @throws NullPointerException if config is null
     */
    public SwapList(SwapListConfig config) throws IOException {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.currentPageIndex = 0;
        this.lastPageIndex = 0;
        this.currentPage = new SwapListPage<>(config);
        this.serializer = new KryoSerializer();
        updateStatus();
    }

    private void updateStatus() throws IOException {
        checkConfig();
        File dir = new File(this.config.getSwapListFilePath());

        if (dir.exists()) {
            String[] list = dir.list((dir1, name) -> name.contains(PAGE_PREFIX));
            if (list != null && list.length > 0) {
                this.lastPageIndex = list.length - 1;
                this.currentPageIndex = this.lastPageIndex;
                loadPage(this.lastPageIndex);
                this.size = this.currentPage.getList().size() + (this.lastPageIndex * this.config.getItemsPerPage());
                assert new File(this.config.getSwapListFilePath(), PAGE_PREFIX + "0").exists();
                assert new File(this.config.getSwapListFilePath(), PAGE_PREFIX + this.currentPageIndex).exists();
            }
        }
    }

    private void checkConfig() throws IOException {
        Path configDir = Paths.get(this.config.getSwapListFilePath());
        Files.createDirectories(configDir);
        File configFile = new File(configDir.toFile(), CONFIG_FILENAME);
        if (!configFile.exists()) {
            try (FileWriter fw = new FileWriter(configFile)) {
                fw.write(String.valueOf(this.config.getItemsPerPage()));
            }
        } else {
            try (FileReader fr = new FileReader(configFile);
                 BufferedReader br = new BufferedReader(fr)) {
                String content = br.readLine();
                if (content == null) {
                    throw new IOException("Config file is empty or corrupted");
                }
                content = content.trim();
                try {
                    int itemsPerPage = Integer.parseInt(content);
                    if (this.config.getItemsPerPage() != itemsPerPage) {
                        throw new IOException("Items per Page not matching");
                    }
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid items per page in config file", e);
                }
            }
        }
    }

    public SwapListConfig getConfig() {
        return config;
    }


    public void add(Serializable item) throws IOException {
        if (this.currentPage.isFull()) {
            if (this.currentPage.isUpdated()) {
                saveCurrentPage();
            }
            loadPage(this.lastPageIndex);
            if (this.currentPage.isFull()) {
                createNewPageInstance();
            }
        } else if (this.currentPageIndex != this.lastPageIndex) {
            if (this.currentPage.isUpdated()) {
                saveCurrentPage();
            }
            loadPage(this.lastPageIndex);
        }
        this.currentPage.add(item);
        this.currentPage.setIsSaved(false);
        this.size++;
    }

    private void createNewPageInstance() {
        this.lastPageIndex++;
        this.currentPage.clear();
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
     * as prefix with "/page." and the consecutive current page index appended (e.g. {@code swap.data} + {@code /page.3} → {@code swap.data/page.3}).
     *
     * @throws IllegalStateException if there is no current page set
     * @throws IOException           if writing or creating the file fails
     */
    private void saveCurrentPage() throws IOException {
        if (currentPage == null) {
            throw new IllegalStateException("no current page set");
        }
        String fileName = getPageFile(currentPageIndex);
        Path filePath = Paths.get(fileName);
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(filePath)) {
            byte[] serialize = serializer.serialize(currentPage);
            this.currentPage.setSaved(true);
            out.write(serialize);
            out.flush();
        }
    }

    private void loadPage(int pageIndex) {
        String fileName = getPageFile(pageIndex);
        Path filePath = Paths.get(fileName);
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            throw new IllegalStateException(String.format("path %s does not exist", fileName));
        }
        try (InputStream is = Files.newInputStream(filePath.toFile().toPath())) {
            @SuppressWarnings("unchecked")
            SwapListPage<Serializable> page = serializer.deserialize(SwapListPage.class, is.readAllBytes());


            //SwapListPage<Serializable> page = (SwapListPage<Serializable>) ois.readObject();
            page.setConfig(this.config);
            this.currentPage = page;
            this.currentPageIndex = pageIndex;

        } catch (IOException e) {
            throw new SwapListException(e);
        }
    }

    private String getPageFile(int pageIndex) {
        String path = config.getSwapListFilePath();
        return path + (path.endsWith("/") ? "" : "/") + PAGE_PREFIX + pageIndex;
    }

    public int getSize() {
        return size;
    }

    public void flush() throws IOException {
        this.saveCurrentPage();
    }
}
