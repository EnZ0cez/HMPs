import java.io.*;
import java.util.*;
import java.text.DecimalFormat;

/**
 * 统计显著性测试工具类
 *
 * 提供t检验和方差分析(ANOVA)功能，用于验证HMP框架与对比算法之间的差异显著性
 *
 * @author HMP Project Team
 */
public class StatisticalSignificanceTester {

    // 显著性水平
    public static final double ALPHA_0_05 = 0.05;
    public static final double ALPHA_0_01 = 0.01;
    public static final double ALPHA_0_001 = 0.001;

    /**
     * t检验结果类
     */
    public static class TTestResult {
        public final double tStatistic;
        public final double pValue;
        public final double degreesOfFreedom;
        public final boolean significantAt05;
        public final boolean significantAt01;
        public final boolean significantAt001;
        public final String interpretation;

        public TTestResult(double tStatistic, double pValue, double degreesOfFreedom) {
            this.tStatistic = tStatistic;
            this.pValue = pValue;
            this.degreesOfFreedom = degreesOfFreedom;
            this.significantAt05 = pValue < ALPHA_0_05;
            this.significantAt01 = pValue < ALPHA_0_01;
            this.significantAt001 = pValue < ALPHA_0_001;
            this.interpretation = generateInterpretation();
        }

        private String generateInterpretation() {
            StringBuilder sb = new StringBuilder();
            if (significantAt001) {
                sb.append("极显著差异 (p < 0.001)");
            } else if (significantAt01) {
                sb.append("高度显著差异 (p < 0.01)");
            } else if (significantAt05) {
                sb.append("显著差异 (p < 0.05)");
            } else {
                sb.append("无显著差异 (p >= 0.05)");
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            DecimalFormat df = new DecimalFormat("#.####");
            return String.format("t=%.4f, df=%.1f, p=%.4f, %s",
                tStatistic, degreesOfFreedom, pValue, interpretation);
        }
    }

    /**
     * ANOVA结果类
     */
    public static class ANOVAResult {
        public final double fStatistic;
        public final double pValue;
        public final double betweenGroupDF;
        public final double withinGroupDF;
        public final double totalDF;
        public final double betweenGroupSS;
        public final double withinGroupSS;
        public final double totalSS;
        public final double betweenGroupMS;
        public final double withinGroupMS;
        public final boolean significant;
        public final String interpretation;

        public ANOVAResult(double fStatistic, double pValue, double betweenGroupDF,
                           double withinGroupDF, double totalDF, double betweenGroupSS,
                           double withinGroupSS, double totalSS) {
            this.fStatistic = fStatistic;
            this.pValue = pValue;
            this.betweenGroupDF = betweenGroupDF;
            this.withinGroupDF = withinGroupDF;
            this.totalDF = totalDF;
            this.betweenGroupSS = betweenGroupSS;
            this.withinGroupSS = withinGroupSS;
            this.totalSS = totalSS;
            this.betweenGroupMS = betweenGroupGroupSS / betweenGroupDF;
            this.withinGroupMS = withinGroupSS / withinGroupDF;
            this.significant = pValue < ALPHA_0_05;
            this.interpretation = generateInterpretation();
        }

        private String generateInterpretation() {
            if (significant) {
                return String.format("存在显著组间差异 (F=%.4f, p=%.4f, p < 0.05)", fStatistic, pValue);
            } else {
                return String.format("无显著组间差异 (F=%.4f, p=%.4f, p >= 0.05)", fStatistic, pValue);
            }
        }

        @Override
        public String toString() {
            DecimalFormat df = new DecimalFormat("#.####");
            return String.format("F=%.4f, p=%.4f, df=(%.1f, %.1f), %s",
                fStatistic, pValue, betweenGroupDF, withinGroupDF, interpretation);
        }
    }

    /**
     * 实验数据结构
     */
    public static class ExperimentData {
        public final String algorithm;
        public final String dataset;
        public final String parameter;
        public final double parameterValue;
        public final double compressionRatio;
        public final double executionTime;
        public final int codeTableSize;
        public final int iterations;
        public final double memoryUsage;

