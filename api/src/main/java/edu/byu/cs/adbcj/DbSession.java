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
package edu.byu.cs.adbcj;

public interface DbSession {

	void beginTransaction();
	
	// Cancelling a commit will cause the transaction to rollback - TODO Write TCK for this
	DbSessionFuture<Void> commit();

	// A rollback cannot be cancelled
	// Rolling back a transaction may cancel pending requests
	DbSessionFuture<Void> rollback();
	
	boolean isInTransaction();
	
	TransactionIsolationLevel getTransactionIsolationLevel();
	
	void setTransactionIsolationLevel(TransactionIsolationLevel transactionIsolationLevel);
	
	DbSessionFuture<ResultSet> executeQuery(String sql);
	
	DbSessionFuture<Result> executeUpdate(String sql);
	
	DbSessionFuture<PreparedStatement> prepareStatement(String sql);
	
	DbSessionFuture<PreparedStatement> prepareStatement(Object key, String sql);

	DbSessionFuture<Void> close(boolean immediate) throws DbException;
	
	boolean isClosed() throws DbException;

}
