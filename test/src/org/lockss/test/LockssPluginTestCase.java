/*
 * $Id$
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.io.*;
import java.util.*;

import org.lockss.config.*;
import org.lockss.state.*;
import org.lockss.plugin.*;

/** Base class for plugin tests. */
public class LockssPluginTestCase extends LockssTestCase {

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
				  tempDirPath);
    MockLockssDaemon daemon = getMockLockssDaemon();
    daemon.getPluginManager().startService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public ArchivalUnit makeAu(String pluginId, Properties props)
      throws Exception {
    return makeAu(pluginId, ConfigurationUtil.fromProps(props));
  }

  private ArchivalUnit makeAu(String pluginId, Configuration config)
      throws Exception {
    return PluginTestUtil.createAndStartAu(pluginId, config);
  }

  public void assertShouldCache(String url, ArchivalUnit au, CachedUrlSet cus) {
    assertShouldCache(url, au);
  }

  public void assertShouldCache(String url, ArchivalUnit au) {
    assertTrue(url+" incorrectly marked as shouldn't cache",
        au.shouldBeCached(url));
  }

  public void assertShouldNotCache(String url, 
                                   ArchivalUnit au, CachedUrlSet cus) {
    assertShouldNotCache(url, au);
  }

  public void assertShouldNotCache(String url, ArchivalUnit au) {
     assertFalse(url+" incorrectly marked as should cache",
         au.shouldBeCached(url));
   }

  
  public void assertShouldCache(Set urls, ArchivalUnit au, CachedUrlSet cus) {
    assertShouldCache(urls, au);
  }

  public void assertShouldCache(Set urls, ArchivalUnit au) {
      Iterator it = urls.iterator(); 
      while (it.hasNext()) {
        assertShouldCache((String)it.next(), au);
      }
    }
    
  public void assertShouldNotCache(Set urls, ArchivalUnit au,
				   CachedUrlSet cus) {
    assertShouldNotCache(urls, au);
  }

  public void assertShouldNotCache(Set urls, ArchivalUnit au) {
      Iterator it = urls.iterator(); 
      while (it.hasNext()) {
        assertShouldNotCache((String)it.next(), au);
      }
    }
    
  /** Assert that the URL matches a substance pattern */
  public void assertSubstanceUrl(String url, ArchivalUnit au) {
    SubstanceChecker checker = new SubstanceChecker(au);
    assertTrue("Not a substance URL: " + url,
	       checker.isSubstanceUrl(url));
  }

  /** Assert that the URL does not match a substance pattern */
  public void assertNotSubstanceUrl(String url, ArchivalUnit au) {
    SubstanceChecker checker = new SubstanceChecker(au);
    assertFalse("Is a substance URL: " + url,
		checker.isSubstanceUrl(url));
  }
}