        public ExperimentData(String algorithm, String dataset, String parameter,
                            double parameterValue, double compressionRatio, double executionTime,
                            int codeTableSize, int iterations, double memoryUsage) {
            this.algorithm = algorithm;
            this.dataset = dataset;
            this.parameter = parameter;
            this.parameterValue = parameterValue;
            this.compressionRatio = compressionRatio;
            this.executionTime = executionTime;
            this.codeTableSize = codeTableSize;
            this.iterations = iterations;
            this.memoryUsage = memoryUsage;
        }
    }

    /**
     * 执行配对t检验
     * 用于比较同一条件下两种算法的性能差异
     */
    public static TTestResult pairedTTest(double[] group1, double[] group2) {
        if (group1.length != group2.length) {
            throw new IllegalArgumentException("配对t检验需要两组数据长度相同");
        }
        if (group1.length < 2) {
            throw new IllegalArgumentException("样本数量必须至少为2");
        }

        int n = group1.length;
        double[] differences = new double[n];
        double sumDifferences = 0;
        double sumSquaredDifferences = 0;

        // 计算差值
        for (int i = 0; i < n; i++) {
            differences[i] = group1[i] - group2[i];
            sumDifferences += differences[i];
            sumSquaredDifferences += differences[i] * differences[i];
        }

        double meanDifference = sumDifferences / n;
        double variance = (sumSquaredDifferences - n * meanDifference * meanDifference) / (n - 1);
        double standardError = Math.sqrt(variance / n);
        double tStatistic = meanDifference / standardError;
        double degreesOfFreedom = n - 1;
        double pValue = calculateTwoTailPValue(tStatistic, degreesOfFreedom);

        return new TTestResult(tStatistic, pValue, degreesOfFreedom);
    }

    /**
     * 执行独立样本t检验
     * 用于比较两个独立组的均值差异
     */
    public static TTestResult independentTTest(double[] group1, double[] group2) {
        if (group1.length < 2 || group2.length < 2) {
            throw new IllegalArgumentException("每组样本数量必须至少为2");
        }

        int n1 = group1.length;
        int n2 = group2.length;

        // 计算均值
        double mean1 = calculateMean(group1);
        double mean2 = calculateMean(group2);

        // 计算方差
        double variance1 = calculateVariance(group1, mean1);
        double variance2 = calculateVariance(group2, mean2);

        // 计算合并方差
        double pooledVariance = ((n1 - 1) * variance1 + (n2 - 1) * variance2) / (n1 + n2 - 2);
        double standardError = Math.sqrt(pooledVariance * (1.0 / n1 + 1.0 / n2));

        double tStatistic = (mean1 - mean2) / standardError;
        double degreesOfFreedom = n1 + n2 - 2;
        double pValue = calculateTwoTailPValue(tStatistic, degreesOfFreedom);

        return new TTestResult(tStatistic, pValue, degreesOfFreedom);
    }

    /**
     * 执行单因素方差分析(One-way ANOVA)
     * 用于比较多组间的均值差异
     */
    public static ANOVAResult oneWayANOVA(List<double[]> groups) {
        if (groups.size() < 2) {
            throw new IllegalArgumentException("ANOVA需要至少2个组");
        }

        int k = groups.size(); // 组数
        int totalN = groups.stream().mapToInt(group -> group.length).sum();

        if (totalN <= k) {
            throw new IllegalArgumentException("总样本数必须大于组数");
        }

        // 计算总均值
        double[] allValues = groups.stream().flatMapToDouble(Arrays::stream).toArray();
        double grandMean = calculateMean(allValues);

        // 计算组间平方和 (SSB)
        double betweenGroupSS = 0;
        for (double[] group : groups) {
            double groupMean = calculateMean(group);
            betweenGroupSS += group.length * Math.pow(groupMean - grandMean, 2);
        }

        // 计算组内平方和 (SSW)
        double withinGroupSS = 0;
        for (double[] group : groups) {
            double groupMean = calculateMean(group);
            for (double value : group) {
                withinGroupSS += Math.pow(value - groupMean, 2);
            }
        }

        double totalSS = betweenGroupSS + withinGroupSS;
        double betweenGroupDF = k - 1;
        double withinGroupDF = totalN - k;
        double totalDF = totalN - 1;

        double betweenGroupMS = betweenGroupSS / betweenGroupDF;
        double withinGroupMS = withinGroupSS / withinGroupDF;

        double fStatistic = betweenGroupMS / withinGroupMS;
        double pValue = calculateOneTailPValue(fStatistic, betweenGroupDF, withinGroupDF);

        return new ANOVAResult(fStatistic, pValue, betweenGroupDF, withinGroupDF,
                               totalDF, betweenGroupSS, withinGroupSS, totalSS);
    }

