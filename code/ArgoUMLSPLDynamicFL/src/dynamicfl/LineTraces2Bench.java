package dynamicfl;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import utils.FeatureUtils;
import utils.FileUtils;
import utils.JDTUtils;
import utils.TraceIdUtils;

public class LineTraces2Bench {

	// greater or equal will be considered whole class (without Refinement)
	public static final double THRESHOLD_GLOBAL_CLASS_LINES_PERCENTAGE = 0.5;

	// greater or equal will be considered whole class (without Refinement)
	public static final double THRESHOLD_METHODS_PERCENTAGE = 0.75;
	// but only classes with greater or equal number of methods
	public static final double THRESHOLD_METHODS_TO_CALCULATE_PERCENTAGE = 1;

	// less will be considered Refinement, greater or equal will be considered the
	// whole method (without Refinement)
	public static final double THRESHOLD_METHOD_LINES_PERCENTAGE = 0.5;

	/**
	 * Create benchmark string
	 * 
	 * @param scenarioPath
	 * @param feature
	 * 
	 * @param classAndLines.
	 *            Key set is the absolute path to each Java file
	 */
	public static List<String> getResultsInBenchmarkFormat(Map<String, List<Integer>> classAbsPathAndLines,
			String feature, FeatureUtils fUtils) {

		List<String> results = new ArrayList<String>();

		// for each Java file
		for (String javaClass : classAbsPathAndLines.keySet()) {

			boolean isFeatureClass = isFeatureClass(javaClass, feature, fUtils);

			// maps to calculate the total number of lines per method (or type if the
			// statement was not inside a method)
			Map<MethodDeclaration, Integer> methodAndLocatedLines = new LinkedHashMap<MethodDeclaration, Integer>();
			Map<TypeDeclaration, Integer> typeAndLocatedLines = new LinkedHashMap<TypeDeclaration, Integer>();

			// Prepare the parser
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setBindingsRecovery(true);

			String source = FileUtils.getStringOfFile(new File(javaClass));
			parser.setSource(source.toCharArray());
			CompilationUnit cu = (CompilationUnit) parser.createAST(null);
			List<MethodDeclaration> methods = JDTUtils.getMethods(cu);
			List<Integer> lines = classAbsPathAndLines.get(javaClass);
			for (int line : lines) {
				int position = cu.getPosition(line, 0);
				MethodDeclaration method = JDTUtils.getMethodThatContainsAPosition(methods, position, position);
				if (method == null) {
					TypeDeclaration type = (TypeDeclaration) cu.types().get(0);
					Integer total = typeAndLocatedLines.get(type);
					if (total == null) {
						total = 0;
					}
					total++;
					typeAndLocatedLines.put(type, total);
				} else {
					Integer total = methodAndLocatedLines.get(method);
					if (total == null) {
						total = 0;
					}
					total++;
					methodAndLocatedLines.put(method, total);
				}
			}

			// global LoC class level
			int classLoC = getNumberOfLoC(cu.toString());
			int fLoC = 0;
			for (Integer l : methodAndLocatedLines.values()) {
				fLoC += l;
			}
			for (Integer l : typeAndLocatedLines.values()) {
				fLoC += l;
			}
			double per = (double) fLoC / (double) classLoC;
			if (per >= THRESHOLD_GLOBAL_CLASS_LINES_PERCENTAGE) {
				results.add(TraceIdUtils.getId((TypeDeclaration) cu.types().get(0)));
				continue;
			}

			// percentage of methods involved in the feature
			int classNMethods = JDTUtils.getMethods(cu).size();
			if (classNMethods >= THRESHOLD_METHODS_TO_CALCULATE_PERCENTAGE) {
				int fNMethods = methodAndLocatedLines.keySet().size();
				double perc = (double) fNMethods / (double) classNMethods;
				if (perc >= THRESHOLD_METHODS_PERCENTAGE) {
					results.add(TraceIdUtils.getId((TypeDeclaration) cu.types().get(0)));
					continue;
				}
			}

			// at this point it was not a whole class trace
			// percentage for each method
			for (MethodDeclaration method : methodAndLocatedLines.keySet()) {
				int located = methodAndLocatedLines.get(method);
				int total = getNumberOfLoC(method.toString());
				double percentage = (double) located / (double) total;

				if (percentage >= THRESHOLD_METHOD_LINES_PERCENTAGE) {
					results.add(TraceIdUtils.getId(method));
				} else {
					results.add(TraceIdUtils.getId(method) + " Refinement");
				}
			}

			for (TypeDeclaration type : typeAndLocatedLines.keySet()) {
				results.add(TraceIdUtils.getId(type) + " Refinement");
			}
		}

		return results;
	}

	/**
	 * Class is in all variants with F and it is not present in any variant without
	 * F, AND the same methods are in all variants with the same line/statements
	 * 
	 * @param javaClass
	 * @param feature
	 * @param fUtils
	 * @return feature class
	 */
	private static boolean isFeatureClass(String javaClass, String feature, FeatureUtils fUtils) {
		List<String> containingF = fUtils.getConfigurationsContainingFeature(feature);
		List<String> notContainingF = fUtils.getConfigurationsNotContainingFeature(feature);

		String relativePathInScenario = javaClass.substring(javaClass.indexOf("src"), javaClass.length());
		
		// Must not appear in variants without the feature
		for (String config : notContainingF) {
			File variantFolder = fUtils.getVariantFolderOfConfig(config);
			File java = new File(variantFolder, relativePathInScenario);
			if (java.exists()) {
				return false;
			}
		}
		
		// Must appear in variants with the feature and with the same content
		for (String config : containingF) {
			File variantFolder = fUtils.getVariantFolderOfConfig(config);
			File java = new File(variantFolder, relativePathInScenario);
			if (!java.exists()) {
				return false;
			} else {
				// TODO check that it is the same content
			}
		}
		

		return true;
	}

	/**
	 * Number of LoC ignoring comments and empty lines
	 */
	public static int getNumberOfLoC(String code) {
		String commentsRegex = "(?://.*)|(/\\*(?:.|[\\n\\r])*?\\*/)";
		code = code.replaceAll(commentsRegex, "");
		String[] lines = code.split("\r\n|\r|\n");
		int total = 0;
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			// remove whitespaces in the beginning of the string
			line = line.trim();
			// ignore empty lines
			if (line.isEmpty()) {
				continue;
			}
			total++;
		}
		return total;
	}

}