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
      au = new Au();
      au.put(Au.PLUGIN, plu);
      assertFalse(predicate.test(au));
      au = new Au();
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
      au = new Au();
      au.put(Au.PLUGIN, plu);
      assertTrue(predicate.test(au));
      au = new Au();
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
    doStatusTest(TdbQueryBuilder.KEY_CLOCKSS_PRESERVED, TdbQueryBuilder.CLOCKSS_PRESERVED_STATUSES);
    doStatusTest(TdbQueryBuilder.KEY_CLOCKSS_PRODUCTION, TdbQueryBuilder.CLOCKSS_PRODUCTION_STATUSES);
    doStatusTest(TdbQueryBuilder.KEY_PRODUCTION, TdbQueryBuilder.PRODUCTION_STATUSES);
    doStatusTest(TdbQueryBuilder.KEY_UNRELEASED, TdbQueryBuilder.UNRELEASED_STATUSES);
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
    assertFalse(predicate instanceof TruePredicate);
    for (String status : Au.STATUSES) {
      Au au = new Au();
      au.put(Au.STATUS, status);
      assertEquals(String.format("%s is not in the set %s", status, statuses),
                   statuses.contains(status),
                   predicate.test(au));
    }
  }

}
