/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair;

import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcher;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.RegexpUtil;

import java.util.List;
import java.util.Properties;

//
// This plugin test framework is set up to run the same tests in two variants - CLOCKSS and GLN
// without having to actually duplicate any of the written tests
//
public class TestSilverchairCommonTheseBooksArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String RID_KEY = "resource_id";
  
  static Logger log = Logger.getLogger(TestSilverchairCommonTheseBooksArchivalUnit.class);
  
  static final String PLUGIN_ID = "org.lockss.plugin.silverchair.SilverchairCommonThemeBooksPlugin";
  static final String ROOT_URL = "http://www.silverroot.com/";
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }
  


  private DefinableArchivalUnit makeAu(String resource, String year)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(RID_KEY, resource);
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    Configuration config = ConfigurationUtil.fromProps(props);
    
    DefinablePlugin ap = new DefinablePlugin();
      ap.initPlugin(getMockLockssDaemon(),PLUGIN_ID);
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }


  List<String> substanceList = ListUtil.list(
    ROOT_URL + "books/book/2160/chapter-pdf/4705971/01_sp471-18-052.pdf",
    ROOT_URL + "books/edited-volume/chapter-pdf/1716079/bk9781839164224-00280.pdf",
    ROOT_URL + "ebooks/book-pdf/622664/wio9781789061246.pdf",
    ROOT_URL + "books/book/2082/chapter-pdf/4535237/sepb-16-01-380-401.pdf",
    ROOT_URL + "books/book/chapter-pdf/4534955/frontmatter.pdf"
  );

  List<String> notSubstanceList = ListUtil.list(
    ROOT_URL + "books/book/2082/chapter/114313785/Front-Matter",
    ROOT_URL + "ebooks/book/773/Frontier-Technology-for-Water-Treatment-and"
  );
  
  public void testCheckSubstanceRules() throws Exception {
    boolean found;
    ArchivalUnit jsAu = makeAu("72","2012");
    PatternMatcher matcher = RegexpUtil.getMatcher();   
    List<Pattern> patList = jsAu.makeSubstanceUrlPatterns();

    log.setLevel("debug3");
    for (String nextUrl : substanceList) {
      log.debug3("testing for substance: "+ nextUrl);
      found = false;
      for (Pattern nextPat : patList) {
            found = matcher.matches(nextUrl, nextPat);
            if (found) break;
      }
      assertEquals(true,found);
    }
    
    for (String nextUrl : notSubstanceList) {
      log.debug3("testing for not substance: "+ nextUrl);
      found = false;
      for (Pattern nextPat : patList) {
            found = matcher.matches(nextUrl, nextPat);
            if (found) break;
      }
      assertEquals(false,found);
    }
  }
}

