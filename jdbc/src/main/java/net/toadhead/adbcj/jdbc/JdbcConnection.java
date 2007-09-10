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
package net.toadhead.adbcj.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.byu.cs.adbcj.Connection;
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbSessionFuture;
import edu.byu.cs.adbcj.Field;
import edu.byu.cs.adbcj.PreparedStatement;
import edu.byu.cs.adbcj.Result;
import edu.byu.cs.adbcj.ResultSet;
import edu.byu.cs.adbcj.TransactionIsolationLevel;
import edu.byu.cs.adbcj.Type;
import edu.byu.cs.adbcj.Value;
import edu.byu.cs.adbcj.support.AbstractTransactionalSession;
import edu.byu.cs.adbcj.support.ConcurrentFutureSessionProxy;
import edu.byu.cs.adbcj.support.DefaultDbFuture;
import edu.byu.cs.adbcj.support.DefaultDbSessionFuture;
import edu.byu.cs.adbcj.support.DefaultField;
import edu.byu.cs.adbcj.support.DefaultResult;
import edu.byu.cs.adbcj.support.DefaultResultSet;
import edu.byu.cs.adbcj.support.DefaultRow;
import edu.byu.cs.adbcj.support.DefaultValue;
import edu.byu.cs.adbcj.support.Request;

public class JdbcConnection extends AbstractTransactionalSession implements Connection {
	
	private final Logger logger = LoggerFactory.getLogger(JdbcConnection.class);
	
	private final JdbcConnectionManager connectionManager;
	private final java.sql.Connection jdbcConnection;
	
	private DbSessionFuture<Void> closeFuture;
	private volatile boolean closed = false;
	
	public JdbcConnection(JdbcConnectionManager connectionManager, java.sql.Connection jdbcConnection) {
		this.connectionManager = connectionManager;
		this.jdbcConnection = jdbcConnection;
	}

	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public synchronized DbSessionFuture<Void> close(boolean immediate) throws DbException {
		if (!closed) {
			closed = true;
			
			if (immediate) {
				cancelPendingRequests(true);
				final ConcurrentFutureSessionProxy<Void> localFuture = new ConcurrentFutureSessionProxy<Void>(this) {
					/**
					 * If the future gets cancelled, unclose the connection.
					 */
					@Override
					public boolean cancel(boolean mayInterruptIfRunning) {
						boolean cancelled = super.cancel(mayInterruptIfRunning);
						if (cancelled) {
							unclose();
						}
						return cancelled;
					}
				}; 
				Future<Void> future = connectionManager.getExecutorService().submit(new Callable<Void>() {
					public Void call() throws Exception {
						try {
							jdbcConnection.close();
						} finally {
							localFuture.setDone();
						}
						return null;
					}
				});
				localFuture.setFuture(future);
				closeFuture = localFuture;
			} else {
				closeFuture = enqueueRequest(new CallableRequest<Void>() {
					public Void call() throws Exception {
						jdbcConnection.close();
						return null;
					}
				});
			}
		}
		return closeFuture;
	}

	private synchronized void unclose() {
		this.closeFuture = null;
		closed = false;
	}
	
	public boolean isClosed() {
		try {
			return closed && jdbcConnection.isClosed();
		} catch (SQLException e) {
			throw new DbException(e);
		}
	}

