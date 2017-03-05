import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbException;


public class Test {

	public static void main(String[] args) throws DbException, Exception {
		ConnectionManager cm = ConnectionManagerProvider.createConnectionManager("adbcj:pooledMysqlnetty://localhost/test", "foo", "dawg");
		Connection connection = cm.connect().get();
		connection.close(true);
	}

}
