/*
 * $Id: PollSpec.java,v 1.4 2003-03-15 03:24:05 claire Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller;

import java.io.*;
import java.net.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.app.*;

public class PollSpec {
  private String auId;
  private String pluginId;
  private String url;
  private String uprBound = null;
  private String lwrBound = null;
  private CachedUrlSet cus = null;
  private static PluginManager pluginMgr = null;

  /**
   * constructor for a "mock" poll spec
   * @param auId the archival unit id
   * @param pluginId the plugin id
   * @param url the url
   * @param lwrBound the lower bound of the url
   * @param uprBound the upper bound of the url
   * @param cus the cached url set
   */
  public PollSpec(String pluginId, String auId, String url,
                  String lwrBound, String uprBound, CachedUrlSet cus) {
    this.auId = auId;
    this.pluginId = pluginId;
    this.url = url;
    this.uprBound = uprBound;
    this.lwrBound = lwrBound;
    this.cus = cus;
  }

  /**
   * construct a pollspec from an existing pollspec but change the
   * upper and lower boundary of the RangedCachedUrlSetSpec
   * @param cus the existing cached url set
   * @param lwrBound the new lower boundary
   * @param uprBound the new upper boundary
   */
  public PollSpec(CachedUrlSet cus, String lwrBound, String uprBound) {
    this.cus = cus;
    ArchivalUnit au = cus.getArchivalUnit();
    auId = au.getAUId();
    pluginId = au.getPluginId();
    CachedUrlSetSpec cuss = cus.getSpec();
    url = cuss.getUrl();
    this.lwrBound = lwrBound;
    this.uprBound = uprBound;
  }

  /**
   * Construct a PollSpec from a CachedUrlSet
   * @param cus the CachedUrlSpec which defines the range of interest
   */
  public PollSpec(CachedUrlSet cus) {
    this.cus = cus;
    ArchivalUnit au = cus.getArchivalUnit();
    auId = au.getAUId();
    pluginId = au.getPluginId();
    CachedUrlSetSpec cuss = cus.getSpec();
    url = cuss.getUrl();
    if (cuss instanceof RangeCachedUrlSetSpec) {
      RangeCachedUrlSetSpec rcuss = (RangeCachedUrlSetSpec)cuss;
      lwrBound = rcuss.getLowerBound();
      uprBound = rcuss.getUpperBound();
    }
  }

  /**
   * Construct a PollSpec from a LcapMessage
   * @param msg the LcapMessage which defines the range of interest
   */
  public PollSpec(LcapMessage msg) {
    auId = msg.getArchivalID();
    pluginId = msg.getPluginID();
    url = msg.getTargetUrl();
    uprBound = msg.getUprBound();
    lwrBound = msg.getLwrBound();
    cus = getPluginManager().findCachedUrlSet(this);
  }

  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  public String getPluginId() {
    return pluginId;
  }

  public String getAUId() {
    return auId;
  }

  public String getUrl() {
    return url;
  }

  public String getLwrBound() {
    return lwrBound;
  }

  public String getUprBound() {
    return uprBound;
  }

  public String getRangeString() {
    if(lwrBound != null || uprBound != null) {
      return lwrBound + " - " + uprBound;
    }
    return null;
  }

  private PluginManager getPluginManager() {
    if(pluginMgr == null) {
      pluginMgr = (PluginManager)LockssDaemon.getManager(
          LockssDaemon.PLUGIN_MANAGER);
    }
    return pluginMgr;
  }

  public String toString() {
    return "[PS: pid=" + pluginId + ", auid=" + auId + ", url=" + url
      + ", l=" + lwrBound + ", u=" + uprBound + "]";
  }
}


