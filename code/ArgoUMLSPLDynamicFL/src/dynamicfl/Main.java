package dynamicfl;

import java.io.File;
import java.util.List;
import java.util.Map;

import dynamicfl.datasetReader.DatasetReader;
import dynamicfl.gridSearch.Configuration;
import dynamicfl.gridSearch.GridSearch;
import dynamicfl.metrics.MetricsModule;
import dynamicfl.metrics.scores.ScoreTriplet;
import dynamicfl.metrics.scores.ScoresBuilder;
import dynamicfl.spectrumbl.SpectrumBasedLocalization;

public class Main {

	// Dataset from https://zenodo.org/record/4262529
	// C:\Users\gabil\Downloads\Dataset\Dataset
	// C:/Users/106836/Downloads/Dataset/Dataset
	// /Users/brunomachado/DocumentsOffline/DynamicFL4/Dataset
	public final static String PATH_DATASET = "C:\\Users\\gabil\\Downloads\\Dataset\\Dataset";

	// https://github.com/but4reuse/argouml-spl-benchmark
	// C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark
	// C:\\Users\\gabil\\eclipse-workspace\\ArgoUMLSPLBenchmark
	// /Users/brunomachado/DocumentsOffline/DynamicFL4/ArgoUMLSPLBenchmark
	public final static String PATH_ARGOUMLSPL_BENCHMARK = "C:\\Users\\gabil\\eclipse-workspace\\ArgoUMLSPLBenchmark";

	// for running the comparisons with the runtime monitoring by unit tests use:
	// ArgoUML\VariantsSourceCodeComparison\tests\variants\
	public final static String PATH_DATASET_EXECUTIONS = PATH_DATASET + File.separator
			+ "ArgoUML" + File.separator + "BenchmarkTracesComparison" + File.separator + "tests" + File.separator + "variants";
			//+ "ArgoUML" + File.separator + "VariantsSourceCodeComparison" + File.separator + "manual" + File.separator + "variants";
	public final static String PATH_DATASET_METHOD_LEVEL_GROUND_TRUTH = PATH_DATASET + File.separator
			//+ "ArgoUML" + File.separator + "VariantsSourceCodeComparison" + File.separator + "manual" + File.separator + "results" + File.separator + "MethodComparison" + File.separator + "groundTruthMethods";
			+ "ArgoUML" + File.separator + "VariantsSourceCodeComparison" + File.separator + "tests" + File.separator + "results" + File.separator + "MethodComparison" + File.separator + "groundTruthMethods";
	// for running the comparisons with the runtime monitoring by unit tests use:
	// ArgoUML\VariantsSourceCodeComparison\tests\results\groundTruthVariants
	public final static String PATH_DATASET_LINE_LEVEL_GROUND_TRUTH = PATH_DATASET + File.separator
			//+ "ArgoUML" + File.separator + "VariantsSourceCodeComparison" + File.separator + "manual" + File.separator + "results" + File.separator + "groundTruthVariants";
			+ "ArgoUML" + File.separator + "VariantsSourceCodeComparison" + File.separator + "tests" + File.separator + "results" + File.separator + "groundTruthVariants";

	public final static boolean ONLY_ORIGINAL_SCENARIO = true;

	// e.g., michelon et al. 2021 execution dataset and groundtruth contains
	// "org.omg" source code but they are not considered by the benchmark
	// see GroundTruthExtractor.getAllArgoUMLSPLRelevantJavaFiles
	// Use IGNORE=false for a fair comparison with Michelon et al. 2021 and false
	// otherwise
	public final static boolean IGNORE_NOT_ARGOUML_TRACES = false;

	public static void main(String[] args) {

		try {

			File mainOutput = new File("output/output_" + System.currentTimeMillis());
			
			// read data set
			Map<String, Map<String, List<Integer>>> featExec = DatasetReader
					.getFeatExec(PATH_DATASET_EXECUTIONS, IGNORE_NOT_ARGOUML_TRACES);
			
			MetricsModule.setDefaultGranularities();

			ScoresBuilder scores = new ScoresBuilder();
			// get sbl results
			GridSearch gridSearch = new GridSearch();
			for (Configuration conf : gridSearch) {
				
				long start = System.currentTimeMillis();
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
				Map<String, Map<String, List<Double>>> result = MetricsModule.compute(
						PATH_ARGOUMLSPL_BENCHMARK, PATH_DATASET_METHOD_LEVEL_GROUND_TRUTH,
						PATH_DATASET_LINE_LEVEL_GROUND_TRUTH, PATH_DATASET_EXECUTIONS, results, output,
						ONLY_ORIGINAL_SCENARIO);

				System.out.println("\n6 Diagram features");
				ScoreTriplet avgScore = MetricsModule.getAvgScore(result);

				scores.add(conf.algo.getName(), Double.toString(conf.threshold_sbfl), avgScore);
			}

			// Set the output file path
			scores.toCSV(mainOutput);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
