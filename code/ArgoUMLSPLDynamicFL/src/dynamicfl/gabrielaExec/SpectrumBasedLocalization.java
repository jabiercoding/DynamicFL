package dynamicfl.gabrielaExec;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fk.stardust.localizer.IFaultLocalizer;
import fk.stardust.localizer.NormalizedRanking;
import fk.stardust.localizer.NormalizedRanking.NormalizationStrategy;
import fk.stardust.localizer.Ranking;
import fk.stardust.traces.INode;
import fk.stardust.traces.ISpectra;
import fk.stardust.traces.ITrace;
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

		System.out.println("\nSpectrum-Based Localization");
		Map<String, Map<String, List<Integer>>> results = new LinkedHashMap<String, Map<String, List<Integer>>>();

		try {
			// Using the Stardust SBFL library
			for (String feature : featExec.keySet()) {

				// TODO test more the 2wise expansion
				// no expansion
				Map<String, Map<String, List<Integer>>> featExec2 = featExec;
				// targeted expansion
				// Map<String, Map<String, List<Integer>>> featExec2 =
				// targetedExpandWith2Wise(feature, featExec);
				// global expansion
				// Map<String, Map<String, List<Integer>>> featExec2 =
				// globalExpandWith2Wise(featExec);

				// prepare results
				Map<String, List<Integer>> featResults = new LinkedHashMap<String, List<Integer>>();
				results.put(feature, featResults);

				// Get the ranking from the SBFL technique
				MySpectraProvider provider = new MySpectraProvider(feature, featExec2);
				ISpectra<String> spectra = provider.loadSpectra();
				// System.out.println(getCSV(spectra));
				IFaultLocalizer<String> localizer = algo;
				Ranking<String> ranking = localizer.localize(spectra);
				NormalizedRanking<String> normalizedRanking = new NormalizedRanking<String>(ranking,
						NormalizationStrategy.ZeroOne);

				if (output != null) {
					File featRankingFile = new File(output, localizer.getName() + "/" + feature + "_ranking.txt");
					File featNormRankingFile = new File(output,
							localizer.getName() + "/" + feature + "_normalized_ranking.txt");
					featRankingFile.getParentFile().mkdirs();
					ranking.save(featRankingFile.getAbsolutePath());
					saveNormalizedRanking(featNormRankingFile.getAbsolutePath(), normalizedRanking);
				}

				// Add to the results if greater or equal to the threshold
				System.out.println("Creating ranking for " + feature);
				Iterator<INode<String>> i = normalizedRanking.iterator();
				while (i.hasNext()) {
					INode<String> node = i.next();
					double suspiciousness = normalizedRanking.getSuspiciousness(node);
					if (suspiciousness >= threshold_sbfl) {
						// System.out.println(node.getIdentifier() + " " + suspiciousness);
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

	/**
	 * It will expand features [A B C] with [A B C A_and_B A_and_C B_and_C], for the
	 * interactions, it will be 1 when it appears in both and 0 otherwise
	 * 
	 * @param featExec
	 * @return the expanded list
	 */
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

	/**
	 * For feature A, it will expand features [A B C] with [A B C A_and_B A_and_C],
	 * for the interactions, it will be 1 when it appears in both and 0 otherwise
	 * 
	 * @param featExec
	 * @return the expanded list
	 */
	public static Map<String, Map<String, List<Integer>>> targetedExpandWith2Wise(String targetFeature,
			Map<String, Map<String, List<Integer>>> featExec) {
		Map<String, Map<String, List<Integer>>> expandedResult = new LinkedHashMap<String, Map<String, List<Integer>>>();
		for (String feature : featExec.keySet()) {
			// put the single features
			expandedResult.put(feature, featExec.get(feature));
		}

		for (String feature2 : featExec.keySet()) {
			if (!targetFeature.equals(feature2)) {
				Map<String, List<Integer>> pairF1F2content = new LinkedHashMap<String, List<Integer>>();
				Map<String, List<Integer>> feat1 = featExec.get(targetFeature);
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
					expandedResult.put(targetFeature + GroundTruthExtractor.AND_FEATURES + feature2, pairF1F2content);
				}
			}
		}
		return expandedResult;
	}

	/**
	 * get a stringbuffer of the spectra
	 * 
	 * @param spectra
	 * @return stringbuffer
	 */
	public static StringBuffer getCSV(ISpectra<String> spectra) {
		StringBuffer buffer = new StringBuffer();
		for (ITrace<String> trace : spectra.getTraces()) {
			buffer.append(",");
			// traces do not have identifier...
			buffer.append(trace);
		}
		buffer.append("\n");
		for (INode<String> node : spectra.getNodes()) {
			buffer.append(node.getIdentifier());
			for (ITrace<String> trace : spectra.getTraces()) {
				if (trace.isInvolved(node)) {
					buffer.append(",1");
				} else {
					buffer.append(",0");
				}
			}
			buffer.append("\n");
		}
		return buffer;
	}

	/**
	 * It saves the normalized ranking. Ranking.save() cannot be used because of
	 * this issue https://github.com/FaKeller/stardust/issues/2
	 * 
	 * @param filename
	 * @param ranking
	 * @throws IOException
	 */
	public static void saveNormalizedRanking(String filename, Ranking<String> ranking) throws IOException {
		FileWriter writer = null;
		try {
			writer = new FileWriter(filename);
			Iterator<INode<String>> i = ranking.iterator();
			while (i.hasNext()) {
				INode<String> el = i.next();
				writer.write(String.format("%s: %f\n", el.toString(), ranking.getSuspiciousness(el)));
			}
		} catch (final Exception e) {
			throw new RuntimeException("Saving the ranking failed.", e);
		} finally {
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		}
	}

}
