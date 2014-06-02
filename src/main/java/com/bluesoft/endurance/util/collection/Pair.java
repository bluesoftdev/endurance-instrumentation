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

/**
 * A simple Tuple that holds a pair of objects. The left is kept in position 0, the right in position 1.
 * <p>
 * @param <L> the type of the left element.
 * @param <R> the type of the right element.
 * <p>
 * @author Dana P'Simer &lt;danap@bluesoftdev.com&gt;
 */
public class Pair<L, R> extends Tuple {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger( Pair.class );

  protected Pair( L left, R right ) {
    super( left, right );
  }

  /**
   * @return the left hand value.
   */
  public L getLeft() {
    return (L)super.get( 0 );
  }

  /**
   * @return the right hand value.
   */
  public R getRight() {
    return (R)super.get( 1 );
  }

  /**
   * Make a pair.
   * <p>
   * @param <L>   the type of the left hand value.
   * @param <R>   the type of the right hand value.
   * @param left  the left hand value.
   * @param right the right hand value.
   * <p>
   * @return the pair.
   */
  public static <L, R> Pair<L, R> pairOf( L left, R right ) {
    return new Pair( left, right );
  }
}
