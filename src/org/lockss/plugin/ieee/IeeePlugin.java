/*
 * $Id: IeeePlugin.java,v 1.8 2004-01-27 04:07:08 tlipkis Exp $
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
package org.lockss.plugin.ieee;

import org.lockss.plugin.base.BasePlugin;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.util.*;

import java.util.List;
import java.util.Collection;
import org.lockss.app.*;
import org.lockss.plugin.configurable.*;

/**
 * IeeePlugin: Plugin class for the Ieee Explore Plugin
 * @author Claire Griffin
 * @version 1.0
 */

public class IeeePlugin extends ConfigurablePlugin {
  static final ConfigParamDescr PU_NUMBER = new ConfigParamDescr();
  static {
    PU_NUMBER.setKey("Pu_Number");
    PU_NUMBER.setDisplayName("Publication Number");
    PU_NUMBER.setType(ConfigParamDescr.TYPE_INT);
    PU_NUMBER.setSize(10);
    PU_NUMBER.setDescription("IEEE publication Number(e.g. '2').");
  }

  private static String PLUGIN_NAME = "IEEE";
  private static String CURRENT_VERSION= "Pre-release";

  static final ConfigParamDescr PD_BASE = ConfigParamDescr.BASE_URL;
  static final ConfigParamDescr PD_VOL = ConfigParamDescr.VOLUME_NUMBER;
  static final ConfigParamDescr PD_YEAR = ConfigParamDescr.YEAR;
  static final ConfigParamDescr PD_PUNUM = PU_NUMBER;


  // public only so test methods can use them
  public static final String AUPARAM_BASE_URL = PD_BASE.getKey();
  public static final String AUPARAM_VOL = PD_VOL.getKey();
  public static final String AUPARAM_PUNUM = PD_PUNUM.getKey();
  public static final String AUPARAM_YEAR = PD_YEAR.getKey();

  public void initPlugin(LockssDaemon daemon){
    //todo: we override initPlugin largely to manually load the values that
    // should be put into the configuration map when we load it from disk
    configurationMap.putString(CM_NAME_KEY, PLUGIN_NAME);
    configurationMap.putString(CM_VERSION_KEY, CURRENT_VERSION);
    configurationMap.putCollection(CM_CONFIG_PROPS_KEY,
                                   ListUtil.list(PD_BASE, PD_PUNUM, PD_YEAR));
    // then call the overridden initializaton.
    super.initPlugin(daemon);
  }

  public ArchivalUnit createAu(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    ArchivalUnit au = new IeeeArchivalUnit(this);
    au.setConfiguration(auConfig);
    return au;
  }

}
