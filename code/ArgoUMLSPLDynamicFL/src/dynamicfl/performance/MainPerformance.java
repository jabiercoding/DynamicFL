package dynamicfl.performance;

import java.io.File;
import java.util.List;
import java.util.Map;

import dynamicfl.Main;
import dynamicfl.datasetReader.DatasetReader;
import dynamicfl.gridSearch.GridSearch;
import dynamicfl.spectrumbl.SpectrumBasedLocalization;
import fk.stardust.localizer.IFaultLocalizer;
import utils.FileUtils;

/**
 * The avg of 30 runs of (a) the dataset reader, and (b) each ranking metric
 * with 0.1 as threshold as a bad case where we have to retrieve most of the
 * nodes from the ranking
 */
public class MainPerformance {

	private static final int NUMBER_OF_RUNS = 30;

	public static void main(String[] args) {
		try {
			File mainOutput = new File("output" + File.separator + "output_" + System.currentTimeMillis());
			File perfFile = new File(mainOutput, "performanceRankingMetrics.txt");
			File perfDatasetReaderFile = new File(mainOutput, "performanceDatasetReader.txt");
			perfFile.getParentFile().mkdirs();

			// read data set
			for (int i = 0; i < NUMBER_OF_RUNS; i++) {
				long start = System.currentTimeMillis();
				// Map<String, Map<String, List<Integer>>> featExec =
				DatasetReader.getFeatExec(Main.PATH_DATASET_EXECUTIONS, Main.IGNORE_NOT_ARGOUML_TRACES);
				long end = System.currentTimeMillis();
				long elapsedTime = end - start;
				String perfEntry = "DatasetReader;" + (elapsedTime / 1000.0);
				FileUtils.appendToFile(perfDatasetReaderFile, perfEntry);
			}

			Map<String, Map<String, List<Integer>>> featExec = DatasetReader.getFeatExec(Main.PATH_DATASET_EXECUTIONS,
					Main.IGNORE_NOT_ARGOUML_TRACES);

			GridSearch gridSearch = new GridSearch();
			for (IFaultLocalizer<String> algo : gridSearch.getAlgorithms()) {
				for (int i = 0; i < NUMBER_OF_RUNS; i++) {
					long start = System.currentTimeMillis();
					System.out.println("\n CURRENT SBL ALGO: " + algo.getName());
					// no output files
					// 0.1 as a worst case of nodes to retrieve from the
					// Map<String, Map<String, List<Integer>>> results =
					SpectrumBasedLocalization.locate(featExec, algo, 0.1, null);
					long end = System.currentTimeMillis();
					long elapsedTime = end - start;
					String perfEntry = algo.getName() + ";" + (elapsedTime / 1000.0);
					FileUtils.appendToFile(perfFile, perfEntry);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
