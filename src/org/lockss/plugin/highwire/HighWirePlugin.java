/*
 * $Id: HighWirePlugin.java,v 1.28 2003-11-07 04:12:00 clairegriffin Exp $
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

package org.lockss.plugin.highwire;

import java.net.*;
import java.util.*;
import gnu.regexp.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;

/**
 * This is a first cut at making a HighWire plugin
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class HighWirePlugin
    extends BasePlugin {
  public static final String LOG_NAME = "HighWirePlugin";
  public static final String PLUGIN_NAME = "HighWire Press";
  public static final String CURRENT_VERSION = "Pre-release";

  static final ConfigParamDescr PD_BASE = ConfigParamDescr.BASE_URL;
  static final ConfigParamDescr PD_VOL = ConfigParamDescr.VOLUME_NUMBER;

  // public only so test methods can use them
  public static final String AUPARAM_BASE_URL = PD_BASE.getKey();
  public static final String AUPARAM_VOL = PD_VOL.getKey();

  private static String titleSpec[][] = {
      { "Test - Shadow1", AUPARAM_BASE_URL, "http://shadow1.lockss.org/"} ,
      { "JHC", AUPARAM_BASE_URL, "http://www.jhc.org/"}
      ,
  };

  public void initPlugin(LockssDaemon daemon){
    super.initPlugin(daemon);
    configurationMap.putString(CM_NAME_KEY, PLUGIN_NAME);
    configurationMap.putString(CM_VERSION_KEY, CURRENT_VERSION);
    configurationMap.putCollection(CM_CONFIG_PROPS_KEY,
                                   ListUtil.list(PD_BASE, PD_VOL));
    configurationMap.putCollection(CM_DEFINING_CONFIG_PROPS_KEY,
                                   ListUtil.list(AUPARAM_BASE_URL, AUPARAM_VOL));
    configurationMap.setMapElement(CM_TITLE_SPEC_KEY, titleSpec);
  }

  public ArchivalUnit createAu(Configuration configInfo) throws ArchivalUnit.
      ConfigurationException {
    ArchivalUnit au = new HighWireArchivalUnit(this);
    au.setConfiguration(configInfo);
    return au;
  }
}