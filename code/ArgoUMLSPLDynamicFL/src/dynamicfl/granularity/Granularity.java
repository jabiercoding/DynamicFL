package dynamicfl.granularity;

public interface Granularity {
	public int precision();
	public int recall();
	public int f1();
	public String outputPathName();
}
