package dynamicfl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import metricsCalculation.MetricsCalculation;
import utils.FeatureUtils;
import utils.FileUtils;

public class DynamicFL2BenchResults {

	static Map<String, String> CACHE_TYPE_ABSPATH = new HashMap<String, String>();

	/**
	 * Compute results
	 * 
	 * @param pathToArgoUMLSPLBenchmark
	 *            e.g., "C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark"
	 * @return map of scenario, feature, (precision, recall, f1, classPrecision,
	 *         classRecall, classF1)
	 * @throws IOException
	 */
	public static Map<String, Map<String, List<Double>>> compute(String pathToArgoUMLSPLBenchmark,
			String pathToMethodLevelGroundTruth, String pathToLineLevelGroundTruth, String pathToOriginalVariant,
			Map<String, Map<String, List<Integer>>> featureClassAndLines, File output, boolean onlyScenarioOriginal)
			throws IOException {

		Map<String, Map<String, List<Double>>> result = new LinkedHashMap<String, Map<String, List<Double>>>();

		// Input path to the benchmark
		File argoUMLSPLBenchmark = new File(pathToArgoUMLSPLBenchmark);

		// Path to the src of the application used for exercising the features, which
		// differs from the benchmark variant only in imports that were used for runtime
		// monitoring with jacoco
		File pathToOriginalVariantDataset = new File(pathToOriginalVariant,
				"ACTIVITYDIAGRAM.config" + File.separator + "src");

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
							pathToOriginalVariantDataset, classAndLines);
					// originalArgoUMLsrc, classAndLines);

					List<String> results = LineTraces2BenchFormat.getResultsInBenchmarkFormat(classAndLines,
							currentFeature, fUtils, pathToOriginalVariantDataset, true);
					// currentFeature, fUtils, originalArgoUMLsrc, true);

					// Save to file benchmarkFormat results
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

					List<String> resultsMethodLevel = LineTraces2MethodComparison
							.getResultsInMethodComparison(absPathAndLines, currentFeature, fUtils, true);

					// Save to file method-level results
					StringBuffer buffer2 = new StringBuffer();
					for (String trace : resultsMethodLevel) {
						buffer2.append(trace);
						buffer2.append("\n");
					}
					File outputScenarioMethod = new File(output, scenario.getName() + "_method");
					outputScenarioMethod.mkdirs();
					File outputScenarioFeature2 = new File(outputScenarioMethod, currentFeature + ".txt");
					try {
						FileUtils.writeFile(outputScenarioFeature2, buffer2.toString());
					} catch (Exception e) {
						e.printStackTrace();
					}

					// Save to file line-level results
					File outputScenarioLine = new File(output, scenario.getName() + "_line");
					outputScenarioLine.mkdirs();

					LineTraces2LineComparison.getResultsInLineComparison(absPathAndLines, currentFeature,
							pathToLineLevelGroundTruth, pathToOriginalVariant, fUtils, outputScenarioLine, true);

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

						System.out.println("\nMethod level metrics");
						List<String> groundTruth3 = LineTraces2MethodComparison
								.readMethodsGroundTruth(pathToMethodLevelGroundTruth, currentFeature);
						double precision3 = MetricsCalculation.getPrecision(groundTruth3, resultsMethodLevel);
						double recall3 = MetricsCalculation.getRecall(groundTruth3, resultsMethodLevel);
						double f13 = MetricsCalculation.getF1(precision3, recall3);
						System.out.println("Precision: " + precision3);
						System.out.println("Recall: " + recall3);
						System.out.println("F1: " + f13);
						resultFeature.add(precision3);
						resultFeature.add(recall3);
						resultFeature.add(f13);

						System.out.println("\nLine level metrics");
						List<Double> metrics = MetricsCalculation.getCSVInformationPerFeature(outputScenarioLine,
								currentFeature);
						System.out.println("Precision: " + metrics.get(0));
						System.out.println("Recall: " + metrics.get(1));
						System.out.println("F1: " + metrics.get(2));
						resultFeature.addAll(metrics);
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
			FileUtils.writeFile(output,
					"Scenario;Feature;Precision;Recall;F1;ClassPrecision;ClassRecall;ClassF1;MethodPrecision;MethodRecall;MethodF1;LinePrecision;LineRecall;LineF1\n");
			for (String scenario : result.keySet()) {
				Map<String, List<Double>> scenarioFeatures = result.get(scenario);
				for (String feature : scenarioFeatures.keySet()) {
					List<Double> metrics = scenarioFeatures.get(feature);
					if (metrics != null && !metrics.isEmpty()) {
						String metricValues = "";
						for (Double v : metrics) {
							metricValues = metricValues + ";" + v;
						}
						FileUtils.appendToFile(output, scenario + ";" + feature + metricValues);
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
		for (String currentClass : classAndLines.keySet()) {

			// check of innerclasses
			String absPath = getAbsPathFromClass(originalArgoUMLsrc, currentClass);
			if (absPath == null) {
				// it does not exist
				System.err.println(currentClass + " does not exist");
				continue;
			}

			// in case of same files (i.e., inner classes)
			List<Integer> current = classAndLines.get(currentClass);
			List<Integer> previous = result.get(absPath);
			if (previous != null) {
				current.addAll(previous);
			}
			result.put(absPath, current);
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
	 * Get absolute path from a class, sometimes a file (compilation unit) has more
	 * than one type (class)
	 * 
	 * @param originalArgoUMLsrc
	 * 
	 * @param targetClass
	 * @return null or the absolute path of the file containing the class
	 */
	public static String getAbsPathFromClass(File originalArgoUMLsrc, String targetClass) {
		String absPath = targetClass.replace('.', File.separatorChar);
		absPath = absPath + ".java";

		File absFile = new File(originalArgoUMLsrc, absPath);
		if (absFile.exists()) {
			return absFile.getAbsolutePath();
		}

		// not found, check for inner classes in sibling classes
		// but check first in the cache
		String inCache = CACHE_TYPE_ABSPATH.get(targetClass);
		if (inCache != null) {
			return inCache;
		}

		for (File f : absFile.getParentFile().listFiles()) {
			if (!f.getName().endsWith(".java")) {
				continue;
			}
			String name = targetClass.substring(targetClass.lastIndexOf(".") + 1, targetClass.length());
			List<TypeDeclaration> types = getTypes(f);
			for (TypeDeclaration type : types) {
				String typeName = type.getName().toString();
				if (name.equals(typeName)) {
					// found
					CACHE_TYPE_ABSPATH.put(targetClass, f.getAbsolutePath());
					return f.getAbsolutePath();
				}
			}
		}
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

	/**
	 * Get all types
	 * 
	 * @param a
	 *            java file
	 * @return list of types, that can be more than one
	 */
	public static List<TypeDeclaration> getTypes(File f) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setBindingsRecovery(true);

		String source = FileUtils.getStringOfFile(f);
		parser.setSource(source.toCharArray());

		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		List<TypeDeclaration> types = new ArrayList<TypeDeclaration>();
		cu.accept(new ASTVisitor() {
			public boolean visit(TypeDeclaration node) {
				types.add(node);
				return true;
			}
		});
		return types;
	}

}
