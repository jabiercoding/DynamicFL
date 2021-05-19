package dynamicfl.gabrielaExec.gridSearch;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ScoresBuilder {
	String[] header;
	List<Row> rows;

	class Row {
		String algo;
		String threshold;
		String precision;
		String recall;
		String f1;

		public Row(String algo, String threshold, double precision, double recall, double f1) {
			this.algo = algo;
			this.threshold = threshold;
			this.precision = Double.toString(precision);
			this.recall = Double.toString(recall);
			this.f1 = Double.toString(f1);
		}

		public String toString(String delim) {
			return algo + delim + threshold + delim + precision + delim + recall + delim + f1;
		}
	}

	public ScoresBuilder() {
		this.header = new String[] { "Algorithm", "Threshold", "Precision", "Recall", "F1"};
		this.rows = new ArrayList<Row>();
	}

	public void add(String algo, String threshold, double precision, double recall, double f1) {
		rows.add(new Row(algo, threshold, precision, recall, f1));
	}

	@Override
	public String toString() {
		String scores = header[0] + "\t" + header[1] + "\t" + header[2] + "\t" + header[3] + "\n" + header[4] + "\n";
		for (Row row : rows) {
			scores += row.toString("\t");
		}
		return scores;
	}

	public void toCSV(File file) {
		try {
			PrintWriter writer = new PrintWriter(file, "UTF-8");
			writer.println(header[0] + "," + header[1] + "," + header[2] + "," + header[3] + "," + header[4] + "\n");
			for (Row row : rows) {
				writer.println(row.toString(","));
			}
			writer.close();
		} catch (Exception  e) {
			e.printStackTrace();
		}
	}

}
