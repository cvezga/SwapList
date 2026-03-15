package com.github.cvezga.swaplist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class SwapListTest {

    @Test
    void swapListDefaultConfig() {
        SwapList swapList = new SwapList("test-swap-list.data");
        SwapListConfig config = swapList.getConfig();
        assertNotNull(config);
        assertEquals("test-swap-list.data", config.getSwapListFilePath());
        assertEquals(1_000, config.getItemsPerPage());
    }

    @Test
    void swapListWithCustomConfig() {
        SwapListConfig config = new SwapListConfig("custom.data", 500);
        SwapList swapList = new SwapList(config);
        assertSame(config, swapList.getConfig());
        assertEquals(500, swapList.getConfig().getItemsPerPage());
    }

    @Test
    void swapListRejectsNullPath() {
        assertThrows(IllegalArgumentException.class, () -> new SwapList((String) null));
        assertThrows(IllegalArgumentException.class, () -> new SwapList(""));
        assertThrows(IllegalArgumentException.class, () -> new SwapList("   "));
    }

    @Test
    void swapListRejectsNullConfig() {
        assertThrows(NullPointerException.class, () -> new SwapList((SwapListConfig) null));
    }

    @Test
    void configRejectsInvalidItemsPerPage() {
        assertThrows(IllegalArgumentException.class, () -> new SwapListConfig("path.data", 0));
        assertThrows(IllegalArgumentException.class, () -> new SwapListConfig("path.data", -1));
    }

    @Test
    void swapListSawpPageCheck() {
        SwapListConfig config = new SwapListConfig("custom.data", 5);
        SwapList swapList = new SwapList(config);
        assertSame(config, swapList.getConfig());
        assertEquals(5, swapList.getConfig().getItemsPerPage());
    }

    @Test
    void saveCurrentPage_writesFileWithPathPrefixAndPageIndex() throws Exception {
        SwapListConfig config = new SwapListConfig("swap.data",5);
        SwapList swapList = new SwapList(config);
        for(int i=0; i<10; i++) {
            swapList.add("This is item #"+(i+1));
        }

        Path path = Paths.get("swap.data.0");
        assertTrue(Files.exists(path), "expected file: " + path);
    }

    @Test
    void updateCurrentPage_readsFileWithPathPrefixAndPageIndex(@TempDir Path dir) throws Exception {
        Path baseFile = dir.resolve("swap.data");
        SwapListConfig config = new SwapListConfig(baseFile.toString(),10_000);
        SwapList swapList = new SwapList(config);
        for(int i=0; i<1_000_000; i++) {
            swapList.add("This is item #"+(i+1));
        }
        for(int i=0; i<100; i++) {
            Serializable serializable = swapList.get(i);
            assertEquals("This is item #"+(i+1), serializable.toString());
        }
    }

}