import java.io.*;
import java.util.*;
import java.text.DecimalFormat;

/**
 * 结果可视化工具
 * 用于生成参数敏感性分析的可视化图表和报告
 */
public class ResultVisualizer {

    /**
     * 主方法 - 生成可视化报告
     */
    public static void main(String[] args) {
        System.out.println("=== 结果可视化工具 ===");

        // 检查结果文件是否存在
        File resultFile = new File("parameter_sensitivity_results.csv");
        if (!resultFile.exists()) {
            System.err.println("错误: 找不到结果文件 'parameter_sensitivity_results.csv'");
            System.err.println("请先运行 ParameterSensitivityAnalysis 实验生成结果文件");
            return;
        }

        // 读取结果数据
        List<ExperimentData> data = readResults(resultFile);

        // 生成可视化报告
        generateVisualizationReport(data);

        System.out.println("可视化报告已生成: visualization_report.html");
        System.out.println("统计摘要已生成: experiment_summary.txt");
    }

    /**
     * 读取实验结果
     */
    private static List<ExperimentData> readResults(File file) {
        List<ExperimentData> data = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
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
        } catch (IOException | NumberFormatException e) {
            System.err.println("读取结果文件时出错: " + e.getMessage());
        }

        return data;
    }

    /**
     * 生成可视化报告
     */
    private static void generateVisualizationReport(List<ExperimentData> data) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("visualization_report.html"))) {
            // HTML头部
            writer.println("<!DOCTYPE html>");
            writer.println("<html lang='zh-CN'>");
            writer.println("<head>");
            writer.println("    <meta charset='UTF-8'>");
            writer.println("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            writer.println("    <title>参数敏感性分析报告</title>");
            writer.println("    <script src='https://cdn.jsdelivr.net/npm/chart.js'></script>");
            writer.println("    <style>");
            writer.println("        body { font-family: Arial, sans-serif; margin: 20px; }");
            writer.println("        .chart-container { width: 800px; height: 400px; margin: 20px 0; }");
            writer.println("        .section { margin: 30px 0; }");
            writer.println("        h1, h2 { color: #333; }");
            writer.println("        table { border-collapse: collapse; width: 100%; }");
            writer.println("        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
            writer.println("        th { background-color: #f2f2f2; }");
            writer.println("        .best { background-color: #e8f5e8; }");
            writer.println("        .worst { background-color: #ffe8e8; }");
            writer.println("    </style>");
            writer.println("</head>");
            writer.println("<body>");

            // 标题
            writer.println("    <h1>参数敏感性分析可视化报告</h1>");
            writer.println("    <p>生成时间: " + new Date() + "</p>");

            // HMP-SA分析
            generateSACharts(writer, data);

            // HMP-HC分析
            generateHCCharts(writer, data);

            // 总结表格
            generateSummaryTable(writer, data);

            writer.println("</body>");
            writer.println("</html>");

        } catch (IOException e) {
            System.err.println("生成可视化报告时出错: " + e.getMessage());
        }

        // 生成文本摘要
        generateTextSummary(data);
    }

    /**
     * 生成HMP-SA图表
     */
    private static void generateSACharts(PrintWriter writer, List<ExperimentData> data) {
        writer.println("    <div class='section'>");
        writer.println("        <h2>HMP-SA算法参数分析</h2>");

        // 冷却率分析
        writer.println("        <h3>冷却率α对性能的影响</h3>");
        writer.println("        <div class='chart-container'>");
        writer.println("            <canvas id='saCoolingChart'></canvas>");
        writer.println("        </div>");

        // 初始温度分析
        writer.println("        <h3>初始温度T_init对性能的影响</h3>");
        writer.println("        <div class='chart-container'>");
        writer.println("            <canvas id='saTempChart'></canvas>");
        writer.println("        </div>");
        writer.println("    </div>");
    }

    /**
     * 生成HMP-HC图表
     */
    private static void generateHCCharts(PrintWriter writer, List<ExperimentData> data) {
        writer.println("    <div class='section'>");
        writer.println("        <h2>HMP-HC算法参数分析</h2>");

        // 最大迭代次数分析
        writer.println("        <h3>最大迭代次数G_max对性能的影响</h3>");
        writer.println("        <div class='chart-container'>");
        writer.println("            <canvas id='hcIterChart'></canvas>");
        writer.println("        </div>");
        writer.println("    </div>");
    }

    /**
     * 生成总结表格
     */
    private static void generateSummaryTable(PrintWriter writer, List<ExperimentData> data) {
        writer.println("    <div class='section'>");
        writer.println("        <h2>实验结果总结</h2>");

        // 按算法分组
        Map<String, List<ExperimentData>> algorithmGroups = new HashMap<>();
        for (ExperimentData exp : data) {
            algorithmGroups.computeIfAbsent(exp.algorithm, k -> new ArrayList<>()).add(exp);
        }

        for (Map.Entry<String, List<ExperimentData>> entry : algorithmGroups.entrySet()) {
            String algorithm = entry.getKey();
            List<ExperimentData> algorithmData = entry.getValue();

            writer.println("        <h3>" + algorithm + " 最佳参数配置</h3>");
            writer.println("        <table>");
            writer.println("            <tr><th>数据集</th><th>参数</th><th>最佳值</th><th>压缩率(%)</th><th>运行时间(s)</th></tr>");

            // 按数据集和参数分组
            Map<String, Map<String, List<ExperimentData>>> datasetParamGroups = new HashMap<>();
            for (ExperimentData exp : algorithmData) {
                datasetParamGroups.computeIfAbsent(exp.dataset, k -> new HashMap<>())
                    .computeIfAbsent(exp.parameter, k -> new ArrayList<>()).add(exp);
            }

            for (Map.Entry<String, Map<String, List<ExperimentData>>> datasetEntry : datasetParamGroups.entrySet()) {
                String dataset = datasetEntry.getKey();

                for (Map.Entry<String, List<ExperimentData>> paramEntry : datasetEntry.getValue().entrySet()) {
                    String parameter = paramEntry.getKey();
                    List<ExperimentData> paramData = paramEntry.getValue();

                    // 找出最佳结果（最小压缩率）
                    ExperimentData best = Collections.min(paramData,
                        Comparator.comparingDouble(e -> e.compressionRatio));

                    writer.printf("            <tr><td>%s</td><td>%s</td><td>%.2f</td><td>%.2f</td><td>%.2f</td></tr>\n",
                        dataset, parameter, best.parameterValue, best.compressionRatio, best.executionTime);
                }
            }

            writer.println("        </table>");
        }

        writer.println("    </div>");

        // JavaScript图表代码
        generateChartScripts(writer, data);
    }

    /**
     * 生成图表JavaScript代码
     */
    private static void generateChartScripts(PrintWriter writer, List<ExperimentData> data) {
        writer.println("    <script>");

        // HMP-SA冷却率图表
        writer.println("        // HMP-SA冷却率图表");
        writer.println("        const saCoolingCtx = document.getElementById('saCoolingChart').getContext('2d');");
        writer.println("        new Chart(saCoolingCtx, {");
        writer.println("            type: 'line',");
        writer.println("            data: {");

        // 准备数据
        Map<String, Map<Double, Double>> saCoolingData = prepareSAData(data, "CoolingRate");

        writer.println("                labels: [0.8, 0.9, 0.95, 0.99],");
        writer.println("                datasets: [");

        int datasetIndex = 0;
        for (Map.Entry<String, Map<Double, Double>> entry : saCoolingData.entrySet()) {
            String datasetName = entry.getKey();
            Map<Double, Double> values = entry.getValue();

            writer.println("                    {");
            writer.printf("                        label: '%s',\n", datasetName);
            writer.println("                        data: [" +
                String.format("%.2f, %.2f, %.2f, %.2f",
                    values.get(0.8), values.get(0.9), values.get(0.95), values.get(0.99)) + "],");
            writer.println("                        borderColor: getColor(" + datasetIndex + "),");
            writer.println("                        fill: false");
            writer.println("                    }");

            if (datasetIndex < saCoolingData.size() - 1) {
                writer.println("                    ,");
            }
            datasetIndex++;
        }

        writer.println("                ]");
        writer.println("            },");
        writer.println("            options: {");
        writer.println("                responsive: true,");
        writer.println("                plugins: {");
        writer.println("                    title: { display: true, text: '冷却率对压缩率的影响' },");
        writer.println("                    legend: { display: true }");
        writer.println("                },");
        writer.println("                scales: {");
        writer.println("                    x: { title: { display: true, text: '冷却率 α' } },");
        writer.println("                    y: { title: { display: true, text: '压缩率 (%)' } }");
        writer.println("                }");
        writer.println("            }");
        writer.println("        });");

        // 颜色函数
        writer.println("        function getColor(index) {");
        writer.println("            const colors = ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF', '#FF9F40'];");
        writer.println("            return colors[index % colors.length];");
        writer.println("        }");

        writer.println("    </script>");
    }

    /**
     * 准备HMP-SA数据
     */
    private static Map<String, Map<Double, Double>> prepareSAData(List<ExperimentData> data, String parameter) {
        Map<String, Map<Double, Double>> result = new HashMap<>();

        for (ExperimentData exp : data) {
            if (exp.algorithm.equals("HMP-SA") && exp.parameter.equals(parameter)) {
                result.computeIfAbsent(exp.dataset, k -> new HashMap<>())
                    .put(exp.parameterValue, exp.compressionRatio);
            }
        }

        return result;
    }

    /**
     * 生成文本摘要
     */
    private static void generateTextSummary(List<ExperimentData> data) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("experiment_summary.txt"))) {
            writer.println("=== 参数敏感性分析实验摘要 ===");
            writer.println("生成时间: " + new Date());
            writer.println("总实验次数: " + data.size());
            writer.println();

            // 统计每种算法的最佳性能
            Map<String, ExperimentData> bestPerformance = new HashMap<>();

            for (ExperimentData exp : data) {
                if (!bestPerformance.containsKey(exp.algorithm) ||
                    exp.compressionRatio < bestPerformance.get(exp.algorithm).compressionRatio) {
                    bestPerformance.put(exp.algorithm, exp);
                }
            }

            writer.println("最佳性能配置:");
            for (Map.Entry<String, ExperimentData> entry : bestPerformance.entrySet()) {
                ExperimentData best = entry.getValue();
                writer.printf("算法: %s\n", entry.getKey());
                writer.printf("  数据集: %s\n", best.dataset);
                writer.printf("  参数: %s = %.2f\n", best.parameter, best.parameterValue);
                writer.printf("  压缩率: %.2f%%\n", best.compressionRatio);
                writer.printf("  运行时间: %.2fs\n", best.executionTime);
                writer.printf("  内存使用: %.2fMB\n\n", best.memoryUsage);
            }

            // 参数敏感性分析
            writer.println("参数敏感性分析:");
            analyzeParameterSensitivity(writer, data);

        } catch (IOException e) {
            System.err.println("生成文本摘要时出错: " + e.getMessage());
        }
    }

    /**
     * 分析参数敏感性
     */
    private static void analyzeParameterSensitivity(PrintWriter writer, List<ExperimentData> data) {
        Map<String, Map<String, List<Double>>> paramAnalysis = new HashMap<>();

        // 按算法和参数分组
        for (ExperimentData exp : data) {
            String key = exp.algorithm + "_" + exp.parameter;
            paramAnalysis.computeIfAbsent(key, k -> new HashMap<>())
                .computeIfAbsent(exp.dataset, k -> new ArrayList<>())
                .add(exp.compressionRatio);
        }

        for (Map.Entry<String, Map<String, List<Double>>> entry : paramAnalysis.entrySet()) {
            String[] parts = entry.getKey().split("_");
            String algorithm = parts[0];
            String parameter = parts[1];

            writer.printf("算法: %s, 参数: %s\n", algorithm, parameter);

            for (Map.Entry<String, List<Double>> datasetEntry : entry.getValue().entrySet()) {
                String dataset = datasetEntry.getKey();
                List<Double> values = datasetEntry.getValue();

                double min = Collections.min(values);
                double max = Collections.max(values);
                double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double stdDev = calculateStdDev(values, avg);

                writer.printf("  数据集 %s: 平均=%.2f, 范围=[%.2f, %.2f], 标准差=%.2f\n",
                    dataset, avg, min, max, stdDev);
            }
            writer.println();
        }
    }

    /**
     * 计算标准差
     */
    private static double calculateStdDev(List<Double> values, double mean) {
        double sumSquared = 0;
        for (double value : values) {
            sumSquared += Math.pow(value - mean, 2);
        }
        return Math.sqrt(sumSquared / values.size());
    }

    /**
     * 实验数据类
     */
    static class ExperimentData {
        String algorithm;
        String dataset;
        String parameter;
        double parameterValue;
        double compressionRatio;
        double executionTime;
        int codeTableSize;
        int iterations;
        double memoryUsage;

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
}