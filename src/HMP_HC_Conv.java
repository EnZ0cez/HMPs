
import ca.pfv.spmf.datastructures.collections.map.AMapIntToInt;
import ca.pfv.spmf.datastructures.collections.map.MapIntToInt;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;


public class HMP_HC_Conv {

    // Constants for the simulated annealing algorithm
    static final int MAX_ITERATIONS = 31;

    // Define threshold, a pattern must improve more than threshold
    static final double IMPROVEMENT_THRESHOLD = 0.001;

    // Codetable to store accepted patterns
    static ArrayList<int[]> codetable = new ArrayList<>();

    // All items in the database
    static int[] allItems = null;

    // The frequency of each item
    static MapIntToInt itemFrequency = null;
    // The maximum length of the generated pattern
    static int longestItemSet = 0;
    // Total weight to select item
    static int totalWeight = 0;

    // Store weight of items
    static List<Map.Entry<Integer, Integer>> cumulativeWeights = null;

    // Count the occurrence of the itemset
    static Map<int[], Integer> itemsetCount = new HashMap<>();

    // Matrix for size 2 patterns
    static SparseTriangularMatrix matrix = null;

    /** buffer **/
    static int[] BUFFER = new int[500];

    static Random random = new Random(System.currentTimeMillis());

    static int max_code_table_size = 53;


