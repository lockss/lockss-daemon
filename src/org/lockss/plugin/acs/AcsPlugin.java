/*
 * $Id: AcsPlugin.java,v 1.4 2003-09-26 23:50:39 eaalto Exp $
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

/**
 * <p>AcsPlugin: Plugin class for the American Chemical Society Plugin</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class AcsPlugin extends BasePlugin {
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
    ARTICLE_URL.setType(ConfigParamDescr.TYPE_STRING);
    ARTICLE_URL.setSize(40);
    ARTICLE_URL.setDescription("base url for articles");
  }

  static final ConfigParamDescr JOURNAL_YEAR = new ConfigParamDescr();
  static {
    JOURNAL_YEAR.setKey("volume_year");
    JOURNAL_YEAR.setDisplayName("Volume Year");
    JOURNAL_YEAR.setType(ConfigParamDescr.TYPE_INT);
    JOURNAL_YEAR.setSize(4);
    JOURNAL_YEAR.setDescription("Year of volume in form 2003 not 03");
  }

  private static String PLUGIN_NAME = "ACS";
  private static String CURRENT_VERSION= "Pre-release";

  static final ConfigParamDescr PD_BASE = ConfigParamDescr.BASE_URL;
  static final ConfigParamDescr PD_JKEY = JOURNAL_KEY;
  static final ConfigParamDescr PD_VOL = ConfigParamDescr.VOLUME_NUMBER;
  static final ConfigParamDescr PD_ARTICLE = ARTICLE_URL;
  static final ConfigParamDescr PD_YEAR = JOURNAL_YEAR;

  // public only so test methods can use them
  public static final String AUPARAM_BASE_URL = PD_BASE.getKey();
  public static final String AUPARAM_VOL = PD_VOL.getKey();
  public static final String AUPARAM_JOURNAL_KEY = PD_JKEY.getKey();
  public static final String AUPARAM_ARTICLE_URL = PD_ARTICLE.getKey();
  public static final String AUPARAM_YEAR = PD_YEAR.getKey();

  private static String titleSpec[][] = {
    { "JCICS",
    AUPARAM_BASE_URL, "http://pubs3.acs.org/",
    AUPARAM_ARTICLE_URL, "http://pubs.acs.org/",
    AUPARAM_JOURNAL_KEY, "jcisd8",
    AUPARAM_VOL, "43",
    AUPARAM_YEAR, "2003"}
  };

  public ArchivalUnit createAu(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    ArchivalUnit au = new AcsArchivalUnit(this);
    au.setConfiguration(auConfig);
    return au;
  }

  public void initPlugin(LockssDaemon daemon) {
    super.initPlugin(daemon);
    setTitleConfig(titleSpec);
  }

  public String getPluginName() {
    return PLUGIN_NAME;
  }

  public String getVersion() {
    return CURRENT_VERSION;
  }

  public List getAuConfigProperties() {
    return ListUtil.list(PD_BASE, PD_ARTICLE, PD_JKEY, PD_VOL, PD_YEAR);
  }

  public Collection getDefiningConfigKeys() {
    return ListUtil.list(AUPARAM_BASE_URL, AUPARAM_ARTICLE_URL,
                         AUPARAM_JOURNAL_KEY, AUPARAM_VOL, AUPARAM_YEAR);
  }

}