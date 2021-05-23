package dynamicfl.gabrielaExec.gridSearch;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import fk.stardust.localizer.sbfl.AbstractSpectrumBasedFaultLocalizer;
import fk.stardust.localizer.sbfl.Ample;
import fk.stardust.localizer.sbfl.Anderberg;
import fk.stardust.localizer.sbfl.ArithmeticMean;
import fk.stardust.localizer.sbfl.Cohen;
import fk.stardust.localizer.sbfl.Dice;
import fk.stardust.localizer.sbfl.Euclid;
import fk.stardust.localizer.sbfl.Fleiss;
import fk.stardust.localizer.sbfl.GeometricMean;
import fk.stardust.localizer.sbfl.Goodman;
import fk.stardust.localizer.sbfl.Hamann;
import fk.stardust.localizer.sbfl.Hamming;
import fk.stardust.localizer.sbfl.HarmonicMean;
import fk.stardust.localizer.sbfl.Jaccard;
import fk.stardust.localizer.sbfl.Kulczynski1;
import fk.stardust.localizer.sbfl.Kulczynski2;
import fk.stardust.localizer.sbfl.M1;
import fk.stardust.localizer.sbfl.M2;
import fk.stardust.localizer.sbfl.Ochiai;
import fk.stardust.localizer.sbfl.Ochiai2;
import fk.stardust.localizer.sbfl.Overlap;
import fk.stardust.localizer.sbfl.RogersTanimoto;
import fk.stardust.localizer.sbfl.Rogot1;
import fk.stardust.localizer.sbfl.Rogot2;
import fk.stardust.localizer.sbfl.RussellRao;
import fk.stardust.localizer.sbfl.Scott;
import fk.stardust.localizer.sbfl.SimpleMatching;
import fk.stardust.localizer.sbfl.Sokal;
import fk.stardust.localizer.sbfl.SorensenDice;
import fk.stardust.localizer.sbfl.Tarantula;
import fk.stardust.localizer.sbfl.Wong1;
import fk.stardust.localizer.sbfl.Wong2;
import fk.stardust.localizer.sbfl.Wong3;
import fk.stardust.localizer.sbfl.Zoltar;

/**
 * This class enables a brute force exploration of algorithms and thresholds in the FL calculation.
 * 
 * It generates and iterates each configuration for the Dynamic Feature localisation calculation.
 * A configuration includes a specific spectrum based fault localisation algorithm and a threshold.
 * @author brunomachado
 */
public class GridSearch implements Iterable<Configuration> {
	List<AbstractSpectrumBasedFaultLocalizer<String>> algos;
	List<Double> thresholds;
	Queue<Configuration> configurations;
	int num_trials = 10;

	public GridSearch() {
		initializeAlgorithms();
		generateThresholds();
		populateConfigurations();
	}

	private void initializeAlgorithms() {
		algos = new ArrayList<>();
		algos.add(new Ample<String>());
		algos.add(new Anderberg<String>());
		algos.add(new ArithmeticMean<String>());
		algos.add(new Cohen<String>());
		algos.add(new Dice<String>());
		algos.add(new Euclid<String>());
		algos.add(new Fleiss<String>());
		algos.add(new GeometricMean<String>());
		algos.add(new Goodman<String>());
		algos.add(new Hamann<String>());
		algos.add(new Hamming<String>());
		algos.add(new HarmonicMean<String>());
		algos.add(new Jaccard<String>());
		algos.add(new Kulczynski1<String>());
		algos.add(new Kulczynski2<String>());
		algos.add(new M1<String>());
		algos.add(new M2<String>());
		algos.add(new Ochiai<String>());
		algos.add(new Ochiai2<String>());
		algos.add(new Overlap<String>());
		algos.add(new RogersTanimoto<String>());
		algos.add(new Rogot1<String>());
		algos.add(new Rogot2<String>());
		algos.add(new RussellRao<String>());
		algos.add(new Scott<String>());
		algos.add(new SimpleMatching<String>());
		algos.add(new Sokal<String>());
		algos.add(new SorensenDice<String>());
		algos.add(new Tarantula<String>());
		algos.add(new Wong1<String>());
		algos.add(new Wong2<String>());
		algos.add(new Wong3<String>());
		algos.add(new Zoltar<String>());

	}

	/*
	 * Based on the number of trials (N), it will generate equidistant N thresholds between 0 and 1.
	 * 
	 *  Example:
	 *  If number of trials is (10), 
	 *  	it will generate { 1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1 }
	 */
	private void generateThresholds() {
//		thresholds = Arrays.asList(1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1);
		this.thresholds = new ArrayList<>();
		double step = (1.0 / num_trials);
		for (int trial = 0; trial < num_trials; trial++) {
			double threshold = trial * step;
			// (1 - threshold) so we start with 1 and then we go descending
			this.thresholds.add(round(1 - threshold));
		}
	}

	private Double round(Double value) {
		DecimalFormat df = new DecimalFormat("#.#");
		df.setRoundingMode(RoundingMode.HALF_UP);
		return Double.parseDouble(df.format(value));
	}	

	private void populateConfigurations() {
		configurations = new LinkedList<>();
		for (AbstractSpectrumBasedFaultLocalizer<String> algo : algos) {
			for (Double threshold_sbfl : thresholds) {
				configurations.add(new Configuration(algo, threshold_sbfl));
			}
		}
	}

	@Override
	public Iterator<Configuration> iterator() {
		return configurations.iterator();
	}

}
