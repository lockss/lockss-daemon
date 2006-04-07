/*
 * $Id: LockssPluginTestCase.java,v 1.1 2006-04-07 22:48:26 troberts Exp $
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

import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;

public class LockssPluginTestCase extends LockssTestCase {

  protected MockLockssDaemon theDaemon;

  public DefinableArchivalUnit makeAu(Properties props, String pluginID)
      throws Exception {
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon, pluginID);
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void shouldCacheTest(String url, boolean shouldCache,
			       ArchivalUnit au, CachedUrlSet cus) {
    UrlCacher uc = au.makeUrlCacher(url);
    if (shouldCache) {
      assertTrue(url+" incorrectly marked as shouldn't cache",
		 uc.shouldBeCached());
    } else {
      assertFalse(url+" incorrectly marked as should cache",
		  uc.shouldBeCached());
    }
  }

  public void shouldCacheTest(Set urls, boolean shouldCache, 
                              ArchivalUnit au, CachedUrlSet cus) {
    Iterator it = urls.iterator(); 
    while (it.hasNext()) {
      shouldCacheTest((String)it.next(), shouldCache, au, cus);
    }
  }
  
}
