/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util.urlconn;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.commons.lang3.tuple.*;

import org.lockss.test.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.util.urlconn.HttpResultMap.HttpResultCodeCategory;
import static org.lockss.util.urlconn.CacheException.*;

public class TestAuHttpResultMap extends LockssTestCase {
  private AuHttpResultMap resultMap = null;
  private MockArchivalUnit mau;
  PatternMap<ResultAction> patMap;


  List<Pair<String,ResultAction>> pairs =
    ListUtil.list(Pair.of("http.*1", ResultAction.remap(200)),
                  Pair.of("http.*2", ResultAction.remap(403)),
                  Pair.of("http.*3", ResultAction.remap(UnknownHostException.class)),
                  Pair.of("http.*4",
                          ResultAction.exClass(RedirectToLoginPageException.class)),
                  Pair.of("http.*5", ResultAction.handler(new MyHttpResultHandler())));

  protected void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    patMap = PatternMap.fromPairs(pairs);
    resultMap = new AuHttpResultMap(new HttpResultMap(), patMap);
  }

  public void testPatternMap() {
    assertEquals(ResultAction.remap(403), patMap.getMatch("http://foo2"));
  }

  public void testMapException() {
    assertNull(resultMap.mapRedirUrl(null, null, "orig111", "notthere",
                                     "nomsg"));
    assertNull(resultMap.mapRedirUrl(null, null, "orig222", "http://foo1",
                                     "200 from orig"));
    assertMapping(PermissionException.class,
                  "403 redir from orig",
                  "http://foo2", "redir from orig");
    assertMapping(RetryableNetworkException.class,
                  "Unknown host: redir from orig3",
                  "http://foo3", "redir from orig3");
    assertMapping(RedirectToLoginPageException.class,
                  "Redirect to login page: http://foo4 redir from orig4",
                  "http://foo4", "redir from orig4");
    assertMapping(RetryableNetworkException.class,
                  "Redirected from: http://orig.url47 to: http://foo5",
//                   "Redirected from: " + http://foo5 to: http://foo5",
                  "http://foo5", "redir from orig5");
  }

  /**
   * Assert that the code maps to the expected exception class.
   * @Return the mapped exception in case the test wants to make
   * additional assertions about it */
  void assertMapping(Class expClass, String expMsg, String url, String msg) {
    CacheException exception =
      resultMap.mapRedirUrl(null, null, "http://orig.url47", url, msg);
    assertClass("Code " + url + " erroneously mapped to", expClass, exception);
    assertEquals(expMsg, exception.getMessage());
  }



  static public class MyHttpResultHandler implements CacheResultHandler {

    public MyHttpResultHandler() {
    }

    public CacheException handleRedirect(ArchivalUnit au,
                                         String url,
                                         String redirToUrl) {
      return new RetryableNetworkException("Redirected from: " + url + " to: " + redirToUrl);
    }

  }
}
