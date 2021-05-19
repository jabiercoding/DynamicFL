package dynamicfl.granularity;

public class LineGranularity implements Granularity {

	static final int PRECISION = 9;
	static final int RECALL = 10;
	static final int F1 = 11;
	static final String PATH = "lineLevelData.txt";
	
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