	public DbSessionFuture<ResultSet> executeQuery(final String sql) {
		checkClosed();
		return enqueueTransactionalRequest(new CallableRequest<ResultSet>() {
			public ResultSet call() throws Exception {
				Statement jdbcStatement = jdbcConnection.createStatement();
				java.sql.ResultSet jdbcResultSet = null;
				try {
					jdbcResultSet = jdbcStatement.executeQuery(sql);
					ResultSetMetaData metaData = jdbcResultSet.getMetaData();
					int columnCount = metaData.getColumnCount();
					DefaultResultSet resultSet = new DefaultResultSet(columnCount);
					
					// Add fields
					for (int i = 1; i <= columnCount; i++) {
						Field field = new DefaultField(
								i - 1,
								metaData.getCatalogName(i),
								metaData.getSchemaName(i),
								metaData.getTableName(i),
								metaData.getTableName(i),
								convertJdbcToAdbcjType(metaData.getColumnType(i)),
								metaData.getColumnLabel(i),
								metaData.getCatalogName(i),
								metaData.getPrecision(i),
								metaData.getScale(i),
								metaData.isAutoIncrement(i),
								metaData.isCaseSensitive(i),
								metaData.isCurrency(i),
								metaData.isDefinitelyWritable(i),
								metaData.isNullable(i) == 1,
								metaData.isReadOnly(i),
								metaData.isSearchable(i),
								metaData.isSigned(i),
								metaData.isWritable(i),
								metaData.getColumnClassName(i)
								);
						resultSet.addField(field);
					}
					
					// Add rows
					while (jdbcResultSet.next()) {
						int fieldCount = resultSet.getFields().size();
						Value[] values = new Value[fieldCount];
						for (int i = 0; i < fieldCount; i++) {
							Field field = resultSet.getFields().get(i);
							Object value = null;
							switch (field.getColumnType()) {
							case BIGINT:
								value = jdbcResultSet.getLong(i + 1);
								break;
							case INTEGER:
								value = jdbcResultSet.getInt(i + 1);
								break;
							case VARCHAR:
								value = jdbcResultSet.getString(i + 1);
								break;
							default:
								throw new IllegalStateException("Don't know how to handle field to type " + field.getColumnType());
							}
							if (jdbcResultSet.wasNull()) {
								value = null;
							}
							values[i] = new DefaultValue(field, value);
						}
						resultSet.addResult(new DefaultRow(resultSet, values));
					}
					
					return resultSet;
				} finally {
					if (jdbcResultSet != null) {
						jdbcResultSet.close();
					}
					if (jdbcStatement != null) {
						jdbcStatement.close();
					}
				}
			}

		});
	}

	public DbSessionFuture<Result> executeUpdate(final String sql) {
		checkClosed();
		return enqueueTransactionalRequest(new CallableRequest<Result>() {
			public Result call() throws Exception {
				Statement statement = jdbcConnection.createStatement();
				try {
					statement.execute(sql);
					List<String> warnings = new LinkedList<String>();
					SQLWarning sqlWarnings = statement.getWarnings();
					while (sqlWarnings != null) {
						warnings.add(sqlWarnings.getLocalizedMessage());
						sqlWarnings = sqlWarnings.getNextWarning();
					}
					return new DefaultResult((long)statement.getUpdateCount(), warnings);
				} finally {
					statement.close();
				}
			}
		});
	}
	
	public DbSessionFuture<PreparedStatement> prepareStatement(String sql) {
		checkClosed();
		// TODO Auto-generated method stub
		return null;
	}
	
	public DbSessionFuture<PreparedStatement> prepareStatement(Object key, String sql) {
		checkClosed();
		// TODO Auto-generated method stub
		return null;
	}

