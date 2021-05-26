package dynamicfl.spectrumbl;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import fk.stardust.localizer.IFaultLocalizer;
import fk.stardust.localizer.sbfl.Wong1;
import fk.stardust.localizer.sbfl.Wong2;

/**
 * Testing the spectrum based localization
 */
public class SpectrumBasedLocalizationTest {

	@Test
	public void testMySpectraProvider() {

		Map<String, Map<String, List<Integer>>> featExec = MySpectraProviderTest.getSimpleFeatExec();

		// Wong1 e_f
		IFaultLocalizer<String> wong1 = new Wong1<String>();
		Map<String, Map<String, List<Integer>>> location = SpectrumBasedLocalization.locate(featExec, wong1, 1.0, null);
		Assert.assertEquals(2, location.get("A").size()); // c2=[8, 7, 6, 5], c1=[4, 3, 2, 1]
		Assert.assertEquals(4, location.get("A").get("c1").size());
		Assert.assertEquals(4, location.get("A").get("c2").size());
		Assert.assertNull(location.get("A").get("c3"));
		Assert.assertEquals(3, location.get("B").size()); // c3=[10], c2=[9, 7, 6, 5], c1=[4, 3, 2, 1]
		Assert.assertEquals(4, location.get("B").get("c1").size());
		Assert.assertEquals(4, location.get("B").get("c2").size());
		Assert.assertEquals(1, location.get("B").get("c3").size());

		// Wong2 e_f - e_p
		IFaultLocalizer<String> wong2 = new Wong2<String>();
		location = SpectrumBasedLocalization.locate(featExec, wong2, 1.0, null);
		Assert.assertEquals(1, location.get("A").size()); // c2=[8]
		Assert.assertNull(location.get("A").get("c1"));
		Assert.assertEquals(1, location.get("A").get("c2").size());
		Assert.assertNull(location.get("A").get("c3"));
		Assert.assertEquals(2, location.get("B").size()); // c3=[10], c2=[9]
		Assert.assertNull(location.get("B").get("c1"));
		Assert.assertEquals(1, location.get("B").get("c2").size());
		Assert.assertEquals(1, location.get("B").get("c3").size());

		// Threshold 0.0
		location = SpectrumBasedLocalization.locate(featExec, wong2, 0.0, null);
		Assert.assertEquals(3, location.get("A").size()); // c3=[10], c2=[9, 7, 6, 5, 8], c1=[4, 3, 2, 1]
		Assert.assertEquals(3, location.get("B").size()); // c3=[10], c2=[9, 7, 6, 5, 8], c1=[4, 3, 2, 1]

	}

}
