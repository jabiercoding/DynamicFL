package dynamicfl.metrics;

import java.io.File;
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
 * LineTraces to Bench aims to convert lines of code to the format of the
 * benchmark making some assumptions e.g., when a class or a method is
 * sufficiently covered to be considered as full or as Refinement tag
 */
public class LineTraces2BenchFormat {

	/**
	 * Create benchmark strings with predefined hard-coded thresholds
	 * 
	 * @param Strings in the benchmark format
	 */
	public static List<String> getResultsInBenchmarkFormat(Map<String, List<Integer>> classAndLines, String feature,
			FeatureUtils fUtils, File originalArgoUMLsrc, boolean crossVariantsCheck) {
		return getResultsInBenchmarkFormat(classAndLines, feature, fUtils, originalArgoUMLsrc, crossVariantsCheck, 0.25,
				0.25, 1, 0.25);
	}

	/**
	 * Create benchmark strings. Using 0.0 for the thresholds will make that only
	 * Class traces will be created, i.e., class trace will be craeted if at least
	 * one line is executed from this class.
	 * 
	 * @return Strings in the benchmark format
	 */
	public static List<String> getResultsInBenchmarkFormatOnlyClasses(Map<String, List<Integer>> classAndLines,
			String feature, FeatureUtils fUtils, File originalArgoUMLsrc, boolean crossVariantsCheck) {
		return getResultsInBenchmarkFormat(classAndLines, feature, fUtils, originalArgoUMLsrc, crossVariantsCheck, 0.0,
				0.0, 1, 0.0);
	}

