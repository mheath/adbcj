/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.adbcj.mysql.netty.org.apache.commons.pool2;

/**
 * A simple base implementation of {@link ObjectPool}.
 * Optional operations are implemented to either do nothing, return a value
 * indicating it is unsupported or throw {@link UnsupportedOperationException}.
 * <p>
 * This class is intended to be thread-safe.
 *
 * @param <T> Type of element pooled in this pool.
 *
 * @version $Revision: 1333925 $
 *
 * @since 2.0
 */
public abstract class BaseObjectPool<T> implements ObjectPool<T> {
    /**
     * Obtains an instance from the pool.
     *
     * @return an instance from the pool
     *
     * @throws Exception if an instance cannot be obtained from the pool
     */
    
    public abstract T borrowObject() throws Exception;

    /**
     * Returns an instance to the pool.
     *
     * @param obj instance to return to the pool
     */
    
    public abstract void returnObject(T obj) throws Exception;

    /**
     * Invalidates an object from the pool.
     * <p>
     * By contract, <code>obj</code> <strong>must</strong> have been obtained
     * using {@link #borrowObject borrowObject}.
     * <p>
     * This method should be used when an object that has been borrowed is
     * determined (due to an exception or other problem) to be invalid.
     *
     * @param obj a {@link #borrowObject borrowed} instance to be disposed.
     * @throws Exception
     */
    
    public abstract void invalidateObject(T obj) throws Exception;

    /**
     * Not supported in this base implementation.
     *
     * @return a negative value.
     */
    
    public int getNumIdle() {
        return -1;
    }

    /**
     * Not supported in this base implementation.
     *
     * @return a negative value.
     */
    
    public int getNumActive() {
        return -1;
    }

    /**
     * Not supported in this base implementation.
     *
     * @throws UnsupportedOperationException
     */
    
    public void clear() throws Exception, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported in this base implementation.Always throws an
     * {@link UnsupportedOperationException}, subclasses should override this
     * behavior.
     *
     * @throws UnsupportedOperationException
     */
    
    public void addObject() throws Exception, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Close this pool. This affects the behavior of <code>isClosed</code> and
     * <code>assertOpen</code>.
     */
    
    public void close() {
        closed = true;
    }

    /**
     * Has this pool instance been closed.
     *
     * @return <code>true</code> when this pool has been closed.
     */
    public final boolean isClosed() {
        return closed;
    }

    /**
     * Throws an <code>IllegalStateException</code> when this pool has been
     * closed.
     *
     * @throws IllegalStateException when this pool has been closed.
     *
     * @see #isClosed()
     */
    protected final void assertOpen() throws IllegalStateException {
        if (isClosed()) {
            throw new IllegalStateException("Pool not open");
        }
    }

    private volatile boolean closed = false;
}
