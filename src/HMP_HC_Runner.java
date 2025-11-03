import ca.pfv.spmf.datastructures.collections.map.AMapIntToInt;
import ca.pfv.spmf.datastructures.collections.map.MapIntToInt;

import java.io.*;
import java.util.*;

/**
 * HMP_HC_Runner implements a Hill Climbing-based Heuristic Mining Pattern algorithm
 * for data compression optimization. The algorithm discovers frequent patterns
 * that best compress transactional databases using local search optimization.
 */
public class HMP_HC_Runner {

    // region Static Configuration
    /** Maximum iterations per pattern optimization in hill climbing */
    static final int MAX_ITERATIONS = 31;

    /** Minimum compression improvement ratio for pattern acceptance */
    static final double IMPROVEMENT_THRESHOLD = 0.001;
    // endregion

    // region Global Data Structures
    /** Code table storing accepted compression patterns */
    static ArrayList<int[]> codetable = new ArrayList<>();

    /** Sorted array of all unique items in the database */
    static int[] allItems = null;

    /** Frequency map for individual items */
    static MapIntToInt itemFrequency;
    /** Length of the longest transaction in the database */
    static int longestItemSet = 0;
    /** Total weight for weighted random selection */
    static int totalWeight = 0;

    /** Cumulative weights for item selection */
    static List<Map.Entry<Integer, Integer>> cumulativeWeights = null;

    /** Frequency map for stored patterns */
    static Map<int[], Integer> itemsetCount = new HashMap<>();

    /** Triangular matrix for pairwise item co-occurrences */
    static SparseTriangularMatrix matrix = null;

    /** Reusable buffer for transaction processing */
    static int[] BUFFER = new int[500];

    /** Random number generator */
    static Random random = new Random(System.currentTimeMillis());

    /** Maximum allowed size for the code table */
    static int max_code_table_size = 1201;
    // endregion

