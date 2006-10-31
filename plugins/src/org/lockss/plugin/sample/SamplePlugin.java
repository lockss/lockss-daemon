/*
 * $Id: SamplePlugin.java,v 1.3 2006-10-31 07:01:07 thib_gc Exp $
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
package org.lockss.plugin.sample;

import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;
import org.lockss.util.*;

/**
 * <p>SamplePlugin: A template for a plugin that requires a base url and the volume
 * number to configure the plugin.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class SamplePlugin extends DefinablePlugin {
  private static String PLUGIN_NAME = "Sample";
  private static String CURRENT_VERSION = "Pre-release";

  static final ConfigParamDescr PD_BASE = ConfigParamDescr.BASE_URL;
  static final ConfigParamDescr PD_VOL = ConfigParamDescr.VOLUME_NUMBER;

  // public only so test methods can use them
  public static final String AUPARAM_BASE_URL = PD_BASE.getKey();
  public static final String AUPARAM_VOL = PD_VOL.getKey();

  public void initPlugin(LockssDaemon daemon){
    //we override initPlugin largely to manually load the values that
    // should be put into the configuration map when we load it from disk
    definitionMap.putString(KEY_PLUGIN_NAME, PLUGIN_NAME);
    definitionMap.putString(KEY_PLUGIN_VERSION, CURRENT_VERSION);
    definitionMap.putCollection(KEY_PLUGIN_CONFIG_PROPS,
                                   ListUtil.list(PD_BASE, PD_VOL));
    // then call the overridden initializaton.
    super.initPlugin(daemon);
  }

  public ArchivalUnit createAu0(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    // create a new archival unit
    ArchivalUnit au = new SampleArchivalUnit(this, definitionMap);
    // then configure it
    au.setConfiguration(auConfig);
    return au;
  }
}
