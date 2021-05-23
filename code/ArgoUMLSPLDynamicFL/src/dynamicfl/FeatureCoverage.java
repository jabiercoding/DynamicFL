package dynamicfl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * FeatureCoverage aims to get the number of lines executed for each feature
 * that belongs solely to the feature (and not BASE) to get the knowledge of how
 * the traces from exercising features are affecting the results, and if the
 * technique could be improved for higher recall
 */
public class FeatureCoverage {

	/**
	 * Computes the ratio of lines of a feature and lines executed
	 * 
	 * @param linesExecFeature
	 * @param feature
	 * @param path
	 *            to the ground truth variant of each feature
	 * @param path
	 *            to the ground truth original variant Key set is the absolute path
	 *            to each Java file
	 * @throws IOException
	 */
	public static void ratioLinesFeatureLinesExecuted(Map<String, List<Integer>> linesExecFeature, String feature,
			String pathToLineLevelGroundTruth, String pathToOriginalVariant) throws IOException {

		File variantFeatureGT = new File(pathToLineLevelGroundTruth, feature.toUpperCase() + ".1");

		File variantBASE = new File(pathToLineLevelGroundTruth, "BASE.1");

		Map<File, List<String>> filesRetrieved = new HashMap<>();
		Map<File, List<String>> filesFeatureVariant = new HashMap<>();
		Map<File, List<String>> filesBASEVariant = new HashMap<>();

		LinkedList<File> filesFeatureVariantList = new LinkedList<>();
		LineTraces2LineComparison.getFilesToProcess(variantFeatureGT, filesFeatureVariantList);
		LinkedList<File> filesBASEVariantList = new LinkedList<>();
		LineTraces2LineComparison.getFilesToProcess(variantBASE, filesBASEVariantList);

		// for each class of the results
		for (String javaClass : linesExecFeature.keySet()) {

			File retrievedFile = new File(javaClass);
			List<String> linesRetrieved = new ArrayList<>();
			List<String> bufferRetrievedFile = new ArrayList<>();

			List<Integer> lines = linesExecFeature.get(javaClass);
			Collections.sort(lines);

			// buffer to read the lines of the original variant used to exercise the feature
			BufferedReader br = new BufferedReader(new FileReader(retrievedFile.getAbsoluteFile()));
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				bufferRetrievedFile.add(sCurrentLine);
			}
			br.close();

			// add to the list of retrieved lines the single lines executed when exercised
			// the feature
			for (int line : lines) {
				sCurrentLine = bufferRetrievedFile.get(line - 1).trim().replaceAll("\t", "").replaceAll("\r", "")
						.replaceAll(" ", "");
				if (!sCurrentLine.equals("") && !sCurrentLine.startsWith("//") && !sCurrentLine.startsWith("/*")
						&& !sCurrentLine.startsWith("*/") && !sCurrentLine.startsWith("*")
						&& !sCurrentLine.startsWith("import") && !sCurrentLine.startsWith("package") && !sCurrentLine.equals("}")
						&& !sCurrentLine.equals("{")) {
					linesRetrieved.add(sCurrentLine);
				}
			}
			filesRetrieved.put(retrievedFile, linesRetrieved);
		}

		// add lines of files existing in a feature variant (contains BASE code)
		List<String> linesFeature = new ArrayList<>();
		for (File f : filesFeatureVariantList) {
			if (!f.isDirectory()) {
				File filenew = new File(String.valueOf(f.toPath()));
				linesFeature = new ArrayList<>();
				BufferedReader br = new BufferedReader(new FileReader(filenew.getAbsoluteFile()));
				String sCurrentLine;
				while ((sCurrentLine = br.readLine()) != null) {
					sCurrentLine = sCurrentLine.trim().replaceAll("\t", "").replaceAll("\r", "").replaceAll(" ", "");
					if (!sCurrentLine.equals("") && !sCurrentLine.startsWith("//") && !sCurrentLine.startsWith("/*")
							&& !sCurrentLine.startsWith("*/") && !sCurrentLine.startsWith("*")
							&& !sCurrentLine.startsWith("import") && !sCurrentLine.startsWith("package") && !sCurrentLine.equals("}")
							&& !sCurrentLine.equals("{")) {
						linesFeature.add(sCurrentLine);
					}
				}
				br.close();
				filesFeatureVariant.put(f, linesFeature);
			}
		}

