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
package org.adbcj.mysql.netty.org.apache.commons.pool2.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.adbcj.mysql.netty.org.apache.commons.pool2.ObjectPool;
import org.adbcj.mysql.netty.org.apache.commons.pool2.PoolUtils;
import org.adbcj.mysql.netty.org.apache.commons.pool2.PoolableObjectFactory;

/**
 * A configurable {@link ObjectPool} implementation.
 * <p>
 * When coupled with the appropriate {@link PoolableObjectFactory},
 * <code>GenericObjectPool</code> provides robust pooling functionality for
 * arbitrary objects.</p>
 * <p>
 * Optionally, one may configure the pool to examine and possibly evict objects
 * as they sit idle in the pool and to ensure that a minimum number of idle
 * objects are available. This is performed by an "idle object eviction" thread,
 * which runs asynchronously. Caution should be used when configuring this
 * optional feature. Eviction runs contend with client threads for access to
 * objects in the pool, so if they run too frequently performance issues may
 * result.</p>
 * <p>
 * The pool can also be configured to detect and remove "abandoned" objects,
 * i.e. objects that have been checked out of the pool but neither used nor
 * returned before the configured
 * {@link AbandonedConfig#getRemoveAbandonedTimeout() removeAbandonedTimeout}.
 * Abandoned object removal can be configured to happen when
 * <code>borrowObject</code> is invoked and the pool is close to starvation, or
 * it can be executed by the idle object evictor, or both. If pooled objects
 * implement the {@link TrackedUse} interface, their last use will be queried
 * using the <code>getLastUsed</code> method on that interface; otherwise
 * abandonment is determined by how long an object has been checked out from
 * the pool.</p>
 * <p>
 * Implementation note: To prevent possible deadlocks, care has been taken to
 * ensure that no call to a factory method will occur within a synchronization
 * block. See POOL-125 and DBCP-44 for more information.</p>
 * <p>
 * This class is intended to be thread-safe.</p>
 *
 * @see GenericKeyedObjectPool
 *
 * @param <T> Type of element pooled in this pool.
 *
 * @version $Revision: 1431458 $
 *
 * @since 2.0
 */
