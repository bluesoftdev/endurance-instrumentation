package com.bluesoft.endurance.util;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author psimerd
 */
public class FastReadWriteLock implements ReadWriteLock {
  private static final Logger LOG = LoggerFactory.getLogger( FastReadWriteLock.class );
  private ThreadLocal<Integer> readHoldCount = new ThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return 0;
    }
  };
  private ThreadLocal<Integer> writeHoldCount = new ThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return 0;
    }
  };
  private AtomicInteger holdCounts = new AtomicInteger( 0 );
  private Queue<Thread> awaitingReadLock;
  private Queue<Thread> awaitingWriteLock;
  private ReadLock readLock = new ReadLock();
  private WriteLock writeLock = new WriteLock();

  public FastReadWriteLock() {
    this( false );
  }

  public FastReadWriteLock( boolean rwFair ) {
    awaitingReadLock = new ConcurrentLinkedQueue<>();
    if ( rwFair ) {
      awaitingWriteLock = awaitingReadLock;
    } else {
      awaitingWriteLock = new ConcurrentLinkedQueue<>();
    }
  }

  @Override
  public Lock readLock() {
    return readLock;
  }

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
    } while ( !holdCounts.compareAndSet( currentHoldCounts, decrementReadCount( currentHoldCounts ) ) );
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
    while ( awaitingReadLock.peek() != current || !(acquired = tryAcquireReadLock()) ) {
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
    while ( awaitingWriteLock.peek() != current || !(acquired = tryAcquireWriteLock()) ) {
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
