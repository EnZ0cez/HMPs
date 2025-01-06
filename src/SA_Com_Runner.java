import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import ca.pfv.spmf.datastructures.collections.map.AMapIntToInt;
import ca.pfv.spmf.datastructures.collections.map.MapIntToInt;
import ca.pfv.spmf.datastructures.collections.map.MapIntToInt.EntryIterator;
import ca.pfv.spmf.datastructures.collections.map.MapIntToInt.MapEntryIntToInt;

public class SA_Com_Runner {

	// Constants for the simulated annealing algorithm
	static final double INITIAL_TEMPERATURE = 100;
	static final double MIN_TEMPERATURE = .1;
	static final double COOLING_RATE = 0.98;
	static int pattern_set_size = 0;
	
	// Define threshold, a pattern must improve more than threshold
	static final double IMPROVEMENT_THRESHOLD = 0.0001;
	static final double ACCEPTANCE_THRESHOLD = 0.005;

	// Codetable to store accepted patterns
	static ArrayList<int[]> codetable = null;
	
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
	static Map<int[], Integer> itemsetCount = null;
	
	// Matrix for size 2 patterns
	static SparseTriangularMatrix matrix = null;

	/** buffer **/
	static int[] BUFFER = new int[500];

	static Random random = new Random(System.currentTimeMillis());

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

