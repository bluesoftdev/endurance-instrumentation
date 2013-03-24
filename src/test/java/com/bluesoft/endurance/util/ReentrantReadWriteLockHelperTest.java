package com.bluesoft.endurance.util;

import com.bluesoft.endurance.instrumentation.Procedure;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.testng.annotations.Test;

/**
 *
 * @author danap
 */
public class ReentrantReadWriteLockHelperTest {

  private ReentrantReadWriteLockHelper lock = new ReentrantReadWriteLockHelper();
  private static final int READER_TIME = 0;
  private static final int WRITER_TIME = 1;

  @Test
  public void testReadLock() throws Exception {
    final long[] timestamps = new long[2];
    final Thread writeLocker = new Thread() {
      @Override
      public void run() {
        lock.writeLock( new Procedure() {
          @Override
          public void func() {
            timestamps[WRITER_TIME] = System.nanoTime();
          }
        } );
      }
    };
    lock.readLock( new Procedure() {
      @Override
      public void func() {
        try {
          writeLocker.start();
          Thread.sleep( 1L );
          timestamps[READER_TIME] = System.nanoTime();
        } catch ( InterruptedException ex ) {
          Logger.getLogger( ReentrantReadWriteLockHelperTest.class.getName() ).log( Level.SEVERE, null, ex );
        }
      }
    } );

    writeLocker.join();

    assert timestamps[READER_TIME] <= timestamps[WRITER_TIME];
  }

  public void testUpgradeReadLock() throws Exception {
    final long[] timestamps = new long[2];
    lock.readLock( new Procedure() {
      @Override
      public void func() {
        lock.writeLock( new Procedure() {
          @Override
          public void func() {
            timestamps[WRITER_TIME] = System.nanoTime();
          }
        } );
        timestamps[READER_TIME] = System.nanoTime();
      }
    } );
    assert timestamps[WRITER_TIME] < timestamps[READER_TIME];
  }
}
