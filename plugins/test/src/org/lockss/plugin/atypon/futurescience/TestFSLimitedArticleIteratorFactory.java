/*
 * $Id$
 */
/*

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.futurescience;

import java.util.regex.Pattern;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.test.*;

public class TestFSLimitedArticleIteratorFactory extends ArticleIteratorTestCase {
	
	
  private final String PLUGIN_NAME = "org.lockss.plugin.atypon.futurescience.ClockssFutureSciencePlugin";
  private final String BASE_URL = "http://www.baseatypon.org/";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  private final String JOURNAL_ID = "foo";
  private final String VOLUME_NAME = "123";
  

  private final Configuration AU_CONFIG = 
      ConfigurationUtil.fromArgs(BASE_URL_KEY, BASE_URL,
    	      JOURNAL_ID_KEY, JOURNAL_ID,
    	      VOLUME_NAME_KEY, VOLUME_NAME);

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
	    
	    // we match to doi/(full|pdf|pdfplus)
	    assertMatchesRE(pat, "http://www.baseatypon.org/doi/pdf/10.1137/foo100818522"); 
	    assertNotMatchesRE(pat, "http://www.baseatypon.org/doi/abs/10.1137/foo.100818522");
	    assertMatchesRE(pat, "http://www.baseatypon.org/doi/full/10.1137/foo-100818522");
	    assertMatchesRE(pat, "http://www.baseatypon.org/doi/full/10.1137/FOO-100818522");
	    assertMatchesRE(pat, "http://www.baseatypon.org/doi/pdfplus/10.1137/FOO100818522");
	    assertMatchesRE(pat, "http://www.baseatypon.org/doi/pdfplus/10.1137/FoO100818522");
	    assertMatchesRE(pat, "http://www.baseatypon.org/doi/pdfplus/10.1137/FOo100818522");
	    // prefix of DOI can have additional dots
	    assertMatchesRE(pat, "http://www.baseatypon.org/doi/full/10.1137.12.13/foo100818522");
	    // PATTERN will allow this but actual article pattern matches will not allow "/" in 2nd part of DOI
	    assertMatchesRE(pat, "http://www.baseatypon.org/doi/full/10.1137/foo.ABC1234-3/fff");

	    // but not to wrong jid
	    assertNotMatchesRE(pat, "http://www.baseatypon.org/doi/abs/10.1137/foq-100818522");
	    assertNotMatchesRE(pat, "http://www.baseatypon.org/doi/abs/10.1137/qfoo-100818522");
	    assertNotMatchesRE(pat, "http://www.baseatypon.org/doi/abs/10.1137/blah.100818522");
	    
	    // as always, test wrong base url
	    assertNotMatchesRE(pat, "http://ametsoc.org/doi/full/10.1175/FOO2009WCAS1006.1");
	  }

  

}