	public static void main(String[] args) throws IOException {
		// The folder containing the .txt files
		String folderPath = "src/";

		// Output file to write results
		String outputFile = "result_SA_Com.txt";

		// Create a File object for the folder
		File folder = new File(folderPath);

		// Get all .txt files in the folder
		File[] files = folder.listFiles((dir, name) -> name.endsWith(".txt"));

		double timeStart = System.currentTimeMillis();

		if (files != null) {
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
				// Iterate through all .txt files
				for (File file : files) {

					List<int[]> database = readItemsetsFromFile(file.getPath());
					initializeDatabase(database);

					for (int i = 1; i <= 10; i++) { // Execute five times for each file
						writer.write("Processing file: " + file.getName() + ", Run: " + i + "\n");
						System.out.println("Processing file: " + file.getName() + ", Run: " + i);

						// Initialize the codetable before processing each file
						codetable = new ArrayList<>();
						itemsetCount = new HashMap<int[], Integer>();

						// Initialize with a random pattern set
						List<int[]> currentPatternSet = new ArrayList<>();
						pattern_set_size = (int) Math.sqrt(database.size());

						updateMatrix(database);

						// Generate initial pattern set
						for (int j = 0; j < pattern_set_size; j++) {
							currentPatternSet
									.add(generateRandomItemset(longestItemSet, itemFrequency, currentPatternSet));
						}

						// Perform the main algorithm logic (this is the existing code logic)
						double initialCompressionSize = calculateSizeInBits(database);
						double currentCompressionSize = initialCompressionSize;
						writer.write("Initial compression size: " + initialCompressionSize + "\n");

						double temperature = INITIAL_TEMPERATURE;
						int iterations = 0;
						int sa_working = 0;

						// Record start time
						long startTime = System.currentTimeMillis();
						// Record memory usage
						MemoryLogger logger = MemoryLogger.getInstance();
						logger.reset(); // Reset memory logger before each execution

						List<int[]> modifiedDatabase = copyDatabase(database);

						// Main loop for the simulated annealing process
						for (int[] pattern : currentPatternSet) {
							while (temperature > MIN_TEMPERATURE) {
								// Try to add a pattern to the codetable
								List<int[]> temp_ct = new ArrayList<>(codetable);
								// Generate neighbor pattern
								int[] newPattern = generateTwoFlipNeighborPattern(pattern);

								if (isPatternInDatabase(newPattern, database)) {
									// Try to add new pattern to codetable
									temp_ct.add(newPattern);
									int newCompressionSize = deleteAndCalculateSizeInBits(modifiedDatabase, temp_ct);

									// Calculate the improvement when adding this pattern
									double compressionImprovement = (currentCompressionSize - newCompressionSize)
											/ currentCompressionSize;

									if (compressionImprovement > IMPROVEMENT_THRESHOLD) {
										// Accept the new pattern set because it improves
										currentCompressionSize = newCompressionSize;
										pattern = newPattern;
									} else if (countOccurrencesInDatabase(newPattern, database) > 0 && Math
											.exp((currentCompressionSize - newCompressionSize) / (temperature)) > random
													.nextDouble()) {
										// Accept the new pattern with a certain probability
										currentCompressionSize = newCompressionSize;
										pattern = newPattern;
										sa_working++;
									}
								}

								// Decrease the temperature
								temperature *= COOLING_RATE;
								iterations++;
							}

							if (isPatternInDatabase(pattern, database)) {
								List<int[]> temp_ct = new ArrayList<>(codetable);
								temp_ct.add(pattern);
								int newComSize = deleteAndCalculateSizeInBits(modifiedDatabase, temp_ct);
								if (((currentCompressionSize - newComSize)
										/ initialCompressionSize) > IMPROVEMENT_THRESHOLD) {
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

//									for (Map.Entry<Integer, Integer> entry : itemFrequency.entrySet()) {
									EntryIterator iter = itemFrequency.iterator();
									while (iter.hasNext()) {
										MapEntryIntToInt entry = iter.next();
										totalWeight += entry.getValue();
										cumulativeWeights
												.add(new AbstractMap.SimpleEntry<>(entry.getKey(), totalWeight));
									}
								}
								currentCompressionSize = newComSize;
							}

						}

						for (int[] pattern : codetable) {
							int occ = countOccurrencesInDatabase(pattern, database);
							itemsetCount.put(pattern, occ);
						}

						updateMatrix(modifiedDatabase);

						// After the main loop ends, get the pattern of length 2 from the matrix and add
						// it to the codetable according to the number of occurrences
						List<Pair> length2Patterns = matrix.getAllPatternsWithOccurrences();

						// Sort by number of occurrences in descending order
						length2Patterns.sort((entry1, entry2) -> Integer.compare(entry2.count, entry1.count));

						// Calculate the total number of current patterns in the matrix
						int totalPatterns = length2Patterns.size();
						int batchSize = Math.max(1, totalPatterns / 20); // Calculate batch size (10% of total, at least
																			// 1)
						int addedPatterns = 0; // Record the number of patterns that have been added
						int newCompressionSize = 0;
						// Loop until the compressed size change is less than the threshold or all
						// patterns are processed
						while (addedPatterns < totalPatterns) {

							// Process the next batch of patterns
							for (int j = 0; j < batchSize && addedPatterns < totalPatterns; j++) {
								// Get the next pattern to add
								Pair entry = length2Patterns.get(0);
								int[] pattern = entry.pattern;
								int count = entry.count;

//								Arrays.sort(pattern);

								// Directly join codetable and itemsetCount
								codetable.add(pattern);
								itemsetCount.put(pattern, count);

								// Update the database and matrix after adding the pattern
								modifiedDatabase = deleteItemset(modifiedDatabase, pattern);
								updateMatrix(modifiedDatabase);
								// Recalculate the patterns and sort again after processing the batch
								length2Patterns = matrix.getAllPatternsWithOccurrences();
								totalPatterns = length2Patterns.size();
								length2Patterns.sort((entry1, entry2) -> Integer.compare(entry2.count, entry1.count));

								// Increment the number of patterns that have been added
								addedPatterns++;
							}

							batchSize = Math.max(1, totalPatterns / 20); // Recalculate the batch size
							// Calculate the current compression size after each batch
							newCompressionSize = deleteAndCalculateSizeInBits(database, codetable);

							// Determine whether the difference between the current compression size and the
							// previous compression size is less than the threshold
							if (Math.abs(currentCompressionSize - newCompressionSize)
									/ initialCompressionSize < ACCEPTANCE_THRESHOLD) {
								break; // If the difference is less than the threshold, stop the loop
							}
							currentCompressionSize = newCompressionSize;
						}

						sortCodetable();

						// Final compression size calculation
						currentCompressionSize = deleteAndCalculateSizeInBits(database, codetable);

						// Clear matrix after each run
						matrix.clear();

						writer.write("Final codetable: " + "\n");
						for (int[] pattern : codetable) {
//							List<Integer> sortedPattern = new ArrayList<>(pattern);
//							Collections.sort(sortedPattern);
							writer.write(
									"Pattern: " + Arrays.toString(pattern) + " count:" + itemsetCount.get(pattern));
							writer.newLine();
						}
						// Write the final code table and results to the file
						writer.write("Final codetable size: " + codetable.size() + "\n");
						writer.write("Final Compression size: " + currentCompressionSize + "\n");
						writer.write("Compression ratio: " + currentCompressionSize / initialCompressionSize * 100);
						writer.newLine();
						// Stop recording memory usage
						double currentMemoryUsage = logger.checkMemory();
						logger.stopRecordingMode();

						// Write memory and time statistics
						long endTime = System.currentTimeMillis();
						long elapsedTime = endTime - startTime;
						writer.write("Elapsed time: " + (double) (elapsedTime) / 1000 + " seconds\n");
						writer.write("Memory usage: " + currentMemoryUsage + " MB\n\n");
						writer.write("SA working: " + sa_working + "\n\n");
						// Flush the writer after each run
						writer.flush();
					}

				}
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("No .txt files found in the specified folder.");
			}
		}

