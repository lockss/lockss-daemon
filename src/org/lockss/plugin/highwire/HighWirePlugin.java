/*
 * $Id: HighWirePlugin.java,v 1.14 2003-02-07 01:07:07 troberts Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * This is a first cut at making a HighWire plugin
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class HighWirePlugin implements LockssPlugin {
  public static final String LOG_NAME = "HighWirePlugin";
  public Map archivalUnits = null;

  private static final String PROP_BASE="org.lockss.plugin.highwire";
  private static final String VOL_PROP=PROP_BASE+".volume";
  private static final String BASE_URL_PROP=PROP_BASE+".base_url";

  public void initPlugin() {
    archivalUnits = new HashMap();
  }

  public void stopPlugin() {
  }

  public ArchivalUnit getArchivalUnit() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public String getPluginName() {
    return "HighWire plugin";
  }

  public String getVersion() {
    return "Pre-release";
  }

  public List getSupportedAUNames() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public Properties getConfigInfo(String AUName) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public ArchivalUnit findAU(Properties configInfo) 
      throws ArchivalUnit.InstantiationException {
    if (configInfo == null) {
      throw new IllegalArgumentException("Called with null configInfo");
    }
    String urlStr = configInfo.getProperty(BASE_URL_PROP);
    if (urlStr == null) {
      throw new IllegalArgumentException("Property didn't have a value for "+
					 BASE_URL_PROP);
    }
    String volStr = configInfo.getProperty(VOL_PROP);
    if (volStr == null) {
      throw new IllegalArgumentException("Property didn't have a value for "+
					 VOL_PROP);
    }

    try {
      URL url = new URL(urlStr);
      int vol = Integer.parseInt(volStr);
      return findAU(url, vol);
    } catch (MalformedURLException murle) {
      throw new ArchivalUnit.InstantiationException(BASE_URL_PROP+
						    " set to a bad url "+
						    urlStr, murle);
    } catch (REException ree) {
      throw new ArchivalUnit.InstantiationException("Regular expression "+
						    "problem", ree);
    }
  }

  private synchronized ArchivalUnit findAU(URL url, int vol) 
      throws REException {
    String key = makeKey(url, vol);
    HighWireArchivalUnit au = (HighWireArchivalUnit)archivalUnits.get(key);
    if (au == null) {
      au = new HighWireArchivalUnit(url, vol);
      archivalUnits.put(key, au);
    }
    return au;
  }

  private String makeKey(URL url, int vol) {
    StringBuffer sb = new StringBuffer();
    sb.append(url.toString());
    sb.append("||");
    sb.append(vol);
    return sb.toString();
  }
  
}
