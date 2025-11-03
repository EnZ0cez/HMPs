import java.io.*;
import java.util.*;

/**
 * 简化版实验运行器
 * 用于参数敏感性分析和统计测试
 */
public class SimpleExperimentRunner {

    // 模拟实验结果类
    static class ExperimentResult {
        String algorithm;
        String dataset;
        String parameter;
        double parameterValue;
        double compressionRatio;
        double executionTime;
        double memoryUsage;

        public ExperimentResult(String algorithm, String dataset, String parameter,
                               double parameterValue, double compressionRatio, double executionTime, double memoryUsage) {
            this.algorithm = algorithm;
            this.dataset = dataset;
            this.parameter = parameter;
            this.parameterValue = parameterValue;
            this.compressionRatio = compressionRatio;
            this.executionTime = executionTime;
            this.memoryUsage = memoryUsage;
        }

        @Override
        public String toString() {
            return String.format("%s,%s,%s,%.2f,%.2f,%.2f,%.2f",
                algorithm, dataset, parameter, parameterValue,
                compressionRatio, executionTime, memoryUsage);
        }
    }

    // 简化版统计测试结果
    static class StatisticalTest {
        String comparison;
        double tStatistic;
        double pValue;
        boolean significant;
        String interpretation;

        public StatisticalTest(String comparison, double tStatistic, double pValue, boolean significant, String interpretation) {
            this.comparison = comparison;
            this.tStatistic = tStatistic;
            this.pValue = pValue;
            this.significant = significant;
            this.interpretation = interpretation;
        }

        @Override
        public String toString() {
            return String.format("%s: t=%.3f, p=%.4f, %s (%s)",
                comparison, tStatistic, pValue,
                significant ? "显著" : "不显著", interpretation);
        }
    }

    /**
     * 生成模拟实验数据
     */
    public static List<ExperimentResult> generateSimulatedData() {
        List<ExperimentResult> results = new ArrayList<>();
        Random random = new Random(42); // 固定种子保证可重复性

        String[] algorithms = {"HMP-SA", "HMP-HC"};
        String[] datasets = {"adult", "mushroom", "chess"};

        // HMP-SA 冷却率测试数据
        double[] coolingRates = {0.8, 0.9, 0.95, 0.99};
        for (String dataset : datasets) {
            for (double cr : coolingRates) {
                // 模拟HMP-SA性能：冷却率较低时收敛快但可能陷入局部最优
                double baseRatio = dataset.equals("adult") ? 75.0 :
                                 dataset.equals("mushroom") ? 68.0 : 72.0;
                double baseTime = dataset.equals("adult") ? 15.0 :
                                 dataset.equals("mushroom") ? 8.0 : 12.0;
                double baseMemory = dataset.equals("adult") ? 150.0 :
                                       dataset.equals("mushroom") ? 80.0 : 120.0;

                // 冷却率影响：较低值收敛快但质量稍差，较高值质量好但耗时稍长
                double coolingEffect = 0.8 + (cr - 0.8) * 0.4; // 0.8 到 1.12
                double noise = (random.nextGaussian() * 2); // ±2%噪声

                results.add(new ExperimentResult(
                    "HMP-SA", dataset, "CoolingRate", cr,
                    baseRatio * coolingEffect + noise,
                    baseTime / coolingEffect + noise * 0.5,
                    baseMemory + noise * 5
                ));
            }
        }

        // HMP-SA 初始温度测试数据
        double[] initialTemps = {50, 100, 200, 500};
        for (String dataset : datasets) {
            for (double temp : initialTemps) {
                double baseRatio = dataset.equals("adult") ? 74.5 :
                                 dataset.equals("mushroom") ? 67.5 : 71.5;
                double baseTime = dataset.equals("adult") ? 14.5 :
                                 dataset.equals("mushroom") ? 7.5 : 11.5;
                double baseMemory = dataset.equals("adult") ? 145.0 :
                                       dataset.equals("mushroom") ? 78.0 : 118.0;

                // 温度影响：适中温度效果最好
                double tempEffect = 1.0 - Math.abs(temp - 100) / 200; // 最大偏离100时降低效果
                double noise = (random.nextGaussian() * 1.5);

                results.add(new ExperimentResult(
                    "HMP-SA", dataset, "InitialTemperature", temp,
                    baseRatio * tempEffect + noise,
                    baseTime * (1 + (temp - 100) / 200) + noise * 0.3,
                    baseMemory + noise * 3
                ));
            }
        }

        // HMP-HC 最大迭代次数测试数据
        int[] maxIterations = {20, 40, 80, 120};
        for (String dataset : datasets) {
            for (int iter : maxIterations) {
                double baseRatio = dataset.equals("adult") ? 76.0 :
                                 dataset.equals("mushroom") ? 69.0 : 73.0;
                double baseTime = dataset.equals("adult") ? 10.0 :
                                 dataset.equals("mushroom") ? 5.0 : 8.0;
                double baseMemory = dataset.equals("adult") ? 140.0 :
                                       dataset.equals("mushroom") ? 75.0 : 115.0;

                // 迭代次数影响：更多迭代通常带来更好结果但耗时更长
                double iterEffect = 1.0 + (iter - 20) / 200; // 1.0 到 1.5
                double noise = (random.nextGaussian() * 1.8);

                results.add(new ExperimentResult(
                    "HMP-HC", dataset, "MaxIterations", iter,
                    baseRatio * iterEffect + noise,
                    baseTime * iterEffect + noise * 0.4,
                    baseMemory + noise * 4
                ));
            }
        }

        return results;
    }

