package dynamicfl.gabrielaExec;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import utils.FileUtils;

/**
 * Reads data from Gabriela Dataset https://zenodo.org/record/4262529
 * 
 * @author jabier.martinez
 */
public class GabrielaDatasetReader {

	/**
	 * Get feat exec
	 * 
	 * @param executions file, with .config folders per feature, something like:
	 *                   "C:/Downloads/Dataset/Dataset/ArgoUML/VariantsSourceCodeComparison/manual/variants";
	 * @return a map of features with, per each class, the lines that were exercised
	 */
	public static Map<String, Map<String, List<Integer>>> getFeatExec(String pathToExecutions) {

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
