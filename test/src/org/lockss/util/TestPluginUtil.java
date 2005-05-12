/*
 * $Id: TestPluginUtil.java,v 1.1 2005-05-12 00:23:07 troberts Exp $
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
  }
}
