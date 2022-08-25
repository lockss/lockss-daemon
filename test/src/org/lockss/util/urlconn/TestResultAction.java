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

public class TestResultAction extends LockssTestCase {

  static final String URL = "http://foo/";

  private MockArchivalUnit mau;
  ResultAction act;

  CacheEvent codeEvent = new CacheEvent.ResponseEvent(404, "40404 message");
  CacheEvent exEvent =
    new CacheEvent.ExceptionEvent(new UnknownHostException("hostname"), "unhost");
  CacheException cex;


  protected void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
  }

  public void testExceptionResult() throws Exception {
    act = ResultAction.fromObject(new RetryableNetworkException("act msg"));
    assertClass(ResultAction.Cls.class, act);
    assertEquals(ResultAction.Type.Class, act.getType());
    assertFalse(act.isRemap());
    try {
      act.getRemapVal("msg");
      fail("getRemapVal() should throw on non-remap ResultAction");
    } catch (UnsupportedOperationException e) {}

    cex = act.makeException(mau, URL, codeEvent);
    assertClass(RetryableNetworkException.class, cex);
    assertEquals("404 40404 message", cex.getMessage());

    cex = act.makeException(mau, URL, exEvent);
    assertClass(RetryableNetworkException.class, cex);
    assertEquals("hostname unhost", cex.getMessage());
  }

  public void testHandlerResult() throws Exception {
    act = ResultAction.fromObject(new MyHttpResultHandler());
    assertClass(ResultAction.Handler.class, act);
    assertEquals(ResultAction.Type.Handler, act.getType());
    assertFalse(act.isRemap());
    try {
      act.getRemapVal("msg");
      fail("getRemapVal() should throw on non-remap ResultAction");
    } catch (UnsupportedOperationException e) {}

    cex = act.makeException(mau, URL, codeEvent);
    assertClass(CE1.class, cex);
    assertEquals("CE message 1", cex.getMessage());

    cex = act.makeException(mau, URL, exEvent);
    assertClass(CE1.class, cex);
    assertEquals("CE message 2", cex.getMessage());
  }

  public void testRemapResult() throws Exception {
    act = ResultAction.fromObject(404);
    assertClass(ResultAction.Remap.class, act);
    assertEquals(ResultAction.Type.Remap, act.getType());
    assertTrue(act.isRemap());
    ResultAction.Remap remapAct = (ResultAction.Remap)act;
    assertEquals(404, remapAct.getRemapVal("tst msg"));

    act = ResultAction.fromObject(UnknownHostException.class);
    assertClass(ResultAction.Remap.class, act);
    assertEquals(ResultAction.Type.Remap, act.getType());
    assertTrue(act.isRemap());
    remapAct = (ResultAction.Remap)act;
    assertClass(UnknownHostException.class, remapAct.getRemapVal("msg 2"));
    assertEquals("msg 2", ((Exception)remapAct.getRemapVal("msg 2")).getMessage());
  }

  public static class MyHttpResultHandler implements CacheResultHandler {
    public MyHttpResultHandler() {
    }

    public CacheException handleResult(ArchivalUnit au,
				       String url,
				       int responseCode) {
      return new CE1("CE message 1");
    }

    public CacheException handleResult(ArchivalUnit au,
				       String url,
				       Exception ex) {
      return new CE1("CE message 2");
    }
  }

  static class CE1 extends CacheException {
    CE1(String msg) {
      super(msg);
    }
  }
}
