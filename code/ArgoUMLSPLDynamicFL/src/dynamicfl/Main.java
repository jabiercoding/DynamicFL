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

/**
 * Main method. Pipeline to get the results in an output folder created in this
 * project. Press F5 once terminated. The user must change PATH_DATASET and
 * PATH_ARGOUMLSPL_BENCHMARK to the paths in the computer. MANUAL_OR_TESTS
 * should be changed to get the results of the manual execution traces or of the
 * testing execution traces
 */
public class Main {

	// Dataset. Inside this path, there must be an ArgoUML folder with a
	// VariantsSourceCodeComparison folder
	// C:\Users\gabil\Downloads\Dataset\Dataset
	// C:/Users/106836/Downloads/Dataset/Dataset
	// /Users/brunomachado/DocumentsOffline/DynamicFL4/Dataset
	public final static String PATH_DATASET = "C:/Users/106836/Downloads/ArgoUML";

	// https://github.com/but4reuse/argouml-spl-benchmark
	// C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark
	// C:\\Users\\gabil\\eclipse-workspace\\ArgoUMLSPLBenchmark
	// /Users/brunomachado/DocumentsOffline/DynamicFL4/ArgoUMLSPLBenchmark
	public final static String PATH_ARGOUMLSPL_BENCHMARK = "C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark";

	public final static String MANUAL_OR_TESTS = "tests"; // "manual" or "tests"

	public final static String PATH_DATASET_EXECUTIONS = PATH_DATASET + File.separator + "ArgoUML" + File.separator
			+ "VariantsSourceCodeComparison" + File.separator + MANUAL_OR_TESTS + File.separator + "variants";
	public final static String PATH_DATASET_METHOD_LEVEL_GROUND_TRUTH = PATH_DATASET + File.separator + "ArgoUML"
			+ File.separator + "VariantsSourceCodeComparison" + File.separator + MANUAL_OR_TESTS + File.separator
			+ "results" + File.separator + "MethodComparison" + File.separator + "groundTruthMethods";
	public final static String PATH_DATASET_LINE_LEVEL_GROUND_TRUTH = PATH_DATASET + File.separator + "ArgoUML"
			+ File.separator + "VariantsSourceCodeComparison" + File.separator + MANUAL_OR_TESTS + File.separator
			+ "results" + File.separator + "groundTruthVariants";

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
			Map<String, Map<String, List<Integer>>> featExec = DatasetReader.getFeatExec(PATH_DATASET_EXECUTIONS,
					IGNORE_NOT_ARGOUML_TRACES);

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
				Map<String, Map<String, List<Double>>> result = MetricsModule.compute(PATH_ARGOUMLSPL_BENCHMARK,
						PATH_DATASET_METHOD_LEVEL_GROUND_TRUTH, PATH_DATASET_LINE_LEVEL_GROUND_TRUTH,
						PATH_DATASET_EXECUTIONS, results, output, ONLY_ORIGINAL_SCENARIO);

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
