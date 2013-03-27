package com.bluesoft.endurance.util;

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
  private ReentrantReadWriteLockHelper lock = new ReentrantReadWriteLockHelper();
  private Map<V, LockHolder> locks = new ConcurrentHashMap<>();

  public ReentrantLock checkOutLock( final V value ) {
    return lock.readLock( new Lambda<ReentrantLock>() {
      @Override
      public ReentrantLock func() {
        LockHolder holder = locks.get( value );
        if ( holder == null ) {
          holder = lock.writeLock( new Lambda<LockHolder>() {
            @Override
            public LockHolder func() {
              LockHolder holder = locks.get( value );
              if ( holder == null ) {
                holder = new LockHolder();
                locks.put( value, holder );
              }
              return holder;
            }
          } );
        }
        return holder.checkOut();
      }
    } );
  }

  public void checkInLock( final V value ) {
    lock.readLock( new Procedure() {
      @Override
      public void func() {
        final LockHolder holder = locks.get( value );
        if ( holder == null ) {
          throw new IllegalStateException( "Reentrant lock for the given value does not exist." );
        }
        holder.checkIn();
        if ( holder.getRefCount() == 0 ) {
          lock.writeLock( new Procedure() {
            @Override
            public void func() {
              if ( holder.getRefCount() == 0 ) {
                locks.remove( value );
              }
            }
          } );
        }
      }
    } );
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