		double timEnd = System.currentTimeMillis();
		System.out.println("Total time: " + ((timEnd - timeStart) / 1000d) + " s");
		System.out.print("Memory: " + MemoryLogger.getInstance().getMaxMemory() + " MB");
	}

	public static int calculateSizeInBits(List<int[]> database) {
//		Map<int[], Integer> patternCount = new HashMap<>();

		// Calculate the size in bits of the modified database
		int totalSizeInBits = 0;

		for (int[] transaction : database) {
			// Each integer is 32 bits
			totalSizeInBits += transaction.length * Integer.SIZE;
		}

		return totalSizeInBits;
	}


	public static int deleteAndCalculateSizeInBits(List<int[]> database, List<int[]> codeTable) {
		List<int[]> modifiedDatabase = new ArrayList<>();
		Map<PatternKey, Integer> patternCount = new HashMap<>();

		// Initialize pattern counts to zero
		for (int[] pattern : codeTable) {
			patternCount.put(new PatternKey(pattern), 0);
		}

		// Process each transaction in order
		for (int[] transaction : database) {
			// Copy the transaction
			System.arraycopy(transaction, 0, BUFFER, 0, transaction.length);

			int length = transaction.length;

			for (int[] pattern : codeTable) {
				PatternKey patternKey = new PatternKey(pattern);
				if (containsAll(BUFFER, length, pattern)) {
					// Increment the count for the pattern
					patternCount.put(patternKey, patternCount.get(patternKey) + 1);

					// Remove the pattern from the transaction
					BUFFER = removePatternFromTransaction(BUFFER, length, pattern);

					length = length - pattern.length;
				}
			}

			// Add the modified transaction to the result
			int[] modifiedTransaction = Arrays.copyOf(BUFFER, length);
			modifiedDatabase.add(modifiedTransaction);

			BUFFER = new int[500];
		}

		// Calculate the size in bits of the modified database
		int totalSizeInBits = 0;

		for (int[] transaction : modifiedDatabase) {
			// Each integer is 32 bits
			totalSizeInBits += transaction.length * Integer.SIZE;
		}

		// Add the size of the transactions in finalResult multiplied by their counts
		for (Map.Entry<PatternKey, Integer> entry : patternCount.entrySet()) {
			int patternSize = 0;
			if (entry.getValue() != 0) {
				int keySize = entry.getKey().pattern.length;
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
		int pos = 0;
		// for each item of this itemset
		for (int j = 0; j < transactionLength; j++) {
			// copy the item except if it is not an item that should be excluded
			if (Arrays.binarySearch(pattern, transaction[j]) < 0) {
				transaction[pos++] = transaction[j];
				pos++;
			}
		}
		return transaction; // return the copy
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
	
	


	public static List<int[]> deleteItemset(List<int[]> database, int[] pattern) {
		List<int[]> modifiedDatabase = new ArrayList<>();
//		int patCounter = 0;

		for (int[] transaction : database) {

			if (containsAll(transaction, pattern)) {
				transaction = cloneItemSetMinusAnItemset(transaction, pattern);
//				patCounter++;
			} else {
				modifiedDatabase.add(transaction);
			}

		}

		return modifiedDatabase;
	}



	// Reads itemsets from a file and returns them as a list of transactions

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

	// Generates a neighboring pattern by modifying or adding a single pattern in
	// the current pattern set
	public static ArrayList<Integer> generateNeighborPattern(ArrayList<Integer> currentPattern) {
		// Create a binary vector representing the current pattern
		int[] binaryVector = new int[allItems.length];

		// Set the binary vector based on the current pattern
		for (int i = 0; i < allItems.length; i++) {
			if (currentPattern.contains(allItems[i])) {
				binaryVector[i] = 1; // Set 1 if the item is in the pattern
			} else {
				binaryVector[i] = 0; // Set 0 if the item is not in the pattern
			}
		}

		// Choose an index to flip based on the frequency of each item
		int flipIndex = itemToIndex(getWeightedRandomItem());// Adjust for 0-based index

		// Only flip to 1 (add an item) if the current pattern length is less than the
		// longest allowed length
		if (binaryVector[flipIndex] == 0 && currentPattern.size() < longestItemSet) {
			binaryVector[flipIndex] = 1; // Add an item (flip from 0 to 1)
		} else if (binaryVector[flipIndex] == 1 && currentPattern.size() > 2) { // Only remove if size > 2 to avoid
																				// single item
			binaryVector[flipIndex] = 0; // Remove an item (flip from 1 to 0)
		}

		// Convert the modified binary vector back to the pattern
		ArrayList<Integer> newPattern = new ArrayList<>();
		for (int i = 0; i < binaryVector.length; i++) {
			if (binaryVector[i] == 1) {
				newPattern.add(allItems[i]);
			}
		}

		return newPattern;
	}

	// Generates itemsets with a length of up to longestItemSet, ensuring no
	// duplicates in currentPatternSet
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

			if (containsElementUnsorted(BUFFERFLIP, length, flipItem1) == false) {
				BUFFERFLIP[length] = flipItem1;
				length++;
			} else {
				removeItemFromUnsortedList(BUFFERFLIP, length, flipItem1);
				length--;
			}

			if (containsElementUnsorted(BUFFERFLIP, length, flipItem2) == false) {
				BUFFERFLIP[length] = flipItem2;
				length++;
			} else {
				removeItemFromUnsortedList(BUFFERFLIP, length, flipItem2);
				length--;
			}

			if (length >= 2) {
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
	 * Check if an itemset contains another itemset. It assumes that itemsets are
	 * sorted according to the lexical order.
	 * 
	 * @param itemset1 the first itemset
	 * @param itemset2 the second itemset
	 * @return true if the first itemset contains the second itemset
	 */
	public static boolean containsOrEquals(int itemset1[], int itemset2[]) {
		// for each item in the first itemset
		loop1: for (int i = 0; i < itemset2.length; i++) {
			// for each item in the second itemset
			for (int j = 0; j < itemset1.length; j++) {
				// if the current item in itemset1 is equal to the one in itemset2
				// search for the next one in itemset1
				if (itemset1[j] == itemset2[i]) {
					continue loop1;
					// if the current item in itemset1 is larger
					// than the current item in itemset2, then
					// stop because of the lexical order.
				} else if (itemset1[j] > itemset2[i]) {
					return false;
				}
			}
			// means that an item was not found
			return false;
		}
		// if all items were found, return true.
		return true;
	}

	public static void initializeDatabase(List<int[]> database) {
		longestItemSet = 0;
		totalWeight = 0;
		itemFrequency = new AMapIntToInt();
		cumulativeWeights  = new ArrayList<Entry<Integer, Integer>>();
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

		// Now calculate the cumulative weight and populate the cumulativeWeights list
		EntryIterator iter = itemFrequency.iterator();
		while (iter.hasNext()) {
			MapEntryIntToInt entry = iter.next();
			totalWeight += entry.getValue();
			cumulativeWeights.add(new AbstractMap.SimpleEntry<>(entry.getKey(), totalWeight));
		}
	}

	public static int countOccurrencesInDatabase(int[] pattern, List<int[]> database) {
		if (pattern.length == 0) {
			return 0;
		}

		int count = 0;
		for (int[] transaction : database) {
			if (containsAll(transaction, pattern)) {
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

		// For each element in the list, check if it exists in the array
		for (int element : pattern) {
			boolean found = false;

			for (int j=0 ; j < listLength; j++) {
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

	public static boolean containsAll(int[] list, int[] pattern) {
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

	// 深拷贝数据库
	public static List<int[]> copyDatabase(List<int[]> database) {
		List<int[]> copy = new ArrayList<>(database.size());

		for (int[] transaction : database) {
			copy.add(Arrays.copyOf(transaction, transaction.length));
		}

		return copy;
	}

	// 对 codetable 进行排序
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
