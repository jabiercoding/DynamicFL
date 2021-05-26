package dynamicfl.metrics.granularity;

/**
 * Defines the granularity for the desired level of instrumentation, for instance line or method level.
 * @author brunomachado
 */
public interface Granularity {
	/**
	 * @return The index of the precision column in the MetricsScenarioFeature.csv files.
	 */
	public int precision();
	
	/**
	 * @return The index of the precision recall in the MetricsScenarioFeature.csv files.
	 */
	public int recall();
	
	/**
	 * @return The index of the f1 column in the MetricsScenarioFeature.csv files.
	 */
	public int f1();
	
	/**
	 * @return The name of the file to write the output scores in each specific granularity level.
	 */
	public String outputPathName();
}
