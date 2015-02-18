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
 * Proxy object for remote access to ArchivalUnit.  Subset of ArchivalUnit
 * interface in which all {@link Plugin}s and {@link ArchivalUnit}s are
 * replaced with {@link PluginProxy}s and {@link AuProxy}s.
 */
public class AuProxy {
  private RemoteApi remoteApi;
  transient ArchivalUnit au;

  AuProxy(ArchivalUnit au, RemoteApi remoteApi) {
    this.remoteApi = remoteApi;
    this.au = au;
  }

  AuProxy(RemoteApi remoteApi) {
    this.remoteApi = remoteApi;
  }

  /** Create an AuProxy for the AU with the given ID.
   * @param auid the AU ID string.
   * @param remoteApi the RemoteApi service
   * @throws NoSuchAU if no AU with the given ID exists
   */
  public AuProxy(String auid, RemoteApi remoteApi)
      throws NoSuchAU {
    au = remoteApi.getAuFromId(auid);
    if (au == null) {
      throw new NoSuchAU(auid);
    }
    this.remoteApi = remoteApi;
  }

  ArchivalUnit getAu() {
    return au;
  }

  /**
   * Return the AU's current configuration.
   * @return a Configuration
   */
  public Configuration getConfiguration() {
    return au.getConfiguration();
  }

  /**
   * Return the AU's TitleConfig, if any.
   * @return a TitleConfig, or null
   */
  public TitleConfig getTitleConfig() {
    return au.getTitleConfig();
  }

  /**
   * Returns the {@link PluginProxy} for the {@link Plugin} to which this
   * AU belongs
   * @return the plugin
   */
  public PluginProxy getPlugin() {
    return remoteApi.findPluginProxy(au.getPlugin());
  }

  /**
   * Returns a unique string identifier for the Plugin.
   * @return a unique id
   */
  public String getPluginId() {
    return au.getPluginId();
  }

  /**
   * Returns a globally unique string identifier for the ArchivalUnit.
   * @return a unique id
   */
  public String getAuId() {
    return au.getAuId();
  }

  /**
   * Returns a human-readable name for the ArchivalUnit.
   * @return the AU name
   */
  public String getName() {
    return au.getName();
  }

  public static class NoSuchAU extends Exception {
    public NoSuchAU(String msg) {
      super(msg);
    }
  }

  protected RemoteApi getRemoteApi() {
    return remoteApi;
  }

  public boolean isActiveAu() {
    return true;
  }
}
