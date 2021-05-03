package dynamicfl.gabrielaExec;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dynamicfl.DynamicFL2BenchResults;
import fk.stardust.localizer.IFaultLocalizer;
import fk.stardust.localizer.Ranking;
import fk.stardust.localizer.sbfl.Jaccard;
import fk.stardust.localizer.sbfl.Ochiai;
import fk.stardust.localizer.sbfl.Ochiai2;
import fk.stardust.traces.INode;
import fk.stardust.traces.ISpectra;

public class MainSpectrum {

	private static final double THRESHOLD_SBFL = 0.5;

	// Dataset from https://zenodo.org/record/4262529
	final static String PATH_DATASET_EXECUTIONS = "C:/Users/106836/Downloads/Dataset/Dataset/ArgoUML/VariantsSourceCodeComparison/manual/variants";

	final static String PATH_ARGOUMLSPL_BENCHMARK = "C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark";

	final static boolean ONLY_ORIGINAL_SCENARIO = true;

	public static void main(String[] args) {

		try {

			File output = new File("output/output_" + System.currentTimeMillis());

			Map<String, Map<String, List<Integer>>> featExec = GabrielaDatasetReader
					.getFeatExec(PATH_DATASET_EXECUTIONS);

			Map<String, Map<String, List<Integer>>> results = new LinkedHashMap<String, Map<String, List<Integer>>>();

			// Using the Stardust SBFL library
			// get the results of a Spectrum-based algo, per pair class,line
			for (String feature : featExec.keySet()) {
				// prepare results
				Map<String, List<Integer>> featResults = new LinkedHashMap<String, List<Integer>>();
				results.put(feature, featResults);

				// Get the ranking from the SBFL technique
				MySpectraProvider provider = new MySpectraProvider(feature, featExec);
				ISpectra<String> spectra = provider.loadSpectra();
				IFaultLocalizer<String> localizer = new Ochiai<String>();
				Ranking<String> ranking = localizer.localize(spectra);
				File featRankingFile = new File(output, localizer.getName() + "/" + feature + ".txt");
				featRankingFile.getParentFile().mkdirs();
				ranking.save(featRankingFile.getAbsolutePath());

				// Add to the results if greater or equal to the threshold
				System.out.println("\nFeature: " + feature + "\n");
				Iterator<INode<String>> i = ranking.iterator();
				while (i.hasNext()) {
					INode<String> node = i.next();
					double suspiciousness = ranking.getSuspiciousness(node);
					if (suspiciousness >= THRESHOLD_SBFL) {
						System.out.println(node.getIdentifier() + " " + suspiciousness);
						String className = node.getIdentifier().split(";")[0];
						Integer line = Integer.parseInt(node.getIdentifier().split(";")[1]);

						List<Integer> lines = featResults.get(className);
						if (lines == null) {
							lines = new ArrayList<Integer>();
						}
						lines.add(line);
						featResults.put(className, lines);
					}
				}
			}

			// Compute results
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
