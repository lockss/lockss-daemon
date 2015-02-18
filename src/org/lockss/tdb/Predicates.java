/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

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
