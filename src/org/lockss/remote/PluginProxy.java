/*
 * $Id: PluginProxy.java,v 1.5 2007-08-12 04:53:30 tlipkis Exp $
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
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * Proxy object for remote access to Plugin.  Subset of Plugin interface in
 * which all {@link Plugin}s and {@link ArchivalUnit}s are replaced with
 * {@link PluginProxy}s and {@link AuProxy}s.
 */
public class PluginProxy {
  private RemoteApi remoteApi;
  transient Plugin plugin;

  PluginProxy(Plugin plugin, RemoteApi remoteApi) {
    this.plugin = plugin;
    this.remoteApi = remoteApi;
  }

  /** Create a PluginProxy for the plugin with the given ID.
   * @param pluginId the plugin ID string.
   * @param remoteApi the RemoteApi service
   * @throws NoSuchPlugin if no Plugin with the given ID exists
   */
  public PluginProxy(String pluginId, RemoteApi remoteApi)
      throws NoSuchPlugin {
    plugin = remoteApi.getPluginFromId(pluginId);
    if (plugin == null) {
      throw new NoSuchPlugin(pluginId);
    }
    this.remoteApi = remoteApi;
  }

  Plugin getPlugin() {
    return plugin;
  }

  /**
   * Return a string that uniquely represents the identity of this plugin
   * @return a string that identifies this plugin
   */
  public String getPluginId() {
    return plugin.getPluginId();
  }

  /**
   * Return a string that represents the current version of this plugin
   * @return a String representing the current version
   */
  public String getVersion() {
    return plugin.getVersion();
  }

  /**
   * Return the humad-readable name of the plugin
   * @return the name
   */
  public String getPluginName() {
    return plugin.getPluginName();
  }

  public String getPublishingPlatform() {
    return plugin.getPublishingPlatform();
  }

  /**
   * Return the list of names of the {@link ArchivalUnit}s and volranges
   * supported by this plugin.
   * @return a List of Strings
   */
  public List getSupportedTitles() {
    return plugin.getSupportedTitles();
  }

  /**
   * Return the (possibly incomplete) parameter assignments
   * that will configure this plugin for the specified title.
   * @param title journal title, as returned by getSupportedTitles().
   * @return the {@link TitleConfig} for the title
   */
  public TitleConfig getTitleConfig(String title) {
    return plugin.getTitleConfig(title);
  }

  /**
   * Return a list of descriptors for configuration parameters required to
   * configure an archival unit for this plugin.
   * @return a List of {@link ConfigParamDescr}s
   */
  public List getAuConfigDescrs() {
    return plugin.getAuConfigDescrs();
  }

  /**
   * Return a collection of {@link AuProxy}s for the {@link ArchivalUnit}s
   * that exist within this plugin.
   * @return a Collection of {@link AuProxy}s
   */
  public Collection getAllAus() {
    return remoteApi.mapAusToProxies(plugin.getAllAus());
  }

  public static class NoSuchPlugin extends Exception {
    public NoSuchPlugin(String msg) {
      super(msg);
    }
  }
}
