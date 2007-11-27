package org.safehaus.adbcj.support;

import java.util.LinkedList;
import java.util.List;

import org.safehaus.adbcj.DbException;
import org.safehaus.adbcj.DbSessionFuture;
import org.safehaus.adbcj.Field;
import org.safehaus.adbcj.ResultEventHandler;
import org.safehaus.adbcj.ResultSet;
import org.safehaus.adbcj.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTransactionalSession extends AbstractSessionRequestQueue {
	
	private final Logger logger = LoggerFactory.getLogger(AbstractTransactionalSession.class);
	
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
		DbSessionFuture<Void> future;
		if (transaction.isBeginScheduled()) {
			future = enqueueCommit(transaction);
			return future;
		} else {
			// If transaction was not started, don't worry about committing transaction
			future = DefaultDbSessionFuture.createCompletedFuture(this, null);
		}
		transaction = null;
		return future;
	}
	
	public synchronized DbSessionFuture<Void> rollback() {
		checkClosed();
		if (!isInTransaction()) {
			throw new DbException(this, "Not currently in a transaction, cannot rollback");
		}
		DbSessionFuture<Void> future;
		if (transaction.isBeginScheduled()) {
			transaction.cancelPendingRequests();
			future = enqueueRollback(transaction);
		} else {
			future = DefaultDbSessionFuture.createCompletedFuture(this, null);
		}
		transaction = null;
		return future;
	}

	protected synchronized <E> DbSessionFuture<E> enqueueTransactionalRequest(Request<E> request) {
		// Check to see if we're in a transaction
		if (transaction != null) {
			if (transaction.isCanceled()) {
				return DefaultDbSessionFuture.createCompletedErrorFuture(
						this, new DbException(this, "Could not execute request; transaction is in failed state"));
			}
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
	
	private DbSessionFuture<Void> enqueueStartTransaction(final Transaction transaction) {
		Request<Void> request = createBeginRequest(transaction);
		return enqueueTransactionalRequest(transaction, request);
	}

	protected Request<Void> createBeginRequest(final Transaction transaction) {
		return new BeginRequest(transaction);
	}
	
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
	
	public DbSessionFuture<ResultSet> executeQuery(String sql) {
		ResultEventHandler<DefaultResultSet> eventHandler = new ResultEventHandler<DefaultResultSet>() {
			public void startFields(DefaultResultSet accumulator) {
				logger.trace("ResultSetEventHandler: startFields");
			}
			public void field(Field field, DefaultResultSet accumulator) {
				logger.trace("ResultSetEventHandler: field");
				accumulator.addField(field);
			}
			public void endFields(DefaultResultSet accumulator) {
				logger.trace("ResultSetEventHandler: endFields");
			}
			public void startResults(DefaultResultSet accumulator) {
				logger.trace("ResultSetEventHandler: startResults");
			}
			public void startRow(DefaultResultSet accumulator) {
				logger.trace("ResultSetEventHandler: startRow");

				int columnCount = accumulator.getFields().size();
				Value[] values = new Value[columnCount];
				DefaultRow row = new DefaultRow(accumulator, values);
				accumulator.addResult(row);
			}
			public void value(Value value, DefaultResultSet accumulator) {
				logger.trace("ResultSetEventHandler: value");
				
				DefaultRow lastRow = (DefaultRow)accumulator.get(accumulator.size() - 1);
				lastRow.getValues()[value.getField().getIndex()] = value;
			}
			public void endRow(DefaultResultSet accumulator) {
				logger.trace("ResultSetEventHandler: endRow");
			}
			public void endResults(DefaultResultSet accumulator) {
				logger.trace("ResultSetEventHandler: endResults");
			}
			public void exception(Throwable t, DefaultResultSet accumulator) {
			}
		};
		DefaultResultSet resultSet = new DefaultResultSet(this);
		return executeQuery0(sql, eventHandler, resultSet);
	}

	@SuppressWarnings("unchecked")
	private <T extends ResultSet> DbSessionFuture<ResultSet> executeQuery0(String sql, ResultEventHandler<T> eventHandler, T accumulator) {
		return (DbSessionFuture<ResultSet>)executeQuery(sql, eventHandler, accumulator);
	}
	
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

	public class Transaction {

		private volatile boolean started = false;
		private volatile boolean beginScheduled = false;
		private volatile boolean canceled = false;
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
			request.setTransaction(this);
			synchronized (AbstractTransactionalSession.this) {
				requests.add(request);
			}
		}
		
		public boolean isCanceled() {
			return canceled;
		}
		
		public void cancelPendingRequests() {
			canceled = true;
			synchronized (AbstractTransactionalSession.this) {
				for (Request<?> request : requests) {
					request.getFuture().cancel(false);
				}
			}
		}
		
	}

}
