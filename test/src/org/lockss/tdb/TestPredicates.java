/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
