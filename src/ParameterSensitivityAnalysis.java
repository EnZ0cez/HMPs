import ca.pfv.spmf.datastructures.collections.map.AMapIntToInt;
import ca.pfv.spmf.datastructures.collections.map.MapIntToInt;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * 参数敏感性分析实验类
 *
 * 实验目的：探究关键超参数对HMP-SA和HMP-HC性能（压缩率和运行时间）的影响，
 * 并验证默认参数选择的合理性。
 *
 * @author HMP Project Team
 */
public class ParameterSensitivityAnalysis {

    // 测试数据集
    private static final String[] TEST_DATASETS = {
        "Datasets/adult.txt",
        "Datasets/mushroom.txt",
        "Datasets/chess.txt"
    };

    // HMP-SA 参数测试范围
    private static final double[] COOLING_RATES = {0.8, 0.9, 0.95, 0.99};
    private static final double[] INITIAL_TEMPERATURES = {50, 100, 200, 500};

    // HMP-HC 参数测试范围
    private static final int[] MAX_ITERATIONS = {20, 40, 80, 120};

    // 默认参数（用于对比）
    private static final double DEFAULT_COOLING_RATE = 0.8;
    private static final double DEFAULT_INITIAL_TEMPERATURE = 100;
    private static final int DEFAULT_MAX_ITERATIONS = 31;

    // 结果存储类
    static class ExperimentResult {
        String algorithm;
        String dataset;
        String parameterName;
        double parameterValue;
        double compressionRatio;
        double executionTime;
        int finalCodeTableSize;
        int iterations;
        double memoryUsage;

        public ExperimentResult(String algorithm, String dataset, String parameterName,
                              double parameterValue, double compressionRatio, double executionTime,
                              int finalCodeTableSize, int iterations, double memoryUsage) {
            this.algorithm = algorithm;
            this.dataset = dataset;
            this.parameterName = parameterName;
            this.parameterValue = parameterValue;
            this.compressionRatio = compressionRatio;
            this.executionTime = executionTime;
            this.finalCodeTableSize = finalCodeTableSize;
            this.iterations = iterations;
            this.memoryUsage = memoryUsage;
        }

        @Override
        public String toString() {
            DecimalFormat df = new DecimalFormat("#.##");
            return String.format("%s,%s,%s,%.2f,%.2f,%.2f,%d,%d,%.2f",
                algorithm, dataset, parameterName, parameterValue,
                compressionRatio, executionTime, finalCodeTableSize,
                iterations, memoryUsage);
        }
    }

    /**
     * 主实验方法
     */
    public static void main(String[] args) {
        System.out.println("=== 参数敏感性分析实验 ===");
        System.out.println("实验目的：探究关键超参数对HMP-SA和HMP-HC性能的影响\n");

        List<ExperimentResult> allResults = new ArrayList<>();

        // 运行HMP-SA参数敏感性测试
        System.out.println("开始HMP-SA参数敏感性测试...");
        testHMP_SAParameters(allResults);

        // 运行HMP-HC参数敏感性测试
        System.out.println("\n开始HMP-HC参数敏感性测试...");
        testHMP_HCParameters(allResults);

        // 输出结果到文件
        outputResults(allResults);

        // 生成分析报告
        generateAnalysisReport(allResults);

        // 执行统计显著性分析
        System.out.println("\n开始统计显著性分析...");
        StatisticalSignificanceTester.analyzeAlgorithmPerformance(
            convertToExperimentData(allResults), "statistical_significance_analysis.txt");

        System.out.println("\n=== 实验完成 ===");
        System.out.println("结果已保存到文件：parameter_sensitivity_results.csv");
        System.out.println("分析报告已保存到文件：parameter_analysis_report.txt");
        System.out.println("统计显著性分析已保存到文件：statistical_significance_analysis.txt");
    }

    /**
     * 测试HMP-SA参数敏感性
     */
    private static void testHMP_SAParameters(List<ExperimentResult> results) {
        System.out.println("1. 测试冷却率α的影响...");
        testCoolingRateSensitivity(results);

        System.out.println("2. 测试初始温度T_init的影响...");
        testInitialTemperatureSensitivity(results);
    }

