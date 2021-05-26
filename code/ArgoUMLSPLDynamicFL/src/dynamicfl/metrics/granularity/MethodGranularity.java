package dynamicfl.metrics.granularity;

public class MethodGranularity implements Granularity {

	static final int PRECISION = 6;
	static final int RECALL = 7;
	static final int F1 = 8;
	static final String PATH = "methodLevelData.txt";
	
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
