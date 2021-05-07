package dynamicfl.gabrielaExec;

import java.io.File;
import java.util.List;
import java.util.Map;

import dynamicfl.DynamicFL2BenchResults;
import dynamicfl.gabrielaExec.gridSearch.Configuration;
import dynamicfl.gabrielaExec.gridSearch.GridSearch;
import dynamicfl.gabrielaExec.gridSearch.ScoresBuilder;

public class MainSpectrum {

	// Dataset from https://zenodo.org/record/4262529
	// C:/Users/106836/Downloads/Dataset/Dataset/ArgoUML/VariantsSourceCodeComparison/manual/variants
	// C:\\Users\\gabil\\Downloads\\dataset\\dataset\\ArgoUML\\VariantsSourceCodeComparison\\manual\\variants
	final static String PATH_DATASET_EXECUTIONS = "C:/Users/106836/Downloads/Dataset/Dataset/ArgoUML/VariantsSourceCodeComparison/manual/variants";

	// C:\Users\106836\Downloads\Dataset\Dataset\ArgoUML\VariantsSourceCodeComparison\manual\results\MethodComparison\groundTruthMethods
	// C:\\Users\\gabil\\Downloads\\dataset\\dataset\\ArgoUML\\VariantsSourceCodeComparison\\manual\\results\\MethodComparison\\groundTruthMethods
	final static String PATH_METHOD_LEVEL_GROUND_TRUTH = "C:\\Users\\106836\\Downloads\\Dataset\\Dataset\\ArgoUML\\VariantsSourceCodeComparison\\manual\\results\\MethodComparison\\groundTruthMethods";

	// https://github.com/but4reuse/argouml-spl-benchmark
	// C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark
	// C:\\Users\\gabil\\eclipse-workspace\\ArgoUMLSPLBenchmark
	final static String PATH_ARGOUMLSPL_BENCHMARK = "C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark";

	final static boolean ONLY_ORIGINAL_SCENARIO = true;

	// e.g., michelon et al. 2021 execution dataset and groundtruth contains
	// "org.omg" source code but they are not considered by the benchmark
	// see GroundTruthExtractor.getAllArgoUMLSPLRelevantJavaFiles
	// Use IGNORE=false for a fair comparison with Michelon et al. 2021 and false
	// otherwise
	final static boolean IGNORE_NOT_ARGOUML_TRACES = false;

	public static void main(String[] args) {

		try {

			File mainOutput = new File("output/output_" + System.currentTimeMillis());

			// read data set
			Map<String, Map<String, List<Integer>>> featExec = GabrielaDatasetReader
					.getFeatExec(PATH_DATASET_EXECUTIONS, IGNORE_NOT_ARGOUML_TRACES);

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
						PATH_ARGOUMLSPL_BENCHMARK, PATH_METHOD_LEVEL_GROUND_TRUTH, results, output,
						ONLY_ORIGINAL_SCENARIO);

				System.out.println("\n6 Diagram features");
				double avgPrecision = DynamicFL2BenchResults.getAvgPrecision(result);
				double avgRecall = DynamicFL2BenchResults.getAvgRecall(result);
				double avgF1 = DynamicFL2BenchResults.getAvgF1(result);

				scores.add(conf.algo.getName(), Double.toString(conf.threshold_sbfl), "Precision", avgPrecision);
				scores.add(conf.algo.getName(), Double.toString(conf.threshold_sbfl), "Recall", avgRecall);
				scores.add(conf.algo.getName(), Double.toString(conf.threshold_sbfl), "F1", avgF1);

				System.out.println("Avg. Precision:\t" + avgPrecision);
				System.out.println("Avg. Recall:\t" + avgRecall);
				System.out.println("Avg. F1:\t" + avgF1);

				System.out.println("\nResults from VAMOS 2021");
				System.out.println("2021 Precision:\t0.068333333");
				System.out.println("2021 Recall:\t0.318333333");
				System.out.println("2021 F1:\t0.105");

				// Results from 2021 solution
				// ActivityDiagram 0.05 0.24 0.08
				// CollaborationDiagram 0.04 0.19 0.06
				// DeploymentDiagram 0.04 0.45 0.07
				// SequenceDiagram 0.12 0.25 0.16
				// StateDiagram 0.08 0.31 0.13
				// UseCaseDiagram 0.08 0.47 0.13
				// AVG: 0.068333333 0.318333333 0.105
			}

			// Set the output file path
			scores.toCSV(new File(mainOutput, "result.csv"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
