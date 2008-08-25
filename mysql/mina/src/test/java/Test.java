import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbSession;
import org.adbcj.Result;
import org.adbcj.ResultSet;
import org.adbcj.Value;
import org.adbcj.mysql.mina.Adbcj;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Adbcj.init();

		ConnectionManager connectionManager = ConnectionManagerProvider.createConnectionManager("adbcj:mysql://localhost/adbcjtck", "adbcjtck", "adbcjtck");

		DbSession session = connectionManager.connect().get();

		// Clear out updates table
		Result result = session.executeUpdate("DELETE FROM updates").get();
		assertNotNull(result);

		System.out.println("Deleted");

		// Insert a row
		result = session.executeUpdate("INSERT INTO updates (id) VALUES (1)").get();
		assertNotNull(result);
		assertEquals(result.getAffectedRows(), Long.valueOf(1));

		System.out.println("Inserted");

		// Select the row
		ResultSet rs = session.executeQuery("SELECT id FROM updates").get();
		assertNotNull(rs);
		assertEquals(rs.size(), 1);
		Value value = rs.get(0).get(0);
		assertEquals(value.getInt(), 1);
		assertEquals(value.getField().getColumnLabel(), "id");

		System.out.println("Selected");

		// Update nothing
		result = session.executeUpdate("UPDATE updates SET id=1 WHERE id=2").get();
		assertNotNull(result);
		assertEquals(result.getAffectedRows(), Long.valueOf(0));

		System.out.println("Fake updated");

		// Update inserted row
		result = session.executeUpdate("UPDATE updates SET id=2").get();
		assertNotNull(result);
		assertEquals(result.getAffectedRows(), Long.valueOf(1));

		System.out.println("Real updated");

		// Delete inserted row
		result = session.executeUpdate("DELETE FROM updates WHERE id=2").get();
		assertNotNull(result);
		assertEquals(result.getAffectedRows(), Long.valueOf(1));

		System.out.println("Row deleted");

		connectionManager.close(true);
	}

}
