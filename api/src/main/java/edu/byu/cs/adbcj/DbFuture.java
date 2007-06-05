package edu.byu.cs.adbcj;
import java.util.concurrent.Future;

/**
 * TODO Add support handling exceptions
 * 
 * @author mheath
 *
 * @param <T>
 * @param <F>
 */
public interface DbFuture<T> extends Future<T> {

	Connection getConnection();
	
	void addListener(DbListener<T> listener);
	
	void removeListener(DbListener<T> listener);
	
}