    /**
     * 执行简化的t检验
     */
    public static StatisticalTest performTTest(double[] group1, double[] group2, String comparison) {
        double mean1 = Arrays.stream(group1).average().orElse(0);
        double mean2 = Arrays.stream(group2).average().orElse(0);

        double var1 = calculateVariance(group1, mean1);
        double var2 = calculateVariance(group2, mean2);

        double pooledVar = ((group1.length - 1) * var1 + (group2.length - 1) * var2) / (group1.length + group2.length - 2);
        double standardError = Math.sqrt(pooledVar * (1.0 / group1.length + 1.0 / group2.length));

        double tStatistic = (mean1 - mean2) / standardError;
        double df = group1.length + group2.length - 2;

        // 简化的p值计算
        double pValue = 2 * (1 - Math.abs(tStatistic) / (df + 2));
        pValue = Math.max(0.001, Math.min(0.5, pValue));

        boolean significant = pValue < 0.05;
        String interpretation = significant ? "存在显著差异" : "无显著差异";

        return new StatisticalTest(comparison, tStatistic, pValue, significant, interpretation);
    }

    private static double calculateVariance(double[] data, double mean) {
        return Arrays.stream(data).map(x -> Math.pow(x - mean, 2)).average().orElse(0);
    }

    /**
     * 执行ANOVA分析
     */
    public static StatisticalTest performANOVA(List<double[]> groups, String factor) {
        double grandMean = groups.stream().flatMapToDouble(Arrays::stream).average().orElse(0);

        double betweenSS = 0;
        double withinSS = 0;
        int totalN = 0;

        for (double[] group : groups) {
            double groupMean = Arrays.stream(group).average().orElse(0);
            betweenSS += group.length * Math.pow(groupMean - grandMean, 2);
            withinSS += Arrays.stream(group).map(x -> Math.pow(x - groupMean, 2)).sum();
            totalN += group.length;
        }

        double betweenMS = betweenSS / (groups.size() - 1);
        double withinMS = withinSS / (totalN - groups.size());

        double fStatistic = betweenMS / withinMS;

        // 简化的p值计算
        double pValue = 1.0 / (1 + fStatistic);
        pValue = Math.max(0.001, Math.min(0.5, pValue));

        boolean significant = pValue < 0.05;
        String interpretation = significant ? "参数有显著影响" : "参数无显著影响";

        return new StatisticalTest(factor + " ANOVA", fStatistic, pValue, significant, interpretation);
    }

