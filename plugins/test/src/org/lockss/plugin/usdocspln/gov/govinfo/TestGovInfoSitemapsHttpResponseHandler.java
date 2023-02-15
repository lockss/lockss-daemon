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

package org.lockss.plugin.usdocspln.gov.govinfo;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.MockLockssUrlConnection;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.HttpResultMap;

import java.io.IOException;
import java.util.Properties;

public class TestGovInfoSitemapsHttpResponseHandler extends LockssTestCase {

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String COLLECTION_KEY = "collection_id";
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();

  private static final Logger log = Logger.getLogger(TestGovInfoSitemapsHttpResponseHandler.class);
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;

  static final String PLUGIN_ID = "org.lockss.plugin.usdocspln.gov.govinfo.GovInfoSitemapsPlugin";

  private static final String[] NON_FATAL_URLS = {
    "https://www.govinfo.gov/app/details/lib/bootstrap/ico/apple-touch-icon-167.png",
    "https://www.govinfo.gov/app/details/lib/bootstrap/ico/apple-touch-icon-76.png",
    "https://www.govinfo.gov/apple-touch-icon-72.png",
    "https://www.govinfo.gov/sites/all/apple-touch-icon-152.png",
    "https://www.govinfo.gov/sites/all/apple-touch-icon-72.png",
    "https://www.govinfo.gov/sites/all/themes/custom/misc/menu-collapsed.png",
    "https://www.govinfo.gov/sites/all/themes/custom/misc/menu-expanded.png",
    "https://www.govinfo.gov/sites/all/themes/custom/misc/menu-leaf.png",
    "https://www.govinfo.gov/app/dynamic/stylesheets/fonts/glyphicons-halflings-regular.svg",
    "https://www.govinfo.gov/sites/all/themes/custom/bootstrap-fdsys/bootstrap/fonts/glyphicons-halflings-regular.eot",
    "https://www.govinfo.gov/sites/all/themes/custom/bootstrap-fdsys/bootstrap/fonts/glyphicons-halflings-regular.woff2",
    "https://www.govinfo.gov/sites/all/themes/custom/bootstrap-fdsys/font-awesome/fonts/fontawesome-webfont.eot",
    "https://www.govinfo.gov/sites/all/themes/custom/bootstrap-fdsys/font-awesome/fonts/fontawesome-webfont.eot%3Fv=4.3.0",
    "https://www.govinfo.gov/sites/all/themes/custom/bootstrap-fdsys/font-awesome/fonts/fontawesome-webfont.svg%3Fv=4.3.0"
  };
  
  private static final String[] FATAL_URLS = {
   "https://www.govinfo.gov/app/details/CHRG-94shrg52600O/CHRG-94shrg52600O",
   "https://www.govinfo.gov/app/details/CHRG-96hhrg63288O/CHRG-96hhrg63288O",
   "https://www.govinfo.gov/app/details/COMPS-1014/context",
   "https://www.govinfo.gov/app/details/COMPS-10272/context",
   "https://www.govinfo.gov/app/details/CRECB-2001-pt20/context",
   "https://www.govinfo.gov/app/details/CRECB-2002-pt9/context",
   "https://www.govinfo.gov/app/details/CRPT-116hrpt587/context",
   "https://www.govinfo.gov/app/details/DCPD-202000052/context",
   "https://www.govinfo.gov/app/details/DCPD-202000835/context",
   "https://www.govinfo.gov/app/details/GOVPUB-RR-7930583c94a57c5fd7d888f159aeefb8/context",
   "https://www.govinfo.gov/app/details/GOVPUB-RR-f8741837020f5bf1d929db30a3c8ef27/context",
   "https://www.govinfo.gov/app/details/PAI-2007-OPM/context",
   "https://www.govinfo.gov/app/details/PAI-2007-PC/context",
   "https://www.govinfo.gov/app/details/USCOURTS-miwb-1_05-ap-80573/USCOURTS-miwb-1_05-ap-80573-0",
   "https://www.govinfo.gov/app/details/USCOURTS-txsb-4_12-bk-31267/USCOURTS-txsb-4_12-bk-31267-1",
   "https://www.govinfo.gov/app/details/USCOURTS-txsb-7_13-ap-07001/USCOURTS-txsb-7_13-ap-07001-0",
   "https://www.govinfo.gov/app/details/USCOURTS-wiwb-1_08-bk-15291/context",
   "https://www.govinfo.gov/content/pkg/CRPT-116hrpt709/html/CRPT-116hrpt709.htm",
   "https://www.govinfo.gov/content/pkg/DCPD-202000385/html/DCPD-202000385.htm",
   "https://www.govinfo.gov/content/pkg/DCPD-202100012/html/DCPD-202100012.htm"
  };

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
    theDaemon.getRepositoryManager();
    plugin = new DefinablePlugin();
    plugin.initPlugin(theDaemon,
        PLUGIN_ID);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return (DefinableArchivalUnit)plugin.configureAu(config, null);
  }

  private DefinableArchivalUnit makeGiAu() throws ArchivalUnit.ConfigurationException {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "https://www.govinfo.gov/");
    props.setProperty(COLLECTION_KEY, "DCPD");
    props.setProperty(YEAR_KEY, "2018");
    return makeAuFromProps(props);
  }

  private void testUrls(String[] urls_list,
                       int responseCode,
                       boolean noFail
      ) throws ArchivalUnit.ConfigurationException, IOException {
    DefinableArchivalUnit GIau = makeGiAu();
    MockLockssUrlConnection conn = new MockLockssUrlConnection();
    for (String url : urls_list) {
      conn.setURL(url);
      CacheException exc = ((HttpResultMap) plugin.getCacheResultMap()).mapException(GIau, conn, responseCode, "foo");
      if (noFail) {
        assertClass(GovInfoRetrySameUrlException.class, exc); // This used to be different for ancillary URLs but not any more
      } else {
        assertClass(GovInfoRetrySameUrlException.class, exc);
      }
    }
  }

  // as the pattern is currently the same for 404 and 504, this test is redundant
  public void testShouldReturnNotFatal504() throws Exception {
    testUrls(NON_FATAL_URLS, 504, true);
  }

  public void testShoulBeFatal504() throws Exception {
    testUrls(FATAL_URLS, 504, false);
  }
}

