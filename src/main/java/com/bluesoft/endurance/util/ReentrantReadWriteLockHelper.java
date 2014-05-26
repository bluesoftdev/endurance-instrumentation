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

import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.bluesoft.endurance.instrumentation.Lambda;
import com.bluesoft.endurance.instrumentation.Procedure;

/**
 * A helper that wraps a ReentrantReadWriteLock and implements template functions for manipulating it.
 * <p>
 * @author danap@bluesoftdev.com &lt;Dana H. P'Simer&gt;
 * @since 1.0.0
 */
public class ReentrantReadWriteLockHelper {

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock( true );

  /**
   * Acquires a read lock and then executes the passed {@link Lambda}. Releases the lock after the lambda exits.
   * <p>
   * @param <T>  the return type of the {@link Lambda}
   * @param todo the lambda to execute.
   * <p>
   * @return the value returned from the lambda.
   */
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

  /**
   * Acquire a read lock and then execute the {@link Procedure}. Releases the lock after the procedure exits.
   * <p>
   * @param todo the procedure to execute.
   */
  public void readLock( final Procedure todo ) {
    readLock( new Lambda<Void>() {
      @Override
      public Void func() {
        todo.func();
        return null;
      }
    } );
  }

  /**
   * Acquires a write lock and then executes the passed {@link Lambda}. Releases the lock after the lambda exits.
   * <p>
   * @param <T>  the return type of the {@link Lambda}
   * @param todo the lambda to execute.
   * <p>
   * @return the value returned from the lambda.
   */
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

  /**
   * Acquire a write lock and then execute the {@link Procedure}. Releases the lock after the procedure exits.
   * <p>
   * @param todo the procedure to execute.
   */
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