    public static void main(String[] args) {
        // Path for input file
        String inputFile = "Datasets/pageblocks.txt";

        // 提取文件名（去掉路径和扩展名）
        String fileName = inputFile.substring(inputFile.lastIndexOf("/") + 1, inputFile.lastIndexOf("."));

        // 根据提取的文件名生成输出文件名
        String outputFileName = "HC_output_" + fileName + ".csv";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            // 写入CSV内容
            writer.write(fileName+",");
        } catch (IOException e) {
            System.err.println("写入文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        List<int[]> database = readItemsetsFromFile(inputFile);
        initializeDatabase(database);

        // Perform the main algorithm logic (this is the existing code logic)
        double initialCompressionSize = calculateSizeInBits(database);
        double currentCompressionSize = initialCompressionSize;
        double improveCompressionSize = initialCompressionSize;

        int iterations = 0;
        // Record start time
        long startTime = System.currentTimeMillis();
        // Record memory usage
        MemoryLogger logger = MemoryLogger.getInstance();
        Random random = new Random();

        List<int[]> modifiedDatabase = copyDatabase(database);
        int codeTableTryCount = 0;
        // Main loop for the simulated annealing process

        // 打开 BufferedWriter 一次，避免在循环中频繁打开和关闭
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName, true))) {

            // 主循环
            while (codetable.size() < max_code_table_size / 3) {
                // 为本次循环创建一个新的 StringBuilder
                StringBuilder loopBuffer = new StringBuilder();


                // Generate a random pattern
                int[] pattern = generateRandomItemset(longestItemSet,itemFrequency, codetable);
                iterations = 0;
                int iterationTryCount = 0;
                // 内部模拟退火循环
                while (iterations < MAX_ITERATIONS) {
                    // Try to add a pattern to the codetable
                    ArrayList<int[]> temp_ct = new ArrayList<>(codetable);
                    // Generate neighbor pattern according to FLIP_NUM
                    int[] newPattern = generateTwoFlipNeighborPattern(pattern);

                    if (isPatternInDatabase(newPattern, database)) {

                        // Try to add new pattern to codetable
                        temp_ct.add(newPattern);
                        int newCompressionSize = deleteAndCalculateSizeInBits(modifiedDatabase, temp_ct);

                        // Calculate the improvement when adding this pattern
                        double compressionImprovement = (improveCompressionSize - newCompressionSize)
                                / improveCompressionSize;
                        if (compressionImprovement > IMPROVEMENT_THRESHOLD) {
                            // Accept the new pattern set because it improves
                            improveCompressionSize = newCompressionSize;
                            pattern = newPattern;
                            iterationTryCount=0;
                        }
                    }

                    double compressionRatio = improveCompressionSize / initialCompressionSize * 100;
                    DecimalFormat df = new DecimalFormat("#.##");
                    String formattedRatio = df.format(compressionRatio);
                    // 将数据写入 loopBuffer 而不是直接写入文件
                    loopBuffer.append(formattedRatio).append(",");

                    iterations++;
                    iterationTryCount++;

                }

                boolean accepted = false;

                if (isPatternInDatabase(pattern, modifiedDatabase)) {
                    if (((currentCompressionSize - improveCompressionSize) / initialCompressionSize) > IMPROVEMENT_THRESHOLD) {
                        codeTableTryCount = 0;
                        codetable.add(pattern);
                        accepted = true;
                        modifiedDatabase = deleteItemset(modifiedDatabase, pattern);
                        itemFrequency.clear();
                        cumulativeWeights.clear();
                        for (int[] transaction : modifiedDatabase) {
                            for (int item : transaction) {
                                int frequency = itemFrequency.get(item);
                                if (frequency == -1) {
                                    itemFrequency.put(item, 1);
                                } else {
                                    itemFrequency.put(item, frequency + 1);
                                }
                            }
                        }

                        cumulativeWeights.clear();
                        totalWeight = 0;
                        MapIntToInt.EntryIterator iter = itemFrequency.iterator();
                        while (iter.hasNext()) {
                            MapIntToInt.MapEntryIntToInt entry = iter.next();
                            totalWeight += entry.getValue();
                            cumulativeWeights.add(new AbstractMap.SimpleEntry<>(entry.getKey(), totalWeight));
                        }
                        currentCompressionSize = improveCompressionSize;
                    }
                }
                codeTableTryCount++;

                if (codeTableTryCount > (double) max_code_table_size / 3 && codeTableTryCount >100)  {
                    break;
                }

                double compressionRatio = currentCompressionSize / initialCompressionSize * 100;
                DecimalFormat dfFinal = new DecimalFormat("#.##");
                String formattedFinalRatio = dfFinal.format(compressionRatio);



                // 如果 accepted=true，则将 loopBuffer 的内容写入文件
                if (accepted) {
                    writer.write(loopBuffer.toString());

                }

                if (codetable.size() > 9) {
                    break;
                }
            }

            // 计算并记录 codetable 中每个模式的出现次数
            for (int[] pattern : codetable) {
                int occ = countOccurrencesInDatabase(pattern, database);
                itemsetCount.put(pattern, occ);
            }

            int pattern_sa = codetable.size();

            updateMatrix(modifiedDatabase);

            // 获取长度为2的模式并按出现次数排序
            List<Pair> length2Patterns = matrix.getAllPatternsWithOccurrences();
            length2Patterns.sort((entry1, entry2) -> Integer.compare(entry2.count, entry1.count));

            int totalPatterns = length2Patterns.size();

            // 处理长度为2的模式
            while (totalPatterns > 0) {
                Pair entry = length2Patterns.get(0);
                int[] pattern = entry.pattern;
                int count = entry.count;

                codetable.add(pattern);
                itemsetCount.put(pattern, count);
                double compressionRatio = deleteAndCalculateSizeInBits(database, codetable) / initialCompressionSize * 100;
                DecimalFormat df = new DecimalFormat("#.##");
                String formattedRatio = df.format(compressionRatio);
                writer.write(formattedRatio+",");
                modifiedDatabase = deleteItemset(modifiedDatabase, pattern);
                updateMatrix(modifiedDatabase);

                length2Patterns = matrix.getAllPatternsWithOccurrences();
                length2Patterns.sort((entry1, entry2) -> Integer.compare(entry2.count, entry1.count));
                totalPatterns = length2Patterns.size();

                if (codetable.size() > 9) {
                    break;
                }
            }

            // 记录当前内存使用
            double currentMemoryUsage = logger.checkMemory();
            logger.stopRecordingMode();
            logger.reset();

            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;

            currentCompressionSize = deleteAndCalculateSizeInBits(database, codetable);
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static int calculateSizeInBits(List<int[]> database) {
        Map<int[], Integer> patternCount = new HashMap<>();

        // Calculate the size in bits of the modified database
        int totalSizeInBits = 0;

        for (int[] transaction : database) {
            // Each integer is 32 bits
            totalSizeInBits += transaction.length * Integer.SIZE;
        }

        return totalSizeInBits;
    }

    public static int deleteAndCalculateSizeInBits(List<int[]> database, List<int[]> codeTable) {

        Map<int[], Integer> patternCount = new HashMap<>();
        // Calculate the size in bits of the modified database
        int totalSizeInBits = 0;

        // Process each transaction in order
        for (int[] transaction : database) {

            // Copy the transaction
            System.arraycopy(transaction, 0, BUFFER, 0, transaction.length);

            int newLength = transaction.length;

            for (int[] pattern : codeTable) {
                if (newLength >= pattern.length && containsAll(BUFFER, newLength,pattern)) {
                    // Increment the count for the pattern
                    patternCount.put(pattern, patternCount.getOrDefault(pattern, 0) + 1);

                    // Remove the pattern from the transaction
                    BUFFER = removePatternFromTransaction(BUFFER, newLength, pattern);

                    newLength = newLength - pattern.length;
                }
            }


            totalSizeInBits += newLength * Integer.SIZE;

            BUFFER = new int[500];

        }



        // Add the size of the transactions in finalResult multiplied by their counts
        for (Map.Entry<int[], Integer> entry : patternCount.entrySet()) {
            int patternSize = 0;
            if (entry.getValue() != 0) {
                int keySize = entry.getKey().length;
                // Calculate the bit size for entry.getValue() using logarithm base 2
                int valueBitSize = (int) Math.ceil(Math.log(entry.getValue() + 1) / Math.log(2));
                patternSize = valueBitSize + keySize * Integer.SIZE;
            }

            totalSizeInBits += patternSize;
        }

        return totalSizeInBits;
    }

    /**
     * Remove some pattern from a transaction of a given length
     *
     * @param itemsetToNotKeep the set of items to be excluded
     * @return the copy
     */
    public static int[] removePatternFromTransaction(int[] transaction, int transactionLength, int[] pattern) {
        // 使用 HashSet 以提高查找效率
        Set<Integer> patternSet = new HashSet<>();
        for (int p : pattern) {
            patternSet.add(p);
        }

        int pos = 0;

        // 遍历每个事务元素，将不在 pattern 中的元素保留
        for (int j = 0; j < transactionLength; j++) {
            // 如果当前项不在 pattern 中，保留它
            if (!patternSet.contains(transaction[j])) {
                transaction[pos++] = transaction[j];
            }
        }

        // 返回一个新的数组，包含有效的元素
        return Arrays.copyOf(transaction, pos);
    }



    /**
     * Make a copy of this itemset but exclude a set of items
     *
     * @param itemsetToNotKeep the set of items to be excluded
     * @return the copy
     */
    public static int[] cloneItemSetMinusAnItemset(int[] itemset, int[] itemsetToNotKeep) {
        // create a new itemset
        int[] newItemset = new int[itemset.length - itemsetToNotKeep.length];
        int i = 0;
        // for each item of this itemset
        for (int j = 0; j < itemset.length; j++) {
            // copy the item except if it is not an item that should be excluded
            if (Arrays.binarySearch(itemsetToNotKeep, itemset[j]) < 0) {
                newItemset[i++] = itemset[j];
            }
        }
        return newItemset; // return the copy
    }

    /**
     * Removes a specified pattern from each transaction in the given database.
     *
     * This method returns a modified version of the database where each transaction
     * that contains the given pattern has that pattern removed. The rest of the
     * items remain unchanged. The method tracks how many times the pattern is
     * removed but does not return that count.
     *
     * @param database the original database of transactions as a list of lists of
     *                 integers
     * @param pattern  the pattern (list of integers) to remove from each
     *                 transaction if present
     * @return a new database with the specified pattern removed from all
     *         transactions in which it appears
     */
    public static List<int[]> deleteItemset(List<int[]> database, int[] pattern) {
        List<int[]> modifiedDatabase = new ArrayList<>();
//		int patCounter = 0;

        for (int[] transaction : database) {

            if (containsOrEquals(transaction, pattern)) {
                transaction = cloneItemSetMinusAnItemset(transaction, pattern);
//				patCounter++;
                modifiedDatabase.add(transaction);
            } else {
                modifiedDatabase.add(transaction);
            }

        }

        return modifiedDatabase;
    }

    /**
     * Reads itemsets (transactions) from a specified file and returns them as a
     * list of transactions.
     *
     * The file is expected to have one transaction per line, with items separated
     * by whitespace. Each line is read and split into individual items (as
     * integers), which are then gathered into an ArrayList<Integer> representing a
     * single transaction. All transactions are collected and returned as a list of
     * lists.
     *
     * @param fileName the name of the file containing the itemsets
     * @return a list of transactions, where each transaction is represented as a
     *         list of integers
     */
    public static List<int[]> readItemsetsFromFile(String fileName) {
        List<int[]> database = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] items = line.split("\\s+");

                int[] transaction = new int[items.length];
                for (int i = 0; i < items.length; i++) {
                    transaction[i] = Integer.valueOf(items[i].trim());
                }

                // Sort the transaction
                Arrays.sort(transaction);
                database.add(transaction);
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return database;
    }

    static int[] SMALLBUFFER = new int[10];

    public static int[] generateRandomItemset(int longestItemSet, MapIntToInt itemFrequency,
                                              List<int[]> currentPatternSet) {

        int randomItemsetSize = 3 + random.nextInt(3); // Random size between 2 and longestItemSet
        int length = 0;
        do {
            length = 0;
            while (length < randomItemsetSize) {
                int randomNumber = getWeightedRandomItem();
                if (!containsElementUnsorted(SMALLBUFFER, length, randomNumber)) {
                    SMALLBUFFER[length] = randomNumber;
                    length++;
                }
            }
            Arrays.sort(SMALLBUFFER, 0, length);
        } while (containsPattern(currentPatternSet, SMALLBUFFER, length)); // Check for duplicates in currentPatternSet

        return Arrays.copyOf(SMALLBUFFER, randomItemsetSize);
    }

    // Check if an element is inside an unsorted array
    private static boolean containsElementUnsorted(int[] list, int listLength, int element) {
        for (int i = 0; i < listLength; i++) {
            if (list[i] == element) {
                return true;
            }
        }
        return false;
    }

    // Check if a pattern is in the pattern set
    private static boolean containsPattern(List<int[]> listOfPatterns, int[] patternBuffer, int patternLength) {
        for (int[] patternInList : listOfPatterns) {
            if (includedIn(patternBuffer, patternLength, patternInList)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a sorted itemset1 is contained in another itemset2
     *
     * @param itemset1 the first itemset
     * @param itemset2 the second itemset
     * @return true if yes, otherwise false
     */
    public static boolean includedIn(int[] itemset1, int itemset1Length, int[] itemset2) {
        int count = 0; // the current position of itemset1 that we want to find in itemset2

        // for each item in itemset2
        for (int i = 0; i < itemset2.length; i++) {
            // if we found the item
            if (itemset2[i] == itemset1[count]) {
                // we will look for the next item of itemset1
                count++;
                // if we have found all items already, return true
                if (count == itemset1Length) {
                    return true;
                }
            }
        }
        // it is not included, so return false!
        return false;
    }

    // Generates a neighboring pattern by modifying or adding two patterns in the
    // current pattern set
    static int[] BUFFERFLIP = new int[20];

    public static int[] generateTwoFlipNeighborPattern(int[] currentPattern) {

        boolean ok = false;
        while (ok == false) {
            int length = currentPattern.length;

            // COPY THE PATTERN
            System.arraycopy(currentPattern, 0, BUFFERFLIP, 0, currentPattern.length);

            // Choose two distinct indices to flip based on the frequency of each item
            int flipItem1 = itemToIndex(getWeightedRandomItem());
            int flipItem2;
            do {
                flipItem2 = itemToIndex(getWeightedRandomItem());
            } while (flipItem1 == flipItem2); // Ensure the indices are different

            if (!containsElementUnsorted(BUFFERFLIP, length, flipItem1) && length < 10) {
                BUFFERFLIP[length] = flipItem1;
                length++;
            } else if(length > 2){
                removeItemFromUnsortedList(BUFFERFLIP, length, flipItem1);
                length--;
            }

            if (!containsElementUnsorted(BUFFERFLIP, length, flipItem2) && length < 10) {
                BUFFERFLIP[length] = flipItem2;
                length++;
            } else if(length > 2){
                removeItemFromUnsortedList(BUFFERFLIP, length, flipItem2);
                length--;
            }

            if (length > 2) {
                Arrays.sort(BUFFERFLIP, 0, length);
                return Arrays.copyOf(BUFFERFLIP, length);
            }
        }

        return null;
    }

    /**
     * Generates itemsets by combining multiple patterns from the
     * SparseTriangularMatrix.
     *
     * @param currentPatternSet The current set of patterns to avoid duplicates.
     * @param numPatterns       The number of base patterns to combine.
     * @return A new pattern generated by combining the specified number of base
     *         patterns.
     * @throws IllegalArgumentException If numPatterns is less than 1 or too large.
     */
    public static ArrayList<Integer> generateRandomItemsetMatrix(ArrayList<ArrayList<Integer>> currentPatternSet,
                                                                 int numPatterns) {

        // Retrieve all patterns of size 2 from the matrix
        List<Pair> patterns = matrix.getAllPatternsWithOccurrences();

        if (patterns.isEmpty()) {
            throw new IllegalStateException("No patterns available in the matrix.");
        }

        // Ensure that numPatterns does not exceed the total number of available
        // patterns
        if (numPatterns > patterns.size()) {
            throw new IllegalArgumentException("numPatterns exceeds the number of available patterns.");
        }

        ArrayList<Integer> randomSet;
        Random random = new Random();

        int num = 2 + random.nextInt(numPatterns);

        do {
            randomSet = new ArrayList<>();
            Set<Integer> uniqueItems = new HashSet<>();

            for (int i = 0; i < num; i++) {

                Map.Entry<List<Integer>, Integer> basePattern = (Map.Entry<List<Integer>, Integer>) patterns.get(i);
                // Add items from the base pattern to the uniqueItems set
                uniqueItems.addAll(basePattern.getKey());
            }

            // Convert the set to a sorted list
            randomSet.addAll(uniqueItems);
            Collections.sort(randomSet);

            // Optional: If you prefer the list to have exactly numPatterns * 2 items,
            // you can add additional logic to handle that. Currently, it ensures
            // all items are unique, so the size may be less than or equal to numPatterns *
            // 2.

        } while (currentPatternSet.contains(randomSet)); // Repeat if the pattern already exists

        return randomSet;
    }

    /**
     * Generates a neighboring pattern by modifying or adding items in the current
     * pattern set.
     *
     * @param currentPattern The current pattern as a list of integers.
     * @param flipNum        The number of positions to flip.
     * @return A new pattern with the specified number of items flipped.
     */
    // 生成邻居模式的函数，返回 int[] 类型的结果
    public static int[] generateNeighborPattern(int[] currentPattern, int flipNum) {
        // 创建一个二进制向量来表示当前模式
        boolean[] binaryVector = new boolean[allItems.length];
        int patternSize = currentPattern.length;

        // 设置二进制向量，根据 currentPattern 中的元素
        for (int i = 0; i < currentPattern.length; i++) {
            int item = currentPattern[i];
            int index = itemToIndex(item);
            if (index != -1) {
                binaryVector[index] = true; // 如果元素在模式中，设置为 true
            }
        }

        // 生成新模式
        int[] newPattern = new int[0];  // 默认的空数组，稍后会更新

        // 无限循环生成新模式，直到满足条件
        while (true) {
            // 选择唯一的翻转索引，基于加权随机选择
            Set<Integer> flipIndices = new HashSet<>();
            while (flipIndices.size() < flipNum) {
                int selectedItem = getWeightedRandomItem();
                int flipIndex = itemToIndex(selectedItem);
                if (flipIndex != -1) {
                    flipIndices.add(flipIndex);
                }
            }

            // 翻转选中的索引
            for (int flipIndex : flipIndices) {
                if (!binaryVector[flipIndex] && patternSize < 6) {
                    binaryVector[flipIndex] = true;  // 添加一个项目（从 false 翻转为 true）
                    patternSize++;
                } else if (binaryVector[flipIndex] && patternSize > 3) {
                    binaryVector[flipIndex] = false; // 移除一个项目（从 true 翻转为 false）
                    patternSize--;
                }
                // 如果不满足条件，则忽略翻转以保持约束
            }

            // 将修改后的二进制向量转换回模式（转成 int[]）
            List<Integer> newPatternList = new ArrayList<>();
            for (int i = 0; i < binaryVector.length; i++) {
                if (binaryVector[i]) {
                    newPatternList.add(allItems[i]); // 仅保留二进制向量中为 true 的项目
                }
            }

            // 转换 List<Integer> 为 int[] 数组
            newPattern = new int[newPatternList.size()];
            for (int i = 0; i < newPatternList.size(); i++) {
                newPattern[i] = newPatternList.get(i);
            }

            // 生成新模式后返回
            break;
        }

        return newPattern;  // 返回 int[] 类型的结果
    }


    // Make a copy of a list of items while removing some given item;
    private static void removeItemFromUnsortedList(int[] currentPattern, int length, int itemToRemove) {
        int pos = 0;
        for (int i = 0; i < length; i++) {
            if (currentPattern[pos] != itemToRemove) {
                currentPattern[pos] = currentPattern[i];
                pos++;
            }
        }
    }

    public static int getWeightedRandomItem() {
        if (totalWeight == 0 || totalWeight < 0) {
            throw new IllegalStateException("Total weight must be initialized and greater than zero.");
        }

        // Generate a random number
        int randomNumber = random.nextInt(totalWeight);

        // Use binary search to find the item in the cumulative weight
        int left = 0;
        int right = cumulativeWeights.size() - 1;

        while (left < right) {
            int mid = (left + right) / 2;
            if (randomNumber < cumulativeWeights.get(mid).getValue()) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }

        return cumulativeWeights.get(left).getKey();
    }


    /**
     * Checks whether a given pattern of items exists in at least one transaction
     * within the provided database.
     *
     * <p>
     * This method converts the given pattern and each transaction into sets, then
     * verifies if any of the transaction sets contains all items of the pattern
     * set. If at least one transaction fully contains the pattern, the method
     * returns true.
     *
     * @param pattern  the pattern to check for, represented as a list of integers.
     * @param database the database of transactions, where each transaction is a
     *                 list of integers.
     * @return {@code true} if the pattern is found in at least one transaction in
     *         the database; {@code false} otherwise.
     */
    // Check if the pattern exists in the database
    public static boolean isPatternInDatabase(int[] pattern, List<int[]> database) {
        for (int[] transaction : database) {
            if (pattern.length <= transaction.length && containsOrEquals(transaction, pattern)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Retrieves the index of the specified item from the global list of all items.
     *
     * <p>
     * This method creates a list from the global collection of items and returns
     * the zero-based index of the specified item. If the item is not present, the
     * method returns -1.
     *
     * @param item the item whose index is to be determined.
     * @return the index of the item in the global list, or -1 if the item does not
     *         exist in the list.
     */
    public static int itemToIndex(int item) {
        // TODO: Could be implemented more efficiently using a HashMap that would be
        // initialized once
        // rather than doing a sequential search
        for (int i = 0; i < allItems.length; i++) {
            if (allItems[i] == item)
                return i;
        }
        return -1;
    }

    /**
     * Check if an itemset contains another itemset. It assumes that itemsets are
     * sorted according to the lexical order.
     *
     * @param itemset1 the first itemset
     * @param itemset2 the second itemset
     * @return true if the first itemset contains the second itemset
     */
    public static boolean containsOrEquals(int itemset1[], int itemset2[]) {
        int i = 0, j = 0;

        // 双指针遍历，假设 itemset1 和 itemset2 已经排序
        while (i < itemset1.length && j < itemset2.length) {
            if (itemset1[i] == itemset2[j]) {
                j++;  // 如果找到了匹配的元素，继续匹配 itemset2 的下一个元素
            }
            i++;  // 无论如何，i 都要向前移动
        }

        // 如果 j 达到了 itemset2 的末尾，说明 itemset1 包含了 itemset2 的所有元素
        return j == itemset2.length;
    }
    /**
     * Initializes the database by processing each transaction and updating item
     * frequencies, pair supports, the longest itemset size, and cumulative weights.
     *
     * The method performs the following actions: 1. Clears previously stored global
     * variables, including item frequencies, codetable, etc. 2. Iterates over each
     * transaction in the database: - Updates the frequency of each item. - Tracks
     * the longest itemset size. - Updates the support of all 2-item patterns in the
     * matrix. 3. Calculates the cumulative weight of items and updates the
     * `cumulativeWeights` list.
     *
     * @param database the list of transactions, where each transaction is
     *                 represented as a list of integers.
     */
    public static void initializeDatabase(List<int[]> database) {
        longestItemSet = 0;
        totalWeight = 0;
        itemFrequency = new AMapIntToInt();
        cumulativeWeights  = new ArrayList<Map.Entry<Integer, Integer>>();
        matrix  = new SparseTriangularMatrix();

        // Iterate through the database and update item frequencies and pair supports
        for (int[] transaction : database) {

            // Update item frequency and the support of each item pair
            for (int i = 0; i < transaction.length; i++) {
                Integer item = transaction[i];

                // Update frequency for item1
                int frequency = itemFrequency.get(item);
                if (frequency == -1) {
                    itemFrequency.put(item, 1);
                } else {
                    itemFrequency.put(item, frequency + 1);
                }
            }

            // Track the longest itemset size
            if (transaction.length > longestItemSet) {
                longestItemSet = transaction.length;
            }

            // Generate and update all 2-item patterns in the matrix
            for (int i = 0; i < transaction.length; i++) {
                Integer item1 = transaction[i];

                // Iterate over the remaining items in the transaction to form 2-item patterns
                for (int j = i + 1; j < transaction.length; j++) {
                    Integer item2 = transaction[j];

                    // Increment the support count for this item pair in the matrix
                    matrix.incrementCount(item1, item2);
                }
            }
        }

        // Initialize list of all items here
        allItems = new int[itemFrequency.size()];
        int index = 0;
        // Now calculate the cumulative weight and populate the cumulativeWeights list
        MapIntToInt.EntryIterator iter = itemFrequency.iterator();
        while (iter.hasNext()) {
            MapIntToInt.MapEntryIntToInt entry = iter.next();
            allItems[index++] = entry.getKey();
            totalWeight += entry.getValue();
            cumulativeWeights.add(new AbstractMap.SimpleEntry<>(entry.getKey(), totalWeight));
        }
        Arrays.sort(allItems);
    }

    /**
     * Counts the occurrences of a specific pattern in the provided database of
     * transactions.
     *
     * This method checks each transaction to see if it contains all items in the
     * given pattern. If so, the transaction is considered a match and is counted.
     *
     * @param pattern  the pattern to search for in the database (a list of
     *                 integers).
     * @param database the list of transactions to search through.
     * @return the number of transactions that contain all items in the specified
     *         pattern.
     */

    public static int countOccurrencesInDatabase(int[] pattern, List<int[]> database) {
        if (pattern.length == 0) {
            return 0;
        }

        int count = 0;
        for (int[] transaction : database) {
            if (containsOrEquals(transaction,pattern)) {
                count++;
            }
        }
        return count;
    }

    public static boolean containsAll(int[] list, List<Integer> pattern) {
        // TODO: NOT EFFICIENT. IF THE ITEMSETS ARE SORTED, IT WOULD BE MUCH FASTER!
        // **********************************************************************

        // For each element in the list, check if it exists in the array
        for (int element : pattern) {
            boolean found = false;

            for (int item : list) {
                if (item == element) {
                    found = true;
                    break;
                }
            }
            if (found == false) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsAll(int[] list, int listLength, int[] pattern) {
        // TODO: NOT EFFICIENT. IF THE ITEMSETS ARE SORTED, IT WOULD BE MUCH FASTER!
        // **********************************************************************
        int j = 0;
        // For each element in the list, check if it exists in the array
        for (int element : pattern) {
            boolean found = false;

            for ( ; j < listLength; j++) {
                int item = list[j];
                if (item == element) {
                    found = true;
                    break;
                }
            }
            if (found == false) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsAll(int[] list, ArrayList<Integer> pattern) {
        // TODO: NOT EFFICIENT. IF THE ITEMSETS ARE SORTED, IT WOULD BE MUCH FASTER!
        // **********************************************************************

        // For each element in the list, check if it exists in the array
        for (int element : pattern) {
            boolean found = false;

            for (int item : list) {
                if (item == element) {
                    found = true;
                    break;
                }
            }
            if (found == false) {
                return false;
            }
        }
        return true;
    }
    /**
     * Creates a deep copy of the provided database.
     *
     * This method duplicates the database such that modifications to the copied
     * database do not affect the original one. Each transaction (which is a list of
     * integers) is copied individually.
     *
     * @param database the original database of transactions.
     * @return a new database that is a deep copy of the original.
     */
    public static List<int[]> copyDatabase(List<int[]> database) {
        List<int[]> copy = new ArrayList<>(database.size());

        for (int[] transaction : database) {
            copy.add(Arrays.copyOf(transaction, transaction.length));
        }

        return copy;
    }

    /**
     * Sorts the codetable based on three criteria: 1. The length of the patterns
     * (longer patterns come first). 2. The number of occurrences of each pattern
     * (more frequent patterns come first). 3. Lexicographical order (alphabetical
     * order) if both length and frequency are the same.
     *
     * This method modifies the `codetable` in place by sorting the patterns
     * according to the rules mentioned.
     */
    public static void sortCodetable() {
        Collections.sort(codetable, new Comparator<int[]>() {
            @Override
            public int compare(int[] pattern1, int[] pattern2) {
                // 1. 按大小（长度）排序
                if (pattern1.length != pattern2.length) {
                    return Integer.compare(pattern2.length, pattern1.length);
                }

                // 2. 按出现次数排序
                int count1 = itemsetCount.getOrDefault(pattern1, 0);
                int count2 = itemsetCount.getOrDefault(pattern2, 0);
                if (count1 != count2) {
                    return Integer.compare(count2, count1); // 降序排序
                }

                // 3. 按字母序（数值大小）排序
                for (int i = 0; i < Math.min(pattern1.length, pattern2.length); i++) {
                    int cmp = Integer.compare(pattern1[i], pattern2[i]);
                    if (cmp != 0) {
                        return cmp;
                    }
                }
                return 0;
            }
        });
    }


    /**
     * Updates the matrix of item pair supports by recalculating the supports based
     * on the current database.
     *
     * This method clears the matrix and then iterates through the database,
     * updating the support count for all 2-item patterns in each transaction.
     *
     * @param database the list of transactions to process.
     */
    public static void updateMatrix(List<int[]> database) {
        matrix.clear();
        for (int[] transaction : database) {
            // Generate and update all 2-item patterns in the matrix
            for (int i = 0; i < transaction.length; i++) {
                Integer item1 = transaction[i];

                // Iterate over the remaining items in the transaction to form 2-item patterns
                for (int j = i + 1; j < transaction.length; j++) {
                    Integer item2 = transaction[j];

                    // Increment the support count for this item pair in the matrix
                    matrix.incrementCount(item1, item2);
                }
            }
        }
    }

}
