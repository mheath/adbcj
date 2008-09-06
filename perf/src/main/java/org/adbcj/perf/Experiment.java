package org.adbcj.perf;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.io.PrintStream;

/**
 *
 */
public abstract class Experiment implements Runnable {

	public static final String USER_NAME = "adbcjtck";
	public static final String PASSWORD = "adbcjtck";

	private final Configuration configuration;
	private final String host;

	private final DescriptiveStatistics timings = new DescriptiveStatistics();

	private Group group;

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public Experiment(Configuration configuration, String host) {
		this.configuration = configuration;
		this.host = host;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	protected String getUrl() {
		return configuration.getUrl(host);
	}

	public double getAverage() {
		return timings.getMean();
	}

	public double getStdDev() {
		return timings.getStandardDeviation();
	}

	public double getMax() {
		return timings.getMax();
	}

	public double getMin() {
		return timings.getMin();
	}

	public void run() {
		try {
			init();
			long start = System.nanoTime();
			execute();
			long end = System.nanoTime();
			timings.addValue(end - start);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			cleanup();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public abstract void init() throws Exception;

	public abstract void execute() throws Exception;

	public abstract void cleanup() throws Exception;

	public void dump(PrintStream out, boolean details) {
		out.println(configuration);
		out.printf("Average: %.03f\n", getAverage());
		out.printf("Stdev:   %.03f\n", getStdDev());
		out.printf("Min:     %.03f\n", getMin());
		out.printf("Max:     %.03f\n", getMax());
		if (details) {
			for (double timing : timings.getValues()) {
				out.println(timing);
			}
		}
	}

	public void reset() {
		timings.clear();
	}
}
