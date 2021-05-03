package dynamicfl.gabrielaExec;

import java.io.File;
import java.util.List;
import java.util.Map;

import dynamicfl.DynamicFL2BenchResults;
import fk.stardust.localizer.IFaultLocalizer;
import fk.stardust.localizer.sbfl.Ochiai;

public class MainSpectrum {

	// Dataset from https://zenodo.org/record/4262529
	final static String PATH_DATASET_EXECUTIONS = "C:/Users/106836/Downloads/Dataset/Dataset/ArgoUML/VariantsSourceCodeComparison/manual/variants";

	final static String PATH_ARGOUMLSPL_BENCHMARK = "C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark";

	final static boolean ONLY_ORIGINAL_SCENARIO = true;

	public static void main(String[] args) {

		try {

			File output = new File("output/output_" + System.currentTimeMillis());

			// read data set
			Map<String, Map<String, List<Integer>>> featExec = GabrielaDatasetReader
					.getFeatExec(PATH_DATASET_EXECUTIONS);

			// get sbl results
			IFaultLocalizer<String> algo = new Ochiai<String>();
			double threshold_sbfl = 0.5;
			Map<String, Map<String, List<Integer>>> results = SpectrumBasedLocalization.locate(featExec, algo, threshold_sbfl,
					output);

			// compute metrics
			// scenario, feature, (precision, recall, f1, classPrecision, classRecall,
			// classF1)
			Map<String, Map<String, List<Double>>> result = DynamicFL2BenchResults.compute(PATH_ARGOUMLSPL_BENCHMARK,
					results, output, ONLY_ORIGINAL_SCENARIO);

			System.out.println("\n6 Diagram features");
			double avgPrecision = DynamicFL2BenchResults.getAvgPrecision(result);
			double avgRecall = DynamicFL2BenchResults.getAvgRecall(result);
			double avgF1 = DynamicFL2BenchResults.getAvgF1(result);
			System.out.println("Avg. Precision:\t" + avgPrecision);
			System.out.println("Avg. Recall:\t" + avgRecall);
			System.out.println("Avg. F1:\t" + avgF1);

			System.out.println("\nResults from SPLC 2020");
			System.out.println("2020 Precision:\t0.068333333");
			System.out.println("2020 Recall:\t0.318333333");
			System.out.println("2020 F1:\t0.105");

			// Results from 2020 solution
//			ActivityDiagram	0.05	0.24	0.08
//			CollaborationDiagram	0.04	0.19	0.06
//			DeploymentDiagram	0.04	0.45	0.07
//			SequenceDiagram	0.12	0.25	0.16
//			StateDiagram	0.08	0.31	0.13
//			UseCaseDiagram 	0.08	0.47	0.13
//			AVG:	0.068333333	0.318333333	0.105

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
