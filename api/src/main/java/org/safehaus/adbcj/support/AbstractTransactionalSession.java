package org.safehaus.adbcj.support;

import java.util.LinkedList;
import java.util.List;

import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.DbSessionFuture;

public abstract class AbstractTransactionalSession extends AbstractSessionRequestQueue {
	
	private volatile Transaction transaction;
	
	public boolean isInTransaction() {
		checkClosed();
		return transaction != null;
	}

	public synchronized void beginTransaction() {
		checkClosed();
		if (isInTransaction()) {
			throw new DbException(this, "Cannot begin new transaction.  Current transaction needs to be committed or rolled back");
		}
		transaction = new Transaction();
	}

	public synchronized DbSessionFuture<Void> commit() {
		checkClosed();
		if (!isInTransaction()) {
			throw new DbException(this, "Not currently in a transaction, cannot commit");
		}
		if (transaction.isBeginScheduled()) {
			DbSessionFuture<Void> future = enqueueCommit(transaction);
			transaction = null;
			return future;
		}
		DefaultDbSessionFuture<Void> future = new DefaultDbSessionFuture<Void>(this);
		future.setDone();
		transaction = null;
		return future;
	}
	
	public synchronized DbSessionFuture<Void> rollback() {
		checkClosed();
		if (!isInTransaction()) {
			throw new DbException(this, "Not currently in a transaction, cannot rollback");
		}
		transaction.cancelPendingRequests();
		if (transaction.isStarted()) {
			DbSessionFuture<Void> future = enqueueRollback(transaction);
			transaction = null;
			return future;
		}
		DefaultDbSessionFuture<Void> future = new DefaultDbSessionFuture<Void>(this);
		future.setDone();
		transaction = null;
		return future;
	}

	protected synchronized <E> DbSessionFuture<E> enqueueTransactionalRequest(Request<E> request) {
		// Check to see if we're in a transaction
		if (transaction != null) {
			// TODO Check to see if transaction is in failed state and return future that indicates the error
			// Schedule starting transaction with database if possible
			if (!transaction.isBeginScheduled()) {
				// Set isolation level if necessary
				enqueueStartTransaction(transaction);
				transaction.setBeginScheduled(true);
			}
			transaction.addRequest(request);
		}
		return enqueueRequest(request);
	}

	protected abstract void sendBegin() throws Exception;

	protected abstract void sendCommit() throws Exception;

	protected abstract void sendRollback() throws Exception;
	
	// Begin transaction enqueueing needs to be synchronized on this and needs to set transaction.setStarted(true)
	// Request must be queued up in Transaction
	private DbSessionFuture<Void> enqueueStartTransaction(final Transaction transaction) {
		Request<Void> request = createBeginRequest(transaction);
		return enqueueTransactionalRequest(transaction, request);
	}

	protected Request<Void> createBeginRequest(final Transaction transaction) {
		return new BeginRequest(transaction);
	}
	
	// Canceled commit needs to execute rollback
	private DbSessionFuture<Void> enqueueCommit(final Transaction transaction) {
		Request<Void> request = createCommitRequest(transaction);
		return enqueueTransactionalRequest(transaction, request);
		
	}

	protected Request<Void> createCommitRequest(final Transaction transaction) {
		return new CommitRequest(transaction);
	}
	
	// Rollback cannot be canceled or removed
	private DbSessionFuture<Void> enqueueRollback(Transaction transaction) {
		Request<Void> request = createRollbackRequest();
		return enqueueTransactionalRequest(transaction, request);
	}

	protected Request<Void> createRollbackRequest() {
		return new RollbackRequest();
	}

	private DbSessionFuture<Void> enqueueTransactionalRequest(final Transaction transaction, Request<Void> request) {
		DefaultDbSessionFuture<Void> future = enqueueRequest(request);
		transaction.addRequest(request);
		return future;
	}

	/**
	 * Throws DbException if session is closed
	 */
	protected abstract void checkClosed() throws DbException;
	
	protected class BeginRequest extends Request<Void> {
		private final Transaction transaction;

		private BeginRequest(Transaction transaction) {
			this.transaction = transaction;
		}

		@Override
		public void execute() throws Exception {
			synchronized (AbstractTransactionalSession.this) {
				transaction.setStarted(true);
			}
			sendBegin();
		}
	}

	protected class CommitRequest extends Request<Void> {
		private final Transaction transaction;
		private boolean executing = false;
		private boolean cancelled = false;

		private CommitRequest(Transaction transaction) {
			this.transaction = transaction;
		}

		public synchronized void execute() throws Exception {
			executing = true;
			if (cancelled) {
				if (transaction.isStarted()) {
					sendRollback();
				}
			} else {
				sendCommit();
			}
		}

		@Override
		public synchronized boolean cancel(boolean mayInterruptIfRunning) {
			// If commit has already started, it can't be stopped
			if (executing) {
				return false;
			}
			if (transaction.isStarted()) {
				return false;
			}
			// If commit is not executing, indicate that commit has been canceled and do rollback
			cancelled = true;
			transaction.cancelPendingRequests();
			return true;
		}
		
		@Override
		public boolean canRemove() {
			return false;
		}
	}

	protected class RollbackRequest extends Request<Void> {
		@Override
		public void execute() throws Exception {
			sendRollback();
		}

		@Override
		// Return false because a rollback cannot be canceled
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}
	}

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
				request.getFuture().cancel(false);
			}
		}
		
	}

}
