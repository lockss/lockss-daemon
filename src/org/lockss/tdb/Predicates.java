/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University,
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.tdb;

/**
 * <p>
 * Useful {@link Predicate} utilities.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class Predicates {

  /**
   * <p>
   * Prevent instantionation.
   * </p>
   * 
   * @since 1.67
   */
  private Predicates() {
    // Prevent instantiation
  }

  /**
   * <p>
   * A predicate that is always true.
   * </p>
   * 
   * @author Thib Guicherd-Callin
   * @param <A> The argument type over which the predicate applies.
   * @since 1.67
   */
  public static class TruePredicate<A> implements Predicate<A> {
    
    @Override
    public boolean test(A a) {
      return true;
    }
    
  }
  
  /**
   * <p>
   * A predicate that is always false.
   * </p>
   * 
   * @author Thib Guicherd-Callin
   * @param <A> The argument type over which the predicate applies.
   * @since 1.67
   */
  public static class FalsePredicate<A> implements Predicate<A> {
    
    @Override
    public boolean test(A a) {
      return false;
    }
    
  }
  
  /**
   * <p>
   * An abstract predicate that has another predicate as a member.
   * </p>
   * 
   * @author Thib Guicherd-Callin
   * @param <A> The argument type over which the predicate applies.
   * @since 1.67
   */
  public static abstract class UnaryPredicate<A> implements Predicate<A> {
    
    /**
     * <p>
     * A predicate.
     * </p>
     * 
     * @since 1.67
     */
    protected Predicate<A> predicate1;
    
    /**
     * <p>
     * Makes a new unary predicate with the given predicate.
     * </p>
     * 
     * @param predicate1 A predicate.
     */
    public UnaryPredicate(Predicate<A> predicate1) {
      this.predicate1 = predicate1;
    }
    
  }

  /**
   * <p>
   * A predicate that is the opposite of another predicate.
   * </p>
   * 
   * @author Thib Guicherd-Callin
   * @param <A> The argument type over which the predicate applies.
   * @since 1.67
   */
  public static class NotPredicate<A> extends UnaryPredicate<A> {
    
    /**
     * <p>
     * Makes a new predicate that is the opposite of the given predicate.
     * </p>
     * 
     * @param predicate1 A predicate to negate.
     * @since 1.67
     */
    public NotPredicate(Predicate<A> predicate1) {
      super(predicate1);
    }
    
    @Override
    public boolean test(A a) {
      return !predicate1.test(a);
    }
    
  }
  
  /**
   * <p>
   * An abstract predicate that has two other predicates as members.
   * </p>
   * 
   * @author Thib Guicherd-Callin
   * @param <A> The argument type over which the predicate applies.
   * @since 1.67
   */
  public static abstract class BinaryPredicate<A> extends UnaryPredicate<A> {
    
    /**
     * <p>
     * Another predicate.
     * </p>
     * 
     * @since 1.67
     */
    protected Predicate<A> predicate2;
    
    /**
     * <p>
     * Makes a new binary predicate with the two given predicates. 
     * </p>
     * 
     * @param predicate1 A predicate.
     * @param predicate2 Another predicate.
     * @since 1.67
     */
    public BinaryPredicate(Predicate<A> predicate1,
                           Predicate<A> predicate2) {
      super(predicate1);
      this.predicate2 = predicate2;
    }
    
  }

  /**
   * <p>
   * A predicate that is true if and only if both of its constituent predicates
   * are true.
   * </p>
   * <p>This is implemented in usual short-circuit manner: if the first
   * predicate evaluates to false, the second predicate is not evaluated.</p>
   * 
   * @author Thib Guicherd-Callin
   * @param <A> The argument type over which the predicate applies.
   * @since 1.67
   */
  public static class AndPredicate<A> extends BinaryPredicate<A> {
    
    /**
     * <p>
     * Makes a new predicate that is true if and only if both given predicates
     * are true.
     * </p>
     * 
     * @param predicate1 A predicate.
     * @param predicate2 Another predicate.
     * @since 1.67
     */
    public AndPredicate(Predicate<A> predicate1,
                        Predicate<A> predicate2) {
      super(predicate1, predicate2);
    }
    
    @Override
    public boolean test(A a) {
      return predicate1.test(a) && predicate2.test(a);
    }
    
  }

  /**
   * <p>
   * A predicate that is true if and only if either of its constituent
   * predicates is true.
   * </p>
   * <p>
   * This is implemented in usual short-circuit manner: if the first
   * predicate evaluates to true, the second predicate is not evaluated.
   * </p>
   * 
   * @author Thib Guicherd-Callin
   * @param <A>
   *          The argument type over which the predicate applies.
   * @since 1.67
   */
  public static class OrPredicate<A> extends BinaryPredicate<A> {
    
    /**
     * <p>
     * Makes a new predicate that is true if and only if either given predicates
     * is true.
     * </p>
     * 
     * @param predicate1 A predicate.
     * @param predicate2 Another predicate.
     * @since 1.67
     */
    public OrPredicate(Predicate<A> predicate1,
                       Predicate<A> predicate2) {
      super(predicate1, predicate2);
    }
    
    @Override
    public boolean test(A a) {
      return predicate1.test(a) || predicate2.test(a);
    }
    
  }

}
