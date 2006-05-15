/*
 * $Id: TestVTETDPlugin.java,v 1.3 2006-05-15 01:09:37 tlipkis Exp $
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

package org.lockss.plugin.etd.vtetdplugin;
import java.io.File;
import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.*;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestVTETDPlugin extends LockssPluginTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String OAI_KEY = ConfigParamDescr.OAI_REQUEST_URL.getKey();

  static final String ROOT_URL = "http://scholar.lib.vt.edu/";
  static final String OAI_PROVIDER = "http://scholar.lib.vt.edu/theses/OAI2/";

  static final String PLUGIN_ID = 
    "org.lockss.plugin.etd.vtetdplugin.VTETDPlugin";

  private DefinablePlugin plugin;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
				  tempDirPath);
  }

  private Properties makeProps(String url, int year, String oaiProvider) {
    Properties props = new Properties();
    props.setProperty(YEAR_KEY, Integer.toString(year));
    props.setProperty(BASE_URL_KEY, url);
    props.setProperty(OAI_KEY, oaiProvider);
    return props;
  }

  public void testShouldCacheProperPages() throws Exception {
    int year = 1999;
    ArchivalUnit bbAu = 
      makeAu(makeProps(ROOT_URL, year, OAI_PROVIDER), PLUGIN_ID);
    MockLockssDaemon theDaemon = getMockLockssDaemon();
    theDaemon.getLockssRepository(bbAu);
    theDaemon.getNodeManager(bbAu);
    BaseCachedUrlSet cus = 
      new BaseCachedUrlSet(bbAu, new RangeCachedUrlSetSpec(ROOT_URL));

    Set urlsToCache = 
      SetUtil.set(
                  "http://scholar.lib.vt.edu/lockss_permission.html",
                  "http://scholar.lib.vt.edu/theses/available/etd-07011999-215241/",
                  "http://scholar.lib.vt.edu/theses/available/etd-10121999-224043/unrestricted/HafnerThesis.pdf"
                  );
    
    assertShouldCache(urlsToCache, bbAu, cus);

    Set urlsNotToCache = 
      SetUtil.set(
                  "http://scholar.lib.vt.edu/theses/available/etd-1898-17448/",
                  "http://scholar.lib.vt.edu/theses/available/etd-7898-13842/unrestricted/Vita.pdf",
                  "http://scholar.lib.vt.edu/theses/available/etd-1899-17448/",
                  "http://scholar.lib.vt.edu/theses/available/etd-7899-13842/unrestricted/Vita.pdf",
                  "http://scholar.lib.vt.edu/theses/available/etd-4198-155242/unrestricted/5.PDF",
                  "http://scholar.lib.vt.edu/theses/available/etd-3132141279612241/",
                  "http://scholar.lib.vt.edu/theses/available/etd-3034112939721181/unrestricted/etd.pdf",
                  "http://scholar.lib.vt.edu/theses/available/etd-07012003-215241/"
                  );
    
    assertShouldNotCache(urlsNotToCache, bbAu, cus);
  }

  public void testGetName() throws Exception {
    DefinableArchivalUnit au = 
      makeAu(makeProps(ROOT_URL, 1999, OAI_PROVIDER), PLUGIN_ID);
    assertEquals("Virginia Tech ETD Collection 1999", au.getName());

    au = makeAu(makeProps(ROOT_URL, 2001, OAI_PROVIDER), PLUGIN_ID);
    assertEquals("Virginia Tech ETD Collection 2001", au.getName());
  }
    
}
