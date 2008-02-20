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
package org.adbcj.support;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.adbcj.DbException;
import org.adbcj.DbSession;
import org.adbcj.DbSessionClosedException;
import org.adbcj.DbSessionFuture;
import org.adbcj.Field;
import org.adbcj.ResultEventHandler;
import org.adbcj.ResultSet;
import org.adbcj.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDbSession implements DbSession {
	
	private final Logger logger = LoggerFactory.getLogger(AbstractDbSession.class);

	protected final Object lock = this;
	
	private final Queue<Request<?>> requestQueue = new LinkedList<Request<?>>(); // Access must by synchronized on lock
	
	private Request<?> activeRequest; // Access must by synchronized on lock
	
	private Transaction transaction; // Access must by synchronized on lock
	
	protected <E> void enqueueRequest(final Request<E> request) {
		synchronized (lock) {
			boolean isEmpty = requestQueue.peek() == null && activeRequest == null;
			requestQueue.add(request);
			if (isEmpty) {
				makeNextRequestActive();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected final <E> Request<E> makeNextRequestActive() {
		Request<E> request;
		synchronized (lock) {
			if (activeRequest != null && !activeRequest.isDone()) {
				throw new DbException("Active request is not done: " + activeRequest);
			}
			request = (Request<E>)requestQueue.poll();
			activeRequest = request;
		}
		if (request != null) {
			try {
				request.invokeExecute();
			} catch (Throwable e) {
				request.error(DbException.wrap(this, e));
			}
		}
		return request;
	}
	
	@SuppressWarnings("unchecked")
	protected <E> Request<E> getActiveRequest() {
		synchronized (lock) {
			return (Request<E>)activeRequest;
		}
	}
	
	protected void cancelPendingRequests(boolean mayInterruptIfRunning) {
		synchronized (lock) {
			for (Iterator<Request<?>> i = requestQueue.iterator(); i.hasNext();) {
				Request<?> request = i.next();
				i.remove();
				request.cancel(mayInterruptIfRunning);
			}
		}
	}
	
	/**
	 * Throws {@link DbSessionClosedException} if session is closed
	 * 
	 * @throws  if {@link DbSession} is closed.
	 */
	protected abstract void checkClosed() throws DbSessionClosedException;

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
	
	//*****************************************************************************************************************
	//
	//  Transaction methods 
	//
	//*****************************************************************************************************************
	
	public boolean isInTransaction() {
		checkClosed();
		synchronized (lock) {
			return transaction != null;
		}
	}

	public void beginTransaction() {
		checkClosed();
		synchronized (lock) {
			if (isInTransaction()) {
				throw new DbException(this, "Cannot begin new transaction.  Current transaction needs to be committed or rolled back");
			}
			transaction = new Transaction();
		}
	}

	public DbSessionFuture<Void> commit() {
		checkClosed();
		if (!isInTransaction()) {
			throw new DbException(this, "Not currently in a transaction, cannot commit");
		}
		DbSessionFuture<Void> future;
		synchronized (lock) {
			if (transaction.isBeginScheduled()) {
				future = enqueueCommit(transaction);
				return future;
			} else {
				// If transaction was not started, don't worry about committing transaction
				future = DefaultDbSessionFuture.createCompletedFuture(this, null);
			}
			transaction = null;
		}
		return future;
	}
	
	public DbSessionFuture<Void> rollback() {
		checkClosed();
		if (!isInTransaction()) {
			throw new DbException(this, "Not currently in a transaction, cannot rollback");
		}
		DbSessionFuture<Void> future;
		synchronized (lock) {
			if (transaction.isBeginScheduled()) {
				transaction.cancelPendingRequests();
				future = enqueueRollback(transaction);
			} else {
				future = DefaultDbSessionFuture.createCompletedFuture(this, null);
			}
			transaction = null;
		}
		return future;
	}

	protected <E> DbSessionFuture<E> enqueueTransactionalRequest(Request<E> request) {
		// Check to see if we're in a transaction
		synchronized (lock) {
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
		}
		enqueueRequest(request);
		return request;
	}

	private Request<Void> enqueueStartTransaction(final Transaction transaction) {
		Request<Void> request = createBeginRequest(transaction);
		enqueueTransactionalRequest(transaction, request);
		return request;
	}

	private Request<Void> enqueueCommit(final Transaction transaction) {
		Request<Void> request = createCommitRequest(transaction);
		enqueueTransactionalRequest(transaction, request);
		return request;
		
	}

	private Request<Void> enqueueRollback(Transaction transaction) {
		Request<Void> request = createRollbackRequest();
		enqueueTransactionalRequest(transaction, request);
		return request;
	}

	private void enqueueTransactionalRequest(final Transaction transaction, Request<Void> request) {
		enqueueRequest(request);
		transaction.addRequest(request);
	}

	// Rollback cannot be cancelled or removed
	protected Request<Void> createRollbackRequest() {
		return new RollbackRequest();
	}

	protected abstract void sendBegin() throws Exception;

	protected abstract void sendCommit() throws Exception;

	protected abstract void sendRollback() throws Exception;
	
	protected Request<Void> createBeginRequest(final Transaction transaction) {
		return new BeginRequest(transaction);
	}
	
	protected Request<Void> createCommitRequest(final Transaction transaction) {
		return new CommitRequest(transaction);
	}
	
	/**
	 * Default request for starting a transaction. 
	 */
	protected class BeginRequest extends Request<Void> {
		private final Transaction transaction;

		private BeginRequest(Transaction transaction) {
			if (transaction == null) {
				throw new IllegalArgumentException("transaction can NOT be null");
			}
			this.transaction = transaction;
		}

		@Override
		public void execute() throws Exception {
			transaction.setStarted(true);
			sendBegin();
		}
	}

	/**
	 * Default request for committing a transaction. 
	 */
	protected class CommitRequest extends Request<Void> {
		private final Transaction transaction;

		private CommitRequest(Transaction transaction) {
			if (transaction == null) {
				throw new IllegalArgumentException("transaction can NOT be null");
			}
			this.transaction = transaction;
		}

		public void execute() throws Exception {
			if (isCancelled()) {
				// If the transaction has started, send a rollback
				if (transaction.isStarted()) {
					sendRollback();
				}
			} else {
				sendCommit();
			}
		}

		@Override
		public boolean cancelRequest(boolean mayInterruptIfRunning) {
			transaction.cancelPendingRequests();
			return true;
		}
		
		@Override
		public boolean canRemove() {
			return false;
		}
		
		@Override
		public boolean canPipeline() {
			return false;
		}
	}

	/**
	 * Default request for rolling back a transaction. 
	 */
	protected class RollbackRequest extends Request<Void> {
		@Override
		public void execute() throws Exception {
			sendRollback();
		}

		@Override
		// Return false because a rollback cannot be cancelled
		public boolean cancelRequest(boolean mayInterruptIfRunning) {
			return false;
		}
	}

	public abstract class Request<T> extends DefaultDbSessionFuture<T> {
		
		private final ResultEventHandler<T> eventHandler;
		private final T accumulator;

		private volatile Object payload;
		private volatile Transaction transaction;
		
		private boolean cancelled; // Access must be synchronized on this 
		private boolean executed; // Access must be synchronized on this
		
		public Request() {
			this(null, null);
		}
		
		public Request(ResultEventHandler<T> eventHandler, T accumulator) {
			super(AbstractDbSession.this);
			this.eventHandler = eventHandler;
			this.accumulator = accumulator;
		}
		
		/**
		 * Checks to see if the request has been cancelled, if not invokes the execute method.  If pipelining, this
		 * method ensures the request does not get executed twice.
		 * 
		 * @throws Exception
		 */
		public final synchronized void invokeExecute() throws Exception {
			if (cancelled || executed) {
				makeNextRequestActive();
			} else {
				executed = true;
				execute();
			}
		}
		
		public final synchronized boolean doCancel(boolean mayInterruptIfRunning) {
			if (executed) {
				return false;
			}
			cancelled = cancelRequest(mayInterruptIfRunning);
			
			// The the request was cancelled and it can be removed
			if (cancelled && canRemove()) {
				synchronized (lock) {
					// Remove the quest and if the removal was successful and this request is active, go to the next request
					if (canRemove() && requestQueue.remove(this) && this == activeRequest) {
						makeNextRequestActive();
					}
				}
			}
			return cancelled;
		}
		
		protected abstract void execute() throws Exception;
		
		protected boolean cancelRequest(boolean mayInterruptIfRunning) {
			return true;
		}

		public boolean canRemove() {
			return true;
		}
		
		public boolean canPipeline() {
			return true;
		}

		public Object getPayload() {
			return payload;
		}

		public void setPayload(Object payload) {
			this.payload = payload;
		}

		public T getAccumulator() {
			return accumulator;
		}
		
		public ResultEventHandler<T> getEventHandler() {
			return eventHandler;
		}
		
		public Transaction getTransaction() {
			return transaction;
		}

		public void setTransaction(Transaction transaction) {
			this.transaction = transaction;
		}
		
		@Override
		public final void setResult(T result) {
			throw new IllegalStateException("You must call complete(T result) instead");
		}
		
		@Override
		public final void setException(Throwable exception) {
			throw new IllegalStateException("You must call error(DbException exception) instead");
		}
		
		public void complete(T result) {
			super.setResult(result);
			makeNextRequestActive();
		}
		
		public void error(DbException exception) {
			super.setException(exception);
			if (transaction != null) {
				transaction.cancelPendingRequests();
			}
			makeNextRequestActive();
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

		public void addRequest(Request<?> request) {
			request.setTransaction(this);
			synchronized (requests) {
				requests.add(request);
			}
		}
		
		public boolean isCanceled() {
			return canceled;
		}
		
		public void cancelPendingRequests() {
			canceled = true;
			synchronized (requests) {
				for (Request<?> request : requests) {
					request.cancel(false);
				}
			}
		}
		
	}

}
