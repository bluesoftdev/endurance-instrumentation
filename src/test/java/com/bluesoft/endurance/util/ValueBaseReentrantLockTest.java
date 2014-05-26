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
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

import org.testng.annotations.Test;

/**
 *
 * @author psimerd
 */
public class ValueBaseReentrantLockTest {
  @Test
  public void test() {
    final DefaultValueBasedReentrantLock<Value> locks = new DefaultValueBasedReentrantLock<>();

    Value foo = new Value( "foo" );
    Value snafu = new Value( "snafu" );
    Value foobar = new Value( "foobar" );
    final Value[] values = new Value[] { foo, snafu, foobar };
    final Map<Value, Integer> maxConcurrent = new ConcurrentHashMap<>();
    final Map<Value, Set<Thread>> concurrentThreads = new ConcurrentHashMap<>();

    ExecutorService executor = Executors.newFixedThreadPool( 5 );
    for ( int i = 0; i < 1000; i++ ) {
      executor.submit( new Runnable() {
        @Override
        public void run() {
          Value value = values[ThreadLocalRandom.current().nextInt( 3 )];
          ReentrantLock lock = locks.checkOutLock( value );
          lock.lock();
          Set<Thread> threads = null;
          try {
            threads = concurrentThreads.get( value );
            if ( threads == null ) {
              concurrentThreads.put( value, threads = new HashSet<>() );
            }
            threads.add( Thread.currentThread() );
            Integer max = maxConcurrent.get( value );
            if ( max == null ) {
              max = threads.size();
            } else {
              max = Math.max( threads.size(), max );
            }
            maxConcurrent.put( value, max );
            Thread.sleep( 100L );
          } catch ( InterruptedException ex ) {
            // IGNORE
          } finally {
            if ( threads != null ) {
              threads.remove( Thread.currentThread() );
            }
            lock.unlock();
            locks.checkInLock( value );
          }
        }
      } );

      for ( Map.Entry<Value, Integer> mce : maxConcurrent.entrySet() ) {
        assert mce.getValue() <= 1 : "expected <= 1 and found " + mce.getValue() + " for " + mce.getKey();
      }
    }

  }

  private class Value {
    private String string;

    public Value( String string ) {
      this.string = string;
    }

    public String getString() {
      return string;
    }

    @Override
    public int hashCode() {
      int hash = 5;
      hash = 67 * hash + Objects.hashCode( this.string );
      return hash;
    }

    @Override
    public boolean equals( Object obj ) {
      if ( obj == null ) {
        return false;
      }
      if ( getClass() != obj.getClass() ) {
        return false;
      }
      final Value other = (Value) obj;
      if ( !Objects.equals( this.string, other.string ) ) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return "Value{string=" + string + '}';
    }
  }
}
