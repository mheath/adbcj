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
import java.sql.Statement;
import java.sql.Types;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import edu.byu.cs.adbcj.Connection;
import edu.byu.cs.adbcj.ConnectionManager;
import edu.byu.cs.adbcj.DbException;
import edu.byu.cs.adbcj.DbFuture;
import edu.byu.cs.adbcj.DbSessionFuture;
import edu.byu.cs.adbcj.Field;
import edu.byu.cs.adbcj.PreparedStatement;
import edu.byu.cs.adbcj.ResultSet;
import edu.byu.cs.adbcj.TransactionIsolationLevel;
import edu.byu.cs.adbcj.Type;
import edu.byu.cs.adbcj.Value;
import edu.byu.cs.adbcj.support.AbstractDbFutureBase;
import edu.byu.cs.adbcj.support.BaseRequestQueue;
import edu.byu.cs.adbcj.support.ConcurrentFutureSessionProxy;
import edu.byu.cs.adbcj.support.DbSessionFutureProxy;
import edu.byu.cs.adbcj.support.DefaultField;
import edu.byu.cs.adbcj.support.DefaultResultSet;
import edu.byu.cs.adbcj.support.DefaultRow;
import edu.byu.cs.adbcj.support.DefaultValue;
import edu.byu.cs.adbcj.support.RequestAction;

public class JdbcConnection extends BaseRequestQueue implements Connection {

	private final JdbcConnectionManager connectionManager;
	private final java.sql.Connection jdbcConnection;
	
	private DbFuture<Void> closeFuture;
	private volatile boolean closed = false;
	
	public JdbcConnection(JdbcConnectionManager connectionManager, java.sql.Connection jdbcConnection) {
		this.connectionManager = connectionManager;
		this.jdbcConnection = jdbcConnection;
	}

	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public void beginTransaction() {
		// TODO Auto-generated method stub
		
	}

	public DbSessionFuture<Void> commit() {
		// TODO Auto-generated method stub
		return null;
	}

	public DbSessionFuture<Void> rollback() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isInTransaction() {
		// TODO Auto-generated method stub
		return false;
	}

	public TransactionIsolationLevel getTransactionIsolationLevel() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setTransactionIsolationLevel(
			TransactionIsolationLevel transactionIsolationLevel) {
		// TODO Auto-generated method stub
		
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
				closeFuture = enqueueCallable(new Callable<Void>() {
					public Void call() throws Exception {
						jdbcConnection.close();
						return null;
					}
				}) ;
			}
		}
		return newFutureProxy(closeFuture);
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
		return newFutureProxy(enqueueCallable(new Callable<ResultSet>() {
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

		}));
	}

	public DbSessionFuture<PreparedStatement> prepareStatement(String sql) {
		// TODO Auto-generated method stub
		return null;
	}

	public DbFuture<Void> ping() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	 * 
	 * End of API methods 
	 * 
	 */

	protected <E> AbstractDbFutureBase<E> enqueueCallable(final Callable<E> task) {
		AbstractDbFutureBase<E> localFuture = enqueueRequest(new RequestAction<E>() {
			private volatile Future<E> future = null;
			public synchronized boolean cancel(boolean mayInterruptIfRunning) {
				if (future == null) {
					return false;
				}
				return future.cancel(mayInterruptIfRunning);
			}
			public synchronized void execute(final AbstractDbFutureBase<E> future) {
				this.future = connectionManager.getExecutorService().submit(new Callable<E>() {
					public E call() throws Exception {
						try {
							E value = task.call();
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
						}
					}
				});
			}
		});
		return localFuture;
	}
	
	@SuppressWarnings("unchecked")
	private <E> DbSessionFuture<E> newFutureProxy(DbFuture<E> future) {
		return new DbSessionFutureProxy<E>((DbFuture<E>)future, this);
	}

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

}
