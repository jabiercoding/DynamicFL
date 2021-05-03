package dynamicfl.gabrielaExec;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fk.stardust.localizer.IFaultLocalizer;
import fk.stardust.localizer.Ranking;
import fk.stardust.traces.INode;
import fk.stardust.traces.ISpectra;

public class SpectrumBasedLocalization {

	/**
	 * Get the results of a Spectrum-based algo, per pair class,line
	 * 
	 * @param featExec
	 * @param output
	 * @return
	 */
	public static Map<String, Map<String, List<Integer>>> locate(Map<String, Map<String, List<Integer>>> featExec,
			IFaultLocalizer<String> algo, double threshold_sbfl, File output) {

		Map<String, Map<String, List<Integer>>> results = new LinkedHashMap<String, Map<String, List<Integer>>>();

		try {
			// Using the Stardust SBFL library
			for (String feature : featExec.keySet()) {
				// prepare results
				Map<String, List<Integer>> featResults = new LinkedHashMap<String, List<Integer>>();
				results.put(feature, featResults);

				// Get the ranking from the SBFL technique
				MySpectraProvider provider = new MySpectraProvider(feature, featExec);
				ISpectra<String> spectra = provider.loadSpectra();
				IFaultLocalizer<String> localizer = algo;
				Ranking<String> ranking = localizer.localize(spectra);

				if (output != null) {
					File featRankingFile = new File(output, localizer.getName() + "/" + feature + ".txt");
					featRankingFile.getParentFile().mkdirs();
					ranking.save(featRankingFile.getAbsolutePath());
				}

				// Add to the results if greater or equal to the threshold
				System.out.println("\nFeature: " + feature + "\n");
				Iterator<INode<String>> i = ranking.iterator();
				while (i.hasNext()) {
					INode<String> node = i.next();
					double suspiciousness = ranking.getSuspiciousness(node);
					if (suspiciousness >= threshold_sbfl) {
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
		} catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}

}