public class GenericObjectPool<T> extends BaseGenericObjectPool<T>
        implements ObjectPool<T>, GenericObjectPoolMBean {

    /**
     * Create a new <code>GenericObjectPool</code> using defaults from
     * {@link GenericObjectPoolConfig}.
     */
    public GenericObjectPool(PoolableObjectFactory<T> factory) {
        this(factory, new GenericObjectPoolConfig());
    }

    /**
     * Create a new <code>GenericObjectPool</code> using a specific
     * configuration.
     *
     * @param config    The configuration to use for this pool instance. The
     *                  configuration is used by value. Subsequent changes to
     *                  the configuration object will not be reflected in the
     *                  pool.
     */
    public GenericObjectPool(PoolableObjectFactory<T> factory,
            GenericObjectPoolConfig config) {

        super(config, ONAME_BASE, config.getJmxNamePrefix());

        if (factory == null) {
            jmxUnregister(); // tidy up
            throw new IllegalArgumentException("factory may not be null");
        }
        this.factory = factory;

        setConfig(config);

        startEvictor(getTimeBetweenEvictionRunsMillis());
    }

    /**
     * Create a new <code>GenericObjectPool</code> that tracks and destroys
     * objects that are checked out, but never returned to the pool.
     *
     * @param config    The base pool configuration to use for this pool instance.
     *                  The configuration is used by value. Subsequent changes to
     *                  the configuration object will not be reflected in the
     *                  pool.
     * @param abandonedConfig  Configuration for abandoned object identification
     *                         and removal.  The configuration is used by value.
     */
    public GenericObjectPool(PoolableObjectFactory<T> factory,
            GenericObjectPoolConfig config, AbandonedConfig abandonedConfig) {
        this(factory, config);
        setAbandonedConfig(abandonedConfig);
    }

    /**
     * Returns the cap on the number of "idle" instances in the pool. If maxIdle
     * is set too low on heavily loaded systems it is possible you will see
     * objects being destroyed and almost immediately new objects being created.
     * This is a result of the active threads momentarily returning objects
     * faster than they are requesting them them, causing the number of idle
     * objects to rise above maxIdle. The best value for maxIdle for heavily
     * loaded system will vary but the default is a good starting point.
     *
     * @return the maximum number of "idle" instances that can be held in the
     *         pool or a negative value if there is no limit
     *
     * @see #setMaxIdle
     */
    
    public int getMaxIdle() {
        return maxIdle;
    }

    /**
     * Returns the cap on the number of "idle" instances in the pool. If maxIdle
     * is set too low on heavily loaded systems it is possible you will see
     * objects being destroyed and almost immediately new objects being created.
     * This is a result of the active threads momentarily returning objects
     * faster than they are requesting them them, causing the number of idle
     * objects to rise above maxIdle. The best value for maxIdle for heavily
     * loaded system will vary but the default is a good starting point.
     *
     * @param maxIdle
     *            The cap on the number of "idle" instances in the pool. Use a
     *            negative value to indicate an unlimited number of idle
     *            instances
     *
     * @see #getMaxIdle
     */
    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    /**
     * Sets the target for the minimum number of idle objects to maintain in
     * the pool. This setting only has an effect if it is positive and
     * {@link #getTimeBetweenEvictionRunsMillis()} is greater than zero. If this
     * is the case, an attempt is made to ensure that the pool has the required
     * minimum number of instances during idle object eviction runs.
     * <p>
     * If the configured value of minIdle is greater than the configured value
     * for maxIdle then the value of maxIdle will be used instead.
     *
     * @param minIdle
     *            The minimum number of objects.
     *
     * @see #getMinIdle()
     * @see #getMaxIdle()
     * @see #getTimeBetweenEvictionRunsMillis()
     */
    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    /**
     * Returns the target for the minimum number of idle objects to maintain in
     * the pool. This setting only has an effect if it is positive and
     * {@link #getTimeBetweenEvictionRunsMillis()} is greater than zero. If this
     * is the case, an attempt is made to ensure that the pool has the required
     * minimum number of instances during idle object eviction runs.
     * <p>
     * If the configured value of minIdle is greater than the configured value
     * for maxIdle then the value of maxIdle will be used instead.
     *
     * @return The minimum number of objects.
     *
     * @see #setMinIdle(int)
     * @see #setMaxIdle(int)
     * @see #setTimeBetweenEvictionRunsMillis(long)
     */
    
    public int getMinIdle() {
        int maxIdle = getMaxIdle();
        if (this.minIdle > maxIdle) {
            return maxIdle;
        } else {
            return minIdle;
        }
    }

    /**
     * Whether or not abandoned object removal is configured for this pool.
     *
     * @return true if this pool is configured to detect and remove
     * abandoned objects
     */
    
    public boolean isAbandonedConfig() {
        return abandonedConfig != null;
    }

    /**
     * Returns true if abandoned object removal is configured for this pool
     * and removal events are to be logged.
     *
     * See {@link AbandonedConfig#getLogAbandoned()}
     */
    
    public boolean getLogAbandoned() {
        return isAbandonedConfig() && abandonedConfig.getLogAbandoned();
    }

    /**
     * Returns true if abandoned object removal is configured to be
     * activated by borrowObject.
     *
     * See {@link AbandonedConfig#getRemoveAbandonedOnBorrow()}
     */
    
    public boolean getRemoveAbandonedOnBorrow() {
        return isAbandonedConfig() &&
        abandonedConfig.getRemoveAbandonedOnBorrow();
    }

    /**
     * Returns true if abandoned object removal is configured to be
     * activated when the evictor runs.
     *
     * See {@link AbandonedConfig#getRemoveAbandonedOnMaintenance()}
     */
    
    public boolean getRemoveAbandonedOnMaintenance() {
        return isAbandonedConfig() &&
        abandonedConfig.getRemoveAbandonedOnMaintenance();
    }

    /**
     * Returns the abandoned object timeout if abandoned object removal
     * is configured for this pool; Integer.MAX_VALUE otherwise.
     *
     * See {@link AbandonedConfig#getRemoveAbandonedTimeout()}
     */
    
    public int getRemoveAbandonedTimeout() {
        return isAbandonedConfig() ?
                abandonedConfig.getRemoveAbandonedTimeout() :
                    Integer.MAX_VALUE;
    }


    /**
     * Sets the base pool configuration.
     *
     * @param conf the new configuration to use. This is used by value.
     *
     * @see GenericObjectPoolConfig
     */
    public void setConfig(GenericObjectPoolConfig conf) {
        setLifo(conf.getLifo());
        setMaxIdle(conf.getMaxIdle());
        setMinIdle(conf.getMinIdle());
        setMaxTotal(conf.getMaxTotal());
        setMaxWaitMillis(conf.getMaxWaitMillis());
        setBlockWhenExhausted(conf.getBlockWhenExhausted());
        setTestOnBorrow(conf.getTestOnBorrow());
        setTestOnReturn(conf.getTestOnReturn());
        setTestWhileIdle(conf.getTestWhileIdle());
        setNumTestsPerEvictionRun(conf.getNumTestsPerEvictionRun());
        setMinEvictableIdleTimeMillis(conf.getMinEvictableIdleTimeMillis());
        setTimeBetweenEvictionRunsMillis(
                conf.getTimeBetweenEvictionRunsMillis());
        setSoftMinEvictableIdleTimeMillis(
                conf.getSoftMinEvictableIdleTimeMillis());
        setEvictionPolicyClassName(conf.getEvictionPolicyClassName());
    }

    /**
     * Sets the abandoned object removal configuration.
     *
     * @param abandonedConfig the new configuration to use. This is used by value.
     *
     * @see AbandonedConfig
     */
    public void setAbandonedConfig(AbandonedConfig abandonedConfig) throws IllegalArgumentException {
        this.abandonedConfig = new AbandonedConfig();
        this.abandonedConfig.setLogAbandoned(abandonedConfig.getLogAbandoned());
        this.abandonedConfig.setLogWriter(abandonedConfig.getLogWriter());
        this.abandonedConfig.setRemoveAbandonedOnBorrow(abandonedConfig.getRemoveAbandonedOnBorrow());
        this.abandonedConfig.setRemoveAbandonedOnMaintenance(abandonedConfig.getRemoveAbandonedOnMaintenance());
        this.abandonedConfig.setRemoveAbandonedTimeout(abandonedConfig.getRemoveAbandonedTimeout());
    }

    /**
     * Obtain a reference to the factory used to create, destroy and validate
     * the objects used by this pool.
     *
     * @return the factory
     */
    public PoolableObjectFactory<T> getFactory() {
        return factory;
    }

    /**
     * Equivalent to <code>{@link #borrowObject(long)
     * borrowObject}({@link #getMaxWaitMillis()})</code>.
     */
    
    public T borrowObject() throws Exception {
        return borrowObject(getMaxWaitMillis());
    }

    /**
     * Borrow an object from the pool using the specific waiting time which only
     * applies if {@link #getBlockWhenExhausted()} is true.
     * <p>
     * If there is one or more idle instance available in the pool, then an
     * idle instance will be selected based on the value of {@link #getLifo()},
     * activated and returned. If activation fails, or {@link #getTestOnBorrow()
     * testOnBorrow} is set to <code>true</code> and validation fails, the
     * instance is destroyed and the next available instance is examined. This
     * continues until either a valid instance is returned or there are no more
     * idle instances available.
     * <p>
     * If there are no idle instances available in the pool, behavior depends on
     * the {@link #getMaxTotal() maxTotal}, (if applicable)
     * {@link #getBlockWhenExhausted()} and the value passed in to the
     * <code>borrowMaxWaitMillis</code> parameter. If the number of instances
     * checked out from the pool is less than <code>maxActive,</code> a new
     * instance is created, activated and (if applicable) validated and returned
     * to the caller.
     * <p>
     * If the pool is exhausted (no available idle instances and no capacity to
     * create new ones), this method will either block (if
     * {@link #getBlockWhenExhausted()} is true) or throw a
     * <code>NoSuchElementException</code> (if
     * {@link #getBlockWhenExhausted()} is false). The length of time that this
     * method will block when {@link #getBlockWhenExhausted()} is true is
     * determined by the value passed in to the <code>borrowMaxWait</code>
     * parameter.
     * <p>
     * When the pool is exhausted, multiple calling threads may be
     * simultaneously blocked waiting for instances to become available. A
     * "fairness" algorithm has been implemented to ensure that threads receive
     * available instances in request arrival order.
     *
     * @param borrowMaxWaitMillis The time to wait in milliseconds for an object
     *                            to become available
     *
     * @return object instance from the pool
     *
     * @throws NoSuchElementException if an instance cannot be returned
     */
    public T borrowObject(long borrowMaxWaitMillis) throws Exception {
        assertOpen();

        if (isAbandonedConfig() &&
                abandonedConfig.getRemoveAbandonedOnBorrow() &&
                (getNumIdle() < 2) &&
                (getNumActive() > getMaxTotal() - 3) ) {
            removeAbandoned();
        }

        PooledObject<T> p = null;

        // Get local copy of current config so it is consistent for entire
        // method execution
        boolean blockWhenExhausted = getBlockWhenExhausted();

        boolean create;
        long waitTime = 0;

        while (p == null) {
            create = false;
            if (blockWhenExhausted) {
                p = idleObjects.pollFirst();
                if (p == null) {
                    create = true;
                    p = create();
                }
                if (p == null) {
                    if (borrowMaxWaitMillis < 0) {
                        p = idleObjects.takeFirst();
                    } else {
                        waitTime = System.currentTimeMillis();
                        p = idleObjects.pollFirst(borrowMaxWaitMillis,
                                TimeUnit.MILLISECONDS);
                        waitTime = System.currentTimeMillis() - waitTime;
                    }
                }
                if (p == null) {
                    throw new NoSuchElementException(
                            "Timeout waiting for idle object");
                }
                if (!p.allocate()) {
                    p = null;
                }
            } else {
                p = idleObjects.pollFirst();
                if (p == null) {
                    create = true;
                    p = create();
                }
                if (p == null) {
                    throw new NoSuchElementException("Pool exhausted");
                }
                if (!p.allocate()) {
                    p = null;
                }
            }

            if (p != null) {
                try {
                    factory.activateObject(p.getObject());
                } catch (Exception e) {
                    try {
                        destroy(p);
                    } catch (Exception e1) {
                        // Ignore - activation failure is more important
                    }
                    p = null;
                    if (create) {
                        NoSuchElementException nsee = new NoSuchElementException(
                                "Unable to activate object");
                        nsee.initCause(e);
                        throw nsee;
                    }
                }
                if (p != null && getTestOnBorrow()) {
                    boolean validate = false;
                    Throwable validationThrowable = null;
                    try {
                        validate = factory.validateObject(p.getObject());
                    } catch (Throwable t) {
                        PoolUtils.checkRethrow(t);
                        validationThrowable = t;
                    }
                    if (!validate) {
                        try {
                            destroy(p);
                            destroyedByBorrowValidationCount.incrementAndGet();
                        } catch (Exception e) {
                            // Ignore - validation failure is more important
                        }
                        p = null;
                        if (create) {
                            NoSuchElementException nsee = new NoSuchElementException(
                                    "Unable to validate object");
                            nsee.initCause(validationThrowable);
                            throw nsee;
                        }
                    }
                }
            }
        }

        updateStatsBorrow(p, waitTime);

        return p.getObject();
    }

    /**
     * Returns an object instance to the pool.
     * <p>
     * If {@link #getMaxIdle() maxIdle} is set to a positive value and the
     * number of idle instances has reached this value, the returning instance
     * is destroyed.
     * <p>
     * If {@link #getTestOnReturn() testOnReturn} == true, the returning
     * instance is validated before being returned to the idle instance pool. In
     * this case, if validation fails, the instance is destroyed.
     * <p>
     * Exceptions encountered destroying objects for any reason are swallowed
     * but remain accessible via {@link #getSwallowedExceptions()}.
     *
     * @param obj instance to return to the pool
     */
    
    public void returnObject(T obj) {
        PooledObject<T> p = allObjects.get(obj);

        if (!isAbandonedConfig()) {
            if (p == null) {
                throw new IllegalStateException(
                        "Returned object not currently part of this pool");
            }
        } else {
            if (p == null) {
                return;  // Object was abandoned and removed
            } else {
                // Make sure object is not being reclaimed
                synchronized(p) {
                    final PooledObjectState state = p.getState();
                    if (state == PooledObjectState.ABANDONED ||
                            state == PooledObjectState.INVALID) {
                        return;
                    } else {
                        p.markReturning(); // Keep from being marked abandoned
                    }
                }
            }
        }

        long activeTime = p.getActiveTimeMillis();

        if (getTestOnReturn()) {
            if (!factory.validateObject(obj)) {
                try {
                    destroy(p);
                } catch (Exception e) {
                    swallowException(e);
                }
                updateStatsReturn(activeTime);
                return;
            }
        }

        try {
            factory.passivateObject(obj);
        } catch (Exception e1) {
            swallowException(e1);
            try {
                destroy(p);
            } catch (Exception e) {
                swallowException(e);
            }
            updateStatsReturn(activeTime);
            return;
        }

        if (!p.deallocate()) {
            throw new IllegalStateException(
                    "Object has already been retured to this pool or is invalid");
        }

        int maxIdle = getMaxIdle();
        if (isClosed() || maxIdle > -1 && maxIdle <= idleObjects.size()) {
            try {
                destroy(p);
            } catch (Exception e) {
                swallowException(e);
            }
        } else {
            if (getLifo()) {
                idleObjects.addFirst(p);
            } else {
                idleObjects.addLast(p);
            }
        }
        updateStatsReturn(activeTime);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Activation of this method decrements the active count and attempts to
     * destroy the instance.
     *
     * @throws Exception             if an exception occurs destroying the
     *                               object
     * @throws IllegalStateException if obj does not belong to this pool
     */
    
    public void invalidateObject(T obj) throws Exception {
        PooledObject<T> p = allObjects.get(obj);
        if (p == null) {
            if (isAbandonedConfig()) {
                return;
            } else {
                throw new IllegalStateException(
                        "Invalidated object not currently part of this pool");
            }
        }
        synchronized (p) {
            if (p.getState() != PooledObjectState.INVALID) {
                destroy(p);
            }
        }
    }

    /**
     * Clears any objects sitting idle in the pool by removing them from the
     * idle instance pool and then invoking the configured
     * {@link PoolableObjectFactory#destroyObject(Object)} method on each idle
     * instance.
     * <p>
     * Implementation notes:
     * <ul>
     * <li>This method does not destroy or effect in any way instances that are
     * checked out of the pool when it is invoked.</li>
     * <li>Invoking this method does not prevent objects being returned to the
     * idle instance pool, even during its execution. Additional instances may
     * be returned while removed items are being destroyed.</li>
     * <li>Exceptions encountered destroying idle instances are swallowed but
     * remain accessible via {@link #getSwallowedExceptions()}.</li>
     * </ul>
     */
    
    public void clear() {
        PooledObject<T> p = idleObjects.poll();

        while (p != null) {
            try {
                destroy(p);
            } catch (Exception e) {
                swallowException(e);
            }
            p = idleObjects.poll();
        }
    }

    /**
     * Returns the total number of instances currently borrowed from this pool
     * but not yet returned.
     */
    
    public int getNumActive() {
        return allObjects.size() - idleObjects.size();
    }

    
    public int getNumIdle() {
        return idleObjects.size();
    }

    /**
     * Closes the pool. Once the pool is closed, {@link #borrowObject()} will
     * fail with IllegalStateException, but {@link #returnObject(Object)} and
     * {@link #invalidateObject(Object)} will continue to work, with returned
     * objects destroyed on return.
     * <p>
     * Destroys idle instances in the pool by invoking {@link #clear()}.
     */
    
    public void close() {
        if (isClosed()) {
            return;
        }

        synchronized (closeLock) {
            if (isClosed()) {
                return;
            }

            // Stop the evictor before the pool is closed since evict() calls
            // assertOpen()
            startEvictor(-1L);

            closed = true;
            // This clear removes any idle objects
            clear();

            jmxUnregister();

            // Release any threads that were waiting for an object
            idleObjects.interuptTakeWaiters();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Successive activations of this method examine objects in sequence,
     * cycling through objects in oldest-to-youngest order.
     */
    
    public void evict() throws Exception {
        assertOpen();

        if (idleObjects.size() > 0) {

            PooledObject<T> underTest = null;
            EvictionPolicy<T> evictionPolicy = getEvictionPolicy();

            synchronized (evictionLock) {
                EvictionConfig evictionConfig = new EvictionConfig(
                        getMinEvictableIdleTimeMillis(),
                        getSoftMinEvictableIdleTimeMillis(),
                        getMinIdle());

                boolean testWhileIdle = getTestWhileIdle();

                for (int i = 0, m = getNumTests(); i < m; i++) {
                    if (evictionIterator == null || !evictionIterator.hasNext()) {
                        if (getLifo()) {
                            evictionIterator = idleObjects.descendingIterator();
                        } else {
                            evictionIterator = idleObjects.iterator();
                        }
                    }
                    if (!evictionIterator.hasNext()) {
                        // Pool exhausted, nothing to do here
                        return;
                    }

                    try {
                        underTest = evictionIterator.next();
                    } catch (NoSuchElementException nsee) {
                        // Object was borrowed in another thread
                        // Don't count this as an eviction test so reduce i;
                        i--;
                        evictionIterator = null;
                        continue;
                    }

                    if (!underTest.startEvictionTest()) {
                        // Object was borrowed in another thread
                        // Don't count this as an eviction test so reduce i;
                        i--;
                        continue;
                    }

                    if (evictionPolicy.evict(evictionConfig, underTest,
                            idleObjects.size())) {
                        destroy(underTest);
                        destroyedByEvictorCount.incrementAndGet();
                    } else {
                        if (testWhileIdle) {
                            boolean active = false;
                            try {
                                factory.activateObject(underTest.getObject());
                                active = true;
                            } catch (Exception e) {
                                destroy(underTest);
                                destroyedByEvictorCount.incrementAndGet();
                            }
                            if (active) {
                                if (!factory.validateObject(underTest.getObject())) {
                                    destroy(underTest);
                                    destroyedByEvictorCount.incrementAndGet();
                                } else {
                                    try {
                                        factory.passivateObject(underTest.getObject());
                                    } catch (Exception e) {
                                        destroy(underTest);
                                        destroyedByEvictorCount.incrementAndGet();
                                    }
                                }
                            }
                        }
                        if (!underTest.endEvictionTest(idleObjects)) {
                            // TODO - May need to add code here once additional
                            // states are used
                        }
                    }
                }
            }
        }
        if (isAbandonedConfig() && abandonedConfig.getRemoveAbandonedOnMaintenance()) {
            removeAbandoned();
        }
    }

    private PooledObject<T> create() throws Exception {
        int localMaxTotal = getMaxTotal();
        long newCreateCount = createCount.incrementAndGet();
        if (localMaxTotal > -1 && newCreateCount > localMaxTotal ||
                newCreateCount > Integer.MAX_VALUE) {
            createCount.decrementAndGet();
            return null;
        }

        T t = null;
        try {
            t = factory.makeObject();
        } catch (Exception e) {
            createCount.decrementAndGet();
            throw e;
        }

        final PooledObject<T> p;
        if (isAbandonedConfig() && abandonedConfig.getLogAbandoned()) {
            p = new PooledObject<T>(t, abandonedConfig.getLogWriter());
        } else {
            p = new PooledObject<T>(t);
        }
        createdCount.incrementAndGet();
        allObjects.put(t, p);
        return p;
    }

    private void destroy(PooledObject<T> toDestory) throws Exception {
        toDestory.invalidate();
        idleObjects.remove(toDestory);
        allObjects.remove(toDestory.getObject());
        try {
            factory.destroyObject(toDestory.getObject());
        } finally {
            destroyedCount.incrementAndGet();
            createCount.decrementAndGet();
        }
    }

    
    void ensureMinIdle() throws Exception {
        int minIdle = getMinIdle();
        if (minIdle < 1) {
            return;
        }

        while (idleObjects.size() < minIdle) {
            PooledObject<T> p = create();
            if (p == null) {
                // Can't create objects, no reason to think another call to
                // create will work. Give up.
                break;
            }
            if (getLifo()) {
                idleObjects.addFirst(p);
            } else {
                idleObjects.addLast(p);
            }
        }
    }

    /**
     * Create an object, and place it into the pool. addObject() is useful for
     * "pre-loading" a pool with idle objects.
     */
    
    public void addObject() throws Exception {
        assertOpen();
        if (factory == null) {
            throw new IllegalStateException(
                    "Cannot add objects without a factory.");
        }
        PooledObject<T> p = create();
        addIdleObject(p);
    }

    private void addIdleObject(PooledObject<T> p) throws Exception {
        if (p != null) {
            factory.passivateObject(p.getObject());
            if (getLifo()) {
                idleObjects.addFirst(p);
            } else {
                idleObjects.addLast(p);
            }
        }
    }

    private int getNumTests() {
        int numTestsPerEvictionRun = getNumTestsPerEvictionRun();
        if (numTestsPerEvictionRun >= 0) {
            return Math.min(numTestsPerEvictionRun, idleObjects.size());
        } else {
            return (int) (Math.ceil(idleObjects.size() /
                    Math.abs((double) numTestsPerEvictionRun)));
        }
    }

    /**
     * Recover abandoned objects which have been checked out but
     * not used since longer than the removeAbandonedTimeout.
     */
    private void removeAbandoned() {
        // Generate a list of abandoned objects to remove
        final long now = System.currentTimeMillis();
        final long timeout =
                now - (abandonedConfig.getRemoveAbandonedTimeout() * 1000L);
        ArrayList<PooledObject<T>> remove = new ArrayList<PooledObject<T>>();
        Iterator<PooledObject<T>> it = allObjects.values().iterator();
        while (it.hasNext()) {
            PooledObject<T> pooledObject = it.next();
            synchronized (pooledObject) {
                if (pooledObject.getState() == PooledObjectState.ALLOCATED &&
                        pooledObject.getLastUsed() <= timeout) {
                    pooledObject.markAbandoned();
                    remove.add(pooledObject);
                }
            }
        }

        // Now remove the abandoned objects
        Iterator<PooledObject<T>> itr = remove.iterator();
        while (itr.hasNext()) {
            PooledObject<T> pooledObject = itr.next();
            if (abandonedConfig.getLogAbandoned()) {
                pooledObject.printStackTrace();
            }
            try {
                invalidateObject(pooledObject.getObject());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //--- JMX support ----------------------------------------------------------

    /**
     * Return an estimate of the number of threads currently blocked waiting for
     * an object from the pool. This is intended for monitoring only, not for
     * synchronization control.
     */
    
    public int getNumWaiters() {
        if (getBlockWhenExhausted()) {
            return idleObjects.getTakeQueueLength();
        } else {
            return 0;
        }
    }


    // --- configuration attributes --------------------------------------------

    private volatile int maxIdle = GenericObjectPoolConfig.DEFAULT_MAX_IDLE;
    private volatile int minIdle = GenericObjectPoolConfig.DEFAULT_MIN_IDLE;
    private final PoolableObjectFactory<T> factory;


    // --- internal attributes -------------------------------------------------

    /*
     * All of the objects currently associated with this pool in any state. It
     * excludes objects that have been destroyed. The size of
     * {@link #allObjects} will always be less than or equal to {@link
     * #_maxActive}. Map keys are pooled objects, values are the PooledObject
     * wrappers used internally by the pool.
     */
    private final Map<T, PooledObject<T>> allObjects =
        new ConcurrentHashMap<T, PooledObject<T>>();
    /*
     * The combined count of the currently created objects and those in the
     * process of being created. Under load, it may exceed {@link #_maxActive}
     * if multiple threads try and create a new object at the same time but
     * {@link #create(boolean)} will ensure that there are never more than
     * {@link #_maxActive} objects created at any one time.
     */
    private final AtomicLong createCount = new AtomicLong(0);
    private final LinkedBlockingDeque<PooledObject<T>> idleObjects =
        new LinkedBlockingDeque<PooledObject<T>>();

    // JMX specific attributes
    private static final String ONAME_BASE =
        "org.apache.commoms.pool2:type=GenericObjectPool,name=";

    // Additional configuration properties for abandoned object tracking
    private volatile AbandonedConfig abandonedConfig = null;
}
