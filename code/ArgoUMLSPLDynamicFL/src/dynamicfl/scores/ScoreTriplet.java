package dynamicfl.scores;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import dynamicfl.granularity.Granularity;

public class ScoreTriplet {
	
	HashMap<Granularity, Values> values;
	
	
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
		this.values.put(granularity, new Values(round(precision), round(recall), round(f1)));
	}
	
	private String round(double value) {
		DecimalFormat df = new DecimalFormat("#.###");
		df.setRoundingMode(RoundingMode.CEILING);
		return df.format(value);
		
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
