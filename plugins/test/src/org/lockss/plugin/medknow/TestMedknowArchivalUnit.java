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

package org.lockss.plugin.medknow;

import java.net.URL;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.*;
import org.lockss.state.AuState;
import org.lockss.test.*;
import org.lockss.util.*;

//
// This plugin test framework is set up to run the same tests in two variants - CLOCKSS and GLN
// without having to actually duplicate any of the written tests
//
public class TestMedknowArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String ISSN_KEY = ConfigParamDescr.JOURNAL_ISSN.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String ROOT_URL = "http://www.jrnl.com/"; //this is not a real url

  private static final Logger log = Logger.getLogger(TestMedknowArchivalUnit.class);

  static final String PLUGIN_ID = "org.lockss.plugin.medknow.MedknowPlugin";
  static final String PluginName = "Medknow Publications";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, int volume, String issn, int year)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
    props.setProperty(ISSN_KEY, issn);
    props.setProperty(YEAR_KEY, Integer.toString(year));
    if (url != null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);

    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(),
        PLUGIN_ID);
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, 1, "1111-1111", 2011);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }

  //
  // Test the crawl rules
  //
  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit ABAu = makeAu(base, 123, "1234-5678", 2011);
    theDaemon.getLockssRepository(ABAu);
    theDaemon.getNodeManager(ABAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(ABAu,
        new RangeCachedUrlSetSpec(base.toString()));

    //permission page
    shouldCacheTest(ROOT_URL+"lockss.txt", true, ABAu, cus);    
    // images (etc.) 
    shouldCacheTest(ROOT_URL+"foo/bar/baz/qux.js", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"foo/bar/baz/qux.jpg", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"foo/bar/baz/qux.jpeg", true, ABAu, cus);
    // but not off the main host
    //http://www.medknow.com/journals/images/aim.gif
    shouldCacheTest("http://www.medknow.com/"+"foo/bar/baz/qux.gif", false, ABAu, cus);

    // manifest page for a volume
    //http://www.jpgmonline.com/backissues.asp
    shouldCacheTest(ROOT_URL+"backissues.asp", true, ABAu, cus);
    // toc page for an issue
    // www.jpgmonline.com/showbackIssue.asp?issn=0022-3859;year=2013;volume=59;issue=3    
    shouldCacheTest(ROOT_URL+"showbackIssue.asp?issn=1234-5678;year=2011;volume=123;issue=7", true, ABAu, cus);
    // even if there is extra stuff that gets normalized off
    shouldCacheTest(ROOT_URL+"showbackIssue.asp?issn=1234-5678;year=2011;volume=123;issue=7;month=March-May", true, ABAu, cus);

    // individual article page - abstract, full-text html and pdf
    // http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=4;spage=306;epage=308;aulast=Pandey;type=2
    shouldCacheTest(ROOT_URL+"article.asp?issn=1234-5678;year=2011;volume=123;issue=", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"article.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"article.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;type=0", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"article.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;type=2", true, ABAu, cus);
    // but not the epub or other types offered
    shouldCacheTest(ROOT_URL+"article.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;type=3", false, ABAu, cus);
    shouldCacheTest(ROOT_URL+"article.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;type=4", false, ABAu, cus);
    shouldCacheTest(ROOT_URL+"article.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;type=5", false, ABAu, cus);

    // citation landing page -- with or without type; we should normalize this to only one landing page (abstract)
    //http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=2;spage=93;epage=97;aulast=Kole;type=0;aid=jpgm_2013_59_2_93_113811
    //http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=2;spage=93;epage=97;aulast=Kole;aid=jpgm_2013_59_2_93_113811
    shouldCacheTest(ROOT_URL+"citation.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;type=0;aid=jpgm_2013_59_2_93_113811", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"citation.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"citation.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;type=0", true, ABAu, cus);
    
    // citation - get this type
    //http://www.jpgmonline.com/citeman.asp?issn=0022-3859;year=2013;volume=59;issue=2;spage=93;epage=97;aulast=Kole;type=0;aid=jpgm_2013_59_2_93_113811;t=5
    shouldCacheTest(ROOT_URL+"citeman.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;type=0;aid=jpgm_2013_59_2_93_113811;t=2", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"citeman.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;t=2", true, ABAu, cus);
    shouldCacheTest(ROOT_URL+"citeman.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;type=0;t=2", true, ABAu, cus);
    
    
    // but not this type
    //http://www.jpgmonline.com/citeman.asp?issn=0022-3859;year=2013;volume=59;issue=2;spage=93;epage=97;aulast=Kole;type=0;aid=jpgm_2013_59_2_93_113811;t=0
    // the "aid part is allowed but normalized off
    shouldCacheTest(ROOT_URL+"citeman.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;type=0;t=1", false, ABAu, cus);
    shouldCacheTest(ROOT_URL+"citeman.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;type=0;t=5", false, ABAu, cus);
    shouldCacheTest(ROOT_URL+"citeman.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;type=0;t=3", false, ABAu, cus);
    shouldCacheTest(ROOT_URL+"citeman.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;t=3", false, ABAu, cus);
    shouldCacheTest(ROOT_URL+"citeman.asp?issn=1234-5678;year=2011;volume=123;issue=7;spage=306;epage=308;aulast=Pandey;type=0;aid=foo;t=3", false, ABAu, cus);
    
    // and get tables and images for the article
    //http://www.jpgmonline.com/viewimage.asp?img=jpgm_2013_59_2_93_113811_t3.jpg
    shouldCacheTest(ROOT_URL+"viewimage.asp?img=jpgm_2013_59_2_93_113811_t3.jpg", true, ABAu, cus);
  }

  
  //http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2014;volume=60;issue=3;spage=239;epage=240;aulast=Phani;type=2
  public void testShouldCacheRealPages() throws Exception {
    URL base = new URL("http://www.jpgmonline.com/");
    ArchivalUnit ABAu = makeAu(base, 60, "0022-3859", 2014);
    theDaemon.getLockssRepository(ABAu);
    theDaemon.getNodeManager(ABAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(ABAu,
        new RangeCachedUrlSetSpec(base.toString()));
    
    
    
    //pdf
    shouldCacheTest("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2014;volume=60;issue=4;spage=355;epage=356;aulast=Jagannathan",
        true, ABAu, cus);
    shouldCacheTest("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2014;volume=60;issue=4;spage=355;epage=356;aulast=Jagannathan;type=2",
        true, ABAu, cus);
    //redirects to this:
    shouldCacheTest("http://www.jpgmonline.com/downloadpdf.asp?issn=0022-3859;year=2014;volume=60;issue=4;spage=355;epage=356;aulast=Jagannathan;type=2",
        true,ABAu, cus);
    shouldCacheTest("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2014;volume=60;issue=3;spage=239;epage=240;aulast=Phani;type=2",
        true, ABAu, cus);

  }

  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 4 digit
    String expected = ROOT_URL+"backissues.asp";
    String perm = ROOT_URL+"lockss.txt";
    DefinableArchivalUnit ABAu = makeAu(url, 123, "1234-5555", 2009);
    assertEquals(ListUtil.list(expected), ABAu.getStartUrls());
    assertEquals(ListUtil.list(perm), ABAu.getPermissionUrls());
  }


  public void testGetName() throws Exception {
    DefinableArchivalUnit au =
        makeAu(new URL("http://www.ajrnl.com/"), 33, "1234-5678", 2009);
    assertEquals(PluginName + ", Base URL http://www.ajrnl.com/, Issn 1234-5678, Year 2009, Volume 33", au.getName());
  }

 
}

