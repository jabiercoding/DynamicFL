package dynamicfl.metrics.scores;

import java.util.HashMap;

import dynamicfl.metrics.granularity.Granularity;

/**
 * Data class that holds a collection of three scores (Values) based on the level of granularity.
 * @author brunomachado
 */
public class ScoreTriplet {
	
	HashMap<Granularity, Values> values;
	
	
	/**
	 * Data class containing three scores: Precision, Recall and F1
	 */
	class Values {
		public String precision;
		public String recall;
		public String f1;
		
		public Values(String precision, String recall, String f1) {
			this.precision = precision;
			this.recall = recall;
			this.f1 = f1;
		}
	}
	
	public ScoreTriplet() {
		this.values = new HashMap<>();
	}
	
	public void add(Granularity granularity, double precision, double recall, double f1) {
		this.values.put(granularity, new Values(Double.toString(precision), Double.toString(recall), Double.toString(f1)));
	}
	
	public String getPrecision(Granularity granularity) {
		return this.values.get(granularity).precision;
	}
	
	public String getRecall(Granularity granularity) {
		return this.values.get(granularity).recall;
	}
	
	public String getF1(Granularity granularity) {
		return this.values.get(granularity).f1;
	}
	
	
}
