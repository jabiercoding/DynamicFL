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
import utils.FileUtils;

public class MainSpectrum {

	private static final double THRESHOLD_SBFL = 0.5;
	
	// Dataset from https://zenodo.org/record/4262529
	final static String pathToExecutions = "C:/Users/106836/Downloads/Dataset/Dataset/ArgoUML/VariantsSourceCodeComparison/manual/variants";

	final static String pathToArgoUMLBenchmark = "C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark";
	
	public static void main(String[] args) {

		try {

			File executions = new File(pathToExecutions);
			if (!executions.exists()) {
				System.err.println(
						"The folder does not exist: Dataset ArgoUML VariantsSourceCodeComparison manual variants");
			}
			Map<String, Map<String, List<Integer>>> featExec = getFeatExec(executions);

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
				ranking.save(feature + "_resulting-ranking.txt");

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
			DynamicFL2BenchResults.compute(pathToArgoUMLBenchmark, results);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param executions file, with .config folders per feature
	 * @return a map of features with, per each class, the lines that were exercised
	 */
	public static Map<String, Map<String, List<Integer>>> getFeatExec(File executions) {

		Map<String, Map<String, List<Integer>>> result = new LinkedHashMap<String, Map<String, List<Integer>>>();

		// Each feature exercise
		for (File featFolder : executions.listFiles()) {
			if (featFolder.getName().endsWith(".config")) {
				String featName = featFolder.getName().substring(0, featFolder.getName().length() - ".config".length());
				System.out.println("\n--" + featName);

				Map<String, List<Integer>> classResult = new LinkedHashMap<String, List<Integer>>();

				// Each class file
				for (File classRuntime : featFolder.listFiles()) {
					if (classRuntime.isFile() && classRuntime.getName().endsWith(".runtime")) {
						String className = classRuntime.getName().substring(0,
								classRuntime.getName().length() - ".runtime".length());
						System.out.println(className);

						List<Integer> lineResult = new ArrayList<Integer>();

						// Each executed line in this class
						int i = 0;
						for (String line : FileUtils.getLinesOfFile(classRuntime)) {
							// ignore first line, it is the file path
							if (i != 0) {
								System.out.println(line);
								int lineNumber = Integer.parseInt(line);
								lineResult.add(lineNumber);
							}
							i++;
						}

						classResult.put(className, lineResult);
					}
				}

				result.put(featName, classResult);
			}
		}

		return result;
	}
}
