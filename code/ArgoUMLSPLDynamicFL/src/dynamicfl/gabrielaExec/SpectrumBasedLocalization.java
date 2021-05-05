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
import groundTruthExtractor.GroundTruthExtractor;

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

		// TODO test more the 2wise expansion
		// featExec = globalExpandWith2Wise(featExec);

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
					File featRankingFile = new File(output, localizer.getName() + "/" + feature + "_ranking.txt");
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
				if (featResults.isEmpty()) {
					results.remove(feature);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return results;
	}

	public static Map<String, Map<String, List<Integer>>> globalExpandWith2Wise(
			Map<String, Map<String, List<Integer>>> featExec) {
		Map<String, Map<String, List<Integer>>> expandedResult = new LinkedHashMap<String, Map<String, List<Integer>>>();
		int i = 0;
		for (String feature : featExec.keySet()) {
			i++;
			// put the single features
			expandedResult.put(feature, featExec.get(feature));
			// intersections
			int j = 0;
			for (String feature2 : featExec.keySet()) {
				j++;
				if (j > i) {
					Map<String, List<Integer>> pairF1F2content = new LinkedHashMap<String, List<Integer>>();
					Map<String, List<Integer>> feat1 = featExec.get(feature);
					Map<String, List<Integer>> feat2 = featExec.get(feature2);
					for (String classFeat1 : feat1.keySet()) {
						List<Integer> feat2lines = feat2.get(classFeat1);
						if (feat2lines != null) {
							// they had the same class
							List<Integer> feat1lines = feat1.get(classFeat1);
							// get the intersection of both lists
							List<Integer> intersection = new ArrayList<Integer>();
							for (Integer line : feat1lines) {
								if (feat2lines.contains(line)) {
									intersection.add(line);
								}
							}
							if (!intersection.isEmpty()) {
								pairF1F2content.put(classFeat1, intersection);
							}
						}
					}
					if (!pairF1F2content.isEmpty()) {
						expandedResult.put(feature + GroundTruthExtractor.AND_FEATURES + feature2, pairF1F2content);
					}
				}
			}
		}
		return expandedResult;
	}

}
