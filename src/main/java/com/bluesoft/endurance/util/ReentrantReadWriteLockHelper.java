package com.bluesoft.endurance.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.bluesoft.endurance.instrumentation.Lambda;
import com.bluesoft.endurance.instrumentation.Procedure;

/**
 *
 * @author psimerd
 */
public class ReentrantReadWriteLockHelper {

  private ReentrantReadWriteLock lock = new ReentrantReadWriteLock( true );

  public <T> T readLock( Lambda<T> todo ) {
    boolean hasReadLock = lock.getReadHoldCount() == 1;
    if ( !hasReadLock ) {
      lock.readLock().lock();
    }
    try {
      return todo.func();
    } finally {
      if ( !hasReadLock ) {
        lock.readLock().unlock();
      }
    }
  }

  public void readLock( final Procedure todo ) {
    readLock( new Lambda<Void>() {
      @Override
      public Void func() {
        todo.func();
        return null;
      }
    } );
  }

  public <T> T writeLock( Lambda<T> todo ) {
    boolean hasReadLock = lock.getReadHoldCount() == 1;
    if ( hasReadLock ) {
      lock.readLock().unlock();
    }
    lock.writeLock().lock();
    try {
      return todo.func();
    } finally {
      if ( hasReadLock ) {
        lock.readLock().lock();
      }
      lock.writeLock().unlock();
    }
  }

  public void writeLock( final Procedure todo ) {
    writeLock( new Lambda<Void>() {
      @Override
      public Void func() {
        todo.func();
        return null;
      }
    } );
  }
}