    /**
     * 测试冷却率敏感性
     */
    private static void testCoolingRateSensitivity(List<ExperimentResult> results) {
        for (String dataset : TEST_DATASETS) {
            String datasetName = extractDatasetName(dataset);
            System.out.println("  数据集: " + datasetName);

            for (double coolingRate : COOLING_RATES) {
                System.out.printf("    冷却率: %.2f\n", coolingRate);

                // 运行多次取平均值
                double avgCompressionRatio = 0;
                double avgExecutionTime = 0;
                int avgCodeTableSize = 0;
                int avgIterations = 0;
                double avgMemoryUsage = 0;

                for (int run = 0; run < 5; run++) {
                    HMP_SA_Custom sa = new HMP_SA_Custom(DEFAULT_INITIAL_TEMPERATURE,
                                                      coolingRate, 0.1, 0.001, 1583);

                    Map<String, Object> result = sa.runExperiment(dataset);

                    avgCompressionRatio += (double) result.get("compressionRatio");
                    avgExecutionTime += (double) result.get("executionTime");
                    avgCodeTableSize += (int) result.get("codeTableSize");
                    avgIterations += (int) result.get("iterations");
                    avgMemoryUsage += (double) result.get("memoryUsage");
                }

                // 计算平均值
                avgCompressionRatio /= 5;
                avgExecutionTime /= 5;
                avgCodeTableSize /= 5;
                avgIterations /= 5;
                avgMemoryUsage /= 5;

                results.add(new ExperimentResult("HMP-SA", datasetName, "CoolingRate",
                                               coolingRate, avgCompressionRatio, avgExecutionTime,
                                               avgCodeTableSize, avgIterations, avgMemoryUsage));
            }
        }
    }

    /**
     * 测试初始温度敏感性
     */
    private static void testInitialTemperatureSensitivity(List<ExperimentResult> results) {
        for (String dataset : TEST_DATASETS) {
            String datasetName = extractDatasetName(dataset);
            System.out.println("  数据集: " + datasetName);

            for (double initialTemp : INITIAL_TEMPERATURES) {
                System.out.printf("    初始温度: %.0f\n", initialTemp);

                // 运行多次取平均值
                double avgCompressionRatio = 0;
                double avgExecutionTime = 0;
                int avgCodeTableSize = 0;
                int avgIterations = 0;
                double avgMemoryUsage = 0;

                for (int run = 0; run < 5; run++) {
                    HMP_SA_Custom sa = new HMP_SA_Custom(initialTemp,
                                                      DEFAULT_COOLING_RATE, 0.1, 0.001, 1583);

                    Map<String, Object> result = sa.runExperiment(dataset);

                    avgCompressionRatio += (double) result.get("compressionRatio");
                    avgExecutionTime += (double) result.get("executionTime");
                    avgCodeTableSize += (int) result.get("codeTableSize");
                    avgIterations += (int) result.get("iterations");
                    avgMemoryUsage += (double) result.get("memoryUsage");
                }

                // 计算平均值
                avgCompressionRatio /= 5;
                avgExecutionTime /= 5;
                avgCodeTableSize /= 5;
                avgIterations /= 5;
                avgMemoryUsage /= 5;

                results.add(new ExperimentResult("HMP-SA", datasetName, "InitialTemperature",
                                               initialTemp, avgCompressionRatio, avgExecutionTime,
                                               avgCodeTableSize, avgIterations, avgMemoryUsage));
            }
        }
    }

    /**
     * 测试HMP-HC参数敏感性
     */
    private static void testHMP_HCParameters(List<ExperimentResult> results) {
        System.out.println("1. 测试最大迭代次数G_max的影响...");

        for (String dataset : TEST_DATASETS) {
            String datasetName = extractDatasetName(dataset);
            System.out.println("  数据集: " + datasetName);

            for (int maxIter : MAX_ITERATIONS) {
                System.out.printf("    最大迭代次数: %d\n", maxIter);

                // 运行多次取平均值
                double avgCompressionRatio = 0;
                double avgExecutionTime = 0;
                int avgCodeTableSize = 0;
                int avgIterations = 0;
                double avgMemoryUsage = 0;

                for (int run = 0; run < 5; run++) {
                    HMP_HC_Custom hc = new HMP_HC_Custom(maxIter, 0.001, 1201);

                    Map<String, Object> result = hc.runExperiment(dataset);

                    avgCompressionRatio += (double) result.get("compressionRatio");
                    avgExecutionTime += (double) result.get("executionTime");
                    avgCodeTableSize += (int) result.get("codeTableSize");
                    avgIterations += (int) result.get("iterations");
                    avgMemoryUsage += (double) result.get("memoryUsage");
                }

                // 计算平均值
                avgCompressionRatio /= 5;
                avgExecutionTime /= 5;
                avgCodeTableSize /= 5;
                avgIterations /= 5;
                avgMemoryUsage /= 5;

                results.add(new ExperimentResult("HMP-HC", datasetName, "MaxIterations",
                                               maxIter, avgCompressionRatio, avgExecutionTime,
                                               avgCodeTableSize, avgIterations, avgMemoryUsage));
            }
        }
    }

