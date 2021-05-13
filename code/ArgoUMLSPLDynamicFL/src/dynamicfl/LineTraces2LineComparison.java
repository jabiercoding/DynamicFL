package dynamicfl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import utils.FeatureUtils;
import utils.FileUtils;
import utils.JDTUtils;
import utils.TraceIdUtils;

/**
 * LineTraces to Line aims to get the lines executed for each class to compute
 * metrics at the line-level e.g., when a line inside a class is executed we
 * compared if the same line is inside the same class of the variant of a
 * feature with a Git diff API
 */
public class LineTraces2LineComparison {

	/**
	 * Create method signature string
	 * 
	 * @param scenarioPath
	 * @param feature
	 * 
	 * @param classAndLines.
	 *            Key set is the absolute path to each Java file
	 * @throws IOException
	 */
	public static void getResultsInLineComparison(Map<String, List<Integer>> classAbsPathAndLines, String feature,
			String pathToLineLevelGroundTruth, String pathToOriginalVariant, FeatureUtils fUtils,
			File outputScenarioLine, boolean crossVariantsCheck) throws IOException {

		File variantOriginal = new File(pathToOriginalVariant,
				feature.toUpperCase() + ".config" + File.separator + "src");

		File variantFeatureGT = new File(pathToLineLevelGroundTruth, feature.toUpperCase() + ".1");

		Map<File, List<String>> filesRetrieved = new HashMap<>();
		Map<File, List<String>> filesFeatureVariant = new HashMap<>();

		LinkedList<File> filesFeatureVariantList = new LinkedList<>();
		getFilesToProcess(variantFeatureGT, filesFeatureVariantList);
		filesFeatureVariantList.remove(variantOriginal);

		// for each class of the results
		for (String javaClass : classAbsPathAndLines.keySet()) {

			// Prepare the parser
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setBindingsRecovery(true);

			String source = FileUtils.getStringOfFile(new File(javaClass));
			parser.setSource(source.toCharArray());
			CompilationUnit cu = (CompilationUnit) parser.createAST(null);
			List<MethodDeclaration> methods = LineTraces2BenchFormat.getMethods(cu);

			File retrievedFile = new File(javaClass);
			List<String> linesRetrieved = new ArrayList<>();
			List<String> bufferRetrievedFile = new ArrayList<>();

			List<String> methodsIncluded = new ArrayList<String>();

			List<Integer> lines = classAbsPathAndLines.get(javaClass);
			Collections.sort(lines);

			// buffer to read the lines of the original variant used to exercise the feature
			BufferedReader br = new BufferedReader(new FileReader(retrievedFile.getAbsoluteFile()));
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				bufferRetrievedFile.add(sCurrentLine);
			}
			br.close();

			// add to the list of retrieved lines the single lines executed when exercised
			// the feature and the method body when the line is inside a method
			for (int line : lines) {
				// get the method of this line
				int position = cu.getPosition(line, 0);
				MethodDeclaration method = JDTUtils.getMethodThatContainsAPosition(methods, position, position);
				if (method != null) {
					String methodId = TraceIdUtils.getId(method);
					// add it if it was not already in the results
					if (!methodsIncluded.contains(methodId)) {
						methodsIncluded.add(methodId);
						int start = cu.getLineNumber(method.getStartPosition());
						int end = cu.getLineNumber(method.getStartPosition() + method.getLength());
						for (int i = start; i <= end; i++) {
							sCurrentLine = bufferRetrievedFile.get(i - 1).trim().replaceAll("\t", "")
									.replaceAll("\r", "").replaceAll(" ", "");
							if (!sCurrentLine.equals("") && !sCurrentLine.startsWith("//")
									&& !sCurrentLine.startsWith("/*") && !sCurrentLine.startsWith("*/")
									&& !sCurrentLine.startsWith("*") && !sCurrentLine.startsWith("import")) {
								linesRetrieved.add(sCurrentLine);
							}
						}
					}
				} else {
					if (!linesRetrieved.contains(bufferRetrievedFile.get(line - 1))) {
						sCurrentLine = bufferRetrievedFile.get(line - 1).trim().replaceAll("\t", "")
								.replaceAll("\r", "").replaceAll(" ", "");
						if (!sCurrentLine.equals("") && !sCurrentLine.startsWith("//") && !sCurrentLine.startsWith("/*")
								&& !sCurrentLine.startsWith("*/") && !sCurrentLine.startsWith("*")
								&& !sCurrentLine.startsWith("import")) {
							linesRetrieved.add(sCurrentLine);
						}
					}
				}
			}

			filesRetrieved.put(retrievedFile, linesRetrieved);

		}

