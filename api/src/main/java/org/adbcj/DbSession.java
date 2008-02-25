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
package org.adbcj;

public interface DbSession {

	void beginTransaction();
	
	// Canceling a commit will cause the transaction to rollback - TODO Write a TCK test to test that canceling a commit converts it to a rollback
	DbSessionFuture<Void> commit();

	// A rollback cannot be canceled
	// Rolling back a transaction may cancel pending requests
	DbSessionFuture<Void> rollback();
	
	/**
	 * Indicates whether or not the current session is involved in a transaction.
	 * 
	 * @return  true if the session is in a transaction, false otherwise
	 */
	boolean isInTransaction();
	
	// TODO Make transaction isolation level a parameter in the config stuff
//	TransactionIsolationLevel getTransactionIsolationLevel();
//	
//	void setTransactionIsolationLevel(TransactionIsolationLevel transactionIsolationLevel);
	
	DbSessionFuture<ResultSet> executeQuery(String sql);
	
	<T> DbSessionFuture<T> executeQuery(String sql, ResultEventHandler<T> eventHandler, T accumulator);
	
	DbSessionFuture<Result> executeUpdate(String sql);
	
	DbSessionFuture<PreparedStatement> prepareStatement(String sql);
	
	DbSessionFuture<PreparedStatement> prepareStatement(Object key, String sql);

	DbSessionFuture<Void> close(boolean immediate) throws DbException;
	
	boolean isClosed() throws DbException;

	boolean isPipeliningEnabled();
	
	void setPipeliningEnabled(boolean pipeliningEnabled);
}
