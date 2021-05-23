package dynamicfl.scores;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dynamicfl.granularity.Granularity;

/**
 * It organises the output scores from the dynamic feature localisation computation.
 * It writes the output scores into a CSV file.
 * @author brunomachado
 */
public class ScoresBuilder {
	String[] header;
	HashMap<String, List<Row>> rows;

	/**
	 * Data class representing a row in the output CSV file.
	 * It contains the SBFL algorithm, the specific threshold and the three scores: Precision, Recall and F1.
	 */
	class Row {
		String algo;
		String threshold;
		String precision;
		String recall;
		String f1;

		public Row(String algo, String threshold, String precision, String recall, String f1) {
			this.algo = algo;
			this.threshold = threshold;
			this.precision = precision;
			this.recall = recall;
			this.f1 = f1;
		}

		public String toString(String delim) {
			return algo + delim + threshold + delim + precision + delim + recall + delim + f1;
		}
	}

	public ScoresBuilder() {
		this.header = new String[] { "Algorithm", "Threshold", "Precision", "Recall", "F1"};
		this.rows = new HashMap<>();
	}

	/**
	 * Adds a row of results containing the algorithm, threshold and the respective scores.
	 * 
	 * @param algo Spectrum based fault localisation algorithm
	 * @param threshold Feature Localisation threshold
	 * @param score Triplet score: Precision, Recall and F1
	 */
	public void add(String algo, String threshold, ScoreTriplet score) {
		for (Granularity granularity : score.values.keySet()) {
			String path = granularity.outputPathName();
			
			if (!this.rows.containsKey(path))
				this.rows.put(path, new ArrayList<Row>());
			
			String precision = score.getPrecision(granularity);
			String recall = score.getRecall(granularity);
			String f1 = score.getF1(granularity);
					
			this.rows.get(path).add(new Row(algo, threshold, precision, recall, f1));
		}
		
	}

	/**
	 * Write all the scores into CSV Files.
	 * Each CSV file shows the results for a specific granularity.
	 * 
	 * @param base Directory File to write the results CSV files
	 */
	public void toCSV(File base) {
		for (String filename : this.rows.keySet()) {
			File output;
			if (base.isDirectory())
				output = new File(base, filename);
			else
				output = new File(base.getParentFile(), filename);
			
			try {
				PrintWriter writer = new PrintWriter(output, "UTF-8");
				writer.println(header[0] + "," + header[1] + "," + header[2] + "," + header[3] + "," + header[4]);
				for (Row row : this.rows.get(filename)) {
					writer.println(row.toString(","));
				}
				writer.close();
			} catch (Exception  e) {
				e.printStackTrace();
			}
		}
	}
}
