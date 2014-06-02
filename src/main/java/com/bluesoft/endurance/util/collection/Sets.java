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
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author Dana P'Simer &lt;danap@bluesoftdev.com&gt;
 */
public class Sets {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger( Sets.class );

  private Sets() {
  }

  public static <T> Set<T> set( T... args ) {
    Set<T> ret = new LinkedHashSet<>();
    ret.addAll( Arrays.asList( args ) );
    return ret;
  }
}