    /**
     * Main method to execute the Hill Climbing pattern mining algorithm.
     * Processes a specified dataset and outputs compression results.
     * 
     * @param args Command line arguments (not used)
     * @throws IOException If file operations fail
     */
    public static void main(String[] args) throws IOException {
        // Specify the single file to process
        String filePath = "Datasets/adult.txt"; // Replace with different actual file path

        // Create a File object for the specified file
        File file = new File(filePath);

        if (file.exists() && file.isFile()) {
            // Extract the file name
            String fileName = file.getName(); // e.g., "yourfile.txt"

            // Remove the file extension to get the base name
            int dotIndex = fileName.lastIndexOf('.');
            String baseName = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex); // e.g., "yourfile"

            // Construct the output file name by appending the base name
            String outputFile = "result_HCMP_" + baseName  + ".txt"; // e.g., "result_HC_yourfile.txt"
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

                for (int run = 1; run <= 10; run++) { // Execute 10 times for the specified file
                    writer.write("Processing file: " + file.getName() + ", Run: " + run + "\n");
                    System.out.println("Processing file: " + file.getName() + ", Run: " + run);


                    // Read the database from the file
                    List<int[]> database = readItemsetsFromFile(file.getPath());
                    initializeDatabase(database);

                    // Initialize compression calculations and performance monitoring
                    double initialCompressionSize = calculateSizeInBits(database);
                    double currentCompressionSize = initialCompressionSize;
                    double improveCompressionSize = initialCompressionSize;

                    writer.write("Initial compression size: " + initialCompressionSize + "\n");

                    int iterations = 0;
                    // Record start time for performance measurement
                    long startTime = System.currentTimeMillis();

                    // Initialize memory usage monitoring
                    MemoryLogger logger = MemoryLogger.getInstance();
                    logger.reset(); // Reset memory logger before each execution

                    Random random = new Random();

                    List<int[]> modifiedDatabase = copyDatabase(database);
                    int codeTableTryCount = 0;

                    // Main Hill Climbing loop - continue until code table reaches size limit
                    while (codetable.size() < max_code_table_size / 3) {

                        // Generate a random pattern as starting point for hill climbing
                        int[] pattern = generateRandomItemset(longestItemSet,itemFrequency, codetable);
                        iterations = 0;
                        int iterationTryCount = 0;
                        
                        // Hill climbing iterations for current pattern
                        while (iterations < MAX_ITERATIONS) {
                            // Create temporary code table to test pattern addition
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

                            iterations++;
                            iterationTryCount++;
                            if (iterationTryCount > 20) { //If the pattern can not find good neighbors, we should move to the next pattern.
                                break;
                            }
                        }

                        if (isPatternInDatabase(pattern, modifiedDatabase)) {

                            if ((( currentCompressionSize- improveCompressionSize) / initialCompressionSize) > IMPROVEMENT_THRESHOLD) {
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
                        codeTableTryCount++;
                        if (codeTableTryCount > 100 && codeTableTryCount > max_code_table_size/3 ) {
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

                        //Update the database and matrix after adding the pattern
                        modifiedDatabase = deleteItemset(modifiedDatabase, pattern);
                        updateMatrix(modifiedDatabase);

                        // Recalculate the patterns and sort again after processing the pattern
                        length2Patterns = matrix.getAllPatternsWithOccurrences();
                        length2Patterns.sort((entry1, entry2) -> Integer.compare(entry2.count, entry1.count));
                        totalPatterns = length2Patterns.size();

                        // Determine whether the difference between the current compression size and the
                        // previous compression size is less than the threshold
                        if (codetable.size() > max_code_table_size-1) {
                            break; // If the difference is less than the threshold, stop the loop
                        }

                    }


                    // Final compression size calculation
                    improveCompressionSize = deleteAndCalculateSizeInBits(database, codetable);

                    // Clear matrix after each run
                    matrix.clear();

                    // Write the final code table and results to the file
                    writer.write("Final codetable: " + "\n");
                    for (int[] pattern : codetable) {
                        writer.write("Pattern: " + Arrays.toString(pattern) + " count: " + itemsetCount.get(pattern));
                        writer.newLine();
                    }
                    writer.write("Final codetable size: " + codetable.size() + "\n");
                    writer.write("Final Compression size: " + improveCompressionSize + "\n");
                    writer.write("Compression ratio: " + (improveCompressionSize / initialCompressionSize * 100) + "%\n");
                    writer.write("Pattern from sa:" + pattern_sa + "\n");

                    // Stop recording memory usage
                    double currentMemoryUsage = logger.checkMemory();
                    logger.stopRecordingMode();

                    // Write memory and time statistics
                    long endTime = System.currentTimeMillis();
                    long elapsedTime = endTime - startTime;
                    writer.write("Elapsed time: " + ((double) elapsedTime) / 1000 + " seconds\n");
                    writer.write("Memory usage: " + currentMemoryUsage + " MB\n\n");
                    writer.write("Iterations: " + iterations + "\n\n");

                    // Clear the codetable and related data structures after each run
                    codetable.clear();
                    itemsetCount.clear();
                    itemFrequency.clear();
                    cumulativeWeights.clear();
                    totalWeight = 0;
                    // Flush the writer after each run
                    writer.flush();
                }

            } catch (IOException e) {
                System.err.println("Error writing to the output file: " + e.getMessage());
            }
        } else {
            System.err.println("The specified file does not exist or is not a file: " + filePath);
        }
    }



    /**
     * Calculates raw database size in bits without compression
     * @param database Transactional database
     * @return Total size in bits (32 bits per integer)
     */
    /**
     * Calculates the uncompressed size in bits of a transactional database.
     * This method provides the baseline compression size before pattern application.
     * 
     * @param database The list of transactions to evaluate
     * @return Total size in bits (each integer takes 32 bits)
     */
    public static int calculateSizeInBits(List<int[]> database) {
        // Calculate the size in bits of the database
        int totalSizeInBits = 0;

        for (int[] transaction : database) {
            // Each integer is 32 bits
            totalSizeInBits += transaction.length * Integer.SIZE;
        }

        return totalSizeInBits;
    }


    /**
     * Calculates compressed size using current code table
     * @param database Transactional database
     * @param codeTable Current compression patterns
     * @return Total size including both residual data and pattern codes
     */
    public static int deleteAndCalculateSizeInBits(List<int[]> database, List<int[]> codeTable) {

        Map<int[], Integer> patternCount = new HashMap<>();
        // Calculate the size in bits of the modified database
        int totalSizeInBits = 0;
        // Process each transaction in order
        for (int[] transaction : database) {
            // Dynamic expansion mechanism
            if (BUFFER.length < transaction.length) {
                BUFFER = new int[Math.max(transaction.length * 2, BUFFER.length)]; // 双倍扩容策略
            }

            // Copy transactions to buffer (overwrite only necessary parts)
            System.arraycopy(transaction, 0, BUFFER, 0, transaction.length);
            int newLength = transaction.length;

            // for each pattern
            for (int[] pattern : codeTable) {
                if (newLength >= pattern.length && containsAll(BUFFER, newLength, pattern)) {
                    // Increment the count for the pattern
                    patternCount.put(pattern, patternCount.getOrDefault(pattern, 0) + 1);
                    // Remove the pattern from the transaction
                    removePatternFromTransaction(BUFFER, newLength, pattern);
                    newLength = newLength - pattern.length;
                }
            }
            totalSizeInBits += newLength * Integer.SIZE;
        }
        //Add the size of the transactions in finalResult multiplied by their counts
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
     * Removes pattern elements from a transaction buffer
     * @param transaction Transaction buffer (modified in-place)
     * @param transactionLength Valid length of transaction data
     * @param pattern Pattern to remove
     * @return New transaction array with pattern elements removed
     */
    public static int[] removePatternFromTransaction(int[] transaction, int transactionLength, int[] pattern) {
        // Use HashSet to improve search efficiency
        int pos = 0;
        for (int j = 0; j < transactionLength; j++) {
            int item = transaction[j];
            // 二分查找替代Hash查找
            if (Arrays.binarySearch(pattern, item) < 0) {
                transaction[pos++] = item;
            }
        }
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
    /**
     * Reads a transactional database from a text file.
     * Each line represents a transaction with space-separated integer items.
     * Transactions are automatically sorted for consistent processing.
     * 
     * @param fileName Path to the input file containing the database
     * @return List of integer arrays representing transactions
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

                // Sort the transaction for consistent processing
                Arrays.sort(transaction);
                database.add(transaction);
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return database;
    }

    static int[] SMALLBUFFER = new int[10];

    /**
     * Generates random itemset weighted by item frequencies
     * @param longestItemSet Length of longest transaction
     * @param itemFrequency Item frequency distribution
     * @param currentPatternSet Existing patterns to avoid duplicates
     * @return New unique pattern not present in currentPatternSet
     */
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


    /**
     * Generates a neighbor pattern using the two-flip operation for hill climbing.
     * This method randomly selects two items and either adds them to the pattern
     * (if not present) or removes them (if present), creating pattern diversity
     * for the local search optimization.
     * 
     * @param currentPattern Base pattern for modification
     * @return New pattern with two items added/removed, or null if pattern becomes too small
     */
    public static int[] generateTwoFlipNeighborPattern(int[] currentPattern) {

        boolean ok = false;
        while (ok == false) {
            int length = currentPattern.length;

            // Copy the current pattern to working buffer
            System.arraycopy(currentPattern, 0, BUFFERFLIP, 0, currentPattern.length);

            // Choose two distinct items to flip based on their frequency weights
            int flipItem1 = itemToIndex(getWeightedRandomItem());
            int flipItem2;
            do {
                flipItem2 = itemToIndex(getWeightedRandomItem());
            } while (flipItem1 == flipItem2); // Ensure the items are different

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
    /**
     * Selects a random item based on weighted probability distribution.
     *
     * Utilizes cumulative frequency weights to achieve O(log n) selection time.
     * The probability of selecting an item is proportional to its frequency weight.
     *
     * @return The selected item's identifier
     * @throws IllegalStateException if total weight is uninitialized or invalid (<= 0)
     *
     * Implementation Details:
     * 1. Requires cumulativeWeights to be sorted in ascending order
     * 2. Requires totalWeight = sum of all individual weights
     * 3. Uses binary search to find the first cumulative weight greater than random value
     *
     * Example: For weights [A:30, B:50, C:20], cumulative weights [30, 80, 100]
     * Random number 45 would select B (first cumulative weight >45 is 80)
     */
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
            if (containsOrEquals(transaction, pattern)) {
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
        // Linear search through sorted items array
        // Note: Could be optimized with binary search since allItems is sorted,
        // or with a HashMap for O(1) lookup if frequent access is needed
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

        // Double pointer traversal, assuming itemset1 and itemset2 are already sorted
        while (i < itemset1.length && j < itemset2.length) {
            if (itemset1[i] == itemset2[j]) {
                j++;  // If a matching element is found, continue matching the next element of itemset2
            }
            i++;  // Regardless, i must move forward
        }

        // If j reaches the end of itemset2, then itemset1 contains all the elements of itemset2.
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

    /**
     * Checks if a list contains all elements of a pattern (unsorted version).
     *
     * Performs a linear search for each pattern element in the list. Suitable for
     * unsorted datasets. For sorted datasets, consider using a more efficient
     * algorithm like binary search or two-pointer technique.
     *
     * @param list         The target array to search in
     * @param listLength   Valid length of the list array (may contain trailing elements)
     * @param pattern      The elements to search for
     * @return             true if all pattern elements exist in the list's valid range,
     *                     false otherwise
     */

    public static boolean containsAll(int[] list, int listLength, int[] pattern) {
        // Early exit for empty pattern
        if (pattern.length == 0) return true;

        int i = 0; // Pointer for list
        int j = 0; // Pointer for pattern

        // Traverse both arrays simultaneously
        while (i < listLength && j < pattern.length) {
            if (list[i] == pattern[j]) {
                // Match found, move both pointers
                i++;
                j++;
            } else if (list[i] < pattern[j]) {
                // List element smaller, move list pointer
                i++;
            } else {
                // List element larger, pattern element missing
                return false;
            }
        }

        // Verify all pattern elements were found
        return j == pattern.length;
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





