/*
 * $Id: TestOaiCrawlSpec.java,v 1.4 2005-10-20 16:43:32 troberts Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.oai.*;

/**
 * This is the test class for org.lockss.daemon.CrawlSpec
 */

public class TestOaiCrawlSpec extends LockssTestCase {

  private List permissionUrls = ListUtil.list("pList");
//  private List cList = ListUtil.list("cList");

  private CrawlRule rule = new MockCrawlRule();

  public TestOaiCrawlSpec(String msg){
    super(msg);
  }

  public void testNullOaiHandlerUrl() throws LockssRegexpException {
    try {
      OaiCrawlSpec cs =
	new OaiCrawlSpec(null, permissionUrls, null, rule, false, null);
      fail("OaiCrawlSpec with null oaiRequestData should throw");
    } catch (IllegalArgumentException e) { }
  }

  public void testSimpleContruction() {
    boolean follow = true;
    OaiRequestData oaiData = new OaiRequestData("handler","ns","tag","setSpec","prefix");
    OaiCrawlSpec cs2 =
      new OaiCrawlSpec(oaiData, permissionUrls, null, rule, follow, null);
    OaiRequestData myOaiData = cs2.getOaiRequestData();
    assertEquals("handler", myOaiData.getOaiRequestHandlerUrl());
    assertEquals("ns", myOaiData.getMetadataNamespaceUrl());
    assertEquals("tag", myOaiData.getUrlContainerTagName());
    assertEquals("setSpec", myOaiData.getAuSetSpec());
    assertEquals("prefix", myOaiData.getMetadataPrefix());
    assertTrue(cs2.getFollowLinkFlag());
  }

}

