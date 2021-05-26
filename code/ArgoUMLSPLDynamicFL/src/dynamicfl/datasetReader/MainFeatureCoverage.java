package dynamicfl.datasetReader;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import dynamicfl.Main;
import dynamicfl.metrics.MetricsModule;

public class MainFeatureCoverage {

	public static void main(String[] args) {
		try {
			File dataset = new File(Main.PATH_DATASET_EXECUTIONS);
			if (!dataset.exists()) {
				System.out.println(
						Main.PATH_DATASET_EXECUTIONS + " does not exist. Modify the path in dynamicfl.Main.java");
			}

			// read data set
			Map<String, Map<String, List<Integer>>> featExec = DatasetReader.getFeatExec(Main.PATH_DATASET_EXECUTIONS,
					Main.IGNORE_NOT_ARGOUML_TRACES);

			// Ratio of lines of a feature with the lines executed
			for (Entry<String, Map<String, List<Integer>>> feat : featExec.entrySet()) {
				System.out.println("Feature: " + feat.getKey());
				File fileSrc = new File(Main.PATH_DATASET_EXECUTIONS + File.separator + feat.getKey() + ".config"
						+ File.separator + "src");
				Map<String, List<Integer>> result = MetricsModule.transformToAbsPathAndLines(feat.getKey(), fileSrc,
						feat.getValue());

				FeatureCoverage.ratioLinesFeatureLinesExecuted(result, feat.getKey(),
						Main.PATH_DATASET_LINE_LEVEL_GROUND_TRUTH, Main.PATH_DATASET_EXECUTIONS);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
