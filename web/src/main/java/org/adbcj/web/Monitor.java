//*********************************************************************
//Copyright 2010 Intellectual Reserve, Inc. All rights reserved.
//This notice may not be removed.
//*********************************************************************
package org.adbcj.web;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Executor;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class Monitor implements DisposableBean, InitializingBean {

	private final Executor executor;
	private volatile int count;

	private final Writer writer;
	private final Writer vmStatWriter;

	private final Process vmstat;

	private volatile boolean closed = false;

	public Monitor(Executor executor, Writer writer, final Writer vmStatWriter) throws IOException {
		this.executor = executor;
		this.writer = writer;
		this.vmStatWriter = vmStatWriter;

		ProcessBuilder builder = new ProcessBuilder("/usr/bin/vmstat", "2");
		vmstat = builder.start();
	}

	public void setCount(int count) {
		this.count = count;
		try {
			vmStatWriter.write("Count: " + count + "\n");
			vmStatWriter.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					IOUtils.copy(vmstat.getInputStream(), vmStatWriter);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		executor.execute(new Runnable() {
			@Override
			public void run() {
				Runtime runtime = Runtime.getRuntime();
				Thread.interrupted();

				while (!closed) {
				long total = runtime.totalMemory();
				long free = runtime.freeMemory();
				long max = runtime.maxMemory();

				try {
						writer.write(String.format("%d %d %d %d\n", count, total, free, max));
						writer.flush();
						Thread.sleep(500);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		});
	}

	@Override
	public void destroy() throws Exception {
		System.out.println("Shutting down monitor");
		closed = true;
		vmstat.destroy();
		vmStatWriter.flush();
		writer.flush();
	}
}
