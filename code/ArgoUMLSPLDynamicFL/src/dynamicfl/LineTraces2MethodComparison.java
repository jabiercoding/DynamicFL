package dynamicfl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import groundTruthExtractor.GroundTruthExtractor;
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

		List<String> results = new ArrayList<String>();
		List<MethodDeclaration> methodsFeature = new ArrayList<MethodDeclaration>();
		List<String> resultsmethods = new ArrayList<String>();

		// for each Java file
		for (String javaClass : classAbsPathAndLines.keySet()) {

			if (crossVariantsCheck) {
				// Class is in all variants with F and it is not present in any variant without
				// F
				boolean isFeatureClass = isFeatureClass(javaClass, feature, fUtils);
				if (!isFeatureClass) {
					// It cannot be related to this feature, discard class
					continue;
				}
			}

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
			List<MethodDeclaration> methods = getMethods(cu);
			List<Integer> lines = classAbsPathAndLines.get(javaClass);
			for (int line : lines) {
				int position = cu.getPosition(line, 0);
				// TODO check if position inside anonymous classes can be anonymous classes
				// inside a "real" method
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
				// all methods of a class are considered part of the feature
				methodsFeature.addAll(getMethods(cu));
				results.add(TraceIdUtils.getId((TypeDeclaration) cu.types().get(0)));
				continue;
			}

			// percentage of methods involved in the feature
			int classNMethods = JDTUtils.getMethods(cu).size();
			if (classNMethods >= THRESHOLD_METHODS_TO_CALCULATE_PERCENTAGE) {
				int fNMethods = methodAndLocatedLines.keySet().size();
				double perc = (double) fNMethods / (double) classNMethods;
				if (perc >= THRESHOLD_METHODS_PERCENTAGE) {
					// all methods of a class are considered part of the feature
					methodsFeature.addAll(getMethods(cu));
					results.add(TraceIdUtils.getId((TypeDeclaration) cu.types().get(0)));
					continue;
				}
			}

			// at this point it was not a whole class trace
			// percentage for each method
			for (MethodDeclaration method : methodAndLocatedLines.keySet()) {

				if (crossVariantsCheck) {
					// Is feature method
					boolean isFeatureMethod = isFeatureMethod(javaClass, method, feature, fUtils);
					if (!isFeatureMethod) {
						continue;
					}
				}

				int located = methodAndLocatedLines.get(method);
				int total = getNumberOfLoC(method.toString());
				double percentage = (double) located / (double) total;

				if (percentage >= THRESHOLD_METHOD_LINES_PERCENTAGE) {
					// add method as part of the feature
					methodsFeature.add(method);
					results.add(TraceIdUtils.getId(method));
				} else {
					// add method as part of the feature
					methodsFeature.add(method);
					results.add(TraceIdUtils.getId(method) + " Refinement");
				}
			}

			for (TypeDeclaration type : typeAndLocatedLines.keySet()) {
				results.add(TraceIdUtils.getId(type) + " Refinement");
			}
		}

		for (MethodDeclaration method : methodsFeature) {
			resultsmethods.add(TraceIdUtils.getId(method));
		}

		return resultsmethods;
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
		Path path = Paths.get(pathToMethodLevelGroundTruth);
		List<String> GTMethods = null;
		try {
			GTMethods = Files.readAllLines(path.resolve(featureFile));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return GTMethods;
	}

	/**
	 * Method is in all variants with F (when the class exists) and it is not
	 * present in any variant without F (when the class exists)
	 * 
	 * @param javaClass
	 * @param method
	 * @param feature
	 * @param fUtils
	 * @return
	 */
	private static boolean isFeatureMethod(String javaClass, MethodDeclaration method, String feature,
			FeatureUtils fUtils) {
		// TODO Refactor, the beginning is the same as isFeatureClass
		// Check if it is a feature interaction F1_and_F2
		String[] features = feature.split(GroundTruthExtractor.AND_FEATURES);
		// Get for the feature, or for the first feature
		List<String> containingF = fUtils.getConfigurationsContainingFeature(features[0]);
		List<String> notContainingF = fUtils.getConfigurationsNotContainingFeature(features[0]);
		// in case of interaction, remove from containingF the configurations where the
		// rest of the features did not appear
		for (int i = 1; i < features.length; i++) {
			List<String> notContainingCurrentF = fUtils.getConfigurationsNotContainingFeature(features[i]);
			containingF.removeAll(notContainingCurrentF);
			for (String notContained : notContainingCurrentF) {
				if (!notContainingF.contains(notContained)) {
					notContainingF.add(notContained);
				}
			}
		}
		String relativePathInScenario = javaClass.substring(javaClass.indexOf("src"), javaClass.length());

		String originalId = TraceIdUtils.getId(method);

		// Must not appear in variants without the feature
		for (String config : notContainingF) {
			File variantFolder = fUtils.getVariantFolderOfConfig(config);
			File java = new File(variantFolder, relativePathInScenario);
			if (java.exists()) {
				ASTParser parser = ASTParser.newParser(AST.JLS8);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				parser.setBindingsRecovery(true);
				String source = FileUtils.getStringOfFile(java);
				parser.setSource(source.toCharArray());
				CompilationUnit cu = (CompilationUnit) parser.createAST(null);
				for (MethodDeclaration m : getMethods(cu)) {
					String variantId = TraceIdUtils.getId(m);
					if (originalId.equals(variantId)) {
						return false;
					}
				}
			}
		}

		// Must appear in variants with the feature
		for (String config : containingF) {
			File variantFolder = fUtils.getVariantFolderOfConfig(config);
			File java = new File(variantFolder, relativePathInScenario);
			if (java.exists()) {
				ASTParser parser = ASTParser.newParser(AST.JLS8);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				parser.setBindingsRecovery(true);
				String source = FileUtils.getStringOfFile(java);
				parser.setSource(source.toCharArray());
				CompilationUnit cu = (CompilationUnit) parser.createAST(null);
				boolean found = false;
				for (MethodDeclaration m : getMethods(cu)) {
					String variantId = TraceIdUtils.getId(m);
					if (originalId.equals(variantId)) {
						found = true;
						break;
					}
				}
				if (!found) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Class is in all variants with F and it is not present in any variant without
	 * F
	 * 
	 * @param javaClass
	 * @param feature
	 * @param fUtils
	 * @return feature class
	 */
	private static boolean isFeatureClass(String javaClass, String feature, FeatureUtils fUtils) {

		// Check if it is a feature interaction F1_and_F2
		String[] features = feature.split(GroundTruthExtractor.AND_FEATURES);
		// Get for the feature, or for the first feature
		List<String> containingF = fUtils.getConfigurationsContainingFeature(features[0]);
		List<String> notContainingF = fUtils.getConfigurationsNotContainingFeature(features[0]);
		// in case of interaction, remove from containingF the configurations where the
		// rest of the features did not appear
		for (int i = 1; i < features.length; i++) {
			List<String> notContainingCurrentF = fUtils.getConfigurationsNotContainingFeature(features[i]);
			containingF.removeAll(notContainingCurrentF);
			for (String notContained : notContainingCurrentF) {
				if (!notContainingF.contains(notContained)) {
					notContainingF.add(notContained);
				}
			}
		}

		String relativePathInScenario = javaClass.substring(javaClass.indexOf("src"), javaClass.length());

		// Must not appear in variants without the feature
		for (String config : notContainingF) {
			File variantFolder = fUtils.getVariantFolderOfConfig(config);
			File java = new File(variantFolder, relativePathInScenario);
			if (java.exists()) {
				return false;
			}
		}

		// Must appear in variants with the feature
		for (String config : containingF) {
			File variantFolder = fUtils.getVariantFolderOfConfig(config);
			File java = new File(variantFolder, relativePathInScenario);
			if (!java.exists()) {
				return false;
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

	/**
	 * Get methods ignoring those in anonymous classes
	 * 
	 * @param cu
	 * @return
	 */
	public static List<MethodDeclaration> getMethods(CompilationUnit cu) {
		List<MethodDeclaration> methods = JDTUtils.getMethods(cu);
		List<MethodDeclaration> toRemove = new ArrayList<MethodDeclaration>();
		for (MethodDeclaration method : methods) {
			if (method.getParent() instanceof AnonymousClassDeclaration) {
				toRemove.add(method);
			}
		}
		methods.removeAll(toRemove);
		return methods;
	}

}