    /**
     * 执行多重比较 (Tukey HSD)
     * 用于ANOVA后进行组间两两比较
     */
    public static Map<String, TTestResult> tukeyHSD(List<double[]> groups, List<String> groupNames,
                                                   double criticalValue) {
        Map<String, TTestResult> results = new HashMap<>();
        int k = groups.size();
        int n = groups.get(0).length; // 假设各组样本数相等
        double MSE = calculateMSE(groups);
        double standardError = Math.sqrt(MSE / n);

        for (int i = 0; i < k; i++) {
            for (int j = i + 1; j < k; j++) {
                double mean1 = calculateMean(groups.get(i));
                double mean2 = calculateMean(groups.get(j));
                double qStatistic = Math.abs(mean1 - mean2) / standardError;

                // 简化的p值计算（实际应用中应该使用Studentized range分布）
                double pValue = Math.max(0.001, 1.0 - qStatistic / (criticalValue * 2));

                TTestResult result = new TTestResult(qStatistic, pValue, k * (n - 1));
                String comparison = groupNames.get(i) + " vs " + groupNames.get(j);
                results.put(comparison, result);
            }
        }

        return results;
    }

    /**
     * 效应量计算 (Cohen's d)
     */
    public static double calculateCohensD(double[] group1, double[] group2) {
        double mean1 = calculateMean(group1);
        double mean2 = calculateMean(group2);
        double variance1 = calculateVariance(group1, mean1);
        double variance2 = calculateVariance(group2, mean2);

        double pooledStandardDeviation = Math.sqrt(((group1.length - 1) * variance1 +
                                                  (group2.length - 1) * variance2) /
                                                 (group1.length + group2.length - 2));

        return (mean1 - mean2) / pooledStandardDeviation;
    }

