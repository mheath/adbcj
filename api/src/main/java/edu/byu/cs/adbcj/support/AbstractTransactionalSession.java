package edu.byu.cs.adbcj.support;

import java.util.LinkedList;
import java.util.List;

import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbListener;
import edu.byu.cs.adbcj.DbSessionFuture;
import edu.byu.cs.adbcj.TransactionIsolationLevel;

public abstract class AbstractTransactionalSession extends AbstractSessionRequestQueue {
	
	private volatile Transaction transaction;
	private volatile TransactionIsolationLevel transactionIsolationLevel = TransactionIsolationLevel.READ_COMMITTED;
	private volatile TransactionIsolationLevel serverSideIsolationLevel = TransactionIsolationLevel.READ_COMMITTED;
	
	public boolean isInTransaction() {
		checkClosed();
		return transaction != null;
	}

	public synchronized void beginTransaction() {
		checkClosed();
		if (isInTransaction()) {
			throw new DbException("Cannot begin new transaction.  Current transaction needs to be committed or rolled back");
		}
		transaction = new Transaction();
	}

	public TransactionIsolationLevel getTransactionIsolationLevel() {
		checkClosed();
		return transactionIsolationLevel;
	}

	public synchronized void setTransactionIsolationLevel(TransactionIsolationLevel transactionIsolationLevel) {
		checkClosed();
		if (transactionIsolationLevel == null) {
			throw new NullPointerException("Cannot set transactionIsolationLevel to null");
		}
		if (isInTransaction() && transaction.isBeginScheduled()) {
			throw new DbException("Can't change transaction isolation level of running transaction.");
		} else {
			this.transactionIsolationLevel = transactionIsolationLevel; 
		}
	}

	public synchronized DbSessionFuture<Void> commit() {
		checkClosed();
		if (!isInTransaction()) {
			throw new DbException("Not currently in a transaction, cannot commit");
		}
		return enqueueCommit(transaction);
	}
	
	public synchronized DbSessionFuture<Void> rollback() {
		checkClosed();
		if (!isInTransaction()) {
			throw new DbException("Not currently in a transaction, cannot rollback");
		}
		transaction.cancelPendingRequests();
		if (transaction.isStarted()) {
			return enqueueRollback(transaction);
		}
		DefaultDbSessionFuture<Void> future = new DefaultDbSessionFuture<Void>(this);
		future.setDone();
		return future;
	}

	protected synchronized <E> DbSessionFuture<E> enqueueTransactionalRequest(Request<E> request) {
		// Check to see if we're in a transaction
		if (transaction != null) {
			// TODO: Check to see if transaction is in failed state and return future that indicates the error
			// Schedule starting transaction with database if possible
			if (!transaction.isBeginScheduled()) {
				// Set isolation level if necessary
				// TODO: If changing isolation level fails, we need to rollback transaction
				if (transactionIsolationLevel != serverSideIsolationLevel) {
					enqueueChangeIsolationLevel(transaction, transactionIsolationLevel).addListener(new DbListener<Void>() {
						private TransactionIsolationLevel privateIsolationLevel = transactionIsolationLevel;
						public void onCompletion(DbFuture<Void> future) throws Exception {
							if (!future.isCancelled()) {
								serverSideIsolationLevel = privateIsolationLevel;
							}
						}
					});
				}
				// TODO: If starting transaction fails, we need to indicate error
				enqueueStartTransaction(transaction);
				transaction.setBeginScheduled(true);
			}
			transaction.addRequest(request);
		}
		return enqueueRequest(request);
	}

	// Begin transaction enqueueing needs to be synchronized on this and needs to set transaction.setStarted(true)
	// Request must be queued up in Transaction
	protected abstract DbSessionFuture<Void> enqueueStartTransaction(Transaction transaction);
	
	// Canceled commit needs to execute rollback
	protected abstract DbSessionFuture<Void> enqueueCommit(Transaction transaction);
	
	// Rollback cannot be canceled or removed
	protected abstract DbSessionFuture<Void> enqueueRollback(Transaction transaction);

	// Request must be queued up in Transaction
	protected abstract DbSessionFuture<Void> enqueueChangeIsolationLevel(Transaction transaction, TransactionIsolationLevel transactionIsolationLevel);
	
	/**
	 * Throws DbException if session is closed
	 */
	protected abstract void checkClosed();
	
	protected class Transaction {

		private volatile boolean started = false;
		private volatile boolean beginScheduled = false;
		private List<Request<?>> requests = new LinkedList<Request<?>>();
		
		/**
		 * Indicates if the transaction has been started on the server (i.e. if 'begin' has been sent to server)
		 * @return
		 */
		public boolean isStarted() {
			return started;
		}
		
		public void setStarted(boolean started) {
			this.started = started;
		}
		
		/**
		 * Indicates if 'begin' has been scheduled to be sent to server but not necessarily sent.
		 * @return
		 */
		public boolean isBeginScheduled() {
			return beginScheduled;
		}

		public void setBeginScheduled(boolean beginScheduled) {
			this.beginScheduled = beginScheduled;
		}

		public List<Request<?>> getRequests() {
			return requests;
		}
		
		public void addRequest(Request<?> request) {
			requests.add(request);
		}
		
		public void cancelPendingRequests() {
			for (Request<?> request : requests) {
				request.cancel(false);
			}
		}
		
	}

}