		List<String> linesFeature = new ArrayList<>();

		// add lines of files existing in a feature variant
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
							&& !sCurrentLine.startsWith("import")) {
						linesFeature.add(sCurrentLine);
					}
				}
				br.close();
				filesFeatureVariant.put(f, linesFeature);
			}
		}

		compareVariants(filesRetrieved, filesFeatureVariant, outputScenarioLine, feature);

	}

	public static void compareVariants(Map<File, List<String>> filesRetrieved,
			Map<File, List<String>> filesFeatureVariant, File outputScenarioLine, String feature) throws IOException {
		String outputCSV = outputScenarioLine.getAbsolutePath();
		String fileStr = outputCSV + File.separator + feature + ".csv";
		File fWriter = new File(fileStr);
		FileWriter csvWriter = new FileWriter(fWriter);

		List<List<String>> headerRows = Arrays.asList(Arrays.asList("fileName", "matchFile", "truepositiveLines",
				"falsepositiveLines", "falsenegativeLines", "originaltotalLines", "retrievedtotalLines"));
		for (List<String> rowData : headerRows) {
			csvWriter.write(String.join(",", rowData));
			csvWriter.write("\n");
		}

		// files retrieved and in featureVariant
		for (Entry<File, List<String>> f : filesFeatureVariant.entrySet()) {
			Boolean fileExistsInRetrieved = false;
			Integer truepositiveLines = 0, falsepositiveLines = 0, falsenegativeLines = 0, originaltotalLines = 0,
					retrievedtotalLines = 0;
			Boolean matchFiles = false;
			List<String> original = f.getValue();
			List<String> revised = new ArrayList<>();

			// compare text of files
			for (Entry<File, List<String>> fRetrieved : filesRetrieved.entrySet()) {
				if (f.getKey().getPath().toString()
						.substring(f.getKey().toPath().toString().indexOf("org" + File.separator) + 4)
						.equals(fRetrieved.getKey().toPath().toString().substring(
								fRetrieved.getKey().toPath().toString().indexOf("org" + File.separator) + 4))) {
					revised = fRetrieved.getValue();

					// Compute diff. Get the Patch object. Patch is the container for computed
					// deltas.
					Patch<String> patch = null;
					patch = DiffUtils.diff(original, revised);
					ArrayList<String> insertedLines = new ArrayList<>();
					ArrayList<String> changedLinesRevised = new ArrayList<>();
					ArrayList<String> changedLinesOriginal = new ArrayList<>();
					ArrayList<String> deletedLines = new ArrayList<>();
					if (patch.getDeltas().size() == 0) {
						// files match
						matchFiles = true;
					} else {
						String del = "", insert = "";
						for (Delta delta : patch.getDeltas()) {
							String line = "";
							if (delta.getType().toString().equals("INSERT")) {
								ArrayList<String> arraylines = (ArrayList<String>) delta.getRevised().getLines();
								for (String deltaaux : arraylines) {
									line = deltaaux.trim().replaceAll("\t", "").replaceAll(",", "").replaceAll(" ", "");
									if (!line.equals("") && !line.startsWith("//") && !line.startsWith("/*")
											&& !line.startsWith("*/") && !line.startsWith("*")) {
										matchFiles = false;
										falsepositiveLines++;
										insert = line;
										insertedLines.add(insert);
									}
								}
							} else if (delta.getType().toString().equals("CHANGE")) {
								ArrayList<String> arraylines = (ArrayList<String>) delta.getRevised().getLines();
								ArrayList<String> arrayOriginal = (ArrayList<String>) delta.getOriginal().getLines();
								for (String deltaaux : arraylines) {
									line = deltaaux.trim().replaceAll("\t", "").replaceAll(",", "").replaceAll(" ", "");
									if (!line.equals("") && !line.startsWith("//") && !line.startsWith("/*")
											&& !line.startsWith("*/") && !line.startsWith("*")) {
										insert = line;
										changedLinesRevised.add(insert);
									}
								}
								for (String deltaaux : arrayOriginal) {
									line = deltaaux.trim().replaceAll("\t", "").replaceAll(",", "").replaceAll(" ", "");
									if (!line.equals("") && !line.startsWith("//") && !line.startsWith("/*")
											&& !line.startsWith("*/") && !line.startsWith("*")) {
										insert = line;
										changedLinesOriginal.add(insert);
									}
								}
							} else {
								ArrayList<String> arraylines = (ArrayList<String>) delta.getOriginal().getLines();
								for (String deltaaux : arraylines) {
									line = deltaaux.trim().replaceAll("\t", "").replaceAll(",", "").replaceAll(" ", "");
									if (!line.equals("") && !line.startsWith("//") && !line.startsWith("/*")
											&& !line.startsWith("*/") && !line.startsWith("*")) {
										matchFiles = false;
										falsenegativeLines++;
										del = line;
										deletedLines.add(del);
									}
								}

							}
						}
					}
					String trimmingDiffLinesalingmentOr = "";
					for (String changedLine : changedLinesOriginal) {
						boolean found = false;
						String aux = "";
						for (String changedrevised : changedLinesRevised) {
							if (changedrevised.contains("//#")) {
								aux = changedrevised.substring(0, changedrevised.indexOf("//#"));
								if (changedLine.equals(aux)) {
									found = true;
									aux = changedrevised;
									break;
								}
							} else if (changedLine.equals(changedrevised)) {
								found = true;
								aux = changedrevised;
								break;
							} else {
								if (changedrevised.contains(changedLine)) {
									trimmingDiffLinesalingmentOr += changedLine;
									if (falsenegativeLines > 0)
										falsenegativeLines--;
								}
							}

						}
						if (!found)
							falsenegativeLines++;
						else
							changedLinesRevised.remove(aux);
					}
					ArrayList<String> changedLinesRevisedAux = new ArrayList<>();
					changedLinesRevisedAux.addAll(changedLinesRevised);
					for (String changedrevised : changedLinesRevisedAux) {
						if (trimmingDiffLinesalingmentOr.contains(changedrevised))
							changedLinesRevised.remove(changedrevised);
					}

					if (changedLinesRevised.size() > 0) {
						falsepositiveLines += changedLinesRevised.size();
						for (String changedline : changedLinesRevised) {
							insertedLines.add(changedline);
						}
					}

					ArrayList<String> diffDeleted = new ArrayList<>();
					Boolean found = false;
					for (String line : deletedLines) {
						for (String insertLine : insertedLines) {
							if (insertLine.equals(line) || insertLine.contains(line)) {
								if (falsepositiveLines > 0)
									falsepositiveLines--;
								if (falsenegativeLines > 0)
									falsenegativeLines--;
								found = true;
								break;
							}
						}
						if (!found) {
							diffDeleted.add(line);
						} else {
							insertedLines.remove(line);
							found = false;
						}
					}

					for (String line : changedLinesOriginal) {
						for (String insertedLine : insertedLines) {
							if (insertedLine.equals(line)) {
								if (falsepositiveLines > 0)
									falsepositiveLines--;
								if (falsenegativeLines > 0)
									falsenegativeLines--;
								found = true;
								break;
							}
						}
						if (found) {
							insertedLines.remove(line);
							found = false;
						}
					}

					retrievedtotalLines = (revised.size() - 1);
					originaltotalLines = original.size() - 1;
					truepositiveLines = retrievedtotalLines - (falsepositiveLines);

					List<List<String>> resultRows = Arrays
							.asList(Arrays.asList(
									f.getKey().toPath().toString()
											.substring(
													f.getKey().toPath().toString().indexOf("org" + File.separator) + 4)
											.replace(",", "and"),
									matchFiles.toString(), truepositiveLines.toString(), falsepositiveLines.toString(),
									falsenegativeLines.toString(), originaltotalLines.toString(),
									retrievedtotalLines.toString()));
					for (List<String> rowData : resultRows) {
						csvWriter.append(String.join(",", rowData));
						csvWriter.append("\n");
					}
					fileExistsInRetrieved = true;
				}

			}
			if (!fileExistsInRetrieved) {
				for (String line : original) {
					String lineaux = line.trim().replaceAll("\t", "").replaceAll(",", "").replaceAll(" ", "");
					if (!lineaux.equals("") && !lineaux.startsWith("//") && !lineaux.startsWith("/*")
							&& !lineaux.startsWith("*/") && !lineaux.startsWith("*")) {
						originaltotalLines++;
					}
				}
				List<List<String>> resultRows = Arrays.asList(Arrays.asList(
						f.getKey().toPath().toString()
								.substring(f.getKey().toPath().toString().indexOf("org" + File.separator) + 4)
								.replace(",", "and"),
						"not", "0", "0", originaltotalLines.toString(), originaltotalLines.toString(),
						retrievedtotalLines.toString()));
				for (List<String> rowData : resultRows) {
					csvWriter.append(String.join(",", rowData));
					csvWriter.append("\n");
				}
			}
		}

		// files retrieved that do not exist in the feature variant
		for (Entry<File, List<String>> fRetrieved : filesRetrieved.entrySet()) {
			Integer truepositiveLines = 0, falsepositiveLines = 0, falsenegativeLines = 0, originaltotalLines = 0,
					retrievedtotalLines = 0;

			Boolean existJustRetrieved = true;
			for (Entry<File, List<String>> f : filesFeatureVariant.entrySet()) {
				if (fRetrieved.getKey().toPath().toString()
						.substring(fRetrieved.getKey().toPath().toString().indexOf("org" + File.separator) + 4)
						.equals(f.getKey().toPath().toString()
								.substring(f.getKey().toPath().toString().indexOf("org" + File.separator) + 4))) {
					existJustRetrieved = false;
				}
			}
			// file just exist in retrieved
			if (existJustRetrieved) {
				// compare text of files
				List<String> original = new ArrayList<>();// Files.readAllLines(fretrieved.toPath());
				File filenew = new File(String.valueOf(fRetrieved.getKey().toPath()));
				BufferedReader br = new BufferedReader(new FileReader(filenew.getAbsoluteFile()));
				String sCurrentLine;
				while ((sCurrentLine = br.readLine()) != null) {
					sCurrentLine = sCurrentLine.trim().replaceAll("\t", "").replaceAll("\r", "").replaceAll(" ", "");
					if (!sCurrentLine.equals("") && !sCurrentLine.startsWith("//") && !sCurrentLine.startsWith("/*")
							&& !sCurrentLine.startsWith("*/") && !sCurrentLine.startsWith("*")
							&& !sCurrentLine.startsWith("import")) {
						original.add(sCurrentLine);
					}
				}
				br.close();
				
				retrievedtotalLines = original.size() - 1;
				falsepositiveLines = retrievedtotalLines;
				falsenegativeLines = 0;
				originaltotalLines = 0;
				truepositiveLines = retrievedtotalLines - (falsepositiveLines);

				List<List<String>> resultRows = Arrays.asList(Arrays.asList(
						fRetrieved.getKey().toPath().toString()
								.substring(fRetrieved.getKey().toPath().toString().indexOf("org" + File.separator) + 4)
								.replace(",", "and"),
						"justOnRetrieved", truepositiveLines.toString(), falsepositiveLines.toString(),
						falsenegativeLines.toString(), originaltotalLines.toString(), retrievedtotalLines.toString()));
				for (List<String> rowData : resultRows) {
					csvWriter.append(String.join(",", rowData));
					csvWriter.append("\n");
				}
			}

		}
		csvWriter.close();
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
	 * Get only java classes from the feature variant directory
	 * 
	 * @param featureVariantDir
	 * @param files
	 * @return
	 */
	private static void getFilesToProcess(File featureVariantDir, List<File> files) {
		if (featureVariantDir.isDirectory()) {
			for (File file : featureVariantDir.listFiles()) {
				if (!files.contains(featureVariantDir) && !file.getName().equals(featureVariantDir.getName()))
					files.add(featureVariantDir);
				getFilesToProcess(file, files);
			}
		} else if (featureVariantDir.isFile() && featureVariantDir.getName()
				.substring(featureVariantDir.getName().lastIndexOf('.') + 1).equals("java")) {
			files.add(featureVariantDir);
		}
	}
}
