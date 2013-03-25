package com.bluesoft.endurance.instrumentation.time;

import java.util.LinkedList;
import java.util.List;
import com.bluesoft.endurance.instrumentation.Lambda;
import com.bluesoft.endurance.instrumentation.Procedure;
import com.bluesoft.endurance.util.ReentrantReadWriteLockHelper;

/**
 *
 * @author psimerd
 */
public class Timing {
  private ReentrantReadWriteLockHelper lock = new ReentrantReadWriteLockHelper();
  private List<Sample> samples = new LinkedList<Sample>();

  public <T> T time( Lambda<T> callBack ) {
    long start = System.nanoTime();
    try {
      return callBack.func();
    } catch ( RuntimeException ex ) {
      throw ex;
    } finally {
      long end = System.nanoTime();
      recordSample( start, end );
    }
  }

  public void time( final Procedure callBack ) {
    time( new Lambda<Void>() {
      @Override
      public Void func() {
        callBack.func();
        return null;
      }
    } );
  }

  public void recordSample( final long startNano, final long endNano ) {
    lock.writeLock( new Procedure() {
      @Override
      public void func() {
        samples.add( new Sample( startNano, endNano ) );
      }
    } );
  }

  public int getCount() {
    return samples.size();
  }

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
        return (long) Math.sqrt( total / samples.size() );
      }
    } );
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
