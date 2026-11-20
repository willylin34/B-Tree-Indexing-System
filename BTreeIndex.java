import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

public class BTreeIndex {
    static final int BLOCK_SIZE = 512;
    static final int MIN_DEGREE = 10;
    static final int MAX_KEYS = 2 * MIN_DEGREE - 1;
    static final int MAX_CHILDREN = 2 * MIN_DEGREE;
    static final String MAGIC = "4348PRJ3";

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }

        String command = args[0];
        String indexFile = args[1];

        try {
            switch (command) {
                case "create":
                    if (args.length != 2) {
                        System.out.println("Error: create needs an index file name.");
                        return;
                    }
                    createIndexFile(indexFile);
                    break;

                case "insert":
                    if (args.length != 4) {
                        System.out.println("Error: insert needs an index file, key, and value.");
                        return;
                    }
                    long insertKey = parseNumber(args[2]);
                    long insertValue = parseNumber(args[3]);
                    insertIndexFile(indexFile, insertKey, insertValue, true);
                    break;

                case "search":
                    if (args.length != 3) {
                        System.out.println("Error: search needs an index file and key.");
                        return;
                    }
                    long searchKey = parseNumber(args[2]);
                    searchIndexFile(indexFile, searchKey);
                    break;

                case "load":
                    if (args.length != 3) {
                        System.out.println("Error: load needs an index file and csv file.");
                        return;
                    }
                    loadIndexFile(indexFile, args[2]);
                    break;

                case "print":
                    if (args.length != 2) {
                        System.out.println("Error: print needs an index file name.");
                        return;
                    }
                    printIndexFile(indexFile);
                    break;

                case "extract":
                    if (args.length != 3) {
                        System.out.println("Error: extract needs an index file and output csv file.");
                        return;
                    }
                    extractIndexFile(indexFile, args[2]);
                    break;

                default:
                    System.out.println("Error: unknown command.");
                    printUsage();
                    break;
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: key/value must be an integer.");
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static void createIndexFile(String fileName) throws IOException {
        File file = new File(fileName);

        if (file.exists()) {
            System.out.println("Error: file already exists.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.writeBytes(MAGIC);
            raf.writeLong(0);
            raf.writeLong(1);

            while (raf.length() < BLOCK_SIZE) {
                raf.writeByte(0);
            }
        }

        System.out.println("Index file created.");
    }

    static void insertIndexFile(String fileName, long key, long value, boolean showMessage) throws IOException {
        File file = new File(fileName);

        if (!file.exists()) {
            System.out.println("Error: index file does not exist.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            if (!isValidIndexFile(raf)) {
                System.out.println("Error: invalid index file.");
                return;
            }

            boolean inserted = insertKeyValue(raf, key, value);

            if (showMessage) {
                if (inserted) {
                    System.out.println("Inserted.");
                } else {
                    System.out.println("Error: duplicate key.");
                }
            }
        }
    }

    static boolean insertKeyValue(RandomAccessFile raf, long key, long value) throws IOException {
        long rootId = readRootId(raf);

        if (rootId != 0 && searchNode(raf, rootId, key) != null) {
            return false;
        }

        if (rootId == 0) {
            long newBlockId = readNextBlockId(raf);

            BTreeNode root = new BTreeNode(newBlockId);
            root.numKeys = 1;
            root.keys[0] = key;
            root.values[0] = value;

            writeNode(raf, root);
            writeRootId(raf, newBlockId);
            writeNextBlockId(raf, newBlockId + 1);
            return true;
        }

        BTreeNode root = readNode(raf, rootId);

        if (root.isFull()) {
            long newRootId = readNextBlockId(raf);
            writeNextBlockId(raf, newRootId + 1);

            BTreeNode newRoot = new BTreeNode(newRootId);
            newRoot.children[0] = root.blockId;
            root.parentId = newRoot.blockId;

            writeNode(raf, root);
            splitChild(raf, newRoot, 0, root);
            writeRootId(raf, newRoot.blockId);

            insertNonFull(raf, newRoot.blockId, key, value);
        } else {
            insertNonFull(raf, root.blockId, key, value);
        }

        return true;
    }

    static void insertNonFull(RandomAccessFile raf, long blockId, long key, long value) throws IOException {
        BTreeNode node = readNode(raf, blockId);

        int i = (int) node.numKeys - 1;

        if (node.isLeaf()) {
            while (i >= 0 && key < node.keys[i]) {
                node.keys[i + 1] = node.keys[i];
                node.values[i + 1] = node.values[i];
                i--;
            }

            node.keys[i + 1] = key;
            node.values[i + 1] = value;
            node.numKeys++;

            writeNode(raf, node);
            return;
        }

        while (i >= 0 && key < node.keys[i]) {
            i--;
        }

        i++;

        BTreeNode child = readNode(raf, node.children[i]);

        if (child.isFull()) {
            splitChild(raf, node, i, child);

            if (key > node.keys[i]) {
                i++;
            }
        }

        insertNonFull(raf, node.children[i], key, value);
    }

    static void splitChild(RandomAccessFile raf, BTreeNode parent, int childIndex, BTreeNode child) throws IOException {
        int mid = MIN_DEGREE - 1;

        long rightId = readNextBlockId(raf);
        writeNextBlockId(raf, rightId + 1);

        BTreeNode right = new BTreeNode(rightId);
        right.parentId = parent.blockId;
        right.numKeys = MIN_DEGREE - 1;

        for (int j = 0; j < MIN_DEGREE - 1; j++) {
            right.keys[j] = child.keys[j + MIN_DEGREE];
            right.values[j] = child.values[j + MIN_DEGREE];
        }

        if (!child.isLeaf()) {
            for (int j = 0; j < MIN_DEGREE; j++) {
                right.children[j] = child.children[j + MIN_DEGREE];
            }
        }

        long medianKey = child.keys[mid];
        long medianValue = child.values[mid];

        child.numKeys = MIN_DEGREE - 1;

        for (int j = (int) parent.numKeys; j >= childIndex + 1; j--) {
            parent.children[j + 1] = parent.children[j];
        }

        parent.children[childIndex + 1] = right.blockId;

        for (int j = (int) parent.numKeys - 1; j >= childIndex; j--) {
            parent.keys[j + 1] = parent.keys[j];
            parent.values[j + 1] = parent.values[j];
        }

        parent.keys[childIndex] = medianKey;
        parent.values[childIndex] = medianValue;
        parent.numKeys++;

        writeNode(raf, child);
        writeNode(raf, right);
        writeNode(raf, parent);
    }

    static void searchIndexFile(String fileName, long key) throws IOException {
        File file = new File(fileName);

        if (!file.exists()) {
            System.out.println("Error: index file does not exist.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            if (!isValidIndexFile(raf)) {
                System.out.println("Error: invalid index file.");
                return;
            }

            long rootId = readRootId(raf);

            if (rootId == 0) {
                System.out.println("Error: key not found.");
                return;
            }

            Long value = searchNode(raf, rootId, key);

            if (value == null) {
                System.out.println("Error: key not found.");
            } else {
                System.out.println(key + "," + value);
            }
        }
    }

    static Long searchNode(RandomAccessFile raf, long blockId, long key) throws IOException {
        BTreeNode node = readNode(raf, blockId);

        int i = 0;

        while (i < node.numKeys && key > node.keys[i]) {
            i++;
        }

        if (i < node.numKeys && key == node.keys[i]) {
            return node.values[i];
        }

        if (node.children[i] == 0) {
            return null;
        }

        return searchNode(raf, node.children[i], key);
    }

    static void loadIndexFile(String indexFile, String csvFile) throws IOException {
        File idx = new File(indexFile);
        File csv = new File(csvFile);

        if (!idx.exists()) {
            System.out.println("Error: index file does not exist.");
            return;
        }

        if (!csv.exists()) {
            System.out.println("Error: csv file does not exist.");
            return;
        }

        int count = 0;

        try (RandomAccessFile raf = new RandomAccessFile(idx, "rw");
             BufferedReader reader = new BufferedReader(new FileReader(csv))) {

            if (!isValidIndexFile(raf)) {
                System.out.println("Error: invalid index file.");
                return;
            }

            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.length() == 0) {
                    continue;
                }

                String[] parts = line.split(",");

                if (parts.length != 2) {
                    System.out.println("Error: bad csv line.");
                    return;
                }

                long key = parseNumber(parts[0].trim());
                long value = parseNumber(parts[1].trim());

                boolean inserted = insertKeyValue(raf, key, value);

                if (inserted) {
                    count++;
                }
            }
        }

        System.out.println("Loaded " + count + " records.");
    }

    static void printIndexFile(String fileName) throws IOException {
        File file = new File(fileName);

        if (!file.exists()) {
            System.out.println("Error: index file does not exist.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (!isValidIndexFile(raf)) {
                System.out.println("Error: invalid index file.");
                return;
            }

            long rootId = readRootId(raf);

            if (rootId == 0) {
                return;
            }

            printNode(raf, rootId);
        }
    }

    static void printNode(RandomAccessFile raf, long blockId) throws IOException {
        BTreeNode node = readNode(raf, blockId);

        for (int i = 0; i < node.numKeys; i++) {
            if (node.children[i] != 0) {
                printNode(raf, node.children[i]);
            }

            System.out.println(node.keys[i] + "," + node.values[i]);
        }

        if (node.children[(int) node.numKeys] != 0) {
            printNode(raf, node.children[(int) node.numKeys]);
        }
    }

    static void extractIndexFile(String indexFile, String outputFile) throws IOException {
        File idx = new File(indexFile);
        File out = new File(outputFile);

        if (!idx.exists()) {
            System.out.println("Error: index file does not exist.");
            return;
        }

        if (out.exists()) {
            System.out.println("Error: output file already exists.");
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(idx, "r")) {
            if (!isValidIndexFile(raf)) {
                System.out.println("Error: invalid index file.");
                return;
            }
        }

        try (RandomAccessFile raf = new RandomAccessFile(idx, "r");
             PrintWriter writer = new PrintWriter(out)) {

            long rootId = readRootId(raf);

            if (rootId != 0) {
                extractNode(raf, rootId, writer);
            }
        }

        System.out.println("Extracted.");
    }

    static void extractNode(RandomAccessFile raf, long blockId, PrintWriter writer) throws IOException {
        BTreeNode node = readNode(raf, blockId);

        for (int i = 0; i < node.numKeys; i++) {
            if (node.children[i] != 0) {
                extractNode(raf, node.children[i], writer);
            }

            writer.println(node.keys[i] + "," + node.values[i]);
        }

        if (node.children[(int) node.numKeys] != 0) {
            extractNode(raf, node.children[(int) node.numKeys], writer);
        }
    }

    static boolean isValidIndexFile(RandomAccessFile raf) throws IOException {
        if (raf.length() < BLOCK_SIZE) {
            return false;
        }

        raf.seek(0);
        byte[] magicBytes = new byte[8];
        raf.readFully(magicBytes);

        String fileMagic = new String(magicBytes);
        return fileMagic.equals(MAGIC);
    }

    static long readRootId(RandomAccessFile raf) throws IOException {
        raf.seek(8);
        return raf.readLong();
    }

    static long readNextBlockId(RandomAccessFile raf) throws IOException {
        raf.seek(16);
        return raf.readLong();
    }

    static void writeRootId(RandomAccessFile raf, long rootId) throws IOException {
        raf.seek(8);
        raf.writeLong(rootId);
    }

    static void writeNextBlockId(RandomAccessFile raf, long nextBlockId) throws IOException {
        raf.seek(16);
        raf.writeLong(nextBlockId);
    }

    static long blockPosition(long blockId) {
        return blockId * BLOCK_SIZE;
    }

    static BTreeNode readNode(RandomAccessFile raf, long blockId) throws IOException {
        raf.seek(blockPosition(blockId));

        BTreeNode node = new BTreeNode(blockId);

        node.blockId = raf.readLong();
        node.parentId = raf.readLong();
        node.numKeys = raf.readLong();

        for (int i = 0; i < MAX_KEYS; i++) {
            node.keys[i] = raf.readLong();
        }

        for (int i = 0; i < MAX_KEYS; i++) {
            node.values[i] = raf.readLong();
        }

        for (int i = 0; i < MAX_CHILDREN; i++) {
            node.children[i] = raf.readLong();
        }

        return node;
    }

    static void writeNode(RandomAccessFile raf, BTreeNode node) throws IOException {
        raf.seek(blockPosition(node.blockId));

        raf.writeLong(node.blockId);
        raf.writeLong(node.parentId);
        raf.writeLong(node.numKeys);

        for (int i = 0; i < MAX_KEYS; i++) {
            raf.writeLong(node.keys[i]);
        }

        for (int i = 0; i < MAX_KEYS; i++) {
            raf.writeLong(node.values[i]);
        }

        for (int i = 0; i < MAX_CHILDREN; i++) {
            raf.writeLong(node.children[i]);
        }

        long end = blockPosition(node.blockId) + BLOCK_SIZE;
        while (raf.getFilePointer() < end) {
            raf.writeByte(0);
        }
    }

    static long parseNumber(String text) {
        return Long.parseLong(text);
    }

    static class BTreeNode {
        long blockId;
        long parentId;
        long numKeys;
        long[] keys;
        long[] values;
        long[] children;

        BTreeNode(long blockId) {
            this.blockId = blockId;
            this.parentId = 0;
            this.numKeys = 0;
            this.keys = new long[MAX_KEYS];
            this.values = new long[MAX_KEYS];
            this.children = new long[MAX_CHILDREN];
        }

        boolean isLeaf() {
            for (int i = 0; i < MAX_CHILDREN; i++) {
                if (children[i] != 0) {
                    return false;
                }
            }
            return true;
        }

        boolean isFull() {
            return numKeys == MAX_KEYS;
        }
    }

    static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java BTreeIndex create <indexfile>");
        System.out.println("  java BTreeIndex insert <indexfile> <key> <value>");
        System.out.println("  java BTreeIndex search <indexfile> <key>");
        System.out.println("  java BTreeIndex load <indexfile> <csvfile>");
        System.out.println("  java BTreeIndex print <indexfile>");
        System.out.println("  java BTreeIndex extract <indexfile> <csvfile>");
    }
}
