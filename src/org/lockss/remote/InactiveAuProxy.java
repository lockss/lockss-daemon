/*
 * $Id$
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

package org.lockss.remote;

import java.io.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * Proxy object for inactive AU, for compatible access to name and config
 * from UI code.  Throws on attempts to access info only available from a
 * running AU */
public class InactiveAuProxy extends AuProxy {
  private String auid;

  /** Create an InactiveAuProxy for the AU with the given ID.
   * @param auid the AU ID string.
   * @param remoteApi the RemoteApi service
   */
  public InactiveAuProxy(String auid, RemoteApi remoteApi) {
    super(remoteApi);
    this.auid = auid;
  }

  /** @throw UnsupportedOperationException */
  ArchivalUnit getAu() {
    throw new UnsupportedOperationException();
  }

  /**
   * Return the AU's current configuration.
   * @return a Configuration
   */
  public Configuration getConfiguration() {
    return getRemoteApi().getStoredAuConfiguration(this);
  }

  /**
   * Returns the {@link PluginProxy} for the {@link Plugin} to which this
   * AU belongs
   * @return the plugin
   */
  public PluginProxy getPlugin() {
    return getRemoteApi().findPluginProxy(getPluginId());
  }

  /**
   * Returns the plugin's ID
   * @return the plugin's ID
   */
  public String getPluginId() {
    return getRemoteApi().pluginIdFromAuId(getAuId());
  }

  /**
   * Returns the AU's ID
   * @return the AU's ID
   */
  public String getAuId() {
    return auid;
  }

  /**
   * Returns a human-readable name for the ArchivalUnit.
   * @return the AU name
   */
  public String getName() {
    Configuration config = getConfiguration();
    String name = config.get(PluginManager.AU_PARAM_DISPLAY_NAME);
    if (StringUtil.isNullString(name)) {
      return auid;
    }
    return name;
  }

  /** @return false */
  public boolean isActiveAu() {
    return false;
  }
}
