package dynamicfl.gabrielaExec.gridSearch;

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

public class GridSearch implements Iterable<Configuration> {
	List<AbstractSpectrumBasedFaultLocalizer<String>> algos;
	List<Double> thresholds;
	Queue<Configuration> configurations;
	int num_trials = 50;

	public GridSearch() {
		this.initializeAlgorithms();
		this.generateThresholds();
		this.populateConfigurations();
	}

	private void initializeAlgorithms() {
		this.algos = new ArrayList<>();
		this.algos.add(new Ample<String>());
		this.algos.add(new Anderberg<String>());
		this.algos.add(new ArithmeticMean<String>());
		this.algos.add(new Cohen<String>());
		this.algos.add(new Dice<String>());
		this.algos.add(new Euclid<String>());
		this.algos.add(new Fleiss<String>());
		this.algos.add(new GeometricMean<String>());
		this.algos.add(new Goodman<String>());
		this.algos.add(new Hamann<String>());
		this.algos.add(new Hamming<String>());
		this.algos.add(new HarmonicMean<String>());
		this.algos.add(new Jaccard<String>());
		this.algos.add(new Kulczynski1<String>());
		this.algos.add(new Kulczynski2<String>());
		this.algos.add(new M1<String>());
		this.algos.add(new M2<String>());
		this.algos.add(new Ochiai<String>());
		this.algos.add(new Ochiai2<String>());
		this.algos.add(new Overlap<String>());
		this.algos.add(new RogersTanimoto<String>());
		this.algos.add(new Rogot1<String>());
		this.algos.add(new Rogot2<String>());
		this.algos.add(new RussellRao<String>());
		this.algos.add(new Scott<String>());
		this.algos.add(new SimpleMatching<String>());
		this.algos.add(new Sokal<String>());
		this.algos.add(new SorensenDice<String>());
		this.algos.add(new Tarantula<String>());
		this.algos.add(new Wong1<String>());
		this.algos.add(new Wong2<String>());
		this.algos.add(new Wong3<String>());
		this.algos.add(new Zoltar<String>());

	}

	private void generateThresholds() {
		this.thresholds = new ArrayList<>();
		double step = (1.0 / this.num_trials);
		for (int trial = 0; trial < this.num_trials; trial++) {
			this.thresholds.add(trial * step);
		}
	}

	private void populateConfigurations() {
		this.configurations = new LinkedList<>();
		for (AbstractSpectrumBasedFaultLocalizer<String> algo : this.algos) {
			for (Double threshold_sbfl : this.thresholds) {
				this.configurations.add(new Configuration(algo, threshold_sbfl));
			}
		}
	}

	@Override
	public Iterator<Configuration> iterator() {
		return this.configurations.iterator();
	}

}
