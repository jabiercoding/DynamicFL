package dynamicfl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import metricsCalculation.MetricsCalculation;
import utils.FeatureUtils;
import utils.FileUtils;

public class DynamicFL2BenchResults {

	/**
	 * Compute results
	 * 
	 * @param pathToArgoUMLSPLBenchmark e.g., "C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark"
	 */
	public static void compute(String pathToArgoUMLSPLBenchmark, Map<String,Map<String, List<Integer>>> featureClassAndLines, File output) {
		
		// Input path to the benchmark
		File argoUMLSPLBenchmark = new File(pathToArgoUMLSPLBenchmark);

		output.mkdirs();

		// We assume that the ArgoUMLSPLBenchmark is in a project in the workspace and
		// the Original scenario was created and used for creating the Pangolin traces
		File originalArgoUMLsrc = new File(argoUMLSPLBenchmark,
				"scenarios/ScenarioOriginalVariant/variants/Original.config/src");
		if (!originalArgoUMLsrc.exists()) {
			System.out.println("Original scenario has to be created first");
			return;
		}

		File outputScenarioFeatureMetrics = new File(output, "MetricsScenarioFeature.csv");
		try {
			FileUtils.writeFile(outputScenarioFeatureMetrics,
					"Scenario;Feature;Precision;Recall;F1;ClassPrecision;ClassRecall;ClassF1\n");
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		File scenariosFolder = new File(argoUMLSPLBenchmark, "scenarios");

		for (File scenario : scenariosFolder.listFiles()) {
			if (scenario.isDirectory()) {
				System.out.println("\nSCENARIO " + scenario.getName() + "\n\n");
				// check if it it was created
				File variants = new File(scenario, "variants");
				if (!variants.exists()) {
					System.out.println("Scenario's variants have to be created first");
					continue;
				}

				// Scenario path
				String scenarioPath = scenario.getAbsolutePath();
				File featureInfo = new File(argoUMLSPLBenchmark, "featuresInfo/features.txt");
				String featuresInfoPath = featureInfo.getAbsolutePath();

				FeatureUtils fUtils = new FeatureUtils(scenarioPath, featuresInfoPath);

				File outputScenario = new File(output, scenario.getName());
				outputScenario.mkdirs();

				for (String currentFeature : fUtils.getFeatureIds()) {

					System.out.println("\n" + currentFeature);

					// Ground truth for the given feature
					File featureGroundTruthFile = new File(argoUMLSPLBenchmark,
							"groundTruth/" + currentFeature + ".txt");

					Map<String, List<Integer>> classAndLines = featureClassAndLines.get(currentFeature);
					
					// feature is not considered
					if (classAndLines == null) {
						classAndLines = new HashMap<String, List<Integer>>();
					}
					
					Map<String, List<Integer>> absPathAndLines = transformToAbsPathAndLines(originalArgoUMLsrc, classAndLines);
					
					List<String> results = LineTraces2BenchFormat.getResultsInBenchmarkFormat(absPathAndLines, currentFeature,
							fUtils);

					// Save to file

					StringBuffer buffer = new StringBuffer();
					for (String trace : results) {
						buffer.append(trace);
						buffer.append("\n");
					}

					File outputScenarioFeature = new File(outputScenario, currentFeature + ".txt");
					try {
						FileUtils.writeFile(outputScenarioFeature, buffer.toString());
					} catch (Exception e) {
						e.printStackTrace();
					}

					// Metrics
					System.out.println("Official Metrics");
					List<String> groundTruth = FileUtils.getLinesOfFile(featureGroundTruthFile);
					double precision = MetricsCalculation.getPrecision(groundTruth, results);
					double recall = MetricsCalculation.getRecall(groundTruth, results);
					double f1 = MetricsCalculation.getF1(precision, recall);
					System.out.println("Precision: " + precision);
					System.out.println("Recall: " + recall);
					System.out.println("F1: " + f1);

					System.out.println("\nUnofficial Class level metrics");
					List<String> groundTruth2 = convertBenchTracesToClassLevel(groundTruth);
					List<String> results2 = convertBenchTracesToClassLevel(results);
					double precision2 = MetricsCalculation.getPrecision(groundTruth2, results2);
					double recall2 = MetricsCalculation.getRecall(groundTruth2, results2);
					double f12 = MetricsCalculation.getF1(precision2, recall2);
					System.out.println("Precision: " + precision2);
					System.out.println("Recall: " + recall2);
					System.out.println("F1: " + f12);

					try {
						FileUtils.appendToFile(outputScenarioFeatureMetrics, scenario.getName() + ";" + currentFeature + ";"
								+ precision + ";" + recall + ";" + f1 + ";" + precision2 + ";" + recall2 + ";" + f12);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	
	private static Map<String, List<Integer>> transformToAbsPathAndLines(File originalArgoUMLsrc, Map<String, List<Integer>> classAndLines) {
		Map<String, List<Integer>> result = new LinkedHashMap<String, List<Integer>>();
		for(String key : classAndLines.keySet()) {
			String absPath = key.replaceAll("\\.", "\\\\");
			absPath = absPath + ".java";
			File absFile = new File(originalArgoUMLsrc, absPath);
			// Filter files which are not part of the originalArgoUMLsrc (libraries etc.)
			if (!absFile.exists()) {
				System.err.println(absFile.getAbsolutePath() + " does not exist");
				continue;
			}
			result.put(absFile.getAbsolutePath(), classAndLines.get(key));
		}
		return result;
	}


	/**
	 * Truncate traces to class level
	 * 
	 * @param traces
	 * @return
	 */
	public static List<String> convertBenchTracesToClassLevel(List<String> traces) {
		List<String> converted = new ArrayList<String>();
		for (String s : traces) {
			// truncate to get only the class name
			converted.add(s.split(" ")[0]);
		}
		// remove duplicated entries
		Set<String> s = new LinkedHashSet<>(converted);
		converted.clear();
		converted.addAll(s);
		return converted;
	}

}