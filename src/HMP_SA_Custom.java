import ca.pfv.spmf.datastructures.collections.map.AMapIntToInt;
import ca.pfv.spmf.datastructures.collections.map.MapIntToInt;

import java.io.*;
import java.util.*;

/**
 * 可配置参数的HMP-SA算法实现
 * 用于参数敏感性分析实验
 */
public class HMP_SA_Custom {

    // 算法参数
    private final double initialTemperature;
    private final double coolingRate;
    private final double minTemperature;
    private final double improvementThreshold;
    private final int maxCodeTableSize;

    // 全局数据结构
    private ArrayList<int[]> codetable = new ArrayList<>();
    private int[] allItems = null;
    private MapIntToInt itemFrequency;
    private int longestItemSet = 0;
    private int totalWeight = 0;
    private List<Map.Entry<Integer, Integer>> cumulativeWeights = null;
    private Map<int[], Integer> itemsetCount = new HashMap<>();
    private SparseTriangularMatrix matrix = null;
    private int[] BUFFER = new int[500];
    private Random random = new Random(System.currentTimeMillis());

    /**
     * 构造函数
     */
    public HMP_SA_Custom(double initialTemperature, double coolingRate, double minTemperature,
                        double improvementThreshold, int maxCodeTableSize) {
        this.initialTemperature = initialTemperature;
        this.coolingRate = coolingRate;
        this.minTemperature = minTemperature;
        this.improvementThreshold = improvementThreshold;
        this.maxCodeTableSize = maxCodeTableSize;
    }

