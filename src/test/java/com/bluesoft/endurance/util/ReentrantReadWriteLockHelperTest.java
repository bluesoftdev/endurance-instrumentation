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