	/**
	 * Create benchmark string
	 * 
	 * @param classAndLines
	 * @param feature
	 * @param fUtils
	 * @param originalArgoUMLsrc
	 * @param crossVariantsCheck                        use for benchmark scenarios
	 *                                                  with more than one variant
	 * @param THRESHOLD_GLOBAL_CLASS_LINES_PERCENTAGE   greater or equal will be
	 *                                                  considered whole class
	 *                                                  (without Refinement)
	 * @param THRESHOLD_METHODS_PERCENTAGE              greater or equal will be
	 *                                                  considered whole class
	 *                                                  (without Refinement)
	 * @param THRESHOLD_METHODS_TO_CALCULATE_PERCENTAGE but only classes with
	 *                                                  greater or equal number of
	 *                                                  methods
	 * @param THRESHOLD_METHOD_LINES_PERCENTAGE         less will be considered
	 *                                                  Refinement, greater or equal
	 *                                                  will be considered the whole
	 *                                                  method (without Refinement)
	 * @return
	 */
	public static List<String> getResultsInBenchmarkFormat(Map<String, List<Integer>> classAndLines, String feature,
			FeatureUtils fUtils, File originalArgoUMLsrc, boolean crossVariantsCheck,
			double THRESHOLD_GLOBAL_CLASS_LINES_PERCENTAGE, double THRESHOLD_METHODS_PERCENTAGE,
			double THRESHOLD_METHODS_TO_CALCULATE_PERCENTAGE, double THRESHOLD_METHOD_LINES_PERCENTAGE) {

		List<String> results = new ArrayList<String>();

		// for each Java file
		for (String javaClass : classAndLines.keySet()) {

			if (!javaClass.startsWith("org.argouml")) {
				// skip org.omg.* as they are not part of the benchmark
				// groundTruthExtractor.GroundTruthExtractor.getAllArgoUMLSPLRelevantJavaFiles(File)
				continue;
			}

			String javaAbsPath = MetricsModule.getAbsPathFromClass(originalArgoUMLsrc, javaClass);

			if (crossVariantsCheck) {
				// Class is in all variants with F and it is not present in any variant without
				// F
				boolean isFeatureClass = isFeatureClass(javaClass, javaAbsPath, feature, fUtils);
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

			String source = FileUtils.getStringOfFile(new File(javaAbsPath));
			parser.setSource(source.toCharArray());
			CompilationUnit cu = (CompilationUnit) parser.createAST(null);

			TypeDeclaration type = getTypeDeclarationByName(cu, javaClass);

			// get the methods of this class, if we take all methods from the compilation
			// unit we might be getting more (more than one class in one Java file)
			List<MethodDeclaration> methods = getMethods(cu, javaClass);
			List<Integer> lines = classAndLines.get(javaClass);
			for (int line : lines) {
				int position = cu.getPosition(line, 0);
				// TODO check if position inside anonymous classes can be anonymous classes
				// inside a "real" method
				MethodDeclaration method = JDTUtils.getMethodThatContainsAPosition(methods, position, position);
				if (method == null) {
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
			int classLoC = getNumberOfLoC(type.toString());
			int fLoC = 0;
			for (Integer l : methodAndLocatedLines.values()) {
				fLoC += l;
			}
			for (Integer l : typeAndLocatedLines.values()) {
				fLoC += l;
			}
			double per = (double) fLoC / (double) classLoC;
			if (per >= THRESHOLD_GLOBAL_CLASS_LINES_PERCENTAGE) {
				results.add(TraceIdUtils.getId(type));
				continue;
			}

			// percentage of methods involved in the feature
			int classNMethods = JDTUtils.getMethods(cu).size();
			if (classNMethods >= THRESHOLD_METHODS_TO_CALCULATE_PERCENTAGE) {
				int fNMethods = methodAndLocatedLines.keySet().size();
				double perc = (double) fNMethods / (double) classNMethods;
				if (perc >= THRESHOLD_METHODS_PERCENTAGE) {
					results.add(TraceIdUtils.getId(type));
					continue;
				}
			}

			// at this point it was not a whole class trace
			// percentage for each method
			for (MethodDeclaration method : methodAndLocatedLines.keySet()) {

				if (crossVariantsCheck) {
					// Is feature method
					boolean isFeatureMethod = isFeatureMethod(javaClass, javaAbsPath, method, feature, fUtils);
					if (!isFeatureMethod) {
						continue;
					}
				}

				int located = methodAndLocatedLines.get(method);
				int total = getNumberOfLoC(method.toString());
				double percentage = (double) located / (double) total;

				if (percentage >= THRESHOLD_METHOD_LINES_PERCENTAGE) {
					results.add(TraceIdUtils.getId(method));
				} else {
					results.add(TraceIdUtils.getId(method) + " Refinement");
				}
			}

			for (TypeDeclaration ctype : typeAndLocatedLines.keySet()) {
				results.add(TraceIdUtils.getId(ctype) + " Refinement");
			}
		}

		return results;
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
	private static boolean isFeatureMethod(String javaClass, String javaAbsPath, MethodDeclaration method,
			String feature, FeatureUtils fUtils) {
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
		String relativePathInScenario = javaAbsPath.substring(javaAbsPath.indexOf("src"), javaAbsPath.length());

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
				for (MethodDeclaration m : getMethods(cu, javaClass)) {
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
				for (MethodDeclaration m : getMethods(cu, javaClass)) {
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
	private static boolean isFeatureClass(String javaClass, String absPathClass, String feature, FeatureUtils fUtils) {

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

		String relativePathInScenario = absPathClass.substring(absPathClass.indexOf("src"), absPathClass.length());

		// Must not appear in variants without the feature
		for (String config : notContainingF) {
			File variantFolder = fUtils.getVariantFolderOfConfig(config);
			File java = new File(variantFolder, relativePathInScenario);
			if (java.exists() && isTypeInJava(javaClass, java)) {
				return false;
			}
		}

		// Must appear in variants with the feature
		for (String config : containingF) {
			File variantFolder = fUtils.getVariantFolderOfConfig(config);
			File java = new File(variantFolder, relativePathInScenario);
			if (!java.exists() || !isTypeInJava(javaClass, java)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Is type in Java file
	 * 
	 * @param javaClass
	 * @param java      file
	 * @return true if the java file contains this type declaration
	 */
	public static boolean isTypeInJava(String javaClass, File java) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setBindingsRecovery(true);

		String source = FileUtils.getStringOfFile(java);
		parser.setSource(source.toCharArray());
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		TypeDeclaration type = getTypeDeclarationByName(cu, javaClass);
		return type != null;
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

	/**
	 * Get methods of a given type
	 * 
	 * @param compilation unit
	 * @return name of the target type declaration
	 */
	public static List<MethodDeclaration> getMethods(CompilationUnit cu, String type) {
		List<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();
		TypeDeclaration t = getTypeDeclarationByName(cu, type);
		if (t != null) {
			MethodDeclaration[] ms = t.getMethods();
			if (ms != null) {
				for (MethodDeclaration m : ms) {
					methods.add(m);
				}
			}
		} else {
			System.err.println(type + " type does not exist in compilation unit");
		}
		return methods;
	}

	/**
	 * Get typeDeclaration by name (one compilation unit usually has one type but it
	 * can have more)
	 * 
	 * @param compilation unit
	 * @param name
	 * @return the typeDeclaration or null if not found
	 */
	public static TypeDeclaration getTypeDeclarationByName(CompilationUnit cu, String javaClass) {
		for (Object o : cu.types()) {
			if (o instanceof TypeDeclaration) {
				TypeDeclaration t = (TypeDeclaration) o;
				String name = t.getName().getFullyQualifiedName();
				// fully qualified name it returns the name, not the package.class etc.
				if (javaClass.endsWith("." + name)) {
					return t;
				}
			}
		}
		return null;
	}

}
