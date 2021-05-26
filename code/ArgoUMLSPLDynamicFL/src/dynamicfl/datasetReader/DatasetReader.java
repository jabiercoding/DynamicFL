package dynamicfl.datasetReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import utils.FileUtils;

/**
 * Reads data from Gabriela Dataset https://zenodo.org/record/4262529
 */
public class DatasetReader {

	/**
	 * Get feat exec. It treats special cases such as .runtime files with "(" and
	 * inner classes
	 * 
	 * @param ignoreNotArgoumlTraces
	 * 
	 * @param executions             file, with .config folders per feature,
	 *                               something like:
	 *                               "C:/Downloads/Dataset/Dataset/ArgoUML/VariantsSourceCodeComparison/manual/variants";
	 * @return a map of features with, per each class, the lines that were exercised
	 */
	public static Map<String, Map<String, List<Integer>>> getFeatExec(String pathToExecutions,
			boolean ignoreNotArgoumlTraces) {
		System.out.println("ExecutionDatasetReader");
		File executions = new File(pathToExecutions);
		if (!executions.exists()) {
			System.err
					.println("The folder does not exist: Dataset ArgoUML VariantsSourceCodeComparison manual variants");
		}

		Map<String, Map<String, List<Integer>>> result = new LinkedHashMap<String, Map<String, List<Integer>>>();

		// Each feature exercise
		for (File featFolder : executions.listFiles()) {
			if (featFolder.getName().endsWith(".config")) {
				String featName = featFolder.getName().substring(0, featFolder.getName().length() - ".config".length());
				System.out.println("Loading execution dataset for " + featName);

				Map<String, List<Integer>> classResult = new LinkedHashMap<String, List<Integer>>();

				// Each class file
				for (File classRuntime : featFolder.listFiles()) {
					if (classRuntime.isFile() && classRuntime.getName().endsWith(".runtime")) {
						String className = classRuntime.getName().substring(0,
								classRuntime.getName().length() - ".runtime".length());

						// System.out.println(className);

						if (ignoreNotArgoumlTraces) {
							if (!className.startsWith("org.argouml")) {
								continue;
							}
						}

						// Cut the method signature from the end of the class name
						if (className.contains("(")) {
							className = className.substring(0, className.lastIndexOf("."));
						}

						List<Integer> lineResult = new ArrayList<Integer>();

						// Each executed line in this class
						boolean firstLine = true;
						for (String line : FileUtils.getLinesOfFile(classRuntime)) {
							// ignore first line, it is the file path
							if (!firstLine) {
								int lineNumber = Integer.parseInt(line);
								lineResult.add(lineNumber);
							}
							firstLine = false;
						}

						// check if it already exist, for the cases of "("
						List<Integer> previous = classResult.get(className);
						if (previous != null) {
							lineResult.addAll(previous);
						}
						Collections.sort(lineResult);
						classResult.put(className, lineResult);
						// System.out.println(lineResult);
					}
				}

				result.put(featName, classResult);
			}
		}

		return result;
	}

}
