/*
 * $Id: ProjectMusePlugin.java,v 1.2 2003-08-27 00:26:42 eaalto Exp $
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

package org.lockss.plugin.projmuse;

import java.util.*;
import org.lockss.daemon.*;
import org.lockss.util.ListUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.base.BasePlugin;
import org.lockss.app.LockssDaemon;

/**
 * This is a first cut at making a Project Muse plugin
 *
 * @author  Emil Aalto
 * @version 0.0
 */

public class ProjectMusePlugin extends BasePlugin {
  private static String PLUGIN_NAME = "Project Muse Plugin";
  private static String CURRENT_VERSION = "Pre-release";

  static final ConfigParamDescr PD_BASE = ConfigParamDescr.BASE_URL;
  static final ConfigParamDescr PD_VOL = ConfigParamDescr.VOLUME_NUMBER;
  static final ConfigParamDescr PD_DIR = ConfigParamDescr.JOURNAL_DIR;
  static final ConfigParamDescr PD_ABBR = ConfigParamDescr.JOURNAL_ABBR;

  // public only so test methods can use them
  public static final String AUPARAM_BASE_URL = PD_BASE.getKey();
  public static final String AUPARAM_VOL = PD_VOL.getKey();
  public static final String AUPARAM_JOURNAL_DIR = PD_DIR.getKey();
  public static final String AUPARAM_JOURNAL_ABBR = PD_ABBR.getKey();

  private static String titleSpec[][] = {
    {"AIM", AUPARAM_JOURNAL_DIR, "american_imago", AUPARAM_JOURNAL_ABBR, "aim"}
  };

  public ArchivalUnit createAU(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    ArchivalUnit au = new ProjectMuseArchivalUnit(this);
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

  public List getAUConfigProperties() {
    return ListUtil.list(PD_BASE, PD_VOL, PD_DIR, PD_ABBR);
  }

  public Collection getDefiningConfigKeys() {
    return ListUtil.list(AUPARAM_BASE_URL, AUPARAM_VOL, AUPARAM_JOURNAL_DIR,
                         AUPARAM_JOURNAL_ABBR);
  }

}
