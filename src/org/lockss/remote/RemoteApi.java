/*
 * $Id: RemoteApi.java,v 1.2 2004-01-08 22:43:48 tlipkis Exp $
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
n
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
import org.apache.commons.collections.ReferenceMap;

/**
 * API for use by UIs and other remote agents.  Provides access to a
 * variety of daemon status and services using only datastructure-like
 * classes that are easy to serialize.
 */
public class RemoteApi extends BaseLockssManager {
  private static Logger log = Logger.getLogger("RemoteApi");

  private PluginManager pluginMgr;

  // cache for proxy objects
  private ReferenceMap auProxies = new ReferenceMap(ReferenceMap.WEAK,
						    ReferenceMap.WEAK);
  private ReferenceMap pluginProxies = new ReferenceMap(ReferenceMap.WEAK,
							ReferenceMap.WEAK);

  public RemoteApi() {
  }

  public void startService() {
    super.startService();
    pluginMgr = getDaemon().getPluginManager();
  }

  /** No config */
  protected void setConfig(Configuration config, Configuration oldConfig,
			   Set changedKeys) {
  }

  /** Create or return an AuProxy for the AU corresponding to the auid.
   * @param auid the auid
   * @return an AuProxy for the AU, or null if no AU exists with the given
   * id.
   */
  public AuProxy findAuProxy(String auid) {
    return findAuProxy(getAuFromId(auid));
  }

  /** Create or return an AuProxy for the AU
   * @param au the AU
   * @return an AuProxy for the AU, or null if the au is null
   */
  synchronized AuProxy findAuProxy(ArchivalUnit au) {
    if (au == null) {
      return null;
    }
    AuProxy aup = (AuProxy)auProxies.get(au);
    if (aup == null || aup.getAu() != au) {
      aup = new AuProxy(au, this);
      auProxies.put(au, aup);
    }
    return aup;
  }

  public synchronized InactiveAuProxy findInactiveAuProxy(String auid) {
    return new InactiveAuProxy(auid, this);
  }

  /** Create or return a PluginProxy for the Plugin corresponding to the id.
   * @param pluginid the plugin id
   * @return a PluginProxy for the Plugin, or null if no Plugin exists with
   * the given id.
   */
  public PluginProxy findPluginProxy(String pluginid) {
    return findPluginProxy(getPluginFromId(pluginid));
  }

  /** Create or return  PluginProxy for the Plugin
   * @param plugin the Plugin
   * @return an PluginProxy for the Plugin, or null if the plugin is null
   */
  synchronized PluginProxy findPluginProxy(Plugin plugin) {
    if (plugin == null) {
      return null;
    }
    PluginProxy pluginp = (PluginProxy)pluginProxies.get(plugin);
    if (pluginp == null || pluginp.getPlugin() != plugin) {
      pluginp = new PluginProxy(plugin, this);
      pluginProxies.put(plugin, pluginp);
    }
    return pluginp;
  }

  /** Find or create an AuProxy for each au in the collection */
  List mapAusToProxies(Collection aus) {
    List res = new ArrayList();
    for (Iterator iter = aus.iterator(); iter.hasNext(); ) {
      AuProxy aup = findAuProxy((ArchivalUnit)iter.next());
      res.add(aup);
    }
    return res;
  }

  /** Find or create a PluginProxy for each Plugin in the collection */
  List mapPluginsToProxies(Collection plugins) {
    List res = new ArrayList();
    for (Iterator iter = plugins.iterator(); iter.hasNext(); ) {
      PluginProxy pluginp = findPluginProxy((Plugin)iter.next());
      res.add(pluginp);
    }
    return res;
  }

  // Forward useful PluginManager methods, translating between real objects
  // and proxies as appropriate.

  /**
   * Convert plugin id to key suitable for property file.  Plugin id is
   * currently the same as plugin class name, but that may change.
   * @param id the plugin id
   * @return String the plugin key
   */
  public static String pluginKeyFromId(String id) {
    return PluginManager.pluginKeyFromId(id);
  }

