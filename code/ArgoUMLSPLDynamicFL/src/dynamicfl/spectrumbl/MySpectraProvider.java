package dynamicfl.spectrumbl;

import java.util.List;
import java.util.Map;

import fk.stardust.provider.ISpectraProvider;
import fk.stardust.traces.IMutableTrace;
import fk.stardust.traces.ISpectra;
import fk.stardust.traces.Spectra;

/**
 * Spectra provider that will create the spectrum based on the dataset
 */
public class MySpectraProvider implements ISpectraProvider<String> {

	String targetFeature;
	Map<String, Map<String, List<Integer>>> featExec;

	public MySpectraProvider(String targetFeature, Map<String, Map<String, List<Integer>>> featExec) {
		this.targetFeature = targetFeature;
		this.featExec = featExec;
	}

	@Override
	public ISpectra<String> loadSpectra() throws Exception {
		final Spectra<String> s = new Spectra<>();

		// one trace per feature
		for (String feature : featExec.keySet()) {
			// Successful is false if it is the target feature (this is because the library
			// is related to testing so if we want to locate a feature we need to say that
			// the test is failing in this trace)
			boolean currentFeat = feature.equals(targetFeature);
			final IMutableTrace<String> trace = s.addTrace(!currentFeat);

			Map<String, List<Integer>> classesExec = featExec.get(feature);
			for (String classKey : classesExec.keySet()) {
				List<Integer> lines = classesExec.get(classKey);
				for (Integer line : lines) {
					trace.setInvolvement(classKey + ";" + line, true);
				}
			}
		}

		return s;
	}

}
