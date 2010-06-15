//*********************************************************************
//Copyright 2010 Intellectual Reserve, Inc. All rights reserved.
//This notice may not be removed.
//*********************************************************************
package org.adbcj.web;

import org.springframework.beans.factory.InitializingBean;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.concurrent.Executor;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class Monitor implements Runnable, InitializingBean {

	private final Executor executor;
	private final int count;

	private final Writer writer;

	public Monitor(Executor executor, int count, Writer writer) {
		this.executor = executor;
		this.count = count;
		this.writer = writer;
	}

	@Override
	public void run() {
		Runtime runtime = Runtime.getRuntime();
		long total = runtime.totalMemory();
		long free = runtime.freeMemory();
		long max = runtime.maxMemory();

		try {
			writer.write(String.format("%d %d %d %d\n", count, total, free, max));
			writer.flush();
			Thread.sleep(500);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		executor.execute(this);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		executor.execute(this);
	}
}
