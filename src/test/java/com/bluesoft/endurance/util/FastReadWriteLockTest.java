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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.bluesoft.endurance.instrumentation.Procedure;
import com.bluesoft.endurance.instrumentation.time.Timing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 *
 * @author psimerd
 */
public class FastReadWriteLockTest {
  private static final Logger LOG = LoggerFactory.getLogger( FastReadWriteLockTest.class );
  private static final int READER_TIME = 0;
  private static final int WRITER_TIME = 1;
  private static final boolean TIMING_ENABLED = true;
  private Timing jucReadTiming = new Timing();
  private Timing jucWriteTiming = new Timing();
  private Timing frwReadTiming = new Timing();
  private Timing frwWriteTiming = new Timing();
  private Timing frwReadFairTiming = new Timing();
  private Timing frwWriteFairTiming = new Timing();

  @DataProvider( name = "locks" )
  public Object[][] locks() {
    return new Object[][] {
      new Object[] { new ReentrantReadWriteLock( false ), jucReadTiming, jucWriteTiming },
      new Object[] { new FastReadWriteLock(), frwReadTiming, frwWriteTiming },
      new Object[] { new FastReadWriteLock( true ), frwReadFairTiming, frwWriteFairTiming }
    };
  }

  @Test( dataProvider = "locks", enabled = TIMING_ENABLED )
  public void testReadWriteLock( final ReadWriteLock lock, final Timing readTimer, final Timing writeTimer ) throws Exception {
    final AtomicInteger readerCount = new AtomicInteger( 0 );
    final AtomicInteger writerCount = new AtomicInteger( 0 );
    ExecutorService executor = Executors.newFixedThreadPool( 10 );
    for ( int i = 0; i < 1000; i++ ) {
      executor.submit( new Runnable() {
        @Override
        public void run() {
          int chance = ThreadLocalRandom.current().nextInt( 100 );
          final Lock theLock = chance < 80 ? lock.readLock() : lock.writeLock();
          final AtomicInteger theCount = chance < 80 ? readerCount : writerCount;
          final AtomicInteger theOtherCount = chance < 80 ? writerCount : readerCount;
          final Timing timer = chance < 80 ? readTimer : writeTimer;
          timer.time( new Procedure() {
            @Override
            public void func() {
              theLock.lock();
              try {
                theCount.incrementAndGet();
                Thread.sleep( 1L );
                if ( theOtherCount.get() != 0 ) {
                  LOG.info( "found a circumstance where the other count was not zero!" );
                }
              } catch ( InterruptedException ex ) {
              } finally {
                theCount.decrementAndGet();
                theLock.unlock();
              }
            }
          } );
        }
      } );
    }

    executor.shutdown();
    executor.awaitTermination( 20, TimeUnit.SECONDS );
    assertEquals( readerCount.get(), 0, "reader count is not zero." );
    assertEquals( writerCount.get(), 0, "writer count is not zero." );
  }

  @AfterClass( enabled = TIMING_ENABLED )
  public void printTimings() {
    LOG.info( "JUC READ {}", jucReadTiming );
    LOG.info( "JUC WRITE {}", jucWriteTiming );
    LOG.info( "FRW READ {}", frwReadTiming );
    LOG.info( "FRW WRITE {}", frwWriteTiming );
    LOG.info( "FRW READ FAIR {}", frwReadFairTiming );
    LOG.info( "FRW WRITE FAIR {}", frwWriteFairTiming );
  }

  @Test( timeOut = 1000L )
  public void testReadLock() throws Exception {
    final ReadWriteLock lock = new FastReadWriteLock();
    final long[] timestamps = new long[2];
    final Thread writeLocker = new Thread() {
      @Override
      public void run() {
        lock.writeLock().lock();
        try {
          timestamps[WRITER_TIME] = System.nanoTime();
        } finally {
          lock.writeLock().unlock();
        }
      }
    };
    lock.readLock().lock();
    try {
      writeLocker.start();
      Thread.sleep( 1L );
      timestamps[READER_TIME] = System.nanoTime();
    } finally {
      lock.readLock().unlock();
    }
    writeLocker.join();

    assertTrue( timestamps[READER_TIME] <= timestamps[WRITER_TIME],
                "reader timestamp, " + timestamps[READER_TIME] + ", was greater than writer timestamp, " + timestamps[WRITER_TIME] );
  }

  @Test( timeOut = 1000L )
  public void testMultipleReadLock() throws Exception {
    final ReadWriteLock lock = new FastReadWriteLock();
    final long[] timestamps = new long[2];
    final Thread writeLocker = new Thread() {
      @Override
      public void run() {
        lock.writeLock().lock();
        try {
          timestamps[WRITER_TIME] = System.nanoTime();
        } finally {
          lock.writeLock().unlock();
        }
      }
    };
    lock.readLock().lock();
    lock.readLock().lock();
    lock.readLock().lock();
    lock.readLock().lock();
    try {
      writeLocker.start();
      Thread.sleep( 1L );
      timestamps[READER_TIME] = System.nanoTime();
    } finally {
      lock.readLock().unlock();
      lock.readLock().unlock();
      lock.readLock().unlock();
      lock.readLock().unlock();
    }
    writeLocker.join();

    assertTrue( timestamps[READER_TIME] <= timestamps[WRITER_TIME],
                "reader timestamp, " + timestamps[READER_TIME] + ", was greater than writer timestamp, " + timestamps[WRITER_TIME] );
  }

  @Test( timeOut = 1000L )
  public void testWriteLock() throws Exception {
    final ReadWriteLock lock = new FastReadWriteLock();
    final long[] timestamps = new long[2];
    final Thread readLocker = new Thread() {
      @Override
      public void run() {
        lock.readLock().lock();
        try {
          timestamps[READER_TIME] = System.nanoTime();
        } finally {
          lock.readLock().unlock();
        }
      }
    };
    lock.writeLock().lock();
    try {
      readLocker.start();
      Thread.sleep( 1L );
      timestamps[WRITER_TIME] = System.nanoTime();
    } finally {
      lock.writeLock().unlock();
    }
    readLocker.join();

    assertTrue( timestamps[READER_TIME] >= timestamps[WRITER_TIME],
                "reader timestamp, " + timestamps[READER_TIME] + ", was less than writer timestamp, " + timestamps[WRITER_TIME] );
  }

  @Test( expectedExceptions = IllegalStateException.class )
  public void testReadUnlockWithoutLock() {
    final ReadWriteLock lock = new FastReadWriteLock();
    lock.readLock().unlock();
  }

  @Test( expectedExceptions = IllegalStateException.class )
  public void testWriteUnlockWithoutLock() {
    final ReadWriteLock lock = new FastReadWriteLock();
    lock.writeLock().unlock();
  }
}
