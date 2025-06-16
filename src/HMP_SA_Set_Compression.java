
import ca.pfv.spmf.datastructures.collections.map.AMapIntToInt;
import ca.pfv.spmf.datastructures.collections.map.MapIntToInt;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class HMP_SA_Set_Compression {

    // Constants for the simulated annealing algorithm
    static final double INITIAL_TEMPERATURE = 100;
    static final double MIN_TEMPERATURE = .1;
    static final double COOLING_RATE = 0.8;

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

    static int max_code_table_size = 170;
    static double stop_compression = 24.1;

    public static void main(String[] args) {

        // Input file containing the sequence of itemsets
        String inputFile = "Datasets/ionosphere.txt";

        List<int[]> database = readItemsetsFromFile(inputFile);
        initializeDatabase(database);
        updateMatrix(database);

        // Perform the main algorithm logic (this is the existing code logic)
        double initialCompressionSize = calculateSizeInBits(database);
        double currentCompressionSize = initialCompressionSize;
        double improveCompressionSize = initialCompressionSize;
        System.out.println("Initial compression size: " + initialCompressionSize);

        double temperature;
        int iterations = 0;
        int dealPatterns = 0;
        int sa_work = 0;
        double curComSize = 0;
        // Record start time
        long startTime = System.currentTimeMillis();
        // Record memory usage
        MemoryLogger logger = MemoryLogger.getInstance();
        Random random = new Random();

        List<int[]> modifiedDatabase = copyDatabase(database);
        int codeTableTryCount = 0;
        // Main loop for the simulated annealing process
        while (codetable.size() < max_code_table_size /3) {// Control the number of patterns from SA added to the
            // codetable

            iterations = 0;
            temperature = INITIAL_TEMPERATURE;
            // Generate a random pattern
            int[] pattern = generateRandomItemset(longestItemSet,itemFrequency, codetable);

            int temperatureTryCount = 0;
            while (temperature > MIN_TEMPERATURE) {
                // Try to add a pattern to the codetable
                ArrayList<int[]> temp_ct = new ArrayList<>(codetable);
                // Generate neighbor pattern according to FLIP_NUM
                int[] newPattern = generateTwoFlipNeighborPattern(pattern);

                if (isPatternInDatabase(newPattern, modifiedDatabase)) {

                    // Try to add new pattern to codetable
                    temp_ct.add(newPattern);
                    int newCompressionSize = deleteAndCalculateSizeInBits(modifiedDatabase, temp_ct);

                    // Calculate the improvement when adding this pattern
                    double compressionImprovement = (improveCompressionSize - newCompressionSize)
                            / improveCompressionSize;
                    double acceptanceProbability = Math
                            .exp((improveCompressionSize - newCompressionSize) / temperature);

                    if (compressionImprovement > IMPROVEMENT_THRESHOLD) {
                        // Accept the new pattern set because it improves
                        improveCompressionSize = newCompressionSize;
                        pattern = newPattern;
                        temperatureTryCount =0;
                    } else if ( acceptanceProbability > random.nextDouble()) {
                        // Accept the new pattern with a certain probability
                        improveCompressionSize = newCompressionSize;
                        pattern = newPattern;
                        sa_work++;
                        temperatureTryCount =0;
                    }
                }

                // Decrease the temperature
                temperature *= COOLING_RATE;
                iterations++;
                temperatureTryCount++;
                if (temperatureTryCount > 10) { //If the pattern can not find good neighbors, we should move to the next pattern.
                    break;
                }
            }

            if (isPatternInDatabase(pattern, modifiedDatabase)) {

                if (((currentCompressionSize - improveCompressionSize) / initialCompressionSize) > IMPROVEMENT_THRESHOLD) {
                    codeTableTryCount = 0;
                    codetable.add(pattern);
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
                        cumulativeWeights
                                .add(new AbstractMap.SimpleEntry<>(entry.getKey(), totalWeight));
                    }
                    currentCompressionSize = improveCompressionSize;
                }

            }
            if (currentCompressionSize / initialCompressionSize < (stop_compression/100)){break;}
            codeTableTryCount++;
            dealPatterns++;
            if (codeTableTryCount > 100 || codeTableTryCount > max_code_table_size/3 ) { //or when it is less than 1/3 codetablesize
                break;
            }

            codeTableTryCount++;
            dealPatterns++;
            if (codeTableTryCount > (100) || codeTableTryCount > max_code_table_size/3  ) {
                break;
            }
        }
        for (int[] pattern : codetable) {
            int occ = countOccurrencesInDatabase(pattern, database);
            itemsetCount.put(pattern, occ);
        }

        int pattern_sa = codetable.size();

        updateMatrix(modifiedDatabase);

        // After the main loop ends, get the pattern of length 2 from the matrix and add
        // it to the codetable according to the number of occurrences
        List<Pair> length2Patterns = matrix.getAllPatternsWithOccurrences();

        // Sort by number of occurrences in descending order
        length2Patterns.sort((entry1, entry2) -> Integer.compare(entry2.count, entry1.count));

        // Initialize variables
        int totalPatterns = length2Patterns.size();

        // Loop until the compressed size change is less than the threshold or all
        // patterns are processed
        while (totalPatterns > 0) {


            // Get the next pattern to add (highest occurrence)
            Pair entry = length2Patterns.get(0);
            int[] pattern = entry.pattern;
            int count = entry.count;

            // Add the pattern to the codetable and itemsetCount
            codetable.add(pattern);
            itemsetCount.put(pattern, count);
            currentCompressionSize = deleteAndCalculateSizeInBits(database,codetable);
            double curCompression =  currentCompressionSize/ initialCompressionSize;
            if ( curCompression < (stop_compression/100)) {
                break;
            }
//			 Update the database and matrix after adding the pattern
            modifiedDatabase = deleteItemset(modifiedDatabase, pattern);
            updateMatrix(modifiedDatabase);

            // Recalculate the patterns and sort again after processing the pattern
            length2Patterns = matrix.getAllPatternsWithOccurrences();
            length2Patterns.sort((entry1, entry2) -> Integer.compare(entry2.count, entry1.count));
            totalPatterns = length2Patterns.size();

            // Determine whether the difference between the current compression size and the
            // previous compression size is less than the threshold
//            if (codetable.size() > max_code_table_size-1) {
//                break; // If the difference is less than the threshold, stop the loop
//            }

        }


        double currentMemoryUsage = logger.checkMemory();
        System.out.println("Current memory usage: " + currentMemoryUsage + " MB");
        logger.stopRecordingMode();
        logger.reset();

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Elapsed time: " + (double) (elapsedTime) / 1000 + " seconds");


        currentCompressionSize = deleteAndCalculateSizeInBits(database, codetable);
        System.out.println("Final codetable: ");
        for (int[] pattern : codetable) {
            System.out.println("Pattern: " + Arrays.toString(pattern) + " count:" + itemsetCount.get(pattern));
        }
        ;
        System.out.println("Code Table size: " + codetable.size());
        System.out.println("Iterations:" + iterations);
        System.out.println("Deal pattern:" + dealPatterns);
        System.out.println("Pattern from sa:" + pattern_sa);
        System.out.println("SA work: " + sa_work);
        System.out.println("Final Compression size: " + currentCompressionSize + ", Compression ratio: "
                + (double) currentCompressionSize / initialCompressionSize * 100);


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
