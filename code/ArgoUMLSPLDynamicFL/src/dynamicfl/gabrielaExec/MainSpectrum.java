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
import fk.stardust.localizer.sbfl.Ochiai2;
import fk.stardust.traces.INode;
import fk.stardust.traces.ISpectra;

public class MainSpectrum {

	private static final double THRESHOLD_SBFL = 0.5;
	
	// Dataset from https://zenodo.org/record/4262529
	final static String PATH_DATASET_EXECUTIONS = "C:/Users/106836/Downloads/Dataset/Dataset/ArgoUML/VariantsSourceCodeComparison/manual/variants";

	final static String PATH_ARGOUMLSPL_BENCHMARK = "C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark";
	
	public static void main(String[] args) {
		
		try {
			
			File output = new File("output/output_" + System.currentTimeMillis());
			
			Map<String, Map<String, List<Integer>>> featExec = GabrielaDatasetReader.getFeatExec(PATH_DATASET_EXECUTIONS);

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
				IFaultLocalizer<String> localizer = new Jaccard<String>();
				Ranking<String> ranking = localizer.localize(spectra);
				File featRankingFile = new File(output, localizer.getName() + "/" + feature + ".txt");
				featRankingFile.getParentFile().mkdirs();
				ranking.save(featRankingFile.getAbsolutePath());

				// Add to the results if greater or equal to the threshold
				System.out.println("\nFeature: " + feature + "\n");
				Iterator<INode<String>> i = ranking.iterator();
				while(i.hasNext()) {
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
			DynamicFL2BenchResults.compute(PATH_ARGOUMLSPL_BENCHMARK, results, output);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