  /**
   * Reconfigure an AU and save the new configuration in the local config
   * file.
   * @param aup the AuProxy
   * @param auConf the new AU configuration, using simple prop keys (not
   * prefixed with org.lockss.au.<i>auid</i>)
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public void setAndSaveAuConfiguration(AuProxy aup,
					Configuration auConf)
      throws ArchivalUnit.ConfigurationException, IOException {
    ArchivalUnit au = aup.getAu();
    pluginMgr.setAndSaveAuConfiguration(au, auConf);
  }

  /**
   * Create an AU and save its configuration in the local config
   * file.
   * @param pluginp the PluginProxy in which to create the AU
   * @param auConf the new AU configuration, using simple prop keys (not
   * prefixed with org.lockss.au.<i>auid</i>)
   * @return the new AuProxy
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public AuProxy createAndSaveAuConfiguration(PluginProxy pluginp,
					     Configuration auConf)
      throws ArchivalUnit.ConfigurationException, IOException {
    Plugin plugin = pluginp.getPlugin();
    ArchivalUnit au = pluginMgr.createAndSaveAuConfiguration(plugin, auConf);
    return findAuProxy(au);
  }

  /**
   * Delete AU configuration from the local config file.
   * @param aup the AuProxy
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public void deleteAuConfiguration(AuProxy aup)
      throws ArchivalUnit.ConfigurationException, IOException {
    ArchivalUnit au = aup.getAu();
    pluginMgr.deleteAuConfiguration(au);
  }

  /**
   * Deactivate an AU
   * @param aup the AuProxy
   * @throws ArchivalUnit.ConfigurationException
   * @throws IOException
   */
  public void deactivateAuConfiguration(AuProxy aup)
      throws ArchivalUnit.ConfigurationException, IOException {
    ArchivalUnit au = aup.getAu();
    pluginMgr.deactivateAuConfiguration(au);
  }

  /**
   * Return the stored config info for an AU (from config file, not from
   * AU instance).
   * @param auid the id of the AU to be deactivated
   * @return the AU's Configuration, with unprefixed keys.
   */
  public Configuration getStoredAuConfiguration(AuProxy aup) {
    return pluginMgr.getStoredAuConfiguration(aup.getAuId());
  }

  /**
   * Return a list of AuProxies for all configured ArchivalUnits.
   * @return the List of AuProxies
   */
  public List getAllAus() {
    return mapAusToProxies(pluginMgr.getAllAus());
  }

  public List getInactiveAus() {
    Collection inactiveAuIds = pluginMgr.getInactiveAuIds();
    if (inactiveAuIds == null || inactiveAuIds.isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    List res = new ArrayList();
    for (Iterator iter = inactiveAuIds.iterator(); iter.hasNext(); ) {
      String auid = (String)iter.next();
      res.add(new InactiveAuProxy(auid, this));
    }
    return res;
  }

  /** Return all the known titles from the title db */
  public Collection findAllTitles() {
    return pluginMgr.findAllTitles();
  }

  /** Find all the plugins that support the given title */
  public Collection getTitlePlugins(String title) {
    return mapPluginsToProxies(pluginMgr.getTitlePlugins(title));
  }

  /** @return Collection of PluginProxies for all plugins that have been
   * registered.  <i>Ie</i>, that are either listed in
   * org.lockss.plugin.registry, or were loaded by a configured AU */
  public Collection getRegisteredPlugins() {
    return mapPluginsToProxies(pluginMgr.getRegisteredPlugins());
  }

  ArchivalUnit getAuFromId(String auid) {
    return pluginMgr.getAuFromId(auid);
  }

  Plugin getPluginFromId(String pluginid) {
    return pluginMgr.getPlugin(pluginKeyFromId(pluginid));
  }

  String pluginIdFromAuId(String auid) {
    return pluginMgr.pluginIdFromAuId(auid);
  }
}