		// add lines of files existing in a variant containing only the BASE code
		List<String> linesBASE = new ArrayList<>();
		for (File f : filesBASEVariantList) {
			if (!f.isDirectory()) {
				File filenew = new File(String.valueOf(f.toPath()));
				linesBASE = new ArrayList<>();
				BufferedReader br = new BufferedReader(new FileReader(filenew.getAbsoluteFile()));
				String sCurrentLine;
				while ((sCurrentLine = br.readLine()) != null) {
					sCurrentLine = sCurrentLine.trim().replaceAll("\t", "").replaceAll("\r", "").replaceAll(" ", "");
					if (!sCurrentLine.equals("") && !sCurrentLine.startsWith("//") && !sCurrentLine.startsWith("/*")
							&& !sCurrentLine.startsWith("*/") && !sCurrentLine.startsWith("*")
							&& !sCurrentLine.startsWith("import") && !sCurrentLine.startsWith("package") && !sCurrentLine.equals("}")
							&& !sCurrentLine.equals("{")) {
						linesBASE.add(sCurrentLine);
					}
				}
				br.close();
				filesBASEVariant.put(f, linesBASE);
			}
		}

		intersectionVariants(filesBASEVariant, filesFeatureVariant, filesRetrieved);
	}

	/**
	 * compare first the feature variant with BASE variant to get the lines that are
	 * solely to a feature, i.e., the lines remaining in the feature variant from
	 * the intersection with the BASE variant compare then, to get the lines that
	 * are in common, the remaining lines from previous intersection containing the
	 * ground truth feature variant with the variant containing the lines of the
	 * feature exercised
	 * 
	 * @param filesVariantCompare
	 *            from the BASE code without any feature
	 * @param filesFeatureVariant
	 *            from the feature code containing BASE
	 * @param filesRetrieved
	 *            from the runtime monitoring
	 * @throws IOException
	 */
	public static void intersectionVariants(Map<File, List<String>> filesBASEVariant,
			Map<File, List<String>> filesFeatureVariant, Map<File, List<String>> filesRetrieved) throws IOException {

		Integer linesIntersection = 0, totalLinesFeature = 0;

		// files in common in filesVariantCompare and in featureVariant
		for (Entry<File, List<String>> f : filesFeatureVariant.entrySet()) {
			List<String> original = f.getValue();
			List<String> revised = new ArrayList<>();

			// compare lines of files
			for (Entry<File, List<String>> fBase : filesBASEVariant.entrySet()) {
				if (f.getKey().getPath().toString()
						.substring(f.getKey().toPath().toString().indexOf("org" + File.separator) + 4)
						.equals(fBase.getKey().toPath().toString()
								.substring(fBase.getKey().toPath().toString().indexOf("org" + File.separator) + 4))) {
					revised = fBase.getValue();

					// remove from file of the feature variant all the lines of BASE
					original.removeAll(revised);
					break;
				}
			}

			totalLinesFeature += original.size();

			if (original.size() > 0) {// otherwise there are no lines of this file that belongs solely to this
				// feature
				for (Entry<File, List<String>> fRetrieved : filesRetrieved.entrySet()) {
					if (f.getKey().getPath().toString()
							.substring(f.getKey().toPath().toString().indexOf("org" + File.separator) + 4)
							.equals(fRetrieved.getKey().toPath().toString().substring(
									fRetrieved.getKey().toPath().toString().indexOf("org" + File.separator) + 4))) {
						revised = fRetrieved.getValue();

						// remove from the array of solely lines of the feature the lines in common from
						// the array of runtime traces
						int originalBeforeIntersectionRevised = original.size();
						original.removeAll(revised);
						int linesInCommonAux = originalBeforeIntersectionRevised - original.size();
						linesIntersection += linesInCommonAux;
						break;
					}
				}
			}
		}
		System.out.println("Ratio: " + (linesIntersection * 100) / totalLinesFeature + "%. Total Lines of the Feature: "
				+ totalLinesFeature + ". Lines in common with the runtime traces: " + linesIntersection + ".");
	}
}
