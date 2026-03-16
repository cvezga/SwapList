# SwapList

A list-like structure that keeps items in memory one page at a time and swaps full pages to disk. Use it when you need index-based access to large sequences without holding everything in memory.

## Requirements

- **Java 8+**
- **Maven 3.6+** (for building)

## Build and test

```bash
mvn clean verify
```

Run only tests:

```bash
mvn test
```

## Installation

Install to the local repository:

```bash
mvn clean install
```

Then add to your project:

```xml
<dependency>
    <groupId>com.github.cvezga</groupId>
    <artifactId>swaplist</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Usage

### Basic setup

```java
// Default: 1000 items per page, swap file path "swap-file.data"
SwapList list = new SwapList("swap-file.data");

// Custom: 500 items per page
SwapList list = new SwapList("swap-file.data", 500);

// Or use a config object
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

**Exceptions:** `add(Serializable)` and `get(int)` throw `IOException` on I/O failure. `get(int)` may also throw `SwapListException` (e.g. when a page file is missing or corrupted) and `IndexOutOfBoundsException` when the index is invalid.

### How paging works

- **Path:** `SwapListConfig.getSwapListFilePath()` is the base path (e.g. `swap.data`).
- **Page files:** Each page is stored as `{path}.{pageIndex}` (e.g. `swap.data.0`, `swap.data.1`).
- **Add:** When the current page reaches `itemsPerPage`, it is serialized to the page file and a new empty page is used.
- **Get:** For index `i`, the page index is `i / itemsPerPage`. If that page is not current, the current page is saved (if dirty), then the required page is loaded from its file.

### API overview

| Class            | Role |
|------------------|------|
| `SwapList`       | Main API: `add(Serializable)`, `get(int)`, `getConfig()`. |
| `SwapListConfig` | Immutable config: file path and `itemsPerPage`. |
| `SwapListPage`   | One in-memory page of items; used internally by `SwapList`. |
| `SwapListException` | Unchecked exception wrapping I/O or deserialization errors (e.g. in `loadPage`). |

### Supported operations

| Method | Supported | Notes |
|--------|-----------|--------|
| `add(Serializable)` | Yes | Appends; page is written when full. |
| `get(int)`         | Yes | Index-based read; loads page from disk if needed. |
| `delete`           | No  | Not implemented. |
| `update`           | No  | Not implemented. |

**Note:** There is no `size()` method; the list does not track total item count.

## License

MIT License. See [LICENSE](LICENSE) or [opensource.org/license/mit](https://opensource.org/license/mit/) for details.
