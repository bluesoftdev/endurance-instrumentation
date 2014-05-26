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

import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages a collection of {@link ReentrantLock}s that are associated with a given value.
 * <p>
 * @param <V> the value type.
 * <p>
 * @author danap@bluesoftdev.com &lt;Dana H. P'Simer&gt;
 * @since 1.0.0
 */
public interface ValueBasedReentrantLock<V> {

  /**
   * checks in the lock associated with the given value.
   * <p>
   * @param value the value.
   */
  void checkInLock( final V value );

  /**
   * checks out the lock associated with the given value.
   * <p>
   * @param value the value
   * <p>
   * @return the lock.
   */
  ReentrantLock checkOutLock( final V value );

}