    /**
     * 运行实验并返回结果
     */
    public Map<String, Object> runExperiment(String inputFilePath) {
        try {
            // 读取数据库
            List<int[]> database = readItemsetsFromFile(inputFilePath);
            initializeDatabase(database);

            // 初始化压缩计算和性能监控
            double initialCompressionSize = calculateSizeInBits(database);
            double currentCompressionSize = initialCompressionSize;
            double improveCompressionSize = initialCompressionSize;

            int iterations = 0;
            int sa_work = 0;
            long startTime = System.currentTimeMillis();

            // 初始化内存使用监控
            MemoryLogger logger = MemoryLogger.getInstance();
            logger.reset();

            List<int[]> modifiedDatabase = copyDatabase(database);
            int codeTableTryCount = 0;

            // 主模拟退火循环
            while (codetable.size() < maxCodeTableSize / 3) {
                iterations = 0;
                double temperature = initialTemperature;

                // 生成随机模式
                int[] pattern = generateRandomItemset(longestItemSet, itemFrequency, codetable);

                int temperatureTryCount = 0;
                while (temperature > minTemperature) {
                    // 尝试添加模式到码表
                    ArrayList<int[]> temp_ct = new ArrayList<>(codetable);
                    int[] newPattern = generateTwoFlipNeighborPattern(pattern);

                    if (isPatternInDatabase(newPattern, modifiedDatabase)) {
                        temp_ct.add(newPattern);
                        int newCompressionSize = deleteAndCalculateSizeInBits(modifiedDatabase, temp_ct);

                        double compressionImprovement = (improveCompressionSize - newCompressionSize) / improveCompressionSize;
                        double acceptanceProbability = Math.exp((improveCompressionSize - newCompressionSize) / temperature);

                        if (compressionImprovement > improvementThreshold) {
                            improveCompressionSize = newCompressionSize;
                            pattern = newPattern;
                            temperatureTryCount = 0;
                        } else if (acceptanceProbability > random.nextDouble()) {
                            improveCompressionSize = newCompressionSize;
                            pattern = newPattern;
                            sa_work++;
                            temperatureTryCount = 0;
                        }
                    }

                    // 降低温度
                    temperature *= coolingRate;
                    iterations++;
                    temperatureTryCount++;

                    if (temperatureTryCount > 10) {
                        break;
                    }
                }

                if (isPatternInDatabase(pattern, modifiedDatabase)) {
                    if (((currentCompressionSize - improveCompressionSize) / initialCompressionSize) > improvementThreshold) {
                        codeTableTryCount = 0;
                        codetable.add(pattern);
                        modifiedDatabase = deleteItemset(modifiedDatabase, pattern);
                        updateItemFrequencies(modifiedDatabase);
                        currentCompressionSize = improveCompressionSize;
                    }
                }

                codeTableTryCount++;
                if (codeTableTryCount > 100 || codeTableTryCount > maxCodeTableSize / 3) {
                    break;
                }
            }

            // 处理长度为2的模式
            processLength2Patterns(database, modifiedDatabase);

            // 最终压缩大小计算
            currentCompressionSize = deleteAndCalculateSizeInBits(database, codetable);

            // 清理矩阵
            matrix.clear();

            // 记录内存使用
            double currentMemoryUsage = logger.checkMemory();
            logger.stopRecordingMode();

            // 计算执行时间
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;

            // 准备结果
            Map<String, Object> result = new HashMap<>();
            result.put("compressionRatio", currentCompressionSize / initialCompressionSize * 100);
            result.put("executionTime", executionTime);
            result.put("codeTableSize", codetable.size());
            result.put("iterations", iterations);
            result.put("memoryUsage", currentMemoryUsage);
            result.put("saWork", sa_work);

            // 清理数据结构
            cleanup();

            return result;

        } catch (Exception e) {
            System.err.println("实验执行出错: " + e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("compressionRatio", 0.0);
            errorResult.put("executionTime", 0.0);
            errorResult.put("codeTableSize", 0);
            errorResult.put("iterations", 0);
            errorResult.put("memoryUsage", 0.0);
            return errorResult;
        }
    }

    /**
     * 更新项目频率
     */
    private void updateItemFrequencies(List<int[]> modifiedDatabase) {
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
    }

    /**
     * 处理长度为2的模式
     */
    private void processLength2Patterns(List<int[]> database, List<int[]> modifiedDatabase) {
        for (int[] pattern : codetable) {
            int occ = countOccurrencesInDatabase(pattern, database);
            itemsetCount.put(pattern, occ);
        }

        updateMatrix(modifiedDatabase);

        List<Pair> length2Patterns = matrix.getAllPatternsWithOccurrences();
        length2Patterns.sort((entry1, entry2) -> Integer.compare(entry2.count, entry1.count));

        while (!length2Patterns.isEmpty()) {
            Pair entry = length2Patterns.get(0);
            int[] pattern = entry.pattern;
            int count = entry.count;

            codetable.add(pattern);
            itemsetCount.put(pattern, count);

            modifiedDatabase = deleteItemset(modifiedDatabase, pattern);
            updateMatrix(modifiedDatabase);

            length2Patterns = matrix.getAllPatternsWithOccurrences();
            length2Patterns.sort((entry1, entry2) -> Integer.compare(entry2.count, entry1.count));

            if (codetable.size() > maxCodeTableSize - 1) {
                break;
            }
        }
    }

    /**
     * 清理数据结构
     */
    private void cleanup() {
        codetable.clear();
        itemsetCount.clear();
        itemFrequency.clear();
        cumulativeWeights.clear();
        totalWeight = 0;
    }

    // 以下方法与原HMP_SA_Runner基本相同，为了可读性进行了简化

    private int calculateSizeInBits(List<int[]> database) {
        int totalSizeInBits = 0;
        for (int[] transaction : database) {
            totalSizeInBits += transaction.length * Integer.SIZE;
        }
        return totalSizeInBits;
    }

    private int deleteAndCalculateSizeInBits(List<int[]> database, List<int[]> codeTable) {
        Map<PatternKey, Integer> patternCount = new HashMap<>();
        int totalSizeInBits = 0;

        for (int[] transaction : database) {
            if (BUFFER.length < transaction.length) {
                BUFFER = new int[Math.max(transaction.length * 2, BUFFER.length)];
            }

            System.arraycopy(transaction, 0, BUFFER, 0, transaction.length);
            int newLength = transaction.length;

            for (int[] pattern : codeTable) {
                if (newLength >= pattern.length && containsAll(BUFFER, newLength, pattern)) {
                    PatternKey key = new PatternKey(pattern);
                    patternCount.put(key, patternCount.getOrDefault(key, 0) + 1);
                    removePatternFromTransaction(BUFFER, newLength, pattern);
                    newLength = newLength - pattern.length;
                }
            }
            totalSizeInBits += newLength * Integer.SIZE;
        }

        for (Map.Entry<PatternKey, Integer> entry : patternCount.entrySet()) {
            if (entry.getValue() != 0) {
                int keySize = entry.getKey().pattern.length;
                int valueBitSize = (int) Math.ceil(Math.log(entry.getValue() + 1) / Math.log(2));
                totalSizeInBits += valueBitSize + keySize * Integer.SIZE;
            }
        }

        return totalSizeInBits;
    }

    private int[] removePatternFromTransaction(int[] transaction, int transactionLength, int[] pattern) {
        Set<Integer> patternSet = new HashSet<>();
        for (int num : pattern) {
            patternSet.add(num);
        }
        int pos = 0;
        for (int j = 0; j < transactionLength; j++) {
            int item = transaction[j];
            if (!patternSet.contains(item)) {
                transaction[pos++] = item;
            }
        }
        return Arrays.copyOf(transaction, pos);
    }

    private List<int[]> deleteItemset(List<int[]> database, int[] pattern) {
        List<int[]> modifiedDatabase = new ArrayList<>();
        for (int[] transaction : database) {
            if (containsOrEquals(transaction, pattern)) {
                transaction = cloneItemSetMinusAnItemset(transaction, pattern);
            }
            modifiedDatabase.add(transaction);
        }
        return modifiedDatabase;
    }

    private int[] cloneItemSetMinusAnItemset(int[] itemset, int[] itemsetToNotKeep) {
        int[] newItemset = new int[itemset.length - itemsetToNotKeep.length];
        int i = 0;
        for (int j = 0; j < itemset.length; j++) {
            if (Arrays.binarySearch(itemsetToNotKeep, itemset[j]) < 0) {
                newItemset[i++] = itemset[j];
            }
        }
        return newItemset;
    }

    private List<int[]> readItemsetsFromFile(String fileName) {
        List<int[]> database = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] items = line.split("\\s+");
                int[] transaction = new int[items.length];
                for (int i = 0; i < items.length; i++) {
                    transaction[i] = Integer.valueOf(items[i].trim());
                }
                Arrays.sort(transaction);
                database.add(transaction);
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return database;
    }

