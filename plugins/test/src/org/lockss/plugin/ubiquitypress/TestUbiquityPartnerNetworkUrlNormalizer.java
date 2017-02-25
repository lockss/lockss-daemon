/*
 * $Id$
 */

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

package org.lockss.plugin.ubiquitypress;

import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.plugin.ubiquitypress.upn.UbiquityPartnerNetworkUrlNormalizer;
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
      // issue toc becomes the manifest
      assertEquals("http://www.xyz.com/lockss/year/2014/",
          norm.normalizeUrl("http://www.xyz.com/7/volume/4/issue/1/", testAU) );
      // print at end of article is stripped
      assertEquals("http://www.xyz.com/articles/10.5334/sta.eu/",
          norm.normalizeUrl("http://www.xyz.com/articles/10.5334/sta.eu/print/", testAU) );
    }
  
}



    