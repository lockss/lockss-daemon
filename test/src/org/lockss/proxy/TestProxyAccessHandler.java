/*
 * $Id$
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

package org.lockss.proxy;

import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.StringUtil;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.mortbay.util.*;

public class TestProxyAccessHandler extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  private MockIdentityManager idMgr = new MockIdentityManager();
  private MyMockPluginManager pluginMgr = new MyMockPluginManager();

  ProxyAccessHandler handler;
  MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    theDaemon = getMockLockssDaemon();
    theDaemon.setPluginManager(pluginMgr);
    theDaemon.setIdentityManager(idMgr);
    theDaemon.setProxyManager(new ProxyManager());

    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin());

    handler = new ProxyAccessHandler(theDaemon, "test");
    handler.setAllowLocal(true);
  }

  void setRepair(HttpRequest req) {
    req.setField(Constants.X_LOCKSS, "foo,repair");
  }

  public void testLocalOk() throws Exception {
     MockHttpRequest req = new MockHttpRequest();

     req.setRemoteAddr("127.0.0.1");

     MockHttpResponse res = new MockHttpResponse();
     handler.handle("/blah/blah.html", "", req, res);
     // handler should just return, not having handled request
     assertEquals(-1, res.getError());
     assertFalse(req.isHandled());
  }

  public void testNoAccess() throws Exception {
     MockHttpRequest req = new MockHttpRequest();

     req.setRemoteAddr("44.0.0.1");

     MockHttpResponse res = new MockHttpResponse();
     handler.handle("/blah/blah.html", "", req, res);
     assertEquals(HttpResponse.__403_Forbidden, res.getError());
     assertTrue(req.isHandled());
  }

  public void testRepairNoContent() throws Exception {
     MockHttpRequest req = new MockHttpRequest();
     setRepair(req);
     req.setRemoteAddr("55.0.0.1");
     req.setURI(new URI("http://www.example.com/blah/blah.html"));

     MockCachedUrl mcu =
       new MockCachedUrl("http://www.example.com/blah/blah.html", mau);
     mcu.setExists(false);
     pluginMgr.setCachedUrl(mcu);

     MockHttpResponse res = new MockHttpResponse();
     handler.handle("http://www.example.com/blah/blah.html", "", req, res);
     assertEquals(HttpResponse.__404_Not_Found, res.getError());
     assertTrue(req.isHandled());
  }

  public void testRepairNoAccess() throws Exception {
     MockHttpRequest req = new MockHttpRequest();
     setRepair(req);
     req.setRemoteAddr("55.0.0.1");
     req.setURI(new URI("http://www.example.com/blah/blah.html"));

     MockCachedUrl mcu =
       new MockCachedUrl("http://www.example.com/blah/blah.html", mau);
     mcu.setExists(true);
     pluginMgr.setCachedUrl(mcu);

     MockHttpResponse res = new MockHttpResponse();
     handler.handle("http://www.example.com/blah/blah.html", "", req, res);
     assertEquals(HttpResponse.__403_Forbidden, res.getError());
     assertTrue(req.isHandled());
  }

  public void testHandleServesRepairToAgreedCache() throws Exception {
     MockHttpRequest req = new MockHttpRequest();
     setRepair(req);
     req.setRemoteAddr("55.0.0.1");
     req.setURI(new URI("http://www.example.com/blah/blah.html"));

     MockCachedUrl mcu =
       new MockCachedUrl("http://www.example.com/blah/blah.html", mau);
     mcu.setExists(true);
     pluginMgr.setCachedUrl(mcu);

     Map agreedMap = new HashMap();
     MockPeerIdentity mockPid = new MockPeerIdentity("55.0.0.1");
     agreedMap.put(mockPid, "1010101010");

     idMgr.setAgeedForAu(mau, agreedMap);
     idMgr.addPeerIdentity("55.0.0.1", mockPid);

     MockHttpResponse res = new MockHttpResponse();
     handler.handle("http://www.example.com/blah/blah.html", "", req, res);
     assertEquals(-1, res.getError());
     assertFalse(req.isHandled());
  }

  private class MyMockPluginManager extends PluginManager {
    CachedUrl cu;
    public CachedUrl findCachedUrl(String urlString) {
      return cu;
    }

    public void setCachedUrl(CachedUrl cu) {
      this.cu = cu;
    }
  }
}
