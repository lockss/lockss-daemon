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

package org.lockss.plugin;

import java.util.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.test.*;

public class TestCuContentIterator extends LockssTestCase {

  static String url1="http://www.example.com/file1.html";
  static String url2="http://www.example.com/file2.html";
  static String url3="http://www.example.com/file3.html";
  static String url4="http://www.example.com/file4.html";

  static List startUrls = ListUtil.list(url1);

  MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    // Need CrawlManager to check global exclusions
    getMockLockssDaemon().getCrawlManager();
    mau = new MockArchivalUnit(new MockPlugin(getMockLockssDaemon()));
  }

  public void testIteratorDefaultOptions() throws Exception {
    assertEquals(true,
		 CurrentConfig.getBooleanParam(CuIterOptions.PARAM_INCLUDED_ONLY,
					       CuIterOptions.DEFAULT_INCLUDED_ONLY));
  }

  public void testIterator() throws Exception {
    ConfigurationUtil.addFromArgs(CuIterOptions.PARAM_INCLUDED_ONLY, "false");
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    MockCachedUrl cu1 = mau.addUrl(url1, "content1");
    MockCachedUrl cu2 = mau.addUrl(url2, "content2");
    MockCachedUrl cu3 = mau.addUrl(url3, false, true);
    MockCachedUrl cu4 = mau.addUrl(url4, "content4");
    mau.removeUrlToBeCached(url2);

    List nodes = ListUtil.list(cus, cu1, cu2, cu3, cu4);

    cus.setHashItSource(nodes);

    assertEquals(ListUtil.list(cu1, cu2, cu4),
		 ListUtil.fromIterator(CuIterator.forCus(cus)));

    assertEquals(ListUtil.list(cu1, cu4),
		 ListUtil.fromIterator(CuIterator.forCus(cus).setIncludedOnly(true)));
    assertEquals(ListUtil.list(cu1, cu2, cu3, cu4),
		 ListUtil.fromIterator(CuIterator.forCus(cus).setContentOnly(false)));
    assertEquals(ListUtil.list(cu1, cu3, cu4),
		 ListUtil.fromIterator(CuIterator.forCus(cus).setContentOnly(false).setIncludedOnly(true)));

    assertEquals(ListUtil.list(cu1, cu2, cu4),
		 ListUtil.fromIterator(CuIterator.forCus(cus)));
    ConfigurationUtil.addFromArgs(CuIterOptions.PARAM_CONTENT_ONLY, "false");
    assertEquals(ListUtil.list(cu1, cu2, cu3, cu4),
		 ListUtil.fromIterator(CuIterator.forCus(cus)));
    ConfigurationUtil.addFromArgs(CuIterOptions.PARAM_CONTENT_ONLY, "true",
				  CuIterOptions.PARAM_INCLUDED_ONLY, "true");
    assertEquals(ListUtil.list(cu1, cu4),
		 ListUtil.fromIterator(CuIterator.forCus(cus)));

    ConfigurationUtil.addFromArgs(CrawlManagerImpl.PARAM_EXCLUDE_URL_PATTERN,
				  "file4\\.html");
    CuIterator cui = CuIterator.forCus(cus);
    assertEquals(ListUtil.list(cu1), ListUtil.fromIterator(cui));
    assertEquals(2, cui.getExcludedCount());
  }

}
