package com.github.cvezga.swaplist;

import com.github.cvezga.swaplist.exception.SwapListException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SwapListTest {

    // --- SwapList construction and config ---

    @Test
    void swapListDefaultConfig(@TempDir Path dir) throws IOException {
        Path baseFile = dir.resolve(dir);
        SwapList swapList = new SwapList(baseFile.toString());
        SwapListConfig config = swapList.getConfig();
        assertNotNull(config);
        assertEquals(baseFile.toString(), config.getSwapListFilePath());
        assertEquals(1_000, config.getItemsPerPage());
    }

    @Test
    void swapListWithCustomConfig(@TempDir Path dir) throws IOException {
        Path baseFile = dir.resolve(dir);
        SwapListConfig config = new SwapListConfig(baseFile.toString(), 500);
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

    @Test
    void swapListWithItemsPerPage(@TempDir Path dir) throws IOException {
        Path baseFile = dir.resolve(dir);
        SwapList swapList = new SwapList(baseFile.toString(), 500);
        assertEquals(500, swapList.getConfig().getItemsPerPage());
        assertEquals(baseFile.toString(), swapList.getConfig().getSwapListFilePath());
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
        Path baseFile = dir.resolve(dir);
        SwapListConfig config = new SwapListConfig(baseFile.toString(), 5);
        SwapList swapList = new SwapList(config);
        // 11 items with page size 5: page0 saved when 6th added, page1 saved when 11th added
        for (int i = 0; i < 11; i++) {
            swapList.add("This is item #" + (i + 1));
        }

        Path page0 = dir.resolve("page.0");
        assertTrue(Files.exists(page0), "expected file: " + page0);
        Path page1 = dir.resolve("page.1");
        assertTrue(Files.exists(page1), "expected file: " + page1);
    }

    @Test
    void updateCurrentPage_readsFileWithPathPrefixAndPageIndex(@TempDir Path dir) throws Exception {
        int numberOfItemsPerPage = 5_000;
        int numberOfItems = 100_000;
        String text = "This is a test string entry for testing some data for item #";
        Path baseFile = dir.resolve(dir);
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
        Path baseFile = dir.resolve(dir);
        SwapList swapList = new SwapList(new SwapListConfig(baseFile.toString(), 2));
        for (int i = 0; i < 6; i++) {
            swapList.add("item-" + i);
        }
        // Page 0 and 1 saved when 3rd and 5th item added; page 2 still in memory
        assertTrue(Files.exists(dir.resolve("page.0")));
        assertTrue(Files.exists(dir.resolve("page.1")));
        // get(0) triggers save of page 2 and load of page 0 → all three files exist
        for (int i = 0; i < 6; i++) {
            assertEquals("item-" + i, swapList.get(i).toString());
        }
        assertTrue(Files.exists(dir.resolve("page.2")));
    }

    @Test
    void testSavedPagesUpdates(@TempDir Path dir) throws IOException {
        Path baseFile = dir.resolve(dir);
        SwapList swapList = new SwapList(new SwapListConfig(baseFile.toString(), 2));
        swapList.add("item-1");
        swapList.add("item-2");
        swapList.add("item-3");
        assertEquals(3, swapList.getSize());
        assertTrue(Files.exists(dir.resolve("page.0")));
        assertFalse(Files.exists(dir.resolve("page.1")));
        assertEquals("item-2", swapList.get(1));
        assertTrue(Files.exists(dir.resolve("page.1")));
        swapList.add("item-4");
        assertEquals(4, swapList.getSize());
        assertEquals("item-1", swapList.get(0));
        assertEquals("item-2", swapList.get(1));
        assertEquals("item-3", swapList.get(2));
        assertEquals("item-4", swapList.get(3));
    }

    @Test
    void comparePerformance(@TempDir Path dir) throws IOException {
        int itemPerList = 100_000;
        Path baseFile1 = dir.resolve("swap1.data");
        SwapList swapList1 = new SwapList(new SwapListConfig(baseFile1.toString(), itemPerList));
        long took1 = writeAndReadTiming(swapList1, itemPerList);
        System.out.println("p1 took: " + took1);

        Path baseFile2 = dir.resolve("swap2.data");
        SwapList swapList2 = new SwapList(new SwapListConfig(baseFile2.toString(), 1_000));
        long took2 = writeAndReadTiming(swapList2, itemPerList);
        System.out.println("p2 took: " + took2 + " itemPerPage: " + 1_000);

    }

    @Test
    void testExistingSwapList(@TempDir Path dir) throws IOException {
        int itemPerList = 10;
        Path baseFile1 = dir.resolve("testExistingSwapList");
        SwapList swapList1 = new SwapList(new SwapListConfig(baseFile1.toString(), itemPerList));
        for (int i = 0; i < 25; i++) {
            swapList1.add("item-" + i);
        }
        swapList1.flush();
        SwapList swapList2 = new SwapList(new SwapListConfig(baseFile1.toString(), itemPerList));
        assertEquals(25, swapList2.getSize());
        for (int i = 0; i < 25; i++) {
            assertEquals("item-" + i, swapList2.get(i));

        }

    }

    private long writeAndReadTiming(SwapList swapList, int itemPerList) throws IOException {
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < itemPerList; i++) {
            swapList.add("item-" + i);
        }
        for (int i = 0; i < swapList.getSize(); i++) {
            Serializable serializable = swapList.get(i);
        }
        return System.currentTimeMillis() - t1;
    }


}
