/*
 * $Id: HighWirePlugin.java,v 1.26 2003-09-12 01:35:51 tlipkis Exp $
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

public class HighWirePlugin extends BasePlugin {
  public static final String LOG_NAME = "HighWirePlugin";

  static final ConfigParamDescr PD_BASE = ConfigParamDescr.BASE_URL;
  static final ConfigParamDescr PD_VOL = ConfigParamDescr.VOLUME_NUMBER;

  // public only so test methods can use them
  public static final String AUPARAM_BASE_URL = PD_BASE.getKey();
  public static final String AUPARAM_VOL = PD_VOL.getKey();

  private static String titleSpec[][] = {
    {"Test - Shadow1", AUPARAM_BASE_URL, "http://shadow1.lockss.org/"},
    {"JHC", AUPARAM_BASE_URL, "http://www.jhc.org/"},
  };

  public void initPlugin(LockssDaemon daemon) {
    super.initPlugin(daemon);
    setTitleConfig(titleSpec);
  }

  public void stopPlugin() {
    super.stopPlugin();
  }

  public String getVersion() {
    return "Pre-release";
  }

  public String getPluginName() {
    return "HighWire Press";
  }

  public List getAUConfigProperties() {
    return ListUtil.list(PD_BASE, PD_VOL);
  }
  
  public Collection getDefiningConfigKeys() {
    return ListUtil.list(AUPARAM_BASE_URL, AUPARAM_VOL);
  }

  public ArchivalUnit createAU(Configuration configInfo) 
      throws ArchivalUnit.ConfigurationException {
    ArchivalUnit au = new HighWireArchivalUnit(this);
    au.setConfiguration(configInfo);
    return au;
  }
}
