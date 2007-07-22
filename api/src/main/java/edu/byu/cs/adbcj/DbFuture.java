package edu.byu.cs.adbcj;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 
 * @author Mike Heath
 *
 * @param <T>  The results type returned by this future's {@link #get()} method.
 */
public interface DbFuture<T> extends Future<T> {

	DbFuture<T> addListener(DbListener<T> listener);
	
	boolean removeListener(DbListener<T> listener);
	
	public T get() throws DbException, InterruptedException;
	
	public T get(long timeout, TimeUnit unit) throws DbException, InterruptedException, TimeoutException;
	
}
