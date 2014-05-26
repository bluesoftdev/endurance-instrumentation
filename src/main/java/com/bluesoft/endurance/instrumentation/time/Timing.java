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

import java.util.LinkedList;
import java.util.List;
import com.bluesoft.endurance.instrumentation.Lambda;
import com.bluesoft.endurance.instrumentation.Procedure;
import com.bluesoft.endurance.util.ReentrantReadWriteLockHelper;

/**
 * A simple class that times operations and stores the samples in memory. Mostly for Test usage not Production code. See bluesoft-metrics for a
 * production class metrics library.
 * <p>
 * @author danap@bluesoftdev.com &lt;Dana H. P'Simer&gt;
 * @since 1.0.0
 */
public class Timing {

  private final ReentrantReadWriteLockHelper lock = new ReentrantReadWriteLockHelper();
  private List<Sample> samples = new LinkedList<>();

  /**
   * Times the operation, i.e. {@link Lambda#func()}
   * <p>
   * @param <T>      the return type of the operation.
   * @param callBack the operation.
   * <p>
   * @return the value returned by the operation.
   */
  public <T> T time( Lambda<T> callBack ) {
    long start = System.nanoTime();
    try {
      return callBack.func();
    } finally {
      long end = System.nanoTime();
      recordSample( start, end );
    }
  }

  /**
   * Times the operation, i.e. {@link Procedure#func()}.
   * <p>
   * @param callBack the operation.
   */
  public void time( final Procedure callBack ) {
    time( new Lambda<Void>() {
      @Override
      public Void func() {
        callBack.func();
        return null;
      }
    } );
  }

  /**
   * Records a sample for the timer.
   * <p>
   * @param startNano the start of the operation in nanoseconds since the beginning of the epoch.
   * @param endNano   the end of the operation in nanoseconds since the beginning of the epoch.
   */
  public void recordSample( final long startNano, final long endNano ) {
    lock.writeLock( new Procedure() {
      @Override
      public void func() {
        samples.add( new Sample( startNano, endNano ) );
      }
    } );
  }

  /**
   * @return the count of samples.
   */
  public int getCount() {
    return samples.size();
  }

  /**
   * @return the maximum timing for all samples.
   */
  public long getMax() {
    return lock.readLock( new Lambda<Long>() {
      @Override
      public Long func() {
        long max = 0;
        for ( Sample s : samples ) {
          max = Math.max( max, s.end - s.start );
        }
        return max;
      }
    } );
  }

  /**
   * @return the minimum timing for all samples.
   */
  public long getMin() {
    return lock.readLock( new Lambda<Long>() {
      @Override
      public Long func() {
        long min = Long.MAX_VALUE;
        for ( Sample s : samples ) {
          min = Math.min( min, s.end - s.start );
        }
        return min;
      }
    } );
  }

  /**
   * @return the average timing for all samples.
   */
  public long getAverage() {
    return lock.readLock( new Lambda<Long>() {
      @Override
      public Long func() {
        if ( samples.isEmpty() ) {
          return 0L;
        }
        long total = 0;
        for ( Sample s : samples ) {
          total += s.end - s.start;
        }
        return total / samples.size();
      }
    } );
  }

  /**
   * @return the standard deviation for all samples.
   */
  public long getStandardDev() {
    return lock.readLock( new Lambda<Long>() {
      @Override
      public Long func() {
        if ( samples.isEmpty() ) {
          return 0L;
        }
        long average = getAverage();
        long total = 0;
        for ( Sample s : samples ) {
          long variance = (s.end - s.start) - average;
          variance = variance * variance;
          total += variance;
        }
        return (long)Math.sqrt( total / samples.size() );
      }
    } );
  }

  /**
   * @return a string representing the current values of the count, average, min, max, and standard deviation.
   */
  @Override
  public String toString() {
    return String.format( "Timing {\n\tcount = %d\n\taverage = %d\n\tmin = %d\n\tmax = %d\n\tstandard deviation = %d\n}",
                          getCount(), getAverage(), getMin(), getMax(), getStandardDev() );
  }

  private class Sample {

    private long start;
    private long end;

    public Sample( long start, long end ) {
      this.start = start;
      this.end = end;
    }

    public long getStart() {
      return start;
    }

    public void setStart( long start ) {
      this.start = start;
    }

    public long getEnd() {
      return end;
    }

    public void setEnd( long end ) {
      this.end = end;
    }
  }
}
