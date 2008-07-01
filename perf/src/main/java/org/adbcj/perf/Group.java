package org.adbcj.perf;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.io.PrintStream;

/**
 * A group of experiments, experiments all doing the same thing but done with different configurations.
 */
public class Group {

	private final String name;
	private final List<Experiment> experiments = new ArrayList<Experiment>();

	public Group(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void addExperiment(Experiment experiment) {
		for (Experiment e : experiments) {
			if (e.getConfiguration() == experiment.getConfiguration()) {
				throw new IllegalStateException("We already have a configuration of that type");
			}
		}
		experiments.add(experiment);
	}

	public Collection<Experiment> getExperiments() {
		return Collections.unmodifiableCollection(experiments);
	}

	public void dump(PrintStream out, boolean details) {
		out.println(name);
		for (Experiment experiment : experiments) {
			out.println();
			experiment.dump(out, details);
		}

		out.println();
	}
}
