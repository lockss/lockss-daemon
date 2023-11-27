/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ubiquitypress.upn;

import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;

public class TestUbiquityPartnerNetworkUrlNormalizer extends LockssTestCase {
  
    private MockLockssDaemon theDaemon;
    DefinableArchivalUnit testAU; 
    static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
    static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
    static final String ROOT_URL = "http://www.xyz.com/"; //this is not a real url  
    
    static final String PLUGIN_ID = "org.lockss.plugin.ubiquitypress.upn.ClockssUbiquityPartnerNetworkPlugin";

    public void setUp() throws Exception {
      super.setUp();
      setUpDiskSpace();
      theDaemon = getMockLockssDaemon();
      theDaemon.getHashService();
      Properties props = new Properties();
      props.setProperty(YEAR_KEY, "2014");
      props.setProperty(BASE_URL_KEY, ROOT_URL);
      Configuration config = ConfigurationUtil.fromProps(props);

      DefinablePlugin ap = new DefinablePlugin();
      ap.initPlugin(getMockLockssDaemon(),
          PLUGIN_ID);
      testAU = (DefinableArchivalUnit)ap.createAu(config);
      
    }

    public void tearDown() throws Exception {
      super.tearDown();
    }

  
    public void testUbiquityPartnerNetworkUrlNormalizer() throws Exception {
      UbiquityPartnerNetworkUrlNormalizer norm = new UbiquityPartnerNetworkUrlNormalizer();
      
      // manifest stays the manifest
      assertEquals("http://www.xyz.com/lockss/year/2014/",
          norm.normalizeUrl("http://www.xyz.com/lockss/year/2014/", testAU) );
      // print at end of article is stripped
      assertEquals("http://www.xyz.com/articles/10.5334/sta.eu/",
          norm.normalizeUrl("http://www.xyz.com/articles/10.5334/sta.eu/print/", testAU) );
      // ?action=download at end of article is stripped
      assertEquals("http://www.xyz.com/fig1.jpg",
          norm.normalizeUrl("http://www.xyz.com/fig1.jpg?action=download", testAU) );
      //https://www.gewina-studium.nl/articles/10.18352/studium.10197/figures/6_Schoolmann_and_Flipse_fig1.jpg?action=download
      assertEquals("http://www.xyz.com/foo.css",
          norm.normalizeUrl("http://www.xyz.com/foo.css", testAU) );
      assertEquals("http://www.xyz.com/foo.css",
          norm.normalizeUrl("http://www.xyz.com/foo.css?2099-12-31", testAU) );
    }
  
}



    