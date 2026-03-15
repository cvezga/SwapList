# SwapList

A list-like structure that keeps items in memory one page at a time and swaps full pages to disk. Use it when you need index-based access to large sequences without holding everything in memory.

## Build

```bash
mvn clean verify
```

## Requirements

- Java 8+
- Maven 3.6+

## Usage

### Basic setup

```java
// Default: 1000 items per page, swap file path "swap-file.data"
SwapList list = new SwapList("swap-file.data");

// Custom: 500 items per page
SwapListConfig config = new SwapListConfig("swap-file.data", 500);
SwapList list = new SwapList(config);
```

### Adding and reading items

Items must be `Serializable` (they are written to disk when a page is full).

```java
// Add items; when the current page is full it is saved and a new page starts
list.add("item1");
list.add("item2");
list.add(someSerializableObject);

// Get by index; if the item is on another page, that page is loaded from disk
Serializable item = list.get(0);
```

### How paging works

- **Path**: `SwapListConfig.getSwapListFilePath()` is the base path (e.g. `swap.data`).
- **Page files**: Each page is stored as `{path}.{pageIndex}` (e.g. `swap.data.0`, `swap.data.1`).
- **Add**: When the current page reaches `itemsPerPage`, it is serialized to the paged file and a new empty page is used.
- **Get**: For index `i`, the page index is `i / itemsPerPage`. If that page is not the current one, the current page is saved (if any), then the required page is loaded from its file.

### API overview

| Class           | Role |
|----------------|------|
| `SwapList`     | Main API: `add(Serializable)`, `get(int)`, `getConfig()`, `getCurrentPage()`, `saveCurrentPage()`, `setCurrentPage(page, index)`. |
| `SwapListConfig` | Immutable config: file path and `itemsPerPage`. |
| `SwapListPage`  | One in-memory page of items; created and used internally by `SwapList`. |

### Manual save / current page

You can control the current page and trigger saves yourself:

```java
SwapList list = new SwapList("swap.data");
list.setCurrentPage(somePage, 2);
list.saveCurrentPage();  // Writes to swap.data.2
```

## License

See repository.