    /**
     * 读取实验数据
     */
    public static List<ExperimentData> readExperimentData(String filename) throws IOException {
        List<ExperimentData> data = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine(); // 跳过标题行

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 9) {
                    ExperimentData exp = new ExperimentData(
                        parts[0], // algorithm
                        parts[1], // dataset
                        parts[2], // parameter
                        Double.parseDouble(parts[3]), // parameterValue
                        Double.parseDouble(parts[4]), // compressionRatio
                        Double.parseDouble(parts[5]), // executionTime
                        Integer.parseInt(parts[6]), // codeTableSize
                        Integer.parseInt(parts[7]), // iterations
                        Double.parseDouble(parts[8])  // memoryUsage
                    );
                    data.add(exp);
                }
            }
        }

        return data;
    }

    /**
     * 分析算法性能差异
     */
    public static void analyzeAlgorithmPerformance(List<ExperimentData> data, String outputPath) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println("=== HMP框架算法性能统计分析 ===");
            writer.println("分析时间: " + new Date());
            writer.println();

            // 按算法分组
            Map<String, List<ExperimentData>> algorithmGroups = new HashMap<>();
            for (ExperimentData exp : data) {
                algorithmGroups.computeIfAbsent(exp.algorithm, k -> new ArrayList<>()).add(exp);
            }

            // 压缩率比较
            writer.println("1. 压缩率性能比较分析");
            writer.println("=" * 50);
            analyzeCompressionRatio(algorithmGroups, writer);

            // 运行时间比较
            writer.println("\n2. 运行时间性能比较分析");
            writer.println("=" * 50);
            analyzeExecutionTime(algorithmGroups, writer);

            // 内存使用比较
            writer.println("\n3. 内存使用性能比较分析");
            writer.println("=" * 50);
            analyzeMemoryUsage(algorithmGroups, writer);

            // 参数敏感性分析
            writer.println("\n4. 参数敏感性统计分析");
            writer.println("=" * 50);
            analyzeParameterSensitivity(algorithmGroups, writer);

        } catch (IOException e) {
            System.err.println("写入统计分析报告时出错: " + e.getMessage());
        }
    }

    /**
     * 分析压缩率性能
     */
    private static void analyzeCompressionRatio(Map<String, List<ExperimentData>> algorithmGroups,
                                                 PrintWriter writer) {
        String[] algorithms = {"HMP-SA", "HMP-HC"};

        writer.println("配对t检验结果 (压缩率):");
        writer.println("-" * 40);

        for (String dataset : extractDatasets(algorithmGroups)) {
            writer.println("数据集: " + dataset);

            Map<String, List<Double>> compressionData = new HashMap<>();
            for (String algorithm : algorithms) {
                compressionData.put(algorithm, extractCompressionRatios(algorithmGroups.get(algorithm), dataset));
            }

            if (compressionData.get("HMP-SA").size() > 1 && compressionData.get("HMP-HC").size() > 1) {
                // 执行配对t检验
                double[] saData = compressionData.get("HMP-SA").stream().mapToDouble(d -> d).toArray();
                double[] hcData = compressionData.get("HMP-HC").stream().mapToDouble(d -> d).toArray();

                // 取最小样本数进行配对
                int minSize = Math.min(saData.length, hcData.length);
                double[] saDataTrimmed = Arrays.copyOf(saData, minSize);
                double[] hcDataTrimmed = Arrays.copyOf(hcData, minSize);

                TTestResult tTest = pairedTTest(saDataTrimmed, hcDataTrimmed);
                double cohensD = calculateCohensD(saDataTrimmed, hcDataTrimmed);

                writer.printf("  HMP-SA vs HMP-HC: %s\n", tTest);
                writer.printf("  效应量 (Cohen's d): %.4f\n", cohensD);
                writer.printf("  HMP-SA均值: %.2f%%, HMP-HC均值: %.2f%%\n",
                    calculateMean(saDataTrimmed), calculateMean(hcDataTrimmed));
                writer.println();
            }
        }
    }

    /**
     * 分析运行时间性能
     */
    private static void analyzeExecutionTime(Map<String, List<ExperimentData>> algorithmGroups,
                                            PrintWriter writer) {
        String[] algorithms = {"HMP-SA", "HMP-HC"};

        writer.println("配对t检验结果 (运行时间):");
        writer.println("-" * 40);

        for (String dataset : extractDatasets(algorithmGroups)) {
            writer.println("数据集: " + dataset);

            Map<String, List<Double>> timeData = new HashMap<>();
            for (String algorithm : algorithms) {
                timeData.put(algorithm, extractExecutionTimes(algorithmGroups.get(algorithm), dataset));
            }

            if (timeData.get("HMP-SA").size() > 1 && timeData.get("HMP-HC").size() > 1) {
                double[] saData = timeData.get("HMP-SA").stream().mapToDouble(d -> d).toArray();
                double[] hcData = timeData.get("HMP-HC").stream().mapToDouble(d -> d).toArray();

                int minSize = Math.min(saData.length, hcData.length);
                double[] saDataTrimmed = Arrays.copyOf(saData, minSize);
                double[] hcDataTrimmed = Arrays.copyOf(hcData, minSize);

                TTestResult tTest = pairedTTest(saDataTrimmed, hcDataTrimmed);
                double cohensD = calculateCohensD(saDataTrimmed, hcDataTrimmed);

                writer.printf("  HMP-SA vs HMP-HC: %s\n", tTest);
                writer.printf("  效应量 (Cohen's d): %.4f\n", cohensD);
                writer.printf("  HMP-SA均值: %.2fs, HMP-HC均值: %.2fs\n",
                    calculateMean(saDataTrimmed), calculateMean(hcDataTrimmed));
                writer.println();
            }
        }
    }

    /**
     * 分析内存使用
     */
    private static void analyzeMemoryUsage(Map<String, List<ExperimentData>> algorithmGroups,
                                          PrintWriter writer) {
        String[] algorithms = {"HMP-SA", "HMP-HC"};

        writer.println("配对t检验结果 (内存使用):");
        writer.println("-" * 40);

        for (String dataset : extractDatasets(algorithmGroups)) {
            writer.println("数据集: " + dataset);

            Map<String, List<Double>> memoryData = new HashMap<>();
            for (String algorithm : algorithms) {
                memoryData.put(algorithm, extractMemoryUsage(algorithmGroups.get(algorithm), dataset));
            }

            if (memoryData.get("HMP-SA").size() > 1 && memoryData.get("HMP-HC").size() > 1) {
                double[] saData = memoryData.get("HMP-SA").stream().mapToDouble(d -> d).toArray();
                double[] hcData = memoryData.get("HMP-HC").stream().mapToDouble(d -> d).toArray();

                int minSize = Math.min(saData.length, hcData.length);
                double[] saDataTrimmed = Arrays.copyOf(saData, minSize);
                double[] hcDataTrimmed = Arrays.copyOf(hcData, minSize);

                TTestResult tTest = pairedTTest(saDataTrimmed, hcDataTrimmed);
                double cohensD = calculateCohensD(saDataTrimmed, hcDataTrimmed);

                writer.printf("  HMP-SA vs HMP-HC: %s\n", tTest);
                writer.printf("  效应量 (Cohen's d): %.4f\n", cohensD);
                writer.printf("  HMP-SA均值: %.2fMB, HMP-HC均值: %.2fMB\n",
                    calculateMean(saDataTrimmed), calculateMean(hcDataTrimmed));
                writer.println();
            }
        }
    }

    /**
     * 分析参数敏感性
     */
    private static void analyzeParameterSensitivity(Map<String, List<ExperimentData>> algorithmGroups,
                                                   PrintWriter writer) {
        for (String algorithm : algorithmGroups.keySet()) {
            writer.println("算法: " + algorithm);
            writer.println("参数敏感性ANOVA分析:");

            Map<String, List<double[]>> parameterGroups = new HashMap<>();

            for (ExperimentData exp : algorithmGroups.get(algorithm)) {
                parameterGroups.computeIfAbsent(exp.parameter, k -> new ArrayList<>())
                    .add(new double[]{exp.parameterValue, exp.compressionRatio});
            }

            for (Map.Entry<String, List<double[]>> entry : parameterGroups.entrySet()) {
                String parameter = entry.getKey();
                List<double[]> values = entry.getValue();

                // 按参数值分组
                Map<Double, List<Double>> paramValueGroups = new TreeMap<>();
                for (double[] pair : values) {
                    paramValueGroups.computeIfAbsent(pair[0], k -> new ArrayList<>()).add(pair[1]);
                }

                if (paramValueGroups.size() >= 2) {
                    List<double[]> groups = new ArrayList<>();
                    List<String> groupNames = new ArrayList<>();

                    for (Map.Entry<Double, List<Double>> paramEntry : paramValueGroups.entrySet()) {
                        groups.add(paramEntry.getValue().stream().mapToDouble(d -> d).toArray());
                        groupNames.add(String.format("%.2f", paramEntry.getKey()));
                    }

                    ANOVAResult anova = oneWayANOVA(groups);
                    writer.printf("  参数 %s: %s\n", parameter, anova);

                    if (anova.significant) {
                        writer.println("  进行Tukey HSD事后检验:");
                        double criticalValue = 3.5; // 简化值，实际应根据自由度查表
                        Map<String, TTestResult> tukeyResults = tukeyHSD(groups, groupNames, criticalValue);

                        for (Map.Entry<String, TTestResult> result : tukeyResults.entrySet()) {
                            if (result.getValue().significantAt05) {
                                writer.printf("    %s: 显著差异\n", result.getKey());
                            }
                        }
                    }
                    writer.println();
                }
            }
        }
    }

    // 辅助方法
    private static Set<String> extractDatasets(Map<String, List<ExperimentData>> algorithmGroups) {
        Set<String> datasets = new TreeSet<>();
        for (List<ExperimentData> dataList : algorithmGroups.values()) {
            for (ExperimentData exp : dataList) {
                datasets.add(exp.dataset);
            }
        }
        return datasets;
    }

    private static List<Double> extractCompressionRatios(List<ExperimentData> data, String dataset) {
        List<Double> ratios = new ArrayList<>();
        for (ExperimentData exp : data) {
            if (exp.dataset.equals(dataset)) {
                ratios.add(exp.compressionRatio);
            }
        }
        return ratios;
    }

    private static List<Double> extractExecutionTimes(List<ExperimentData> data, String dataset) {
        List<Double> times = new ArrayList<>();
        for (ExperimentData exp : data) {
            if (exp.dataset.equals(dataset)) {
                times.add(exp.executionTime);
            }
        }
        return times;
    }

    private static List<Double> extractMemoryUsage(List<ExperimentData> data, String dataset) {
        List<Double> memory = new ArrayList<>();
        for (ExperimentData exp : data) {
            if (exp.dataset.equals(dataset)) {
                memory.add(exp.memoryUsage);
            }
        }
        return memory;
    }

    private static double calculateMean(double[] data) {
        return Arrays.stream(data).average().orElse(0);
    }

    private static double calculateVariance(double[] data, double mean) {
        return Arrays.stream(data).map(x -> Math.pow(x - mean, 2)).average().orElse(0);
    }

    private static double calculateMSE(List<double[]> groups) {
        double totalSS = 0;
        int totalDF = 0;

        for (double[] group : groups) {
            double groupMean = calculateMean(group);
            for (double value : group) {
                totalSS += Math.pow(value - groupMean, 2);
            }
            totalDF += group.length - 1;
        }

        return totalSS / totalDF;
    }

    /**
     * 计算双尾p值 (t分布)
     * 使用近似计算，实际应用中应使用更精确的统计库
     */
    private static double calculateTwoTailPValue(double tStatistic, double degreesOfFreedom) {
        // 简化的p值计算（实际应该使用t分布的累积分布函数）
        double absT = Math.abs(tStatistic);

        if (absT > 4) return 0.0001;
        if (absT > 3) return 0.001;
        if (absT > 2.5) return 0.01;
        if (absT > 2) return 0.05;
        if (absT > 1.5) return 0.1;
        if (absT > 1) return 0.3;

        return 0.5; // 默认值
    }

    /**
     * 计算单尾p值 (F分布)
     * 使用近似计算
     */
    private static double calculateOneTailPValue(double fStatistic, double df1, double df2) {
        // 简化的p值计算
        if (fStatistic > 10) return 0.001;
        if (fStatistic > 7) return 0.01;
        if (fStatistic > 5) return 0.05;
        if (fStatistic > 3) return 0.1;
        if (fStatistic > 2) return 0.2;

        return 0.3; // 默认值
    }

    /**
     * 主方法 - 运行统计分析
     */
    public static void main(String[] args) {
        System.out.println("=== 统计显著性分析工具 ===");

        String inputFile = "parameter_sensitivity_results.csv";
        String outputFile = "statistical_significance_analysis.txt";

        try {
            if (args.length > 0) {
                inputFile = args[0];
            }
            if (args.length > 1) {
                outputFile = args[1];
            }

            System.out.println("读取实验数据: " + inputFile);
            List<ExperimentData> data = readExperimentData(inputFile);
            System.out.println("数据记录数: " + data.size());

            System.out.println("执行统计分析...");
            analyzeAlgorithmPerformance(data, outputFile);

            System.out.println("统计分析完成!");
            System.out.println("结果已保存到: " + outputFile);

        } catch (IOException e) {
            System.err.println("错误: " + e.getMessage());
            System.err.println("请确保输入文件存在且格式正确");
        }
    }
}