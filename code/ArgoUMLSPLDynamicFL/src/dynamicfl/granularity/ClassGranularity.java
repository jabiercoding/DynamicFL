package dynamicfl.granularity;

public class ClassGranularity implements Granularity {

	static final int PRECISION = 3;
	static final int RECALL = 4;
	static final int F1 = 5;
	static final String PATH = "classLevelData.txt";
	
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
