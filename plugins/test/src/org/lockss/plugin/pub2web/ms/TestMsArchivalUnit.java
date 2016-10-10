/*
 * $Id:$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pub2web.ms;

import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.*;
import org.lockss.state.AuState;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestMsArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String ROOT_URL = "http://www.jrnl.com/"; //this is not a real url

  private static final Logger log = Logger.getLogger(TestMsArchivalUnit.class);

  static final String PLUGIN_ID = "org.lockss.plugin.pub2web.ms.ClockssMicrobiologySocietyJournalsPlugin";
  static final String PluginName = "Microbiology Society Journals Plugin (CLOCKSS)";
    

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL base_url, int volume, String jid)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
    props.setProperty(JID_KEY, jid);
    if (base_url != null) {
      props.setProperty(BASE_URL_KEY, base_url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);

    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(),
        PLUGIN_ID);
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  //
  // Test the crawl rules
  //
  //http://jgv.microbiologyresearch.org/deliver/fulltext/jgv/96/10/3090_jgv000250.pdf?itemId=/content/journal/jgv/10.1099/jgv.0.000250&mimeType=pdf&isFastTrackArticle=      
//  http://jgv.microbiologyresearch.org/deliver/fulltext/jgv/96/10/3072_jgv000259.pdf?itemId=/content/journal/jgv/10.1099/jgv.0.000259&mimeType=pdf
 // excluded: why
//  http://jgv.microbiologyresearch.org/deliver/fulltext/jgv/96/12/toc.pdf?itemId=/content/journal/jgv/96/12/tocpdf1&mimeType=pdf

  public void testShouldCacheProperPages() throws Exception {
    String REAL_ROOT= "http://jgv.microbiologyresearch.org/";
    String OTHER_ROOT = "http://www.microbiologyresearch.org/";
    URL base = new URL(REAL_ROOT);
    ArchivalUnit  msau = makeAu(base, 96, "jgv");
    theDaemon.getLockssRepository(msau);
    theDaemon.getNodeManager(msau);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(msau,
        new RangeCachedUrlSetSpec(base.toString()));

    //manifest page
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/clockssissues?volume=96", true, msau, cus);    
    //toc
    //http://jgv.microbiologyresearch.org/content/journal/jgv/96/9
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/96/12", true, msau, cus);    
    //toc contents
    shouldCacheTest(REAL_ROOT+"articles/renderlist.action?fmt=ahah&items=http://sgm.metastore.ingenta.com/content/journal/jgv/10.1099/jgv.0.000294,http://sgm.metastore.ingenta.com/content/journal/jgv/10.1099/jgv.0.000314", true, msau,cus);
   
    // article landing page/abstract
    // http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.070979-0
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/vir.0.070979-0", true, msau, cus);
 
    // citation files
    // http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.070979-0/cite/endnote
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/vir.0.070979-0/cite/endnote", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/vir.0.070979-0/cite/refworks", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/vir.0.070979-0/cite/bibtex", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/vir.0.070979-0/cite/plaintext", true, msau, cus);

    //but not the citation links off the toc
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/96/12/cite/refworks", false, msau, cus);
    
    //FULL-TEXT
    //crawler friendly
    // http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.069872-0?crawler=true&mimetype=application/pdf
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/vir.0.069872-0?crawler=true&mimetype=application/pdf", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/vir.0.069872-0?crawler=true&mimetype=html", true, msau, cus);
   
    //pdf links with redirection - leave in this because this type of link handles toc and supplemental data    
    shouldCacheTest(REAL_ROOT+"deliver/fulltext/jgv/96/10/3090_jgv000250.pdf?itemId=/content/journal/jgv/10.1099/jgv.0.000250&mimeType=pdf&isFastTrackArticle=", true, msau, cus);    
    shouldCacheTest(REAL_ROOT+"deliver/fulltext/jgv/96/12/3457_jgv000286.pdf?itemId=/content/journal/jgv/10.1099/jgv.0.000286&mimeType=pdf", true, msau, cus);    
    //redirects to:
    shouldCacheTest(OTHER_ROOT+"docserver/fulltext/jgv/96/12/3457_jgv000286.pdf?expires=1458588778&id=id&accname=guest&checksum=06E5E7675BC310642B40D918B52C8A42", true, msau, cus);     
    
    //supplemental data
    //http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/jgv.0.000003/supp-data
    //http://jgv.microbiologyresearch.org/deliver/fulltext/jgv/96/1/64816a.pdf?itemId=/content/suppdata/jgv/10.1099/vir.0.064816-0-1&mimeType=pdf
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/10.1099/jgv.0.000003/supp-data", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"deliver/fulltext/jgv/96/1/64816a.pdf?itemId=/content/suppdata/jgv/10.1099/vir.0.064816-0-1&mimeType=pdf", true, msau, cus);
    
    //trickier supplementary data from jmmcr (Case Reports)
    shouldCacheTest(REAL_ROOT+"deliver/fulltext/supplementary-figures_jmmcr.0.000015.pdf?itemId=/content/jmmcr.0.000015&mimeType=pdf", true, msau,cus);
    //which redirects to
    shouldCacheTest(OTHER_ROOT+"docserver/fulltext/supplementary-figures_jmmcr.0.000015.pdf?expires=1472242012&id=id&accname=guest&checksum=07327FDAFD1DAE43172F16234A39DEC7", true, msau,cus);
    
    // images (etc.) 
    shouldCacheTest(REAL_ROOT+"content/jgv/10.1099/vir.0.000205.vir000205-f01", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"docserver/fulltext/jgv/96/9/vir000205-f1_thmb.gif", true, msau, cus);
    shouldCacheTest(REAL_ROOT+"docserver/fulltext/jgv/96/11/jgv000266-f4.gif", true, msau, cus);
    
    //supporting files with url args
    shouldCacheTest(REAL_ROOT + "js/sgm/plugins.js?1", true, msau, cus);
    shouldCacheTest("http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML", true, msau, cus);
    
    
    //excluded
    shouldCacheTest(REAL_ROOT+"content/journal/jgv/97/1", false, msau,cus);    

  }

  


  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 4 digit
    String expected = ROOT_URL+"content/journal/foo/clockssissues?volume=123";
 
    DefinableArchivalUnit au = makeAu(url, 123, "foo");
    assertEquals(ListUtil.list(expected), au.getStartUrls());
  }


  public void testGetName() throws Exception {
    DefinableArchivalUnit au =
        makeAu(new URL("http://www.ajrnl.com/"), 33, "blah");
    //Microbiology Society Journals Plugin (CLOCKSS), Base URL %s, Journal ID %s, Volume %s
    assertEquals(PluginName + ", Base URL http://www.ajrnl.com/, Journal ID blah, Volume 33", au.getName());
  }

 
}

