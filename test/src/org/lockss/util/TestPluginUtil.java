/*
 * $Id: TestPluginUtil.java,v 1.2 2006-09-23 19:23:56 tlipkis Exp $
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

package org.lockss.util;
import org.lockss.test.*;
import org.lockss.plugin.*;

/**
 * @author  Thomas S. Robertson
 * @version 0.0
 */
public class TestPluginUtil extends LockssTestCase {
  public void testGetBaseUrlNullCu() {
    try {
      PluginUtil.getBaseUrl(null);
      fail("PluginUtil.getBaseUrl(null) should throw a NPE");
    } catch (NullPointerException ex) {
    }
  }

  public void testGetBaseUrlDirNodeFast() {
    ConfigurationUtil.setFromArgs(PluginUtil.PARAM_DIR_NODE_CHECK_SLASH,
				  "false");
    String noslash = "http://www.example.com/bar";
    String slash = noslash + "/";
    String bad = "http://www.example.com/bar?qu=ery";
    String badslash = "http://www.example.com/bar/?qu=ery";

    MockCachedUrl mcu = new MockCachedUrl(slash);
    assertEquals(slash, PluginUtil.getBaseUrl(mcu));
    mcu.setProperty(CachedUrl.PROPERTY_NODE_URL, slash); 
    assertEquals(slash, PluginUtil.getBaseUrl(mcu));

    mcu = new MockCachedUrl(noslash);
    assertEquals(noslash, PluginUtil.getBaseUrl(mcu));
    mcu.setProperty(CachedUrl.PROPERTY_NODE_URL, slash); 
    assertEquals(slash, PluginUtil.getBaseUrl(mcu));

    mcu = new MockCachedUrl(bad);
    assertEquals(bad, PluginUtil.getBaseUrl(mcu));
    mcu.setProperty(CachedUrl.PROPERTY_NODE_URL, badslash); 
    assertEquals(badslash, PluginUtil.getBaseUrl(mcu));
  }

  public void testGetBaseUrlDirNodeCareful() {
    ConfigurationUtil.setFromArgs(PluginUtil.PARAM_DIR_NODE_CHECK_SLASH,
				  "true");
    String noslash = "http://www.example.com/bar";
    String slash = noslash + "/";
    String bad = "http://www.example.com/bar?qu=ery";
    String badslash = "http://www.example.com/bar?how=wouldthishappen";

    MockCachedUrl mcu = new MockCachedUrl(slash);
    assertEquals(slash, PluginUtil.getBaseUrl(mcu));
    mcu.setProperty(CachedUrl.PROPERTY_NODE_URL, slash); 
    assertEquals(slash, PluginUtil.getBaseUrl(mcu));

    mcu = new MockCachedUrl(noslash);
    assertEquals(noslash, PluginUtil.getBaseUrl(mcu));
    mcu.setProperty(CachedUrl.PROPERTY_NODE_URL, slash); 
    assertEquals(slash, PluginUtil.getBaseUrl(mcu));

    mcu = new MockCachedUrl(bad);
    assertEquals(bad, PluginUtil.getBaseUrl(mcu));
    mcu.setProperty(CachedUrl.PROPERTY_NODE_URL, badslash); 
    assertEquals(bad, PluginUtil.getBaseUrl(mcu));
  }

  public void testGetBaseUrlNoRedirect() {
    String url = "http://www.example.com";
    MockCachedUrl mcu = new MockCachedUrl(url);
    assertEquals(url, PluginUtil.getBaseUrl(mcu));
  }

  public void testGetBaseUrlRedirect() {
    String url = "http://www.example.com";
    String url2 = "http://www.example.com/extra_level/";
    MockCachedUrl mcu = new MockCachedUrl(url);

    CIProperties props = new CIProperties();
    props.put(CachedUrl.PROPERTY_CONTENT_URL, url2);
    mcu.setProperties(props);

    assertEquals(url2, PluginUtil.getBaseUrl(mcu));

    // Should ignore node_url if there's a redirect
    mcu.setProperty(CachedUrl.PROPERTY_NODE_URL, url + "/"); 
    assertEquals(url2, PluginUtil.getBaseUrl(mcu));
  }
}
