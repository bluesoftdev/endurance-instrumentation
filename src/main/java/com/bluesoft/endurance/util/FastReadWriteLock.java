/*
 * Copyright 2014 Dana H. P'Simer & BluesSoft Development, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bluesoft.endurance.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link ReadWriteLock} that is faster then the one provided by the JDK.
 * <p>
 * @author danap@bluesoftdev.com &lt;Dana H. P'Simer&gt;
 * @since 1.0.0
 */
public class FastReadWriteLock implements ReadWriteLock {

  @SuppressWarnings( "unused" )
  private static final Logger LOG = LoggerFactory.getLogger( FastReadWriteLock.class );
  private final ThreadLocal<Integer> readHoldCount = new ThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return 0;
    }
  };
  private final ThreadLocal<Integer> writeHoldCount = new ThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return 0;
    }
  };
  private final AtomicInteger holdCounts = new AtomicInteger( 0 );
  private final Queue<Thread> awaitingReadLock;
  private final Queue<Thread> awaitingWriteLock;
  private final ReadLock readLock = new ReadLock();
  private final WriteLock writeLock = new WriteLock();

  /**
   * Create a FastReadWriteLock that does not use "fair" scheduling.
   */
  public FastReadWriteLock() {
    this( false );
  }

  /**
   * Create a FastReadWriteLock.
   * <p>
   * @param rwFair true if "fair" scheduling should be used.
   */
  public FastReadWriteLock( boolean rwFair ) {
    awaitingReadLock = new ConcurrentLinkedQueue<>();
    if ( rwFair ) {
      awaitingWriteLock = awaitingReadLock;
    } else {
      awaitingWriteLock = new ConcurrentLinkedQueue<>();
    }
  }

  /**
   * @return the read lock for this {@link ReadWriteLock}
   */
  @Override
  public Lock readLock() {
    return readLock;
  }

  /**
   * @return the write lock for this {@link ReadWriteLock}
   */
  @Override
  public Lock writeLock() {
    return writeLock;
  }

  private int writeLocks( int holdCounts ) {
    return holdCounts & 0xFFFF;
  }

  private int readLocks( int holdCounts ) {
    return holdCounts >>> 16;
  }

  private int incrementReadCount( int holdCounts ) {
    return ((readLocks( holdCounts ) + 1) << 16) | writeLocks( holdCounts );
  }

  private int decrementReadCount( int holdCounts ) {
    final int newReadCount = readLocks( holdCounts ) - 1;
    final int shiftedReadCount = (newReadCount) << 16;
    return shiftedReadCount | writeLocks( holdCounts );
  }

  boolean tryAcquireWriteLock() {
    if ( holdCounts.compareAndSet( writeHoldCount.get(), writeHoldCount.get() + 1 ) ) {
      writeHoldCount.set( writeHoldCount.get() + 1 );
      return true;
    }
    return false;
  }

  boolean tryAcquireReadLock() {
    int currentHoldCounts = holdCounts.get();
    if ( writeLocks( currentHoldCounts ) == 0 && holdCounts.compareAndSet( currentHoldCounts, incrementReadCount( currentHoldCounts ) ) ) {
      readHoldCount.set( readHoldCount.get() + 1 );
      return true;
    }
    return false;
  }

  void releaseReadLock() {
    int currentHoldCounts;
    do {
      currentHoldCounts = holdCounts.get();
    } while (!holdCounts.compareAndSet( currentHoldCounts, decrementReadCount( currentHoldCounts ) ));
    readHoldCount.set( readHoldCount.get() - 1 );
  }

  void releaseWriteLock() {
    holdCounts.decrementAndGet();
    writeHoldCount.set( writeHoldCount.get() - 1 );
  }

  private boolean acquireReadLock( boolean interruptable, long nanos ) {
    // if we already have a read lock, just increment the count and continue.
    if ( readHoldCount.get() > 0 ) {
      tryAcquireReadLock();
      return true;
    }
    boolean wasInterrupted = false;
    Thread current = Thread.currentThread();
    awaitingReadLock.add( current );

    boolean acquired = false;
    // Block while not first in queue or cannot acquire lock
    while (awaitingReadLock.peek() != current || !(acquired = tryAcquireReadLock())) {
      if ( nanos != -1L ) {
        long start = System.nanoTime();
        LockSupport.parkNanos( this, nanos );
        if ( System.nanoTime() - start > nanos ) {
          break;
        }
      } else {
        LockSupport.park( this );
      }
      if ( Thread.interrupted() ) {
        wasInterrupted = true;
        if ( interruptable ) {
          break;
        }
      }
    }
    awaitingReadLock.remove();
    if ( acquired ) {
      // let the next reader go too
      LockSupport.unpark( awaitingReadLock.peek() );
    }
    if ( wasInterrupted ) {
      if ( acquired ) {
        releaseReadLock();
      }
      current.interrupt();
    }
    return acquired;
  }

  private boolean acquireWriteLock( boolean interruptable, long nanos ) {
    // if we already have a read lock, just increment the count and continue.
    if ( writeHoldCount.get() > 0 ) {
      tryAcquireWriteLock();
      return true;
    }
    boolean acquired = false;
    boolean wasInterrupted = false;
    Thread current = Thread.currentThread();
    awaitingWriteLock.add( current );

    // Block while not first in queue or cannot acquire lock
    while (awaitingWriteLock.peek() != current || !(acquired = tryAcquireWriteLock())) {
      if ( nanos != -1L ) {
        long start = System.nanoTime();
        LockSupport.parkNanos( this, nanos );
        if ( System.nanoTime() - start > nanos ) {
          break;
        }
      } else {
        LockSupport.park( this );
      }
      if ( Thread.interrupted() ) {
        wasInterrupted = true;
        if ( interruptable ) {
          break;
        }
      }
    }

    awaitingWriteLock.remove();
    if ( wasInterrupted ) {
      if ( acquired ) {
        releaseWriteLock();
      }
      current.interrupt();
    }
    return acquired;
  }

  private class WriteLock implements Lock {

    @Override
    public void lock() {
      acquireWriteLock( false, -1L );
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      acquireWriteLock( true, -1L );
    }

    @Override
    public boolean tryLock() {
      return tryAcquireWriteLock();
    }

    @Override
    public boolean tryLock( long time, TimeUnit unit ) throws InterruptedException {
      return acquireWriteLock( true, unit.toNanos( time ) );
    }

    @Override
    public void unlock() {
      int holds = writeHoldCount.get();
      if ( holds == 0 ) {
        throw new IllegalStateException( "no write lock aquired by current thread" );
      }
      releaseWriteLock();
      if ( holds == 1 ) {
        LockSupport.unpark( awaitingReadLock.peek() );
        LockSupport.unpark( awaitingWriteLock.peek() );
      }
    }

    @Override
    public Condition newCondition() {
      throw new UnsupportedOperationException( "Conditions are not supported." );
    }
  }

  private class ReadLock implements Lock {

    @Override
    public void lock() {
      acquireReadLock( false, -1L );
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      if ( Thread.currentThread().isInterrupted() ) {
        Thread.currentThread().interrupt();
      }
      acquireReadLock( true, -1L );
    }

    @Override
    public boolean tryLock() {
      return tryAcquireReadLock();
    }

    @Override
    public boolean tryLock( long time, TimeUnit unit ) throws InterruptedException {
      return acquireReadLock( true, unit.toNanos( time ) );
    }

    @Override
    public void unlock() {
      int readHolds = readHoldCount.get();
      if ( readHolds == 0 ) {
        throw new IllegalStateException( "no read lock aquired by current thread" );
      }
      releaseReadLock();
      LockSupport.unpark( awaitingReadLock.peek() );
      LockSupport.unpark( awaitingWriteLock.peek() );
    }

    @Override
    public Condition newCondition() {
      throw new UnsupportedOperationException( "Conditions are not supported." );
    }
  }
}
