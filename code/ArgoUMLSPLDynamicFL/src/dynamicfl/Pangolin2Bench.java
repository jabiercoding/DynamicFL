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

import groundTruthExtractor.GroundTruthExtractor;
import utils.FileUtils;
import utils.TraceIdUtils;

public class Pangolin2Bench {

	// less or equal will be ignored
	public static final double THRESHOLD_PANGOLIN_SCORE = 0.0;

	// greater or equal will be considered whole class (without Refinement)
	public static final double THRESHOLD_GLOBAL_CLASS_LINES_PERCENTAGE = 0.5;

	// greater or equal will be considered whole class (without Refinement)
	public static final double THRESHOLD_METHODS_PERCENTAGE = 0.75;
	// but only classes with greater or equal number of methods
	public static final double THRESHOLD_METHODS_TO_CALCULATE_PERCENTAGE = 1;

	// less will be considered Refinement, greater or equal will be considered the
	// whole method (without Refinement)
	public static final double THRESHOLD_METHOD_LINES_PERCENTAGE = 0.5;

	public static void main(String[] args) {

		// We assume that the ArgoUMLSPLBenchmark is in a project in the workspace and
		// the Original scenario was created and used for creating the Pangolin traces
		File argoUMLsrc = new File(
				"C:/git/argouml-spl-benchmark/ArgoUMLSPLBenchmark/scenarios/ScenarioOriginalVariant/variants/Original.config/src");
		if (!argoUMLsrc.exists()) {
			System.out.println("Scenario has to be created first");
			return;
		}

		// Parse pangolin results file
		File pangolinResultsFile = new File("resultsPangolin/ACTIVITY_ADD_ELEMENTS.csv");

		Map<String, List<Integer>> classAndLines = parsePangolinCSV(pangolinResultsFile, argoUMLsrc);

		createBenchmarkString(classAndLines);
	}

	/**
	 * Parse Pangolin csv
	 * 
	 * @param pangolinResultsFile
	 * @param srcFolder
	 * @return
	 */
	public static Map<String, List<Integer>> parsePangolinCSV(File pangolinResultsFile, File srcFolder) {
		Map<String, List<Integer>> classAndLines = new LinkedHashMap<String, List<Integer>>();

		for (String line : FileUtils.getLinesOfFile(pangolinResultsFile)) {

			String[] split = line.split(",");
			String scoreString = split[split.length - 1];
			// ignore header
			if (scoreString.equalsIgnoreCase("Score")) {
				continue;
			}
			double score = Double.parseDouble(scoreString);
			// ignore low scores
			if (score <= THRESHOLD_PANGOLIN_SCORE) {
				continue;
			}

			String packageName = split[0];
			String packagePath = packageName.replaceAll("\\.", "\\\\");
			File packageFile = new File(srcFolder, packagePath);
			// ignore source code that it is not part of ArgoUML src (e.g., libraries)
			if (!packageFile.exists()) {
				continue;
			}

			String className = split[1];
			File classFile = new File(packageFile, className + ".java");

			// ignore inner classes
			if (!classFile.exists()) {
				continue;
			}

			List<Integer> codeLines = classAndLines.get(classFile.getAbsolutePath());
			if (codeLines == null) {
				codeLines = new ArrayList<Integer>();
			}
			int codeLine = Integer.parseInt(split[split.length - 2]);
			codeLines.add(codeLine);
			classAndLines.put(classFile.getAbsolutePath(), codeLines);
		}
		return classAndLines;
	}

	/**
	 * Create benchmark string
	 * 
	 * @param classAndLines.
	 *            Key set is the absolute path to each Java file
	 */
	public static void createBenchmarkString(Map<String, List<Integer>> classAndLines) {

		// for each Java file
		for (String javaClass : classAndLines.keySet()) {

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
			List<MethodDeclaration> methods = GroundTruthExtractor.getMethods(cu);
			List<Integer> lines = classAndLines.get(javaClass);
			for (int line : lines) {
				int position = cu.getPosition(line, 0);
				MethodDeclaration method = GroundTruthExtractor.getMethodThatContainsAPosition(methods, position,
						position);
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
				System.out.println(TraceIdUtils.getId((TypeDeclaration) cu.types().get(0)));
				continue;
			}

			// percentage of methods involved in the feature
			int classNMethods = GroundTruthExtractor.getMethods(cu).size();
			if (classNMethods >= THRESHOLD_METHODS_TO_CALCULATE_PERCENTAGE) {
				int fNMethods = methodAndLocatedLines.keySet().size();
				double perc = (double) fNMethods / (double) classNMethods;
				if (perc >= THRESHOLD_METHODS_PERCENTAGE) {
					System.out.println(TraceIdUtils.getId((TypeDeclaration) cu.types().get(0)));
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
					System.out.println(TraceIdUtils.getId(method));
				} else {
					System.out.println(TraceIdUtils.getId(method) + " Refinement");
				}
			}

			for (TypeDeclaration type : typeAndLocatedLines.keySet()) {
				System.out.println(TraceIdUtils.getId(type) + " Refinement");
			}
		}
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