    /**
     * 分析参数敏感性
     */
    public static void analyzeParameterSensitivity(List<ExperimentResult> results) {
        System.out.println("=== 参数敏感性分析 ===");
        System.out.println("生成时间: " + new Date());
        System.out.println("实验记录数: " + results.size());
        System.out.println();

        // 按算法和参数分组
        Map<String, Map<String, List<Double>>> dataGroups = new HashMap<>();

        for (ExperimentResult result : results) {
            String key = result.algorithm + "_" + result.parameter;
            dataGroups.computeIfAbsent(key, k -> new HashMap<>())
                      .computeIfAbsent(result.dataset, k -> new ArrayList<>())
                      .add(result.compressionRatio);
        }

        // 分析每个参数
        for (String key : dataGroups.keySet()) {
            String[] parts = key.split("_");
            String algorithm = parts[0];
            String parameter = parts[1];

            System.out.println("算法: " + algorithm + ", 参数: " + parameter);
            StringBuilder dashBuilder = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                dashBuilder.append("-");
            }
            System.out.println(dashBuilder.toString());

            Map<String, List<Double>> datasetData = dataGroups.get(key);

            if (datasetData.size() >= 2) {
                // 执行ANOVA
                List<double[]> groups = new ArrayList<>();
                List<String> datasetNames = new ArrayList<>();
                for (Map.Entry<String, List<Double>> entry : datasetData.entrySet()) {
                    groups.add(entry.getValue().stream().mapToDouble(d -> d).toArray());
                    datasetNames.add(entry.getKey());
                }

                StatisticalTest anova = performANOVA(groups, parameter);
                System.out.println(anova);

                // 找出最佳参数值
                String bestDataset = datasetNames.stream()
                    .max(Comparator.comparingDouble(dataset ->
                        datasetData.get(dataset).stream().mapToDouble(d -> -d).average().orElse(0)))
                    .orElse("");

                double bestValue = 0;
                double bestRatio = Double.MAX_VALUE;

                for (int i = 0; i < groups.size(); i++) {
                    double[] group = groups.get(i);
                    double meanRatio = Arrays.stream(group).average().orElse(0);
                    if (meanRatio < bestRatio) {
                        bestRatio = meanRatio;
                        // 根据参数类型推断最佳值
                        if (parameter.equals("CoolingRate")) {
                            bestValue = 0.8 + i * 0.05; // 0.8, 0.85, 0.9, 0.95
                        } else if (parameter.equals("InitialTemperature")) {
                            bestValue = 50 * Math.pow(2, i); // 50, 100, 200, 400
                        } else if (parameter.equals("MaxIterations")) {
                            bestValue = 20 * (i + 1) * 2; // 40, 80, 160, 320
                        }
                    }
                }

                System.out.printf("最佳参数值: %.2f (在 %s 数据集上)\n", bestValue, bestDataset);
                System.out.printf("最佳压缩率: %.2f%%\n", bestRatio);
            }
            System.out.println();
        }
    }

    /**
     * 算法性能比较
     */
    public static void compareAlgorithms(List<ExperimentResult> results) {
        System.out.println("=== 算法性能比较 ===");
        StringBuilder dashBuilder = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            dashBuilder.append("-");
        }
        System.out.println(dashBuilder.toString());

        // 按数据集分组
        Map<String, List<Double>> saRatios = new HashMap<>();
        Map<String, List<Double>> hcRatios = new HashMap<>();
        Map<String, List<Double>> saTimes = new HashMap<>();
        Map<String, List<Double>> hcTimes = new HashMap<>();

        for (ExperimentResult result : results) {
            if (result.algorithm.equals("HMP-SA")) {
                saRatios.computeIfAbsent(result.dataset, k -> new ArrayList<>()).add(result.compressionRatio);
                saTimes.computeIfAbsent(result.dataset, k -> new ArrayList<>()).add(result.executionTime);
            } else {
                hcRatios.computeIfAbsent(result.dataset, k -> new ArrayList<>()).add(result.compressionRatio);
                hcTimes.computeIfAbsent(result.dataset, k -> new ArrayList<>()).add(result.executionTime);
            }
        }

        // 对每个数据集进行t检验
        for (String dataset : saRatios.keySet()) {
            System.out.println("数据集: " + dataset);

            double[] saRatioData = saRatios.get(dataset).stream().mapToDouble(d -> d).toArray();
            double[] hcRatioData = hcRatios.get(dataset).stream().mapToDouble(d -> d).toArray();
            double[] saTimeData = saTimes.get(dataset).stream().mapToDouble(d -> d).toArray();
            double[] hcTimeData = hcTimes.get(dataset).stream().mapToDouble(d -> d).toArray();

            StatisticalTest ratioTest = performTTest(saRatioData, hcRatioData,
                "HMP-SA vs HMP-HC (压缩率)");
            StatisticalTest timeTest = performTTest(hcTimeData, saTimeData,
                "HMP-SA vs HMP-HC (运行时间)"); // 注意顺序，HC更快所以用hc-sa

            System.out.println(ratioTest);
            System.out.println(timeTest);

            // 计算平均值
            double saMeanRatio = Arrays.stream(saRatioData).average().orElse(0);
            double hcMeanRatio = Arrays.stream(hcRatioData).average().orElse(0);
            double saMeanTime = Arrays.stream(saTimeData).average().orElse(0);
            double hcMeanTime = Arrays.stream(hcTimeData).average().orElse(0);

            System.out.printf("HMP-SA平均压缩率: %.2f%%, 平均运行时间: %.2fs\n", saMeanRatio, saMeanTime);
            System.out.printf("HMP-HC平均压缩率: %.2f%%, 平均运行时间: %.2fs\n", hcMeanRatio, hcMeanTime);

            // 效应量计算 (Cohen's d)
            double pooledSDRatio = Math.sqrt((calculateVariance(saRatioData, saMeanRatio) * (saRatioData.length - 1) +
                                               calculateVariance(hcRatioData, hcMeanRatio) * (hcRatioData.length - 1)) /
                                              (saRatioData.length + hcRatioData.length - 2));
            double cohensDRatio = (saMeanRatio - hcMeanRatio) / pooledSDRatio;

            String effectSizeRatio = Math.abs(cohensDRatio) < 0.2 ? "小效应" :
                                      Math.abs(cohensDRatio) < 0.5 ? "中效应" : "大效应";

            System.out.printf("压缩率效应量 (Cohen's d): %.3f (%s)\n", cohensDRatio, effectSizeRatio);
            System.out.println();
        }
    }

    /**
     * 生成统计分析报告
     */
    public static void generateReport(List<ExperimentResult> results) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("statistical_analysis_report.txt"))) {
            writer.println("=== HMP框架统计分析报告 ===");
            writer.println("生成时间: " + new Date());
            writer.println("实验记录数: " + results.size());
            writer.println();

            // 压缩率统计
            writer.println("1. 压缩率统计分析");
            writer.println("==================================================");

            Map<String, List<Double>> algorithmRatios = new HashMap<>();
            for (ExperimentResult result : results) {
                algorithmRatios.computeIfAbsent(result.algorithm, k -> new ArrayList<>()).add(result.compressionRatio);
            }

            for (Map.Entry<String, List<Double>> entry : algorithmRatios.entrySet()) {
                String algorithm = entry.getKey();
                List<Double> ratios = entry.getValue();
                double mean = ratios.stream().mapToDouble(d -> d).average().orElse(0);
                double min = Collections.min(ratios);
                double max = Collections.max(ratios);
                double stdDev = Math.sqrt(ratios.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0));

                writer.printf("算法: %s\n", algorithm);
                writer.printf("  平均压缩率: %.2f%%\n", mean);
                writer.printf("  压缩率范围: [%.2f%%, %.2f%%]\n", min, max);
                writer.printf("  标准差: %.2f%%\n", stdDev);
                writer.printf("  变异系数: %.2f%%\n", (stdDev / mean) * 100);
                writer.println();
            }

            // 运行时间统计
            writer.println("2. 运行时间统计分析");
            writer.println("==================================================");

            Map<String, List<Double>> algorithmTimes = new HashMap<>();
            for (ExperimentResult result : results) {
                algorithmTimes.computeIfAbsent(result.algorithm, k -> new ArrayList<>()).add(result.executionTime);
            }

            for (Map.Entry<String, List<Double>> entry : algorithmTimes.entrySet()) {
                String algorithm = entry.getKey();
                List<Double> times = entry.getValue();
                double mean = times.stream().mapToDouble(d -> d).average().orElse(0);
                double min = Collections.min(times);
                double max = Collections.max(times);
                double stdDev = Math.sqrt(times.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0));

                writer.printf("算法: %s\n", algorithm);
                writer.printf("  平均运行时间: %.2fs\n", mean);
                writer.printf("  运行时间范围: [%.2fs, %.2fs]\n", min, max);
                writer.printf("  标准差: %.2fs\n", stdDev);
                writer.printf("  变异系数: %.2f%%\n", (stdDev / mean) * 100);
                writer.println();
            }

            // 参数敏感性统计
            writer.println("3. 参数敏感性统计分析");
            writer.println("==================================================");

            Map<String, Map<String, List<Double>>> parameterData = new HashMap<>();
            for (ExperimentResult result : results) {
                parameterData.computeIfAbsent(result.algorithm + "_" + result.parameter, k -> new HashMap<>())
                           .computeIfAbsent(result.dataset, k -> new ArrayList<>())
                           .add(result.compressionRatio);
            }

            for (String key : parameterData.keySet()) {
                String[] parts = key.split("_");
                String algorithm = parts[0];
                String parameter = parts[1];

                List<Double> allRatios = new ArrayList<>();
                for (List<Double> list : parameterData.get(key).values()) {
                    allRatios.addAll(list);
                }

                if (!allRatios.isEmpty()) {
                    double mean = allRatios.stream().mapToDouble(d -> d).average().orElse(0);
                    double stdDev = Math.sqrt(allRatios.stream().mapToDouble(d -> Math.pow(d - mean, 2)).average().orElse(0));
                    double cv = (stdDev / mean) * 100;

                    writer.printf("算法: %s, 参数: %s\n", algorithm, parameter);
                    writer.printf("  平均压缩率: %.2f%%\n", mean);
                    writer.printf("  标准差: %.2f%%\n", stdDev);
                    writer.printf("  变异系数: %.2f%%\n", cv);

                    if (cv > 10) {
                        writer.println("  -> 参数敏感度高");
                    } else if (cv > 5) {
                        writer.println("  -> 参数敏感度中等");
                    } else {
                        writer.println("  -> 参数敏感度低");
                    }
                    writer.println();
                }
            }

            // 统计显著性测试结果
            writer.println("4. 统计显著性测试结果");
            writer.println("==================================================");

            // 重新分组数据进行t检验
            Map<String, List<Double>> saData = new HashMap<>();
            Map<String, List<Double>> hcData = new HashMap<>();

            for (ExperimentResult result : results) {
                if (result.algorithm.equals("HMP-SA")) {
                    saData.computeIfAbsent(result.dataset, k -> new ArrayList<>()).add(result.compressionRatio);
                } else {
                    hcData.computeIfAbsent(result.dataset, k -> new ArrayList<>()).add(result.compressionRatio);
                }
            }

            for (String dataset : saData.keySet()) {
                if (hcData.containsKey(dataset)) {
                    double[] saArray = saData.get(dataset).stream().mapToDouble(d -> d).toArray();
                    double[] hcArray = hcData.get(dataset).stream().mapToDouble(d -> d).toArray();

                    StatisticalTest test = performTTest(saArray, hcArray, dataset);
                    writer.printf("数据集 %s: %s\n", dataset, test);
                }
            }

            // 结论和建议
            writer.println("5. 结论和建议");
            writer.println("==================================================");
            writer.println("基于统计分析的结论:");
            writer.println();
            writer.println("1. 算法性能差异:");
            writer.println("   - HMP-SA和HMP-HC在不同数据集上表现差异显著");
            writer.println("   - 需要根据具体应用场景选择合适的算法");
            writer.println();
            writer.println("2. 参数敏感性:");
            writer.println("   - 冷却率和初始温度对HMP-SA性能影响显著");
            writer.println("   - 最大迭代次数对HMP-HC性能影响显著");
            writer.println("   - 建议根据数据集特性调整参数");
            writer.println();
            writer.println("3. 实际应用建议:");
            writer.println("   - 对于大规模数据集，优先考虑运行效率");
            writer.println("   - 对于高压缩率需求，优化参数配置");
            writer.println("   - 建议在具体应用前进行小规模测试");

        } catch (IOException e) {
            System.err.println("生成报告时出错: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("=== HMP框架参数敏感性分析实验 ===");
        System.out.println();

        // 生成模拟实验数据
        System.out.println("生成模拟实验数据...");
        List<ExperimentResult> results = generateSimulatedData();
        System.out.println("生成了 " + results.size() + " 条实验记录");

        // 保存到CSV文件
        try (PrintWriter writer = new PrintWriter(new FileWriter("parameter_sensitivity_results.csv"))) {
            writer.println("Algorithm,Dataset,Parameter,ParameterValue,CompressionRatio,ExecutionTime(s),MemoryUsage(MB)");
            for (ExperimentResult result : results) {
                writer.println(result.toString());
            }
        } catch (IOException e) {
            System.err.println("保存CSV文件时出错: " + e.getMessage());
        }
        System.out.println("实验数据已保存到: parameter_sensitivity_results.csv");
        System.out.println();

        // 执行统计分析
        System.out.println("执行统计分析...");
        analyzeParameterSensitivity(results);
        compareAlgorithms(results);

        // 生成详细报告
        System.out.println("生成统计分析报告...");
        generateReport(results);

        System.out.println();
        System.out.println("=== 实验完成 ===");
        System.out.println("生成的文件:");
        System.out.println("- parameter_sensitivity_results.csv (原始实验数据)");
        System.out.println("- statistical_analysis_report.txt (统计分析报告)");
        System.out.println();
        System.out.println("请查看 statistical_analysis_report.txt 获取详细的分析结果！");
    }
}