package dynamicfl.gabrielaExec.gridSearch;

import fk.stardust.localizer.sbfl.AbstractSpectrumBasedFaultLocalizer;

public class Configuration {
	public AbstractSpectrumBasedFaultLocalizer<String> algo;
	public double threshold_sbfl;

	public Configuration(AbstractSpectrumBasedFaultLocalizer<String> algo, double threshold_sbfl) {
		this.algo = algo;
		this.threshold_sbfl = threshold_sbfl;
	}

}
