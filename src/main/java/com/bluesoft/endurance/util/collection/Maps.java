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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Dana P'Simer &lt;danap@bluesoftdev.com&gt;
 */
public class Maps {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger( Maps.class );

  private Maps() {
  }

  public static <V> Map<V, V> map( V... keysAndValues ) {
    Map<V, V> ret = new LinkedHashMap<>();
    assert keysAndValues.length % 2 == 0;
    for ( int i = 0; i < keysAndValues.length; i += 2 ) {
      ret.put( keysAndValues[i], keysAndValues[i + 1] );
    }
    return ret;
  }

  public static <K, V> Map<K, V> map( Pair<K, V>... keysAndValues ) {
    Map<K, V> ret = new LinkedHashMap<>();
    assert keysAndValues.length % 2 == 0;
    for ( Pair<K, V> keyAndValue : keysAndValues ) {
      ret.put( keyAndValue.getLeft(), keyAndValue.getRight() );
    }
    return ret;
  }

  public static <V> SortedMap<V, V> sortedMap( V... keysAndValues ) {
    SortedMap<V, V> ret = new TreeMap<>();
    assert keysAndValues.length % 2 == 0;
    for ( int i = 0; i < keysAndValues.length; i += 2 ) {
      ret.put( keysAndValues[i], keysAndValues[i + 1] );
    }
    return ret;
  }

  public static <K, V> SortedMap<K, V> sortedMap( Pair<K, V>... keysAndValues ) {
    SortedMap<K, V> ret = new TreeMap<>();
    assert keysAndValues.length % 2 == 0;
    for ( Pair<K, V> keyAndValue : keysAndValues ) {
      ret.put( keyAndValue.getLeft(), keyAndValue.getRight() );
    }
    return ret;
  }
}
