/*
 * $Id: TestCachedUrlSetSpec.java,v 1.2 2002-11-14 22:00:38 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;
import java.util.*;
import junit.framework.TestCase;
import gnu.regexp.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.CrawlSpec
 */

public class TestCachedUrlSetSpec extends LockssTestCase {
  public TestCachedUrlSetSpec(String msg){
    super(msg);
  }

  public void testIllRECachedUrlSetSpec() throws REException {
    try {
      new RECachedUrlSetSpec(null, "foo");
      fail("RECachedUrlSetSpec with null url should throw");
    } catch (NullPointerException e) {
    }
  }

  public void testRECachedUrlSetSpecEquivalence() throws REException {
    CachedUrlSetSpec cuss1 = new RECachedUrlSetSpec("foo", (String)null);
    CachedUrlSetSpec cuss2 = new RECachedUrlSetSpec("foo", (RE)null);
    CachedUrlSetSpec cuss3 = new RECachedUrlSetSpec("foo");
    CachedUrlSetSpec cuss4 = new RECachedUrlSetSpec("bar");
    assertEquals(cuss1, cuss2);
    assertEquals(cuss1, cuss3);
    assertEquals(cuss2, cuss3);
    assertTrue(!cuss3.equals(cuss4));
    String re1 = "123.*";
    String re2 = "456";
    CachedUrlSetSpec cuss5 = new RECachedUrlSetSpec("xxx", re1);
    CachedUrlSetSpec cuss6 = new RECachedUrlSetSpec("xxx", new RE(re1));
    CachedUrlSetSpec cuss7 = new RECachedUrlSetSpec("xxx", new RE(re2));
    assertEquals(cuss5, cuss6);
    assertTrue(!cuss6.equals(cuss7));
  }

  public void testRECachedUrlSetSpec() throws REException {
    CachedUrlSetSpec cuss1 = new RECachedUrlSetSpec("foo", (RE)null);
    assertEquals(ListUtil.list("foo"), cuss1.getPrefixList());
    assertTrue(cuss1.matches("foo"));
    assertTrue(cuss1.matches("foobar"));
    assertTrue(cuss1.matches("foo/bar"));
    assertTrue(!cuss1.matches("1foo"));
    CachedUrlSetSpec cuss2 = new RECachedUrlSetSpec("foo", "/123");
    assertTrue(cuss2.matches("foo/bar/123/x"));
    assertTrue(!cuss2.matches("/123foo"));
    CachedUrlSetSpec cuss3 = new RECachedUrlSetSpec("foo", "123$");
    assertTrue(cuss3.matches("foo/bar/123"));
    assertTrue(!cuss3.matches("foo/123/bar"));
  }

  public void testIllAnyCachedUrlSetSpec() {
    try {
      new AnyCachedUrlSetSpec(ListUtil.list("dd"));
      fail("Wrong type AnyCachedUrlSetSpec list item");
    } catch (ClassCastException e) {
    }
  }    

  public void testNullAnyCachedUrlSetSpec() {
    try {
      new AnyCachedUrlSetSpec(null);
      fail("AnyCachedUrlSetSpec(null) should fail");
    } catch (NullPointerException e) {
    }
  }    

  public void testEmptyAnyCachedUrlSetSpec() {
    CachedUrlSetSpec cuss = new AnyCachedUrlSetSpec(Collections.EMPTY_LIST);
    assertTrue(!cuss.matches(""));
    assertTrue(!cuss.matches("foo"));
  }    

  public void testAnyCachedUrlSetSpec() throws REException {
    CachedUrlSetSpec re1 = new RECachedUrlSetSpec("foo", (RE)null);
    CachedUrlSetSpec re2 = new RECachedUrlSetSpec("bar", "12");
    List prefixes = ListUtil.list("foo", "bar");
    List l = ListUtil.list(re1, re2);
    CachedUrlSetSpec any = new AnyCachedUrlSetSpec(l);
    assertTrue(any.matches("foo/fjfjf"));
    assertTrue(any.matches("bar/123"));
    assertTrue(!any.matches("bar/132"));

    List al = any.getPrefixList();
    Set as = new HashSet(al);
    assertEquals(new HashSet(prefixes), as);
    al.add("xyz");
    assertEquals(as, new HashSet(any.getPrefixList()));
  }
}
