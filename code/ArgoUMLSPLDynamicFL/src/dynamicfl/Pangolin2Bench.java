package dynamicfl;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import metricsCalculation.MetricsCalculation;
import utils.FeatureUtils;
import utils.FileUtils;

public class Pangolin2Bench {

	// less or equal will be ignored
	public static final double THRESHOLD_PANGOLIN_SCORE = 0.5;

	public static void main(String[] args) {

		File argoUMLSPLBenchmark = new File("C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark");

		// We assume that the ArgoUMLSPLBenchmark is in a project in the workspace and
		// the Original scenario was created and used for creating the Pangolin traces
		File argoUMLsrc = new File(argoUMLSPLBenchmark,
				"scenarios/ScenarioOriginalVariant/variants/Original.config/src");
		if (!argoUMLsrc.exists()) {
			System.out.println("Scenario has to be created first");
			return;
		}
		
		// Scenario path
		String scenarioPath = "C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark/scenarios/ScenarioTraditionalVariants";
		String featuresInfoPath = "C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark/featuresInfo/features.txt";
		FeatureUtils fUtils = new FeatureUtils(scenarioPath, featuresInfoPath);
		
		for (String currentFeature : fUtils.getFeatureIds()) {
			
			System.out.println("\n" + currentFeature);

			// Parse pangolin results file for a given feature
			// TODO Pangolin results should use feature IDs. Then remove the replaceAll here
			File featurePangolinResultsFile = new File("resultsPangolin2/" + currentFeature.replaceAll("DIAGRAM", "") + "_ADD_ELEMENTS.csv");
			if(!featurePangolinResultsFile.exists()) {
				System.out.println("Skipping "+ currentFeature + ": " + featurePangolinResultsFile.getAbsolutePath() + " does not exist.");
				continue;
			}
			
			// Ground truth for the given feature
			File featureGroundTruthFile = new File(argoUMLSPLBenchmark, "groundTruth/" + currentFeature + ".txt");

			Map<String, List<Integer>> classAndLines = parsePangolinCSV(featurePangolinResultsFile, argoUMLsrc);

			List<String> results = LineTraces2Bench.getResultsInBenchmarkFormat(classAndLines, currentFeature, fUtils);

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
			groundTruth = convertBenchTracesToClassLevel(groundTruth);
			results = convertBenchTracesToClassLevel(results);
			precision = MetricsCalculation.getPrecision(groundTruth, results);
			recall = MetricsCalculation.getRecall(groundTruth, results);
			f1 = MetricsCalculation.getF1(precision, recall);
			System.out.println("Precision: " + precision);
			System.out.println("Recall: " + recall);
			System.out.println("F1: " + f1);
		}
	}

	/**
	 * Parse Pangolin csv
	 * 
	 * @param pangolinResultsFile
	 * @param srcFolder
	 * @return
	 */
	public static Map<String, List<Integer>> parsePangolinCSV(File pangolinResultsFile, File srcFolder) {
		Map<String, List<Integer>> classAndLines = new LinkedHashMap<String, List<Integer>>();

		for (String line : FileUtils.getLinesOfFile(pangolinResultsFile)) {

			String[] split = line.split(",");
			String scoreString = split[split.length - 1];
			// ignore header
			if (scoreString.equalsIgnoreCase("Score")) {
				continue;
			}
			double score = Double.parseDouble(scoreString);
			// ignore low scores
			if (score <= THRESHOLD_PANGOLIN_SCORE) {
				continue;
			}

			String packageName = split[0];
			String packagePath = packageName.replaceAll("\\.", "\\\\");
			File packageFile = new File(srcFolder, packagePath);
			// ignore source code that it is not part of ArgoUML src (e.g., libraries)
			if (!packageFile.exists()) {
				continue;
			}

			String className = split[1];
			File classFile = new File(packageFile, className + ".java");

			// ignore inner classes
			if (!classFile.exists()) {
				continue;
			}

			List<Integer> codeLines = classAndLines.get(classFile.getAbsolutePath());
			if (codeLines == null) {
				codeLines = new ArrayList<Integer>();
			}
			int codeLine = Integer.parseInt(split[split.length - 2]);
			codeLines.add(codeLine);
			classAndLines.put(classFile.getAbsolutePath(), codeLines);
		}
		return classAndLines;
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