    private int[] generateRandomItemset(int longestItemSet, MapIntToInt itemFrequency,
                                      List<int[]> currentPatternSet) {
        int[] SMALLBUFFER = new int[10];
        int randomItemsetSize = 3 + random.nextInt(3);
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
        } while (containsPattern(currentPatternSet, SMALLBUFFER, length));
        return Arrays.copyOf(SMALLBUFFER, randomItemsetSize);
    }

    private int[] generateTwoFlipNeighborPattern(int[] currentPattern) {
        int[] BUFFERFLIP = new int[20];
        while (true) {
            int length = currentPattern.length;
            System.arraycopy(currentPattern, 0, BUFFERFLIP, 0, currentPattern.length);

            int flipItem1 = itemToIndex(getWeightedRandomItem());
            int flipItem2;
            do {
                flipItem2 = itemToIndex(getWeightedRandomItem());
            } while (flipItem1 == flipItem2);

            if (!containsElementUnsorted(BUFFERFLIP, length, flipItem1) && length < 10) {
                BUFFERFLIP[length] = flipItem1;
                length++;
            } else if (length > 2) {
                removeItemFromUnsortedList(BUFFERFLIP, length, flipItem1);
                length--;
            }

            if (!containsElementUnsorted(BUFFERFLIP, length, flipItem2) && length < 10) {
                BUFFERFLIP[length] = flipItem2;
                length++;
            } else if (length > 2) {
                removeItemFromUnsortedList(BUFFERFLIP, length, flipItem2);
                length--;
            }

            if (length > 2) {
                Arrays.sort(BUFFERFLIP, 0, length);
                return Arrays.copyOf(BUFFERFLIP, length);
            }
        }
    }

    private int getWeightedRandomItem() {
        if (totalWeight == 0) {
            throw new IllegalStateException("Total weight must be initialized.");
        }
        int randomNumber = random.nextInt(totalWeight);
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

    private boolean isPatternInDatabase(int[] pattern, List<int[]> database) {
        for (int[] transaction : database) {
            if (containsOrEquals(transaction, pattern)) {
                return true;
            }
        }
        return false;
    }

    private int itemToIndex(int item) {
        for (int i = 0; i < allItems.length; i++) {
            if (allItems[i] == item) {
                return i;
            }
        }
        return -1;
    }

    private boolean containsOrEquals(int itemset1[], int itemset2[]) {
        int i = 0, j = 0;
        while (i < itemset1.length && j < itemset2.length) {
            if (itemset1[i] == itemset2[j]) {
                j++;
            }
            i++;
        }
        return j == itemset2.length;
    }

    private void initializeDatabase(List<int[]> database) {
        longestItemSet = 0;
        totalWeight = 0;
        itemFrequency = new AMapIntToInt();
        cumulativeWeights = new ArrayList<Map.Entry<Integer, Integer>>();
        matrix = new SparseTriangularMatrix();

        for (int[] transaction : database) {
            for (int i = 0; i < transaction.length; i++) {
                Integer item = transaction[i];
                int frequency = itemFrequency.get(item);
                if (frequency == -1) {
                    itemFrequency.put(item, 1);
                } else {
                    itemFrequency.put(item, frequency + 1);
                }
            }

            if (transaction.length > longestItemSet) {
                longestItemSet = transaction.length;
            }

            for (int i = 0; i < transaction.length; i++) {
                Integer item1 = transaction[i];
                for (int j = i + 1; j < transaction.length; j++) {
                    Integer item2 = transaction[j];
                    matrix.incrementCount(item1, item2);
                }
            }
        }

        allItems = new int[itemFrequency.size()];
        int index = 0;
        MapIntToInt.EntryIterator iter = itemFrequency.iterator();
        while (iter.hasNext()) {
            MapIntToInt.MapEntryIntToInt entry = iter.next();
            allItems[index++] = entry.getKey();
            totalWeight += entry.getValue();
            cumulativeWeights.add(new AbstractMap.SimpleEntry<>(entry.getKey(), totalWeight));
        }
        Arrays.sort(allItems);
    }

    private int countOccurrencesInDatabase(int[] pattern, List<int[]> database) {
        if (pattern.length == 0) {
            return 0;
        }
        int count = 0;
        for (int[] transaction : database) {
            if (containsOrEquals(transaction, pattern)) {
                count++;
            }
        }
        return count;
    }

    private boolean containsAll(int[] list, int listLength, int[] pattern) {
        if (pattern.length == 0) return true;
        int i = 0, j = 0;
        while (i < listLength && j < pattern.length) {
            if (list[i] == pattern[j]) {
                i++;
                j++;
            } else if (list[i] < pattern[j]) {
                i++;
            } else {
                return false;
            }
        }
        return j == pattern.length;
    }

    private List<int[]> copyDatabase(List<int[]> database) {
        List<int[]> copy = new ArrayList<>(database.size());
        for (int[] transaction : database) {
            copy.add(Arrays.copyOf(transaction, transaction.length));
        }
        return copy;
    }

    private void updateMatrix(List<int[]> database) {
        matrix.clear();
        for (int[] transaction : database) {
            for (int i = 0; i < transaction.length; i++) {
                Integer item1 = transaction[i];
                for (int j = i + 1; j < transaction.length; j++) {
                    Integer item2 = transaction[j];
                    matrix.incrementCount(item1, item2);
                }
            }
        }
    }

    private boolean containsElementUnsorted(int[] list, int listLength, int element) {
        for (int i = 0; i < listLength; i++) {
            if (list[i] == element) {
                return true;
            }
        }
        return false;
    }

    private boolean containsPattern(List<int[]> listOfPatterns, int[] patternBuffer, int patternLength) {
        for (int[] patternInList : listOfPatterns) {
            if (includedIn(patternBuffer, patternLength, patternInList)) {
                return true;
            }
        }
        return false;
    }

    private boolean includedIn(int[] itemset1, int itemset1Length, int[] itemset2) {
        int count = 0;
        for (int i = 0; i < itemset2.length; i++) {
            if (itemset2[i] == itemset1[count]) {
                count++;
                if (count == itemset1Length) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeItemFromUnsortedList(int[] currentPattern, int length, int itemToRemove) {
        int pos = 0;
        for (int i = 0; i < length; i++) {
            if (currentPattern[pos] != itemToRemove) {
                currentPattern[pos] = currentPattern[i];
                pos++;
            }
        }
    }
}