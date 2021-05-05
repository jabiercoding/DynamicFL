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
		String score;
		String value;

		public Row(String algo, String threshold, String score, double value) {
			this.algo = algo;
			this.threshold = threshold;
			this.score = score;
			this.value = Double.toString(value);
		}

		public String toString(String delim) {
			return algo + delim + threshold + delim + score + delim + value;
		}
	}

	public ScoresBuilder() {
		this.header = new String[] { "Algorithm", "Threshold", "Score", "Value" };
		this.rows = new ArrayList<Row>();
	}

	public void add(String algo, String threshold, String score, double value) {
		rows.add(new Row(algo, threshold, score, value));
	}

	@Override
	public String toString() {
		String scores = header[0] + "\t" + header[1] + "\t" + header[2] + "\t" + header[3] + "\n";
		for (Row row : rows) {
			scores += row.toString("\t");
		}
		return scores;
	}

	public void toCSV(File file) {
		try {
			PrintWriter writer = new PrintWriter(file, "UTF-8");
			writer.println(header[0] + "," + header[1] + "," + header[2] + "," + header[3]);
			for (Row row : rows) {
				writer.println(row.toString(","));
			}
			writer.close();
		} catch (Exception  e) {
			e.printStackTrace();
		}
	}

}
