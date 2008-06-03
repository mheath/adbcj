/*
 *   Copyright (c) 2007 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.adbcj.tck.test;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbException;
import org.adbcj.DbFuture;
import org.adbcj.DbListener;
import org.adbcj.DbSessionFuture;
import org.adbcj.ResultSet;
import org.adbcj.Row;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;


// TODO Write test for result set metadata
@Test(invocationCount=50, threadPoolSize=10, timeOut = 50000)
public class SelectTest {

	private ConnectionManager connectionManager;
	
	@Parameters({"url", "user", "password"})
	@BeforeTest
	public void createConnectionManager(String url, String user, String password) {
		connectionManager = ConnectionManagerProvider.createConnectionManager(url, user, password);
	}

	@AfterTest
	public void closeConnectionManager() {
		DbFuture<Void> closeFuture = connectionManager.close(true);
		closeFuture.getUninterruptably();
	}

	public void testSimpleSelect() throws DbException, InterruptedException {
		final boolean[] callbacks = {false};
		final CountDownLatch latch = new CountDownLatch(callbacks.length); 
		
		Connection connection = connectionManager.connect().get();
		try {
			ResultSet resultSet = connection.executeQuery("SELECT int_val, str_val FROM simple_values ORDER BY int_val").addListener(new DbListener<ResultSet>() {
				public void onCompletion(DbFuture<ResultSet> future) throws Exception {
					System.out.println("In callback");
					future.get().size();
					callbacks[0] = true;
					latch.countDown();
					System.out.println("Finished callback");
				}
			}).get();
			
			Assert.assertEquals(6, resultSet.size());
			
			Iterator<Row> i = resultSet.iterator();
			
			Row nullRow = null;
			Row row = i.next();
			if (row.get(0).isNull()) {
				nullRow = row;
				row = i.next();
			}
			Assert.assertEquals(row.get(0).getInt(), 0);
			Assert.assertEquals(row.get(1).getValue(), "Zero");
			row = i.next();
			Assert.assertEquals(row.get(0).getInt(), 1);
			Assert.assertEquals(row.get(1).getValue(), "One");
			row = i.next();
			Assert.assertEquals(row.get(0).getInt(), 2);
			Assert.assertEquals(row.get(1).getValue(), "Two");
			row = i.next();
			Assert.assertEquals(row.get(0).getInt(), 3);
			Assert.assertEquals(row.get(1).getValue(), "Three");
			row = i.next();
			Assert.assertEquals(row.get(0).getInt(), 4);
			Assert.assertEquals(row.get(1).getValue(), "Four");
	
			if (i.hasNext() && nullRow == null) {
				nullRow = i.next();
			}
			
			Assert.assertEquals(nullRow.get(0).getValue(), null);
			Assert.assertEquals(nullRow.get(1).getValue(), null);
	
			
			Assert.assertTrue(!i.hasNext(), "There were too many rows in result set");
			
			latch.await();
			Assert.assertTrue(callbacks[0], "Result set callback was not invoked");
		} finally {
			connection.close(true);
		}
	}
	
	public void testMultipleSelectStatements() throws Exception {
		Connection connection = connectionManager.connect().get();
		
		List<DbFuture<ResultSet>> futures = new LinkedList<DbFuture<ResultSet>>();
		for (int i = 0; i < 1000; i++) {
			futures.add(
					connection.executeQuery(String.format("SELECT *, %d FROM simple_values", i))
					);
		}
		
		for (DbFuture<ResultSet> future : futures) {
			try {
				future.get(5, TimeUnit.MINUTES);
			} catch (TimeoutException e) {
				throw new AssertionError("Timed out waiting on future: " + future);
			}
		}
	}
	
	public void testBrokenSelect() throws Exception {
		Connection connection = connectionManager.connect().get();
		
		DbSessionFuture<ResultSet> future = connection.executeQuery("SELECT broken_query");
		try {
			future.get(5, TimeUnit.SECONDS);
			throw new AssertionError("Issues a bad query, future should have failed");
		} catch (DbException e) {
			// Pass
		} finally {
			connection.close(true).get();
		}
	}
	
}
