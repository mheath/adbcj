package org.safehaus.adbcj.tck;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import org.safehaus.adbcj.Connection;
import org.safehaus.adbcj.ConnectionManager;
import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.DbFuture;
import org.safehaus.adbcj.DbListener;
import org.safehaus.adbcj.ResultSet;
import org.safehaus.adbcj.Row;
import org.testng.Assert;
import org.testng.annotations.Test;


// TODO Write test for result set metadata
public class SelectTest extends ConnectionManagerDataProvider {

	@Test(dataProvider="connectionManagerDataProvider", timeOut=5000)
	public void testSimpleSelect(ConnectionManager connectionManager) throws DbException, InterruptedException {
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
	
}
