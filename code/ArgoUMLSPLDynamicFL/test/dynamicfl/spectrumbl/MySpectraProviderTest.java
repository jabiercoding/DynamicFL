package dynamicfl.spectrumbl;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import fk.stardust.traces.ISpectra;

/**
 * Testing the spectra provider
 */
public class MySpectraProviderTest {

	@Test
	public void testMySpectraProvider() {
		try {
			Map<String, Map<String, List<Integer>>> featExec = getSimpleFeatExec();

			MySpectraProvider provider = new MySpectraProvider("A", featExec);
			ISpectra<String> spectra = provider.loadSpectra();

			Assert.assertEquals(1, spectra.getFailingTraces().size());
			Assert.assertEquals(1, spectra.getSuccessfulTraces().size());
			Assert.assertEquals(10, spectra.getNodes().size());
			Assert.assertEquals(2, spectra.getTraces().size());
			Assert.assertTrue(spectra.hasNode("c1;1"));
			Assert.assertTrue(spectra.hasNode("c3;10"));
			Assert.assertFalse(spectra.hasNode("c2;1"));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a basic feat exec example
	 * 
	 * @return featExec
	 */
	public static Map<String, Map<String, List<Integer>>> getSimpleFeatExec() {
		new LinkedHashMap<String, Map<String, List<Integer>>>();
		Map<String, Map<String, List<Integer>>> featExec = new LinkedHashMap<String, Map<String, List<Integer>>>();
		Map<String, List<Integer>> classLinesA = new LinkedHashMap<String, List<Integer>>();
		classLinesA.put("c1", Arrays.asList(1, 2, 3, 4));
		classLinesA.put("c2", Arrays.asList(5, 6, 7, 8));
		featExec.put("A", classLinesA);

		Map<String, List<Integer>> classLinesB = new LinkedHashMap<String, List<Integer>>();
		classLinesB.put("c1", Arrays.asList(1, 2, 3, 4));
		classLinesB.put("c2", Arrays.asList(5, 6, 7, 9));
		classLinesB.put("c3", Arrays.asList(10));
		featExec.put("B", classLinesB);
		return featExec;
	}
}
