/*
 * $Id: $
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ojs2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.plugin.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.definable.*;

public class TestOJS2SubstanceChecker extends LockssTestCase {
  static final String PLUGIN_ID = "org.lockss.plugin.ojs2.ClockssOJS2Plugin";
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  
  static final String ROOT_URL = "http://www.example.com/ojs/";
  static final String JOURNAL_ID = "j_id";
  
  private MockLockssDaemon theDaemon;
  
  public TestOJS2SubstanceChecker(String msg) {
    super(msg);
  }
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
  }
  
  private DefinableArchivalUnit makeAu(String url, String jid, String year)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(JOURNAL_ID_KEY, jid);
    props.setProperty(YEAR_KEY, year);
    if (url != null) {
      props.setProperty(BASE_URL_KEY, url);
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon, PLUGIN_ID);
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  
  /*
      ROOT_URL+"",
      
   */
  List<String> substanceList = ListUtil.list(
      ROOT_URL+"article/view/782/1344",
      ROOT_URL+"article/view/v21i1p3/pdf",
      ROOT_URL+"article/download/883/1541",
      ROOT_URL+"article/download/1027/LucasN6.pdf",
      ROOT_URL+"article/view/1027/LucasN6.pdf",
      ROOT_URL+"j_id/article/view/782/1344",
      ROOT_URL+"j_id/article/view/v21i1p3/pdf",
      ROOT_URL+"j_id/article/download/883/1541",
      ROOT_URL+"j_id/article/download/1027/LucasN6.pdf",
      ROOT_URL+"j_id/article/view/1027/LucasN6.pdf",
      
      ROOT_URL+"index.php/article/view/782/1344",
      ROOT_URL+"index.php/article/view/v21i1p3/pdf",
      ROOT_URL+"index.php/article/download/883/1541",
      ROOT_URL+"index.php/article/download/1027/LucasN6.pdf",
      ROOT_URL+"index.php/article/view/1027/LucasN6.pdf",
      ROOT_URL+"index.php/j_id/article/view/782/1344",
      ROOT_URL+"index.php/j_id/article/view/v21i1p3/pdf",
      ROOT_URL+"index.php/j_id/article/download/883/1541",
      ROOT_URL+"index.php/j_id/article/download/1027/LucasN6.pdf",
      ROOT_URL+"index.php/j_id/article/view/1027/LucasN6.pdf"
      );
  
  List<String> notSubstanceList = ListUtil.list(
      ROOT_URL+"article/viewFile/783/1345/3459",  // IMAGE file?
      ROOT_URL+"article/view/814/1404?foo",
      ROOT_URL+"article/view/814/1404#1",
      ROOT_URL+"article/view/814/1404.htm",
      ROOT_URL+"article/view/783",
      ROOT_URL+"index.php/j_id/article/view/1027",
      ROOT_URL+"index.php/j_id/gateway/lockss?year=2014",
      ROOT_URL+"index.php/j_id/issue/view/92",
      ROOT_URL+"js/inlinePdf.js",
      ROOT_URL+"js/pages/search/SearchFormHandler.js",
      ROOT_URL+"lib/pkp/js/classes/Handler.js",
      ROOT_URL+"lib/pkp/styles/common.css",
      ROOT_URL+"lib/pkp/styles/lib/jquery.pnotify.default.css",
      ROOT_URL+"lib/pkp/styles/splitter/ui-bg_pane.gif",
      ROOT_URL+"lib/pkp/templates/images/icons/font-default.png",
      ROOT_URL+"lib/pkp/templates/images/pkp.gif",
      ROOT_URL+"styles/articleView.css",
      ROOT_URL+"styles/common.css"
      );
  

  public void testCheckSubstanceRules() throws Exception {
    ArchivalUnit tau = makeAu(ROOT_URL, JOURNAL_ID, "2014");
       
    assert(tau.makeSubstanceUrlPatterns().size() == 1);
    boolean found = false;
    
    // <string>"^%s(?:index[.]php/)?(?:%s/)?article/(?:view(?:File)?|download)/[^/]+/[^/?#&amp;.]+([./]pdf)?$", base_url, journal_id</string>
    // Note: '&amp;' in XML becomes '&' in Java
    String strPat = "^" + ROOT_URL + "(?:index[.]php/)?(?:" + JOURNAL_ID + "/)?article/(?:view(?:File)?|download)/[^/]+/[^/?#&.]+([./]pdf)?$";
    Pattern thisPat = Pattern.compile(strPat);
    
    for (String nextUrl : substanceList) {
      log.debug("testing for substance: "+ nextUrl);
      Matcher m = thisPat.matcher(nextUrl);
      found = m.matches();
      if (!found) break;
    }
    assertEquals(true, found);
    
    for (String nextUrl : notSubstanceList) {
      log.debug("testing for not substance: "+ nextUrl);
      Matcher m = thisPat.matcher(nextUrl);
      found = m.matches();
      if (found) break;
    }
    assertEquals(false,found);
  }
  
}
