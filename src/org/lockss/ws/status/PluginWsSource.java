/*
 * $Id$
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * Container for the information that is used as the source for a query related
 * to plugins.
 */
package org.lockss.ws.status;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.lockss.app.LockssDaemon;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.util.ExternalizableMap;
import org.lockss.ws.entities.PluginWsResult;

public class PluginWsSource extends PluginWsResult {
  private Plugin plugin;

  private boolean pluginIdPopulated = false;
  private boolean namePopulated = false;
  private boolean versionPopulated = false;
  private boolean typePopulated = false;
  private boolean definitionPopulated = false;
  private boolean registryPopulated = false;
  private boolean urlPopulated = false;
  private boolean auCountPopulated = false;
  private boolean publishingPlatformPopulated = false;

  private LockssDaemon theDaemon = null;
  private PluginManager pluginMgr = null;
  private PluginManager.PluginInfo pluginInfo = null;

  public PluginWsSource(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public String getPluginId() {
    if (!pluginIdPopulated) {
      setPluginId(plugin.getPluginId());
      pluginIdPopulated = true;
    }

    return super.getPluginId();
  }

  @Override
  public String getName() {
    if (!namePopulated) {
      setName(plugin.getPluginName());
      namePopulated = true;
    }

    return super.getName();
  }

  @Override
  public String getVersion() {
    if (!versionPopulated) {
      setVersion(plugin.getVersion());
      versionPopulated = true;
    }

    return super.getVersion();
  }

  @Override
  public String getType() {
    if (!typePopulated) {
      setType(getPluginManager().getPluginType(plugin));
      typePopulated = true;
    }

    return super.getType();
  }

  @Override
  public Map<String, String> getDefinition() {
    if (!definitionPopulated) {
      if (plugin instanceof DefinablePlugin) {
        ExternalizableMap eMap = ((DefinablePlugin)plugin).getDefinitionMap();

        if (eMap.size() > 0) {
          Map<String, String> result = new HashMap<String, String>();

          for (Map.Entry<String, Object> entry : eMap.entrySet()) {
            result.put((String)entry.getKey(), entry.getValue().toString());
          }

          setDefinition(result);
        }
      }

      definitionPopulated = true;
    }

    return super.getDefinition();
  }

  @Override
  public String getRegistry() {
    if (!registryPopulated) {
      if (getPluginInfo() != null) {
	ArchivalUnit au = pluginInfo.getRegistryAu();

	if (au != null) {
	  setRegistry(au.getName());
	}
      }

      registryPopulated = true;
    }

    return super.getRegistry();
  }

  @Override
  public String getUrl() {
    if (!urlPopulated) {
      if (getPluginInfo() != null) {
	String infoCuUrl = pluginInfo.getCuUrl();

	if (infoCuUrl != null) {
	  setUrl(infoCuUrl);
	}
      }

      urlPopulated = true;
    }

    return super.getUrl();
  }

  @Override
  public Integer getAuCount() {
    if (!auCountPopulated) {
      Collection<ArchivalUnit> allAus = plugin.getAllAus();

      if (allAus != null) {
	setAuCount(allAus.size());
      }

      auCountPopulated = true;
    }

    return super.getAuCount();
  }

  @Override
  public String getPublishingPlatform() {
    if (!publishingPlatformPopulated) {
      setPublishingPlatform(plugin.getPublishingPlatform());
      publishingPlatformPopulated = true;
    }

    return super.getPublishingPlatform();
  }

  /**
   * Provides the plugin info, initializing it if necessary.
   * 
   * @return a PluginManager.PluginInfo with the plugin info.
   */
  private PluginManager.PluginInfo getPluginInfo() {
    if (pluginInfo == null) {
      if (getPluginManager().isLoadablePlugin(plugin)) {
	pluginInfo = pluginMgr.getLoadablePluginInfo(plugin);
      }
    }

    return pluginInfo;
  }

  /**
   * Provides the plugin manager, initializing it if necessary.
   * 
   * @return a PluginManager with the plugin manager.
   */
  private PluginManager getPluginManager() {
    if (pluginMgr == null) {
      pluginMgr = getTheDaemon().getPluginManager();
    }

    return pluginMgr;
  }

  /**
   * Provides the daemon, initializing it if necessary.
   * 
   * @return a LockssDaemon with the daemon.
   */
  private LockssDaemon getTheDaemon() {
    if (theDaemon == null) {
      theDaemon = LockssDaemon.getLockssDaemon();
    }

    return theDaemon;
  }
}
