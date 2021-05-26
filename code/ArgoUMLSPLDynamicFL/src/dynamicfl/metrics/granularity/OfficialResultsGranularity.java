package dynamicfl.metrics.granularity;

public class OfficialResultsGranularity implements Granularity {

	static final int PRECISION = 0;
	static final int RECALL = 1;
	static final int F1 = 2;
	static final String PATH = "benchData.txt";
	
	@Override
	public int precision() {
		return PRECISION;

	}

	@Override
	public int recall() {
		return RECALL;

	}

	@Override
	public int f1() {
		return F1;

	}

	@Override
	public String outputPathName() {
		return PATH;
	}

}
