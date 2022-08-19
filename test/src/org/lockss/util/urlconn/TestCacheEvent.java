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
import static org.lockss.util.urlconn.CacheEvent.EventType;


public class TestCacheEvent extends LockssTestCase {

  static final String URL = "http://foo/";

  private MockArchivalUnit mau;
  CacheEvent act;

  CacheEvent codeEvent = new CacheEvent.ResponseEvent(404, "40404 message");
  CacheEvent exEvent =
    new CacheEvent.ExceptionEvent(new UnknownHostException("hostname"), "unhost");
  CacheException cex;
  MyHttpResultHandler handler;


  protected void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    handler = new MyHttpResultHandler();
  }

  public void testResponseCodeEvent() throws Exception {
    CacheEvent evt = new CacheEvent.ResponseEvent(401, "foobar");
    assertEquals(EventType.RESPONSE_CODE, evt.getEventType());
    assertEquals("Response status 401", evt.getResultString());
    cex = evt.invokeHandler(handler, mau, URL);
    assertClass(CE1.class, cex);
    assertEquals("CE message code", cex.getMessage());
  }

  public void testExceptionEvent() throws Exception {
    CacheEvent evt =
      new CacheEvent.ExceptionEvent(new UnknownHostException("unhost42"), 
                                    "foobar");
    assertEquals(EventType.EXCEPTION, evt.getEventType());
    assertEquals("unhost42", evt.getResultString());
    cex = evt.invokeHandler(handler, mau, URL);
    assertClass(CE1.class, cex);
    assertEquals("CE message ex", cex.getMessage());
  }

  public void testRedirectEvent() throws Exception {
    CacheEvent evt =
      new CacheEvent.RedirectEvent("http://redir.to/",
                                   "redir message");
    assertEquals(EventType.REDIRECT_TO_URL, evt.getEventType());
    assertEquals("http://redir.to/", evt.getResultString());
    cex = evt.invokeHandler(handler, mau, URL);
    assertClass(CE1.class, cex);
    assertEquals("CE message redir", cex.getMessage());
  }

  public static class MyHttpResultHandler implements CacheResultHandler {
    public MyHttpResultHandler() {
    }

    @Override
    public CacheException handleResult(ArchivalUnit au,
				       String url,
				       int responseCode) {
      return new CE1("CE message code");
    }

    @Override
    public CacheException handleResult(ArchivalUnit au,
				       String url,
				       Exception ex) {
      return new CE1("CE message ex");
    }

    @Override
    public CacheException handleRedirect(ArchivalUnit au,
                                         String url,
                                         String redirToUrl) {
      return new CE1("CE message redir");
    }
  }

  static class CE1 extends CacheException {
    CE1(String msg) {
      super(msg);
    }
  }
}
