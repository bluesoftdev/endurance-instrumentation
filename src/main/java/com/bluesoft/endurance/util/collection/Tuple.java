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
package com.bluesoft.endurance.util.collection;

import java.util.Arrays;

/**
 * A simple immutable holder of a group of values.
 * <p>
 * @author Dana P'Simer &lt;danap@bluesoftdev.com&gt;
 */
public class Tuple<T> {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger( Tuple.class );
  private final T[] values;

  protected Tuple( T... values ) {
    this.values = Arrays.copyOf( values, values.length );
  }

  public Object get( int i ) {
    return values[i];
  }

  public static <T> Tuple of( T... values ) {
    return new Tuple( values );
  }
}
