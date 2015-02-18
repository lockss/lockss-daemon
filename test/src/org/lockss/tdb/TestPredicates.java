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

import org.lockss.tdb.Predicates.AndPredicate;
import org.lockss.tdb.Predicates.FalsePredicate;
import org.lockss.tdb.Predicates.NotPredicate;
import org.lockss.tdb.Predicates.OrPredicate;
import org.lockss.tdb.Predicates.TruePredicate;
import org.lockss.test.LockssTestCase;

public class TestPredicates extends LockssTestCase {

  public void testTrue() throws Exception {
    assertTrue(new TruePredicate().test(null));
  }
  
  public void testFalse() throws Exception {
    assertFalse(new FalsePredicate().test(null));
  }
  
  public void testNot() throws Exception {
    assertFalse(new NotPredicate(new TruePredicate()).test(null));
    assertTrue(new NotPredicate(new FalsePredicate()).test(null));
  }
  
  public void testAnd() throws Exception {
    assertFalse(new AndPredicate(new FalsePredicate(), new FalsePredicate()).test(null));
    assertFalse(new AndPredicate(new FalsePredicate(), new TruePredicate()).test(null));
    assertFalse(new AndPredicate(new TruePredicate(), new FalsePredicate()).test(null));
    assertTrue(new AndPredicate(new TruePredicate(), new TruePredicate()).test(null));
    assertFalse(new AndPredicate(new FalsePredicate(),
                                 new Predicate() {
                                   @Override
                                   public boolean test(Object a) {
                                     fail("Second predicate should not have been evaluated");
                                     return false;
                                   }
                                 }).test(null));
  }
  
  public void testOr() throws Exception {
    assertFalse(new OrPredicate(new FalsePredicate(), new FalsePredicate()).test(null));
    assertTrue(new OrPredicate(new FalsePredicate(), new TruePredicate()).test(null));
    assertTrue(new OrPredicate(new TruePredicate(), new FalsePredicate()).test(null));
    assertTrue(new OrPredicate(new TruePredicate(), new TruePredicate()).test(null));
    assertTrue(new OrPredicate(new TruePredicate(),
                               new Predicate() {
                                 @Override
                                 public boolean test(Object a) {
                                   fail("Second predicate should not have been evaluated");
                                   return false;
                                 }
                               }).test(null));
  }
  
}
