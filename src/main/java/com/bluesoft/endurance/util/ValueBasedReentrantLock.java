package com.bluesoft.endurance.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.bluesoft.endurance.instrumentation.Lambda;
import com.bluesoft.endurance.instrumentation.Procedure;

/**
 *
 * @author psimerd
 */
public class ValueBasedReentrantLock<V> {
  private ReentrantLock lock = new ReentrantLock();
  private Map<V, LockHolder> locks = new HashMap<>();

  public ReentrantLock checkOutLock( final V value ) {
    lock.lock();
    try {
      LockHolder holder = locks.get( value );
      if ( holder == null ) {
        holder = new LockHolder();
        locks.put( value, holder );
      }
      return holder.checkOut();
    } finally {
      lock.unlock();
    }
  }

  public void checkInLock( final V value ) {
    lock.lock();
    try {
      final LockHolder holder = locks.get( value );
      if ( holder == null ) {
        throw new IllegalStateException( "Reentrant lock for the given value does not exist." );
      }
      holder.checkIn();
      if ( holder.getRefCount() == 0 ) {
        locks.remove( value );
      }
    } finally {
      lock.unlock();
    }
  }

  private class LockHolder {
    private ReentrantLock lock;
    private int refCount = 0;

    public LockHolder() {
      lock = new ReentrantLock();
    }

    public synchronized ReentrantLock checkOut() {
      refCount += 1;
      return lock;
    }

    public synchronized void checkIn() {
      refCount -= 1;
    }

    public int getRefCount() {
      return refCount;
    }
  }
}
