/*

Copyright (c) 2000-2017, Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;

import org.lockss.tdb.AntlrUtil.SyntaxError;
import org.lockss.tdb.Predicates.TruePredicate;
import org.lockss.test.LockssTestCase;

public class TestTdbQueryBuilder extends LockssTestCase {

  public void testNoOptions() throws Exception {
    TdbQueryBuilder tdbq = new TdbQueryBuilder();
    Map<String, Object> options = new HashMap<String, Object>();
    CommandLineAccessor cmd = new MockCommandLineAccessor() {
      @Override
      public boolean hasOption(String opt) {
        return false;
      }
    };
    tdbq.processCommandLine(options, cmd);
    Predicate<Au> predicate = tdbq.getAuPredicate(options);
    assertNotNull(predicate);
    assertTrue(predicate instanceof TruePredicate);
  }
  
  public void testAlliance() throws Exception {
    TdbQueryBuilder tdbq = new TdbQueryBuilder();
    Map<String, Object> options = new HashMap<String, Object>();
    CommandLineAccessor cmd = new MockCommandLineAccessor() {
      @Override
      public boolean hasOption(String opt) {
        return TdbQueryBuilder.KEY_ALLIANCE.equals(opt);
      }
    };
    tdbq.processCommandLine(options, cmd);
    Predicate<Au> predicate = tdbq.getAuPredicate(options);
    assertNotNull(predicate);
    assertFalse(predicate instanceof TruePredicate);
    for (String plu : TdbQueryBuilder.NON_ALLIANCE_PLUGINS) {
      Au au;
      au = new Au(null);
      au.put(Au.PLUGIN, plu);
      assertFalse(predicate.test(au));
      au = new Au(null);
      au.put(Au.PLUGIN, "X" + plu);
      assertTrue(predicate.test(au));
    }
  }
  
  public void testNonAlliance() throws Exception {
    TdbQueryBuilder tdbq = new TdbQueryBuilder();
    Map<String, Object> options = new HashMap<String, Object>();
    CommandLineAccessor cmd = new MockCommandLineAccessor() {
      @Override
      public boolean hasOption(String opt) {
        return TdbQueryBuilder.KEY_NON_ALLIANCE.equals(opt);
      }
    };
    tdbq.processCommandLine(options, cmd);
    Predicate<Au> predicate = tdbq.getAuPredicate(options);
    assertNotNull(predicate);
    assertFalse(predicate instanceof TruePredicate);
    for (String plu : TdbQueryBuilder.NON_ALLIANCE_PLUGINS) {
      Au au;
      au = new Au(null);
      au.put(Au.PLUGIN, plu);
      assertTrue(predicate.test(au));
      au = new Au(null);
      au.put(Au.PLUGIN, "X" + plu);
      assertFalse(predicate.test(au));
    }
  }
  
  public void testSyntaxErrors() throws Exception {
    for (final String query : Arrays.asList("incomplete",
                                            "incomplete is",
                                            "incomplete is not",
                                            "incomplete =",
                                            "incomplete !=",
                                            "incomplete ~",
                                            "incomplete !~",
                                            "invalid ==",
                                            "invalid @",
                                            "status is ShouldBeQuoted",
                                            "status is 'unterminated string",
                                            "status is \"unterminated string",
                                            "( unbalanced",
                                            "unbalanced )")) {
      TdbQueryBuilder tdbq = new TdbQueryBuilder();
      Map<String, Object> options = new HashMap<String, Object>();
      CommandLineAccessor cmd = new MockCommandLineAccessor() {
        @Override
        public boolean hasOption(String opt) {
          return TdbQueryBuilder.KEY_QUERY.equals(opt);
        }
        @Override
        public String getOptionValue(String opt) {
          return query;
        }
      };
      try {
        tdbq.processCommandLine(options, cmd);
        fail("Should have caused a syntax error: " + query);
      }
      catch (SyntaxError expected) {
        // Expected
      }
    }
  }

  public void testSingleStatusOptions() throws Exception {
    doStatusTest(TdbQueryBuilder.KEY_CRAWLING, Au.STATUS_CRAWLING);
    doStatusTest(TdbQueryBuilder.KEY_DEEP_CRAWL, Au.STATUS_DEEP_CRAWL);
    doStatusTest(TdbQueryBuilder.KEY_DOWN, Au.STATUS_DOWN);
    doStatusTest(TdbQueryBuilder.KEY_EXISTS, Au.STATUS_EXISTS);
    doStatusTest(TdbQueryBuilder.KEY_EXPECTED, Au.STATUS_EXPECTED);
    doStatusTest(TdbQueryBuilder.KEY_FINISHED, Au.STATUS_FINISHED);
    doStatusTest(TdbQueryBuilder.KEY_FROZEN, Au.STATUS_FROZEN);
    doStatusTest(TdbQueryBuilder.KEY_ING_NOT_READY, Au.STATUS_ING_NOT_READY);
    doStatusTest(TdbQueryBuilder.KEY_MANIFEST, Au.STATUS_MANIFEST);
    doStatusTest(TdbQueryBuilder.KEY_NOT_READY, Au.STATUS_NOT_READY);
    doStatusTest(TdbQueryBuilder.KEY_READY, Au.STATUS_READY);
    doStatusTest(TdbQueryBuilder.KEY_READY_SOURCE, Au.STATUS_READY_SOURCE);
    doStatusTest(TdbQueryBuilder.KEY_RELEASED, Au.STATUS_RELEASED);
    doStatusTest(TdbQueryBuilder.KEY_RELEASING, Au.STATUS_RELEASING);
    doStatusTest(TdbQueryBuilder.KEY_SUPERSEDED, Au.STATUS_SUPERSEDED);
    doStatusTest(TdbQueryBuilder.KEY_TESTING, Au.STATUS_TESTING);
    doStatusTest(TdbQueryBuilder.KEY_WANTED, Au.STATUS_WANTED);
    doStatusTest(TdbQueryBuilder.KEY_ZAPPED, Au.STATUS_ZAPPED);
  }

  public void testMultiStatusOptions() throws Exception {
    doStatusTest(TdbQueryBuilder.KEY_ALL, TdbQueryBuilder.ALL_STATUSES);
    doStatusTest(TdbQueryBuilder.KEY_ANY_AND_ALL, Au.STATUSES);
    doStatusTest(TdbQueryBuilder.KEY_CLOCKSS_INGEST, TdbQueryBuilder.CLOCKSS_INGEST_STATUSES);
    doStatusTest(TdbQueryBuilder.KEY_CLOCKSS_PRESERVED, TdbQueryBuilder.CLOCKSS_PRESERVED_STATUSES);
    doStatusTest(TdbQueryBuilder.KEY_CLOCKSS_PRODUCTION, TdbQueryBuilder.CLOCKSS_PRODUCTION_STATUSES);
    doStatusTest(TdbQueryBuilder.KEY_PRODUCTION, TdbQueryBuilder.PRODUCTION_STATUSES);
    doStatusTest(TdbQueryBuilder.KEY_UNRELEASED, TdbQueryBuilder.UNRELEASED_STATUSES);
    doStatusTest(TdbQueryBuilder.KEY_VIABLE, TdbQueryBuilder.VIABLE_STATUSES);
  }
  
  protected void doStatusTest(String statusOpt,
                              String status)
      throws Exception {
    doStatusTest(statusOpt, Arrays.asList(status));
  }

  
  protected void doStatusTest(final String statusOpt,
                              List<String> statuses)
      throws Exception {
    TdbQueryBuilder tdbq = new TdbQueryBuilder();
    Map<String, Object> options = new HashMap<String, Object>();
    CommandLineAccessor cmd = new MockCommandLineAccessor() {
      @Override
      public boolean hasOption(String opt) {
        return statusOpt.equals(opt);
      }
    };
    tdbq.processCommandLine(options, cmd);
    Predicate<Au> predicate = tdbq.getAuPredicate(options);
    assertNotNull(predicate);
    for (String status : Au.STATUSES) {
      Au au = new Au(null);
      au.put(Au.STATUS, status);
      assertEquals(String.format("%s is not in the set %s", status, statuses),
                   statuses.contains(status),
                   predicate.test(au));
    }
  }

}
