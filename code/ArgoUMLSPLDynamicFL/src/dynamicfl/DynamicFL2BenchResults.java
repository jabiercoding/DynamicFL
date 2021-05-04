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
	 * @param pathToArgoUMLSPLBenchmark e.g.,
	 *                                  "C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark"
	 * @return map of scenario, feature, (precision, recall, f1, classPrecision,
	 *         classRecall, classF1)
	 */
	public static Map<String, Map<String, List<Double>>> compute(String pathToArgoUMLSPLBenchmark,
			Map<String, Map<String, List<Integer>>> featureClassAndLines, File output, boolean onlyScenarioOriginal) {

		Map<String, Map<String, List<Double>>> result = new LinkedHashMap<String, Map<String, List<Double>>>();

		// Input path to the benchmark
		File argoUMLSPLBenchmark = new File(pathToArgoUMLSPLBenchmark);

		output.mkdirs();

		// We assume that the ArgoUMLSPLBenchmark is in a project in the workspace and
		// the Original scenario was created and used for creating the Pangolin traces
		File originalArgoUMLsrc = new File(argoUMLSPLBenchmark,
				"scenarios/ScenarioOriginalVariant/variants/Original.config/src");
		if (!originalArgoUMLsrc.exists()) {
			System.out.println("Original scenario has to be created first");
			return result;
		}

		File scenariosFolder = new File(argoUMLSPLBenchmark, "scenarios");

		for (File scenario : scenariosFolder.listFiles()) {

			if (scenario.isDirectory()) {
				if (onlyScenarioOriginal && !scenario.getName().equals("ScenarioOriginalVariant")) {
					continue;
				}

				System.out.println("\nSCENARIO " + scenario.getName() + "\n\n");
				// check if it it was created
				File variants = new File(scenario, "variants");
				if (!variants.exists()) {
					System.out.println("Scenario's variants have to be created first");
					continue;
				}

				Map<String, List<Double>> resultScenario = new LinkedHashMap<String, List<Double>>();
				result.put(scenario.getName(), resultScenario);

				// Scenario path
				String scenarioPath = scenario.getAbsolutePath();
				File featureInfo = new File(argoUMLSPLBenchmark, "featuresInfo/features.txt");
				String featuresInfoPath = featureInfo.getAbsolutePath();

				FeatureUtils fUtils = new FeatureUtils(scenarioPath, featuresInfoPath);

				File outputScenario = new File(output, scenario.getName());
				outputScenario.mkdirs();

				// for (String currentFeature : fUtils.getFeatureIds()) {
				for (String currentFeature : featureClassAndLines.keySet()) {

					List<Double> resultFeature = new ArrayList<Double>();
					resultScenario.put(currentFeature, resultFeature);

					System.out.println("\n" + currentFeature);

					// Ground truth for the given feature
					File featureGroundTruthFile = new File(argoUMLSPLBenchmark,
							"groundTruth/" + currentFeature + ".txt");

					Map<String, List<Integer>> classAndLines = featureClassAndLines.get(currentFeature);

					// feature is not considered
					if (classAndLines == null) {
						classAndLines = new HashMap<String, List<Integer>>();
					}

					Map<String, List<Integer>> absPathAndLines = transformToAbsPathAndLines(currentFeature,
							originalArgoUMLsrc, classAndLines);

					List<String> results = LineTraces2BenchFormat.getResultsInBenchmarkFormat(absPathAndLines,
							currentFeature, fUtils, true);

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
					if (!featureGroundTruthFile.exists()) {
						System.out.println(
								"Results found for " + currentFeature + " but it does not exist in the GroundTruth");
					} else {
						List<String> groundTruth = FileUtils.getLinesOfFile(featureGroundTruthFile);
						double precision = MetricsCalculation.getPrecision(groundTruth, results);
						double recall = MetricsCalculation.getRecall(groundTruth, results);
						double f1 = MetricsCalculation.getF1(precision, recall);
						System.out.println("Precision: " + precision);
						System.out.println("Recall: " + recall);
						System.out.println("F1: " + f1);
						resultFeature.add(precision);
						resultFeature.add(recall);
						resultFeature.add(f1);

						System.out.println("\nUnofficial Class level metrics");
						List<String> groundTruth2 = convertBenchTracesToClassLevel(groundTruth);
						List<String> results2 = convertBenchTracesToClassLevel(results);
						double precision2 = MetricsCalculation.getPrecision(groundTruth2, results2);
						double recall2 = MetricsCalculation.getRecall(groundTruth2, results2);
						double f12 = MetricsCalculation.getF1(precision2, recall2);
						System.out.println("Precision: " + precision2);
						System.out.println("Recall: " + recall2);
						System.out.println("F1: " + f12);
						resultFeature.add(precision2);
						resultFeature.add(recall2);
						resultFeature.add(f12);
					}
				}
			}
		}
		resultsToFile(result, new File(output, "MetricsScenarioFeature.csv"));
		return result;
	}

	/**
	 * Results to file
	 * 
	 * @param result
	 * @param output
	 */
	public static void resultsToFile(Map<String, Map<String, List<Double>>> result, File output) {
		try {
			FileUtils.writeFile(output, "Scenario;Feature;Precision;Recall;F1;ClassPrecision;ClassRecall;ClassF1\n");
			for (String scenario : result.keySet()) {
				Map<String, List<Double>> scenarioFeatures = result.get(scenario);
				for (String feature : scenarioFeatures.keySet()) {
					List<Double> metrics = scenarioFeatures.get(feature);
					if (metrics != null && !metrics.isEmpty()) {
						FileUtils.appendToFile(output,
								scenario + ";" + feature + ";" + metrics.get(0) + ";" + metrics.get(1) + ";"
										+ metrics.get(2) + ";" + metrics.get(3) + ";" + metrics.get(4) + ";"
										+ metrics.get(5));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Transform to absolute paths, keep the same lines
	 * 
	 * @param originalArgoUMLsrc
	 * @param classAndLines
	 * @return
	 */
	private static Map<String, List<Integer>> transformToAbsPathAndLines(String currentFeature, File originalArgoUMLsrc,
			Map<String, List<Integer>> classAndLines) {
		Map<String, List<Integer>> result = new LinkedHashMap<String, List<Integer>>();
		for (String key : classAndLines.keySet()) {

			// check of innerclasses
			String absPath = key + "";
			String innerClassCase = getCompilationUnitFromClass(key);
			if (innerClassCase != null) {
				absPath = innerClassCase;
			}

			absPath = absPath.replaceAll("\\.", "\\\\");
			absPath = absPath + ".java";

			File absFile = new File(originalArgoUMLsrc, absPath);
			// Filter files which are not part of the originalArgoUMLsrc (libraries etc.)
			if (!absFile.exists()) {
				System.err.println(absFile.getAbsolutePath() + " does not exist");
				continue;
			}

			// in case of same files (i.e., inner classes)
			List<Integer> current = classAndLines.get(key);
			List<Integer> previous = result.get(absFile.getAbsolutePath());
			if (previous != null) {
				current.addAll(previous);
			}
			result.put(absFile.getAbsolutePath(), current);
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

	/**
	 * Replace the name of the Inner Class by the Class name of the file where the
	 * Inner Class is contained
	 * 
	 * @param potentialInnerClass
	 * @return null or the class containing the inner class
	 */
	public static String getCompilationUnitFromClass(String innerClass) {

		if (innerClass.contains("org.argouml.uml.ui.behavior.state_machines.UMLCallEventOperationComboBox2")
				|| innerClass.contains("org.argouml.uml.ui.behavior.state_machines.UMLCallEventOperationComboBoxModel"))
			return "org.argouml.uml.ui.behavior.state_machines.PropPanelCallEvent";

		if (innerClass.contains("org.argouml.uml.ui.behavior.common_behavior.ActionCreateArgument"))
			return "org.argouml.uml.ui.behavior.common_behavior.PropPanelAction";

		if (innerClass.contains("org.argouml.uml.ui.behavior.common_behavior.UMLActionSequenceActionListModel")
				|| innerClass.contains("org.argouml.uml.ui.behavior.common_behavior.UMLActionSequenceActionList"))
			return "org.argouml.uml.ui.behavior.common_behavior.PropPanelActionSequence";

		if (innerClass.contains("org.argouml.uml.ui.behavior.activity_graphs.UMLPartitionActivityGraphListModel")
				|| innerClass.contains("org.argouml.uml.ui.behavior.activity_graphs.UMLPartitionContentListModel"))
			return "org.argouml.uml.ui.behavior.activity_graphs.PropPanelPartition";

		if (innerClass.contains("org.argouml.uml.diagram.ui.FigAssociationEndAnnotation")
				|| innerClass.contains("org.argouml.uml.diagram.ui.FigOrdering")
				|| innerClass.contains("org.argouml.uml.diagram.ui.FigRole"))
			return "org.argouml.uml.diagram.ui.FigAssociation";

		if (innerClass.contains("org.argouml.cognitive.checklist.ui.TableModelChecklist"))
			return "org.argouml.cognitive.checklist.ui.TabChecklist";

		if (innerClass.contains("org.argouml.uml.diagram.collaboration.ui.FigMessageGroup"))
			return "org.argouml.uml.diagram.collaboration.ui.FigAssociationRole";

		if (innerClass.contains("org.argouml.util.TokenSep"))
			return "org.argouml.util.MyTokenizer";

		if (innerClass.contains("org.argouml.uml.ui.behavior.common_behavior.UMLLinkAssociationComboBoxModel")
				|| innerClass.contains("org.argouml.uml.ui.behavior.common_behavior.ActionSetLinkAssociation"))
			return "org.argouml.uml.ui.behavior.common_behavior.PropPanelLink";

		if (innerClass.contains("org.argouml.cognitive.checklist.ui.TableModelChecklist"))
			return "org.argouml.cognitive.checklist.ui.TabChecklist";

		if (innerClass.contains("org.argouml.uml.ui.foundation.core.UMLNodeDeployedComponentListModel"))
			return "org.argouml.uml.ui.foundation.core.PropPanelNode";

		return null;
	}

	public static double getAvgPrecision(Map<String, Map<String, List<Double>>> result) {
		return getAvg(result, 0);
	}

	public static double getAvgRecall(Map<String, Map<String, List<Double>>> result) {
		return getAvg(result, 1);
	}

	public static double getAvgF1(Map<String, Map<String, List<Double>>> result) {
		return getAvg(result, 2);
	}

	private static double getAvg(Map<String, Map<String, List<Double>>> result, int index) {
		double avg = 0;
		double numFeat = 0;
		for (String scenario : result.keySet()) {
			Map<String, List<Double>> scenarioFeatures = result.get(scenario);
			for (String feature : scenarioFeatures.keySet()) {
				List<Double> metrics = scenarioFeatures.get(feature);
				if (metrics != null && !metrics.isEmpty()) {
					if (!metrics.get(0).isNaN()) {
						avg += metrics.get(index);
						numFeat++;
					}
				}
			}
		}
		return avg / numFeat;
	}

}
