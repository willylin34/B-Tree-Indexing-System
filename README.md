# Disk-Based B-Tree Indexing System

A command-line **B-Tree indexing system built in Java** that stores key–value records in a persistent binary index file. The project demonstrates how a balanced search-tree data structure can be implemented with fixed-size disk blocks, rather than relying on an in-memory collection or an external database library.

## Project Overview

The program creates and manages a B-Tree index containing **64-bit integer keys and values**. It supports inserting and searching records, loading records from CSV files, printing records in sorted key order, and exporting the index to CSV.

The implementation uses Java's `RandomAccessFile` to read and update individual tree nodes at fixed offsets in the index file. When a node becomes full, the insertion algorithm splits the node and promotes its median key to maintain the B-Tree structure.

## Technologies

- **Language:** Java
- **Core concepts:** B-Trees, recursive search and insertion, node splitting, file-backed data structures
- **File I/O:** `RandomAccessFile`, `BufferedReader`, `PrintWriter`
- **Data formats:** Fixed-size binary index files and two-column CSV files

No third-party libraries or build framework are required.

## Implementation Details

### B-Tree structure

- **Minimum degree:** 10
- **Maximum keys per node:** 19
- **Maximum child pointers per node:** 20
- **Key and value types:** Java `long` (signed 64-bit integers)
- **Duplicate keys:** Rejected during insertion

Search traverses the B-Tree by comparing the target key against the sorted keys in each node. Insertion places a new key–value pair in the appropriate leaf and splits full nodes when necessary, including splitting the root.

### Persistent disk storage

The binary index uses **512-byte blocks**. Block 0 contains the file header, including a format identifier, the root block ID, and the next available block ID. Subsequent blocks store B-Tree nodes, including node metadata, keys, values, and child block IDs.

The program accesses nodes by seeking directly to `block ID × 512` in the index file. Records remain available across separate command-line runs.

### Supported commands

| Command | Description |
| --- | --- |
| `create` | Create a new empty index file |
| `insert` | Insert a numeric key–value pair; reject duplicate keys |
| `search` | Look up a key and print its associated value |
| `load` | Import key–value pairs from a two-column CSV file |
| `print` | Display all records in ascending key order |
| `extract` | Export all records to a new CSV file in ascending key order |

## Getting Started

### Requirements

- Java Development Kit (JDK) with `javac` and `java` available on your command line

### Compile

From the repository directory, run:

```bash
javac BTreeIndex.java
```

### Run the project

**1. Create an index file**

```bash
java BTreeIndex create example.idx
```

**2. Insert records**

```bash
java BTreeIndex insert example.idx 10 100
java BTreeIndex insert example.idx 30 300
java BTreeIndex insert example.idx 20 200
```

**3. Search for a key**

```bash
java BTreeIndex search example.idx 20
```

Expected output:

```text
20,200
```

**4. Print records in key order**

```bash
java BTreeIndex print example.idx
```

Expected output:

```text
10,100
20,200
30,300
```

**5. Import records from CSV**

Create a file named `records.csv` containing two integers per line, without a header:

```csv
40,400
50,500
60,600
```

Then run:

```bash
java BTreeIndex load example.idx records.csv
```

**6. Export the index to CSV**

```bash
java BTreeIndex extract example.idx exported.csv
```

The output file contains the stored key–value pairs in ascending key order. The `extract` command requires an output filename that does **not** already exist, and `create` also refuses to overwrite an existing index file.

## Design Considerations and Limitations

- This is an educational, single-process command-line implementation; it does not implement concurrent access, transaction handling, or crash recovery.
- Keys and values must be valid signed 64-bit integers. CSV input must consist of exactly two comma-separated numeric fields per nonempty line, without a column header.
- Duplicate keys are skipped during CSV loading. A malformed line may stop loading after earlier records have already been inserted.
- The implementation supports insertion and search, but **not key deletion**.
- The repository does not currently include an automated test suite or performance benchmarks; no production-level throughput or scalability claims are made.

Potential extensions include automated tests for multi-level node splits, deletion support, range queries, and performance comparisons against alternative indexing structures.

## Repository Files

```text
B-Tree-Indexing-System/
├── BTreeIndex.java    # Main Java implementation
├── README.md          # Project documentation
└── devlog.md          # Development notes
```

**Note:** The source file and public Java class must have the same name. If the code still declares `public class Project3`, change that declaration to `public class BTreeIndex` before using the commands shown above. Also update the `printUsage()` messages from `java Project3` to `java BTreeIndex`.
