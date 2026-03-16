package com.github.cvezga.swaplist;

import com.github.cvezga.swaplist.exception.SwapListException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SwapListTest {

    // --- SwapList construction and config ---

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
    }

    @Test
    void swapListRejectsBlankPath() {
        assertThrows(IllegalArgumentException.class, () -> new SwapList(""));
        assertThrows(IllegalArgumentException.class, () -> new SwapList("   "));
    }

    @Test
    void swapListRejectsNullConfig() {
        assertThrows(NullPointerException.class, () -> new SwapList((SwapListConfig) null));
    }

    // --- SwapListConfig validation ---

    @Test
    void configRejectsInvalidItemsPerPage() {
        assertThrows(IllegalArgumentException.class, () -> new SwapListConfig("path.data", 0));
        assertThrows(IllegalArgumentException.class, () -> new SwapListConfig("path.data", -1));
    }

    @Test
    void configRejectsNullPath() {
        assertThrows(IllegalArgumentException.class, () -> new SwapListConfig(null, 10));
    }

    @Test
    void configRejectsBlankPath() {
        assertThrows(IllegalArgumentException.class, () -> new SwapListConfig("", 10));
        assertThrows(IllegalArgumentException.class, () -> new SwapListConfig("  ", 10));
    }

    @Test
    void configToStringContainsPathAndItemsPerPage() {
        SwapListConfig config = new SwapListConfig("foo.data", 42);
        String s = config.toString();
        assertTrue(s.contains("foo.data"), "toString should contain path");
        assertTrue(s.contains("42"), "toString should contain itemsPerPage");
    }

    // --- File creation and paging ---

    @Test
    void saveCurrentPage_writesFileWithPathPrefixAndPageIndex(@TempDir Path dir) throws Exception {
        Path baseFile = dir.resolve("swap.data");
        SwapListConfig config = new SwapListConfig(baseFile.toString(), 5);
        SwapList swapList = new SwapList(config);
        // 11 items with page size 5: page0 saved when 6th added, page1 saved when 11th added
        for (int i = 0; i < 11; i++) {
            swapList.add("This is item #" + (i + 1));
        }

        Path page0 = dir.resolve("swap.data.0");
        assertTrue(Files.exists(page0), "expected file: " + page0);
        Path page1 = dir.resolve("swap.data.1");
        assertTrue(Files.exists(page1), "expected file: " + page1);
    }

    @Test
    void updateCurrentPage_readsFileWithPathPrefixAndPageIndex(@TempDir Path dir) throws Exception {
        int numberOfItemsPerPage = 5_000;
        int numberOfItems = 100_000;
        String text = "This is a test string entry for testing some data for item #";
        Path baseFile = dir.resolve("swap.data");
        SwapListConfig config = new SwapListConfig(baseFile.toString(), numberOfItemsPerPage);
        SwapList swapList = new SwapList(config);
        for (int i = 0; i < numberOfItems; i++) {
            swapList.add(text + (i + 1));
        }
        assertEquals(numberOfItems, swapList.getSize());
        for (int i = 0; i < numberOfItems; i++) {
            Serializable serializable = swapList.get(i);
            assertEquals(text + (i + 1), serializable.toString());
        }
    }

    @Test
    void get_readsFromCorrectPageAcrossBoundary(@TempDir Path dir) throws Exception {
        Path baseFile = dir.resolve("list.data");
        int pageSize = 100;
        SwapListConfig config = new SwapListConfig(baseFile.toString(), pageSize);
        SwapList swapList = new SwapList(config);
        for (int i = 0; i < pageSize + 50; i++) {
            swapList.add("item-" + i);
        }
        assertEquals("item-0", swapList.get(0).toString());
        assertEquals("item-99", swapList.get(99).toString());
        assertEquals("item-100", swapList.get(100).toString());
        assertEquals("item-149", swapList.get(149).toString());
    }

    @Test
    void get_throwsWhenIndexOutOfBounds(@TempDir Path dir) throws Exception {
        Path baseFile = dir.resolve("list.data");
        SwapList swapList = new SwapList(new SwapListConfig(baseFile.toString(), 10));
        swapList.add("a");
        swapList.add("b");
        assertThrows(IndexOutOfBoundsException.class, () -> swapList.get(2));
    }

    @Test
    void get_negativeIndex_throws(@TempDir Path dir) throws Exception {
        Path baseFile = dir.resolve("list.data");
        SwapList swapList = new SwapList(new SwapListConfig(baseFile.toString(), 10));
        swapList.add("a");
        // With one item, pageIndex=-1/10=0 so we stay on current page; pageItemIndex=-1 → ArrayList throws
        assertThrows(IndexOutOfBoundsException.class, () -> swapList.get(-1));
    }

    @Test
    void get_whenPageFileMissing_throwsSwapListException(@TempDir Path dir) throws Exception {
        Path baseFile = dir.resolve("list.data");
        SwapList swapList = new SwapList(new SwapListConfig(baseFile.toString(), 10));
        for (int i = 0; i < 5; i++) {
            swapList.add("item-" + i);
        }
        // Page 1 was never written; loading it should fail
        SwapListException ex = assertThrows(SwapListException.class, () -> swapList.get(10));
        assertNotNull(ex.getCause());
    }

    // --- SwapListPage (used by SwapList; behavior affects list) ---

    @Test
    void swapListPage_addWhenFullThrows() {
        SwapListConfig config = new SwapListConfig("ignored.data", 2);
        SwapListPage<Serializable> page = new SwapListPage<>(config);
        page.add("a");
        page.add("b");
        assertTrue(page.isFull());
        assertThrows(IllegalStateException.class, () -> page.add("c"));
    }

    @Test
    void swapListPage_getOutOfBoundsThrows() {
        SwapListConfig config = new SwapListConfig("ignored.data", 10);
        SwapListPage<Serializable> page = new SwapListPage<>(config);
        page.add("only");
        assertThrows(IndexOutOfBoundsException.class, () -> page.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> page.get(-1));
    }

    @Test
    void swapListPage_isFullBoundary() {
        SwapListConfig config = new SwapListConfig("ignored.data", 2);
        SwapListPage<Serializable> page = new SwapListPage<>(config);
        assertFalse(page.isFull());
        page.add("a");
        assertFalse(page.isFull());
        page.add("b");
        assertTrue(page.isFull());
    }

    @Test
    void swapListPage_isSavedDefaultsFalseAndCanBeSet() {
        SwapListConfig config = new SwapListConfig("ignored.data", 10);
        SwapListPage<Serializable> page = new SwapListPage<>(config);
        assertFalse(page.isSaved());
        page.setSaved(true);
        assertTrue(page.isSaved());
    }

    @Test
    void addAcrossMultiplePages_andReadBack_allPagesWritten(@TempDir Path dir) throws Exception {
        Path baseFile = dir.resolve("swap.data");
        SwapList swapList = new SwapList(new SwapListConfig(baseFile.toString(), 2));
        for (int i = 0; i < 6; i++) {
            swapList.add("item-" + i);
        }
        // Page 0 and 1 saved when 3rd and 5th item added; page 2 still in memory
        assertTrue(Files.exists(dir.resolve("swap.data.0")));
        assertTrue(Files.exists(dir.resolve("swap.data.1")));
        // get(0) triggers save of page 2 and load of page 0 → all three files exist
        for (int i = 0; i < 6; i++) {
            assertEquals("item-" + i, swapList.get(i).toString());
        }
        assertTrue(Files.exists(dir.resolve("swap.data.2")));
    }

}
