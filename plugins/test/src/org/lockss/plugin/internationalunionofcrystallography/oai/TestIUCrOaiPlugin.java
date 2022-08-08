/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
    assertShouldCache(BASE_URL + "e/issues/2020/lockss.html", true, au);
    assertShouldCache(SCRIPT_URL + "lockss.html", true, au);
    assertShouldCache(SCRIPT_URL + "lockss?au_oai_set=" + OAI_SET + "&au_oai_date=" + OAI_DATE, true, au);
    assertShouldCache(SCRIPT_URL + "auid=org%7Clockss%7Cplugin%7Cinternationalunionofcrystallography%7Coai%7CClockssIUCrOaiPlugin%26au_oai_date%7E2015-06" +
        "%26au_oai_set%7Eiucrdata%26base...", false, au); // old style, should be excluded now

    // article pages
    assertShouldCache(BASE_URL + "x/issues/2016/01/00/su4003/su4003.pdf", true, au);
    assertShouldCache(SCRIPT_URL + "x/issues/2016/01/00/su4003/su4003.pdf", true, au);
    assertShouldCache(BASE_URL + "cgi-bin/paper?STUFF", true, au);
    assertShouldCache(SCRIPT_URL + "cgi-bin/paper?STUFF&more", true, au);
    assertShouldCache(BASE_URL + "cgi-bin/sendcif?STUFF", true, au);
    assertShouldCache(SCRIPT_URL + "cgi-bin/sendcif?STUFF", true, au);
    
    // Permission URL fails should cache check
    assertShouldCache(JOURNALS_URL + "e/issues/2016/01/00/su4003/su4003.pdf", false, au);
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
    assertEquals("International Union of Crystallography Plugin (CLOCKSS), Base URL " + BASE_URL +
        ", Script URL " + SCRIPT_URL + ", OAI Set " + OAI_SET + ", OAI Date " + OAI_DATE,
        au.getName());
  }

}
