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

package org.lockss.plugin.internationalunionofcrystallography.oai;


import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.test.*;

public class TestIUCrOaiPlugin extends LockssPluginTestCase {

  protected MockLockssDaemon daemon;
  private final String PLUGIN_NAME = "org.lockss.plugin.internationalunionofcrystallography.oai.ClockssIUCrOaiPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String SCRIPT_URL_KEY = "script_url";
  static final String OAI_SET_KEY = "au_oai_set";
  static final String OAI_DATE_KEY = "au_oai_date";
  private static final String BASE_URL = "http://www.example.com/";
  private static final String SCRIPT_URL = "http://scripts.example.com/";
  private static final String OAI_SET = "iucrdata";
  private static final String OAI_DATE = "2015-06";
  private static final String JOURNALS_URL = "http://journals.iucr.org/";
  private static final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      SCRIPT_URL_KEY, SCRIPT_URL,
      OAI_SET_KEY, OAI_SET,
      OAI_DATE_KEY, OAI_DATE);


  public void setUp() throws Exception {
    super.setUp();
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }

  protected ArchivalUnit createAu(Configuration config) throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME, config);
  }

  public void testCreateAu() {
    try {
      createAu(ConfigurationUtil.fromArgs(
          BASE_URL_KEY, BASE_URL,
          SCRIPT_URL_KEY, SCRIPT_URL));
      fail("Bad AU configuration should throw configuration exception");
    }
    catch (ConfigurationException ex) {
    }
    try {
      createAu(AU_CONFIG);
    }
    catch (ConfigurationException ex) {
      fail("Unable to creat AU from valid configuration");
    }
  }
  
  public void testShouldCacheProperPages() throws Exception {
    ArchivalUnit au = createAu(AU_CONFIG);

    // start page
    assertShouldCache(BASE_URL + "x/issues/2016/lockss.html", true, au);
    assertShouldCache(SCRIPT_URL + "lockss.html", true, au);
    assertShouldCache(SCRIPT_URL + "auid=org%7Clockss%7Cplugin%7Cinternationalunionofcrystallography%7Coai%7CClockssIUCrOaiPlugin%26au_oai_date%7E2015-06" +
        "%26au_oai_set%7Eiucrdata%26base...", true, au);

    // article pages
    assertShouldCache(BASE_URL + "x/issues/2016/01/00/su4003/su4003.pdf", true, au);
    assertShouldCache(SCRIPT_URL + "x/issues/2016/01/00/su4003/su4003.pdf", true, au);
    assertShouldCache(BASE_URL + "cgi-bin/paper?STUFF", true, au);
    assertShouldCache(SCRIPT_URL + "cgi-bin/paper?STUFF&more", true, au);
    assertShouldCache(BASE_URL + "cgi-bin/sendcif?STUFF", true, au);
    assertShouldCache(SCRIPT_URL + "cgi-bin/sendcif?STUFF", true, au);
    
    // Permission URL fails should cache check
    assertShouldCache(JOURNALS_URL + "x/issues/2016/01/00/su4003/su4003.pdf", false, au);
    assertShouldCache(JOURNALS_URL + "e/issues/2010/lockss.html", false, au);
    
    // images, css, js
    assertShouldCache(BASE_URL + "jQuery/css/smoothness/images/ui-bg_flat_75_ffffff_40x100.png", true, au);
    assertShouldCache(SCRIPT_URL + "lots/of/stuff/100.png", true, au);
    
    // specified bad pages
    assertShouldCache(BASE_URL + "lots/of/stuff?buy=yes", false, au);
    assertShouldCache(SCRIPT_URL + "lots/of/stuff?buy=yes", false, au);
    assertShouldCache(JOURNALS_URL + "lots/of/stuff?buy=yes", false, au);
    assertShouldCache(BASE_URL + "lots/of/stuff?buy=yes", false, au);
    assertShouldCache(SCRIPT_URL + "cgi-bin/sendcif?STUFF&Qmime=cif", false, au);

    // facebook
    assertShouldCache("http://www.facebook.com/pages/IGI-Global/138206739534176?ref=sgm", false, au);

    // LOCKSS
    assertShouldCache("http://lockss.stanford.edu", false, au);

    // other site
    assertShouldCache("http://exo.com/~noid/ConspiracyNet/satan.html", false, au);
  }

  private void assertShouldCache(String url, boolean shouldCache, ArchivalUnit au) {
    
    assertEquals(url + " ", shouldCache, 
        au.shouldBeCached(url)
        )
        ;
  }

  public void testGetName() throws Exception {
    ArchivalUnit au = createAu(AU_CONFIG);
    assertEquals("International Union of Crystallography OAI Plugin (CLOCKSS), Base URL " + BASE_URL +
        ", Script URL " + SCRIPT_URL + ", OAI Set " + OAI_SET + ", OAI Date " + OAI_DATE,
        au.getName());
  }

}
