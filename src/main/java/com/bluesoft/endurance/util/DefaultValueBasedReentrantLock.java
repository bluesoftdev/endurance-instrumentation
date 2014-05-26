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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages a collection of {@link ReentrantLock}s that are associated with a given value.
 * <p>
 * @param <V> the value type.
 * <p>
 * @author danap@bluesoftdev.com &lt;Dana H. P'Simer&gt;
 * @since 1.0.0
 */
public class DefaultValueBasedReentrantLock<V> implements ValueBasedReentrantLock<V> {

  private final ReentrantLock lock = new ReentrantLock();
  private final Map<V, LockHolder> locks = new HashMap<>();

  /**
   * checks out the lock associated with the given value.
   * <p>
   * @param value the value
   * <p>
   * @return the lock.
   */
  @Override
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

  /**
   * checks in the lock associated with the given value.
   * <p>
   * @param value the value.
   */
  @Override
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

    private final ReentrantLock lock;
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
