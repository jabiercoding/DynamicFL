package dynamicfl.gridSearch;

import fk.stardust.localizer.IFaultLocalizer;

/**
 * Data class that contains a spectrum-based fault localisation algorithm and a threshold.
 * Defines the configuration required to compute the dynamic feature localisation results.
 * @author brunomachado
 */
public class Configuration {
	public IFaultLocalizer<String> algo;
	public double threshold_sbfl;

	public Configuration(IFaultLocalizer<String> algo, double threshold_sbfl) {
		this.algo = algo;
		this.threshold_sbfl = threshold_sbfl;
	}
	
	@Override
	public String toString() {
		return algo.getName() + " " + Double.toString(threshold_sbfl);
	}

}
