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
package com.bluesoft.endurance.instrumentation.time;

import com.bluesoft.endurance.instrumentation.Lambda;
import com.bluesoft.endurance.instrumentation.Procedure;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.testng.annotations.Test;

/**
 *
 * @author danap
 */
public class TimingTest {

  private static final int COUNT = 100;

  @Test
  public void testTiming() throws Exception {
    Timing test = new Timing();
    test.time( new Procedure() {
      @Override
      public void func() {
        try {
          Thread.sleep( 100 );
        } catch ( InterruptedException ex ) {
          Logger.getLogger( TimingTest.class.getName() ).log( Level.SEVERE, null, ex );
        }
      }
    } );
    assert Math.abs( test.getAverage() - 100000000L ) < 5000000 : "test.average = " + test.getAverage();
    assert Math.abs( test.getMax() - 100000000L ) < 5000000 : "test.max = " + test.getMax();
    assert Math.abs( test.getMin() - 100000000L ) < 5000000 : "test.min = " + test.getMin();
    assert test.getStandardDev() == 0 : "test.stdDev = " + test.getStandardDev();
  }

  @Test( /**/ timeOut = 4000L /**/ )
  public void testRandomTimingsSeries() {
    Timing test = new Timing();
    final Random rand = new Random();
    long expectedMin = Long.MAX_VALUE;
    long expectedMax = 0;
    long totalTime = 0L;
    final long expectedStandardDeviation = 10000000L;
    for ( int i = 0; i < COUNT; i++ ) {
      long time = test.time( new Lambda<Long>() {
        @Override
        public Long func() {
          long time = -1;
          try {
            do {
              time = 30000000L + (long)(rand.nextGaussian() * expectedStandardDeviation);
            } while (time < 0);
            Thread.sleep( time / 1000000L );
          } catch ( InterruptedException ex ) {
            Logger.getLogger( TimingTest.class.getName() ).log( Level.SEVERE, null, ex );
          }
          return time;
        }
      } );
      if ( time != -1 ) {
        totalTime += time;
        expectedMin = Math.min( expectedMin, time );
        expectedMax = Math.max( expectedMax, time );
      }
    }
    long expectedAverage = totalTime / COUNT;
    assert Math.abs( test.getAverage() - expectedAverage ) < 5000000 : "test.average = " + test.getAverage();
    assert Math.abs( test.getMin() - expectedMin ) < 5000000 : "test.min = " + test.getMin();
    assert Math.abs( test.getMax() - expectedMax ) < 5000000 : "test.max = " + test.getMax();
    assert Math.abs( test.getStandardDev() - expectedStandardDeviation ) < 5000000L : "test.stdDev = " + test.
            getStandardDev();
  }

  @Test( /* timeOut = 4000L /**/)
  public void testMultiThreadedRandomTimingsSeries() throws Exception {
    final Timing test = new Timing();
    final Random rand = new Random();
    final AtomicLong expectedMin = new AtomicLong( Long.MAX_VALUE );
    final AtomicLong expectedMax = new AtomicLong( 0 );
    final AtomicLong totalTime = new AtomicLong( 0L );
    final long expectedStandardDeviation = 10000000L;
    ExecutorService executor = Executors.newFixedThreadPool( 5 );
    List<Future<?>> futures = new ArrayList<>();
    for ( int i = 0; i < 5; i++ ) {
      futures.add( executor.submit( new Runnable() {
        @Override
        public void run() {
          for ( int i = 0; i < COUNT; i++ ) {
            long time = test.time( new Lambda<Long>() {
              @Override
              public Long func() {
                long time = -1;
                try {
                  do {
                    time = 30000000L + (long)(rand.nextGaussian() * expectedStandardDeviation);
                  } while (time < 0);
                  Thread.sleep( time / 1000000L );
                } catch ( InterruptedException ex ) {
                  Logger.getLogger( TimingTest.class.getName() ).log( Level.SEVERE, null, ex );
                }
                return time;
              }
            } );
            if ( time != -1 ) {
              totalTime.addAndGet( time );
              long exMin;
              while ((exMin = expectedMin.get()) > time) {
                expectedMin.compareAndSet( exMin, time );
              }
              long exMax;
              while ((exMax = expectedMax.get()) < time) {
                expectedMax.compareAndSet( exMax, time );
              }
            }
          }
        }
      }, null ) );
    }

    for ( Future<?> f : futures ) {
      f.get();
    }

    long expectedAverage = totalTime.get() / (COUNT * 5);
    assert Math.abs( test.getAverage() - expectedAverage ) < 5000000L : "expectedAverage = " + expectedAverage + ", test.average = " + test.
            getAverage();
    assert Math.abs( test.getMin() - expectedMin.get() ) < 5000000L : "test.min = " + test.getMin();
    assert Math.abs( test.getMax() - expectedMax.get() ) < 5000000L : "test.max = " + test.getMax();
    assert Math.abs( test.getStandardDev() - expectedStandardDeviation ) < 5000000L : "test.stdDev = " + test.getStandardDev();
  }
}
