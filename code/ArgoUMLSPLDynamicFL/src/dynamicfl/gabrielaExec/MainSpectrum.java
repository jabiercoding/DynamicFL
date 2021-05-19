package dynamicfl.gabrielaExec;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import dynamicfl.DynamicFL2BenchResults;
import dynamicfl.FeatureCoverage;
import dynamicfl.gabrielaExec.gridSearch.Configuration;
import dynamicfl.gabrielaExec.gridSearch.GridSearch;
import dynamicfl.gabrielaExec.gridSearch.ScoresBuilder;

public class MainSpectrum {

	// Dataset from https://zenodo.org/record/4262529
	// C:\Users\gabil\Downloads\Dataset\Dataset
	// C:/Users/106836/Downloads/Dataset
	// /Users/brunomachado/DocumentsOffline/DynamicFL4/Dataset
	final static String PATH_DATASET = "/Users/brunomachado/DocumentsOffline/DynamicFL4/Dataset";

	// https://github.com/but4reuse/argouml-spl-benchmark
	// C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark
	// C:\\Users\\gabil\\eclipse-workspace\\ArgoUMLSPLBenchmark
	// /Users/brunomachado/DocumentsOffline/DynamicFL4/ArgoUMLSPLBenchmark
	final static String PATH_ARGOUMLSPL_BENCHMARK = "/Users/brunomachado/DocumentsOffline/DynamicFL4/ArgoUMLSPLBenchmark";

	// for running the comparisons with the runtime monitoring by unit tests use:
	// ArgoUML\VariantsSourceCodeComparison\tests\variants\
	final static String PATH_DATASET_EXECUTIONS = PATH_DATASET + File.separator
			+ "ArgoUML" + File.separator + "VariantsSourceCodeComparison" + File.separator + "manual" + File.separator + "variants";
	final static String PATH_DATASET_METHOD_LEVEL_GROUND_TRUTH = PATH_DATASET + File.separator
			+ "ArgoUML" + File.separator + "VariantsSourceCodeComparison" + File.separator + "manual" + File.separator + "results" + File.separator + "MethodComparison" + File.separator + "groundTruthMethods";
	// for running the comparisons with the runtime monitoring by unit tests use:
	// ArgoUML\VariantsSourceCodeComparison\tests\results\groundTruthVariants
	final static String PATH_DATASET_LINE_LEVEL_GROUND_TRUTH = PATH_DATASET + File.separator
			+ "ArgoUML" + File.separator + "VariantsSourceCodeComparison" + File.separator + "manual" + File.separator + "results" + File.separator + "groundTruthVariants";

	final static boolean ONLY_ORIGINAL_SCENARIO = true;

	// e.g., michelon et al. 2021 execution dataset and groundtruth contains
	// "org.omg" source code but they are not considered by the benchmark
	// see GroundTruthExtractor.getAllArgoUMLSPLRelevantJavaFiles
	// Use IGNORE=false for a fair comparison with Michelon et al. 2021 and false
	// otherwise
	final static boolean IGNORE_NOT_ARGOUML_TRACES = true;

	public static void main(String[] args) {

		try {

			File mainOutput = new File("output/output_" + System.currentTimeMillis());
			
			System.out.println(PATH_DATASET_EXECUTIONS);
			System.out.println(PATH_DATASET_METHOD_LEVEL_GROUND_TRUTH);
			// read data set
			Map<String, Map<String, List<Integer>>> featExec = GabrielaDatasetReader
					.getFeatExec(PATH_DATASET_EXECUTIONS, IGNORE_NOT_ARGOUML_TRACES);

			// Ratio of lines of a feature with the lines executed
			for (Entry<String, Map<String, List<Integer>>> feat : featExec.entrySet()) {
				System.out.println("Feature: " + feat.getKey());
				File fileSrc = new File(
						PATH_DATASET_EXECUTIONS + File.separator + feat.getKey() + ".config" + File.separator + "src");
				Map<String, List<Integer>> result = DynamicFL2BenchResults.transformToAbsPathAndLines(feat.getKey(),
						fileSrc, feat.getValue());
				FeatureCoverage.ratioLinesFeatureLinesExecuted(result, feat.getKey(),
						PATH_DATASET_LINE_LEVEL_GROUND_TRUTH, PATH_DATASET_EXECUTIONS);
			}

			long start = System.currentTimeMillis();

			ScoresBuilder scores = new ScoresBuilder();
			// get sbl results
			GridSearch gridSearch = new GridSearch();
			for (Configuration conf : gridSearch) {
				File output = new File(mainOutput, conf.algo.getName() + "_" + conf.threshold_sbfl);
				System.out.println("\n CURRENT SBL ALGO: " + conf);
				Map<String, Map<String, List<Integer>>> results = SpectrumBasedLocalization.locate(featExec, conf.algo,
						conf.threshold_sbfl, output);

				long end = System.currentTimeMillis();
				long elapsedTime = end - start;
				System.out.println("\nTime in seconds for feature location: " + (elapsedTime / 1000.0));

				// compute metrics
				// scenario, feature, (precision, recall, f1, classPrecision, classRecall,
				// classF1, methodPrecision, methodRecall, methodF1)
				Map<String, Map<String, List<Double>>> result = DynamicFL2BenchResults.compute(
						PATH_ARGOUMLSPL_BENCHMARK, PATH_DATASET_METHOD_LEVEL_GROUND_TRUTH,
						PATH_DATASET_LINE_LEVEL_GROUND_TRUTH, PATH_DATASET_EXECUTIONS, results, output,
						ONLY_ORIGINAL_SCENARIO);

				System.out.println("\n6 Diagram features");
				double avgPrecision = DynamicFL2BenchResults.getAvgPrecision(result);
				double avgRecall = DynamicFL2BenchResults.getAvgRecall(result);
				double avgF1 = DynamicFL2BenchResults.getAvgF1(result);

				scores.add(conf.algo.getName(), Double.toString(conf.threshold_sbfl), avgPrecision, avgRecall, avgF1);
				
				System.out.println("Avg. Precision:\t" + avgPrecision);
				System.out.println("Avg. Recall:\t" + avgRecall);
				System.out.println("Avg. F1:\t" + avgF1);
			}

			// Set the output file path
			scores.toCSV(new File(mainOutput, "result.csv"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
