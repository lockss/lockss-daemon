/*
 * $Id: HighWirePlugin.java,v 1.18 2003-03-01 01:19:16 tal Exp $
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
import org.lockss.util.*;
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
  public Map archivalUnits = null;

  // public only so test methods can use them
  public static final String BASE_URL_PROP = "base_url";
  public static final String VOL_PROP = "volume";

  public void initPlugin() {
    super.initPlugin();
    archivalUnits = new HashMap();
  }

  public void stopPlugin() {
    super.stopPlugin();
  }

  public String getVersion() {
    return "Pre-release";
  }

  public List getSupportedAUNames() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public List getAUConfigProperties() {
    return ListUtil.list(BASE_URL_PROP, VOL_PROP);
  }

  public String getAUIdFromConfig(Configuration configInfo) 
      throws ArchivalUnit.ConfigurationException {
    if (configInfo == null) {
      throw new ArchivalUnit.ConfigurationException("Null configInfo");
    }
    String urlStr = configInfo.get(BASE_URL_PROP);
    if (urlStr == null) {
      throw new
	ArchivalUnit.ConfigurationException("No configuration value for "+
					    BASE_URL_PROP);
    }
    String volStr = configInfo.get(VOL_PROP);
    if (volStr == null) {
      throw new
	ArchivalUnit.ConfigurationException("No Configuration value for "+
					    VOL_PROP);
    }

    try {
      URL url = new URL(urlStr);
      int vol = Integer.parseInt(volStr);
      return constructAUId(url, vol);
    } catch (MalformedURLException murle) {
      throw new ArchivalUnit.ConfigurationException(BASE_URL_PROP+
						    " set to a bad url "+
						    urlStr, murle);
    }
  }

  // tk - MUST canonicalize URL
  public static String constructAUId(URL url, int vol) {
    StringBuffer sb = new StringBuffer();
    sb.append(vol);
    sb.append("|");
    sb.append(url.toString());
    return sb.toString();
  }

  public static URL UrlFromAUId(String auId)
      throws MalformedURLException{
    int pos = auId.indexOf("|");
    return new URL(auId.substring(pos + 1));
  }

  public static int volumeFromAUId(String auId) {
    int pos = auId.indexOf("|");
    return Integer.parseInt(auId.substring(0, pos));
  }

  public ArchivalUnit createAU(Configuration configInfo) 
      throws ArchivalUnit.ConfigurationException {
    return new HighWireArchivalUnit(this, configInfo);
  }
}
