package dynamicfl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import utils.FeatureUtils;
import utils.FileUtils;
import utils.JDTUtils;
import utils.TraceIdUtils;

/**
 * LineTraces to Method aims to get the methods executed to compute metrics at
 * the method-level only e.g., when a line of a method is executed the method
 * signature is considered part of the feature
 */
public class LineTraces2MethodComparison {

	/**
	 * Create method signature string
	 * 
	 * @param scenarioPath
	 * @param feature
	 * 
	 * @param classAndLines.
	 *            Key set is the absolute path to each Java file
	 */
	public static List<String> getResultsInMethodComparison(Map<String, List<Integer>> classAbsPathAndLines,
			String feature, FeatureUtils fUtils, boolean crossVariantsCheck) {

		List<String> resultsMethods = new ArrayList<String>();
		
		// for each class of the results
		for(String javaClass: classAbsPathAndLines.keySet()) {
			// Prepare the parser
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setBindingsRecovery(true);

			String source = FileUtils.getStringOfFile(new File(javaClass));
			parser.setSource(source.toCharArray());
			CompilationUnit cu = (CompilationUnit) parser.createAST(null);
			List<MethodDeclaration> methods = LineTraces2BenchFormat.getMethods(cu);
			
			// for each line in this class
			List<Integer> lines = classAbsPathAndLines.get(javaClass);
			for (int line : lines) {
				// get the method of this line
				int position = cu.getPosition(line, 0);
				MethodDeclaration method = JDTUtils.getMethodThatContainsAPosition(methods, position, position);
				if (method != null) {
					String methodId = TraceIdUtils.getId(method);
					// add it if it was not already in the results
					if(!resultsMethods.contains(methodId)) {
						resultsMethods.add(methodId);
					}
				}
			}
		}

		return resultsMethods;
	}

	/**
	 * Method to read method signature from ground truth
	 * 
	 * @param pathToMethodLevelGroundTruth
	 * @param feature
	 * @return
	 */
	public static List<String> readMethodsGroundTruth(String pathToMethodLevelGroundTruth, String feature) {
		String featureFile = "";
		if (feature.contains("COLLABORATION") || feature.contains("DEPLOYMENT"))
			featureFile = feature.toLowerCase() + ".txt";
		else
			featureFile = feature.substring(0, 1).toUpperCase() + feature.substring(1).toLowerCase() + ".txt";
		featureFile = featureFile.replace("diagram", "");
		File path = new File(pathToMethodLevelGroundTruth, featureFile);
		List<String> GTMethods = FileUtils.getLinesOfFile(path);
		return GTMethods;
	}

}