	public DbFuture<Void> ping() {
		checkClosed();
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	 * 
	 * End of API methods 
	 * 
	 */
	
	// *********** Transaction method implementations **************************
	
	@Override
	protected DbSessionFuture<Void> enqueueChangeIsolationLevel(final Transaction transaction,
			final TransactionIsolationLevel transactionIsolationLevel) {
		CallableRequest<Void> request = new CallableRequest<Void>() {
					public Void call() throws Exception {
						transaction.setStarted(true);
						logger.debug("Changing isolation level to {}", transactionIsolationLevel);
						int isolationLevel;
						switch (transactionIsolationLevel) {
						case NONE:
							isolationLevel = java.sql.Connection.TRANSACTION_NONE;
							break;
						case READ_COMMITTED:
							isolationLevel = java.sql.Connection.TRANSACTION_READ_COMMITTED;
							break;
						case READ_UNCOMMITTED:
							isolationLevel = java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
							break;
						case REPEATABLE_READ:
							isolationLevel = java.sql.Connection.TRANSACTION_REPEATABLE_READ;
							break;
						case SERIALIZABLE:
							isolationLevel = java.sql.Connection.TRANSACTION_SERIALIZABLE;
							break;
						default:
							throw new DbException("Can't handle transaction isolation level " + transactionIsolationLevel);
						}
						jdbcConnection.setTransactionIsolation(isolationLevel);
						return null;
					}
				};
		DefaultDbSessionFuture<Void> future = enqueueRequest(request);
		transaction.addRequest(request);
		return future;
	}

	@Override
	protected DbSessionFuture<Void> enqueueCommit(final Transaction transaction) {
		CallableRequest<Void> request = new CallableRequest<Void>() {
			private boolean executing = false;
			private boolean canceled = false;
			public Void call() throws Exception {
				executing = true;
				if (canceled) {
					jdbcConnection.rollback();
				} else {
					logger.debug("Committing transaction");
					jdbcConnection.commit();
				}
				return null;
			}
			@Override
			public synchronized boolean cancel(boolean mayInterruptIfRunning) {
				if (executing) {
					return false;
				}
				canceled = true;
				transaction.cancelPendingRequests();
				return true;
			}
			@Override
			public boolean canRemove() {
				return !transaction.isStarted();
			}
		};
		DefaultDbSessionFuture<Void> future = enqueueRequest(request);
		transaction.addRequest(request);
		return future;
	}

	@Override
	protected DbSessionFuture<Void> enqueueRollback(Transaction transaction) {
		CallableRequest<Void> request = new CallableRequest<Void>() {
			public Void call() throws Exception {
				logger.debug("Rolling back transaction");
				jdbcConnection.rollback();
				return null;
			}
		};
		DefaultDbSessionFuture<Void> future = enqueueRequest(request);
		transaction.addRequest(request);
		return future;
	}

	@Override
	protected DbSessionFuture<Void> enqueueStartTransaction(final Transaction transaction) {
		CallableRequest<Void> request = new CallableRequest<Void>() {
			public Void call() throws Exception {
				logger.debug("Starting transaction");
				transaction.setStarted(true);
				jdbcConnection.setAutoCommit(false);
				return null;
			}
		};
		DefaultDbSessionFuture<Void> future = enqueueRequest(request);
		transaction.addRequest(request);
		return future;
	}

	// *********** JDBC Specific method implementations ************************
	
	@Override
	protected void checkClosed() {
		if (isClosed()) {
			throw new DbException("Connection is closed");
		}
	}

	// TODO: Move this into Type enum
	private Type convertJdbcToAdbcjType(int columnType) {
		switch (columnType) {
		case Types.ARRAY:
			return Type.ARRAY;
		case Types.BIGINT:
			return Type.BIGINT;
		case Types.BINARY:
			return Type.BINARY;
		case Types.BIT:
			return Type.BIT;
		case Types.BLOB:
			return Type.BLOB;
		case Types.BOOLEAN:
			return Type.BOOLEAN;
		case Types.CHAR:
			return Type.CHAR;
		case Types.CLOB:
			return Type.CLOB;
		case Types.DATALINK:
			return Type.DATALINK;
		case Types.DATE:
			return Type.DATE;
		case Types.DECIMAL:
			return Type.DECIMAL;
		case Types.DISTINCT:
			return Type.DISTINCT;
		case Types.DOUBLE:
			return Type.DOUBLE;
		case Types.FLOAT:
			return Type.FLOAT;
		case Types.INTEGER:
			return Type.INTEGER;
		case Types.JAVA_OBJECT:
			return Type.JAVA_OBJECT;
		case Types.LONGNVARCHAR:
			return Type.LONGVARCHAR;
		case Types.LONGVARBINARY:
			return Type.LONGVARBINARY;
		case Types.LONGVARCHAR:
			return Type.LONGVARCHAR;
		case Types.NULL:
			return Type.NULL;
		case Types.NUMERIC:
			return Type.NUMERIC;
		case Types.OTHER:
			return Type.OTHER;
		case Types.REAL:
			return Type.REAL;
		case Types.REF:
			return Type.REF;
		case Types.SMALLINT:
			return Type.SMALLINT;
		case Types.STRUCT:
			return Type.STRUCT;
		case Types.TIME:
			return Type.TIME;
		case Types.TIMESTAMP:
			return Type.TIMESTAMP;
		case Types.TINYINT:
			return Type.TINYINT;
		case Types.VARBINARY:
			return Type.VARBINARY;
		case Types.VARCHAR:
			return Type.VARCHAR;
		default:
			throw new IllegalStateException("Don't know how to handle column type" + columnType);	
		}
	}
	
	private abstract class CallableRequest<E> extends Request<E> implements Callable<E> {
		private volatile Future<E> future = null;

		@Override
		public synchronized boolean cancel(boolean mayInterruptIfRunning) {
			if (future == null) {
				return false;
			}
			return future.cancel(mayInterruptIfRunning);
		}
		@Override
		public synchronized void execute(final DefaultDbFuture<E> future) {
			this.future = connectionManager.getExecutorService().submit(new Callable<E>() {
				public E call() throws Exception {
					try {
						E value = CallableRequest.this.call();
						future.setValue(value);
						return value;
					} catch (Exception e) {
						if (e instanceof DbException) {
							future.setException((DbException)e);
						} else {
							future.setException(new DbException(e));
						}
						throw e;
					} finally {
						future.setDone();
						makeNextRequestActive();
					}
				}
			});
		}

	}

}