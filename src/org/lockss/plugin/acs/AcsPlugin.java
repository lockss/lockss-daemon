/*
 * $Id: AcsPlugin.java,v 1.11 2004-02-17 21:46:00 clairegriffin Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.acs;

import org.lockss.daemon.*;
import org.lockss.plugin.base.BasePlugin;
import org.lockss.plugin.*;
import org.lockss.util.*;

import java.util.List;
import java.util.Collection;
import org.lockss.app.*;
import org.lockss.plugin.configurable.*;

/**
 * AcsPlugin: Plugin class for the American Chemical Society Plugin
 * @author Claire Griffin
 * @version 1.0
 */

public class AcsPlugin extends ConfigurablePlugin {
  static final ConfigParamDescr JOURNAL_KEY = new ConfigParamDescr();
  static {
    JOURNAL_KEY.setKey("journal_key");
    JOURNAL_KEY.setDisplayName("Journal ID");
    JOURNAL_KEY.setType(ConfigParamDescr.TYPE_STRING);
    JOURNAL_KEY.setSize(20);
    JOURNAL_KEY.setDescription("Key used to identify journal in script (e.g. 'jcisd8').");
  }

  static final ConfigParamDescr ARTICLE_URL = new ConfigParamDescr();
  static {
    ARTICLE_URL.setKey("article_url");
    ARTICLE_URL.setDisplayName("Article URL");
    ARTICLE_URL.setType(ConfigParamDescr.TYPE_URL);
    ARTICLE_URL.setSize(40);
    ARTICLE_URL.setDescription("base url for articles");
  }

  private static String PLUGIN_NAME = "ACS";
  private static String CURRENT_VERSION= "Pre-release";

  static final ConfigParamDescr PD_BASE = ConfigParamDescr.BASE_URL;
  static final ConfigParamDescr PD_JKEY = JOURNAL_KEY;
  static final ConfigParamDescr PD_VOL = ConfigParamDescr.VOLUME_NUMBER;
  static final ConfigParamDescr PD_ARTICLE = ARTICLE_URL;
  static final ConfigParamDescr PD_YEAR = ConfigParamDescr.YEAR;

  // public only so test methods can use them
  public static final String AUPARAM_BASE_URL = PD_BASE.getKey();
  public static final String AUPARAM_VOL = PD_VOL.getKey();
  public static final String AUPARAM_JOURNAL_KEY = PD_JKEY.getKey();
  public static final String AUPARAM_ARTICLE_URL = PD_ARTICLE.getKey();
  public static final String AUPARAM_YEAR = PD_YEAR.getKey();

  public void initPlugin(LockssDaemon daemon){
    //todo: we override initPlugin largely to manually load the values that
    // should be put into the configuration map when we load it from disk
    definitionMap.putString(CM_NAME_KEY, PLUGIN_NAME);
    definitionMap.putString(CM_VERSION_KEY, CURRENT_VERSION);
    definitionMap.putCollection(CM_CONFIG_PROPS_KEY,
                                   ListUtil.list(PD_BASE, PD_ARTICLE, PD_JKEY,
                                                 PD_VOL, PD_YEAR));
    // then call the overridden initializaton.
    super.initPlugin(daemon);
  }

  public ArchivalUnit createAu(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    ArchivalUnit au = new AcsArchivalUnit(this, definitionMap);
    au.setConfiguration(auConfig);
    return au;
  }
}
