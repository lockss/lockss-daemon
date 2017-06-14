/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.internationalunionofcrystallography;

import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.Tdb;
import org.lockss.daemon.*;
import org.lockss.daemon.OpenUrlResolver.OpenUrlInfo;
import org.lockss.daemon.TestOpenUrlResolver.MySimulatedPlugin4;
import org.lockss.db.DbManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.state.AuState;
import org.lockss.test.*;
import org.lockss.util.*;

//
// This plugin test framework is set up to run using the actual plugin
// information available in the TDB file
//a
public class TestIUCrFeatureUrlHelper extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau0, sau1_1, sau1_2, sau2, sau3_1, sau3_2;
  private PluginManager pluginManager;
  OpenUrlResolver openUrlResolver;
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String SCRIPT_URL_KEY = "script_url";
  static final String OAI_KEY = "au_oai_set";
  static final String DATE_KEY = "au_oai_date";
  
  
  private static final Logger log = Logger.getLogger(TestIUCrFeatureUrlHelper.class);

  static final String PLUGIN_ID = "org.lockss.plugin.internationalunionofcrystallography.oai.ClockssIUCrOaiPlugin";
  static final String PluginName = "International Union of Crystallography OAI Plugin";

  public void setUp() throws Exception {
    super.setUp();

    final String tempDirPath = setUpDiskSpace();
    ConfigurationUtil.addFromArgs(MetadataManager.PARAM_INDEXING_ENABLED, 
                                  "false");
    
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    pluginManager = theDaemon.getPluginManager();
    pluginManager.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginManager.startService();
    theDaemon.getCrawlManager();
    
    // Make a copy of current config so can add tdb
    Configuration config = ConfigManager.getCurrentConfig().copy();
    Tdb tdb = new Tdb();


    
    Properties tdbProps = new Properties();
    tdbProps.setProperty("title", "IUCrJ");
    tdbProps.setProperty("issnl", "2052-2525");
    tdbProps.setProperty("eissn", "2052-2525");
    tdbProps.setProperty("attributes.volume", "1");
    tdbProps.setProperty("attributes.publisher", "International Union of Crystallography");
    tdbProps.setProperty("plugin", PLUGIN_ID);
    tdbProps.setProperty("param.1.key", "base_url");
    tdbProps.setProperty("param.1.value", "http://journals.iucr.org/");
    tdbProps.setProperty("param.2.key", "script_url");
    tdbProps.setProperty("param.2.value", "http://scripts.iucr.org/");
    tdbProps.setProperty("param.3.key", OAI_KEY);
    tdbProps.setProperty("param.3.value", "iucrj");
    tdbProps.setProperty("param.4.key", DATE_KEY);
    tdbProps.setProperty("param.4.value", "2014-03");
    tdb.addTdbAuFromProperties(tdbProps);
    
    tdbProps = new Properties();
    tdbProps.setProperty("title", "Acta Crystallographica Section E: Crystallographic Communications");
    tdbProps.setProperty("eissn", "2056-9890");
    tdbProps.setProperty("attributes.volume", "71");
    tdbProps.setProperty("attributes.publisher", "International Union of Crystallography");
    tdbProps.setProperty("plugin", PLUGIN_ID);
    tdbProps.setProperty("param.1.key", "base_url");
    tdbProps.setProperty("param.1.value", "http://journals.iucr.org/");
    tdbProps.setProperty("param.2.key", "script_url");
    tdbProps.setProperty("param.2.value", "http://scripts.iucr.org/");
    tdbProps.setProperty("param.3.key", OAI_KEY);
    tdbProps.setProperty("param.3.value", "actacryste");
    tdbProps.setProperty("param.4.key", DATE_KEY);
    tdbProps.setProperty("param.4.value", "2015-03");
    tdb.addTdbAuFromProperties(tdbProps);    
    
    config.setTdb(tdb);
    ConfigurationUtil.installConfig(config);    
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  
  public void testFeatureHelper() throws Exception {
    DefinablePlugin myplug = new DefinablePlugin();
    myplug.initPlugin(getMockLockssDaemon(),
        PLUGIN_ID);
    TypedEntryMap map = new TypedEntryMap();
    OpenUrlResolver our = new OpenUrlResolver(theDaemon);
    
    /* STUB */
    /* currently this does nothing because the paramMap is empty */
    OpenUrlInfo resolved = our.getJournalUrl(null, myplug, map);
    if (resolved.isResolved()) {
      log.debug3("was resolved");
    }

  }


}