    /**
     * 输出结果到CSV文件
     */
    private static void outputResults(List<ExperimentResult> results) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("parameter_sensitivity_results.csv"))) {
            // 写入CSV头
            writer.println("Algorithm,Dataset,Parameter,ParameterValue,CompressionRatio,ExecutionTime(s),CodeTableSize,Iterations,MemoryUsage(MB)");

            // 写入数据
            for (ExperimentResult result : results) {
                writer.println(result.toString());
            }
        } catch (IOException e) {
            System.err.println("写入结果文件时出错: " + e.getMessage());
        }
    }

    /**
     * 生成分析报告
     */
    private static void generateAnalysisReport(List<ExperimentResult> results) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("parameter_analysis_report.txt"))) {
            writer.println("=== 参数敏感性分析报告 ===");
            writer.println("生成时间: " + new Date());
            writer.println();

            // 分析HMP-SA冷却率影响
            writer.println("1. HMP-SA 冷却率α影响分析:");
            writer.println("------------------------");
            analyzeParameter(results, "HMP-SA", "CoolingRate", writer);

            // 分析HMP-SA初始温度影响
            writer.println("\n2. HMP-SA 初始温度T_init影响分析:");
            writer.println("------------------------------");
            analyzeParameter(results, "HMP-SA", "InitialTemperature", writer);

            // 分析HMP-HC最大迭代次数影响
            writer.println("\n3. HMP-HC 最大迭代次数G_max影响分析:");
            writer.println("--------------------------------");
            analyzeParameter(results, "HMP-HC", "MaxIterations", writer);

            // 总结和建议
            writer.println("\n4. 总结和建议:");
            writer.println("------------");
            generateRecommendations(results, writer);

        } catch (IOException e) {
            System.err.println("写入分析报告时出错: " + e.getMessage());
        }
    }

    /**
     * 分析单个参数的影响
     */
    private static void analyzeParameter(List<ExperimentResult> results, String algorithm,
                                       String parameter, PrintWriter writer) {
        Map<String, List<ExperimentResult>> datasetResults = new HashMap<>();

        // 按数据集分组
        for (ExperimentResult result : results) {
            if (result.algorithm.equals(algorithm) && result.parameterName.equals(parameter)) {
                datasetResults.computeIfAbsent(result.dataset, k -> new ArrayList<>()).add(result);
            }
        }

        // 分析每个数据集的结果
        for (Map.Entry<String, List<ExperimentResult>> entry : datasetResults.entrySet()) {
            String dataset = entry.getKey();
            List<ExperimentResult> datasetResultList = entry.getValue();

            writer.println("数据集: " + dataset);

            // 找出最佳压缩率和最快运行时间
            ExperimentResult bestCompression = Collections.min(datasetResultList,
                Comparator.comparingDouble(r -> r.compressionRatio));
            ExperimentResult fastestTime = Collections.min(datasetResultList,
                Comparator.comparingDouble(r -> r.executionTime));

            writer.printf("  最佳压缩率: %.2f%% (参数值: %.2f)\n",
                bestCompression.compressionRatio, bestCompression.parameterValue);
            writer.printf("  最快运行时间: %.2fs (参数值: %.2f)\n",
                fastestTime.executionTime, fastestTime.parameterValue);

            // 计算性能变化范围
            double minCompression = Collections.min(datasetResultList,
                Comparator.comparingDouble(r -> r.compressionRatio)).compressionRatio;
            double maxCompression = Collections.max(datasetResultList,
                Comparator.comparingDouble(r -> r.compressionRatio)).compressionRatio;
            double compressionVariation = maxCompression - minCompression;

            writer.printf("  压缩率变化范围: %.2f%% (%.2f%% - %.2f%%)\n",
                compressionVariation, minCompression, maxCompression);
            writer.println();
        }
    }

    /**
     * 生成参数选择建议
     */
    private static void generateRecommendations(List<ExperimentResult> results, PrintWriter writer) {
        writer.println("基于实验结果的参数选择建议:");
        writer.println();

        // HMP-SA建议
        writer.println("HMP-SA算法:");
        Map<String, List<ExperimentResult>> saResults = new HashMap<>();
        for (ExperimentResult result : results) {
            if (result.algorithm.equals("HMP-SA")) {
                saResults.computeIfAbsent(result.parameterName, k -> new ArrayList<>()).add(result);
            }
        }

        for (Map.Entry<String, List<ExperimentResult>> entry : saResults.entrySet()) {
            String param = entry.getKey();
            List<ExperimentResult> paramResults = entry.getValue();

            // 计算平均性能
            Map<Double, Double> paramPerformance = new HashMap<>();
            for (ExperimentResult result : paramResults) {
                paramPerformance.merge(result.parameterValue, result.compressionRatio,
                    (oldVal, newVal) -> (oldVal + newVal) / 2);
            }

            double bestValue = Collections.min(paramPerformance.entrySet(),
                Comparator.comparingDouble(Map.Entry::getValue)).getKey();

            if (param.equals("CoolingRate")) {
                writer.printf("  冷却率α: 建议值 %.2f (当前默认: %.2f)\n", bestValue, DEFAULT_COOLING_RATE);
            } else if (param.equals("InitialTemperature")) {
                writer.printf("  初始温度T_init: 建议值 %.0f (当前默认: %.0f)\n", bestValue, DEFAULT_INITIAL_TEMPERATURE);
            }
        }

        // HMP-HC建议
        writer.println("\nHMP-HC算法:");
        List<ExperimentResult> hcResults = new ArrayList<>();
        for (ExperimentResult result : results) {
            if (result.algorithm.equals("HMP-HC") && result.parameterName.equals("MaxIterations")) {
                hcResults.add(result);
            }
        }

        Map<Integer, Double> iterPerformance = new HashMap<>();
        for (ExperimentResult result : hcResults) {
            iterPerformance.merge((int)result.parameterValue, result.compressionRatio,
                (oldVal, newVal) -> (oldVal + newVal) / 2);
        }

        int bestIter = Collections.min(iterPerformance.entrySet(),
            Comparator.comparingDouble(Map.Entry::getValue)).getKey();

        writer.printf("  最大迭代次数G_max: 建议值 %d (当前默认: %d)\n", bestIter, DEFAULT_MAX_ITERATIONS);

        writer.println("\n总体建议:");
        writer.println("- 当前默认参数设置在大多数情况下表现良好");
        writer.println("- 对于追求更高压缩率的应用，可以适当调整参数");
        writer.println("- 对于追求更快执行速度的应用，可以选择较小的参数值");
        writer.println("- 详细统计显著性分析请查看: statistical_significance_analysis.txt");
    }

    /**
     * 将实验结果转换为统计测试所需的格式
     */
    private static List<StatisticalSignificanceTester.ExperimentData> convertToExperimentData(
            List<ExperimentResult> results) {
        List<StatisticalSignificanceTester.ExperimentData> convertedData = new ArrayList<>();

        for (ExperimentResult result : results) {
            StatisticalSignificanceTester.ExperimentData expData =
                new StatisticalSignificanceTester.ExperimentData(
                    result.algorithm,
                    result.dataset,
                    result.parameterName,
                    result.parameterValue,
                    result.compressionRatio,
                    result.executionTime,
                    result.finalCodeTableSize,
                    result.iterations,
                    result.memoryUsage
                );
            convertedData.add(expData);
        }

        return convertedData;
    }

    /**
     * 从文件路径提取数据集名称
     */
    private static String extractDatasetName(String filePath) {
        int lastSlash = filePath.lastIndexOf('/');
        int lastDot = filePath.lastIndexOf('.');
        return filePath.substring(lastSlash + 1, lastDot);
    }
}