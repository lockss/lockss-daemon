/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.stanforduniversitypress;

import java.util.regex.Pattern;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestSUPressZipSourceArticleIteratorFactory extends ArticleIteratorTestCase {
	
	
  private final String PLUGIN_NAME = "org.lockss.plugin.clockss.stanforduniversitypress.ClockssSUPressOnix2BooksZipSourcePlugin";
  private final String BASE_URL = "http://www.example.com/";
  private final String YEAR = "2012";

  private final Configuration AU_CONFIG = 
      ConfigurationUtil.fromArgs(BASE_URL_KEY, BASE_URL,
          YEAR_KEY, YEAR);
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();

  public void setUp() throws Exception {
    super.setUp();
    au = createAu();
  }
  
  public void tearDown() throws Exception {
	    super.tearDown();
	  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
      PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertMatchesRE(pat,"http://www.example.com/2012/StanfordToCLOCKSS_Jan302014.zip!/May_2014/BV_ONIX_feed_stanford_nodatelimit_ISBNGroup_189.xml");
    assertNotMatchesRE(pat,"http://www.example.com/2012/StanfordToCLOCKSS_Jan302014.zip!/May_2014/BV_ONIX_feed_stanford_nodatelimit_ISBNGroup_189.pdf");
    assertNotMatchesRE(pat,"http://www.example.com/2012/StanfordToCLOCKSS_Jan302014.zip!/__MACOSX/May_2014/BV_ONIX_feed_stanford_nodatelimit_ISBNGroup_189.xml");
    assertNotMatchesRE(pat,"http://www.example.com/2012/StanfordToCLOCKSS_Jan302014.zip!/__MACOSX/May_2014/BV_ONIX_feed_stanford_nodatelimit_ISBNGroup_189.pdf");
    assertNotMatchesRE(pat,"http://www.example.com/2012/StanfordToCLOCKSS_Jan302014.zip!/May_2014/__MACOSX/BV_ONIX_feed_stanford_nodatelimit_ISBNGroup_189.xml");
    assertMatchesRE(pat,"http://www.example.com/2012/StanfordToCLOCKSS_Jan302014.zip!/BV_ONIX_feed_stanford_nodatelimit_ISBNGroup_189.xml");
    assertNotMatchesRE(pat,"http://www.example.com/2012/StanfordToCLOCKSS_Jan302014.zip!/May_2014/otherdir/__MACOSX/May_2014/otherdir/BV_ONIX_feed_stanford_nodatelimit_ISBNGroup_189.xml");
    assertNotMatchesRE(pat,"http://www.example.com/2012/StanfordToCLOCKSS_Jan302014.zip!/May_2014/__MACOSX/.__BV_ONIX_feed_stanford_nodatelimit_ISBNGroup_189.xml");
   }


}