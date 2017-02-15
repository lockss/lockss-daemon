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
package org.lockss.ws.status;

import java.util.Map;
import org.lockss.app.LockssDaemon;
import org.lockss.config.TdbAu;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.ws.entities.TdbAuWsResult;
import org.lockss.ws.entities.TdbPublisherWsResult;
import org.lockss.ws.entities.TdbTitleWsResult;

/**
 * Container for the information that is used as the source for a query related
 * to title database archival units.
 */
public class TdbAuWsSource extends TdbAuWsResult {
  private TdbAu.Id tdbAuId;
  private TdbAu tdbAu = null;

  private boolean auIdPopulated = false;
  private boolean namePopulated = false;
  private boolean pluginNamePopulated = false;
  private boolean tdbTitlePopulated = false;
  private boolean tdbPublisherPopulated = false;
  private boolean downPopulated = false;
  private boolean activePopulated = false;
  private boolean paramsPopulated = false;
  private boolean attrsPopulated = false;
  private boolean propsPopulated = false;

  private LockssDaemon theDaemon = null;
  private PluginManager pluginMgr = null;
  private Plugin plugin = null;

  public TdbAuWsSource(TdbAu.Id tdbAuId) {
    this.tdbAuId = tdbAuId;
  }

  @Override
  public String getAuId() {
    if (!auIdPopulated) {
      try {
	setAuId(getTdbAu().getAuId(getPluginManager()));
      } catch (IllegalStateException ise) {
	// Do nothing.
      }

      auIdPopulated = true;
    }

    return super.getAuId();
  }

  @Override
  public String getName() {
    if (!namePopulated) {
      setName(getTdbAu().getName());
      namePopulated = true;
    }

    return super.getName();
  }

  @Override
  public String getPluginName() {
    if (!pluginNamePopulated) {
      if (getPlugin() != null) {
	setPluginName(plugin.getPluginId());
      }

      pluginNamePopulated = true;
    }

    return super.getPluginName();
  }

  @Override
  public TdbTitleWsResult getTdbTitle() {
    if (!tdbTitlePopulated) {
      setTdbTitle(new TdbTitleWsSource(getTdbAu().getTdbTitle()));
      tdbTitlePopulated = true;
    }

    return super.getTdbTitle();
  }

  @Override
  public TdbPublisherWsResult getTdbPublisher() {
    if (!tdbPublisherPopulated) {
      setTdbPublisher(new TdbPublisherWsSource(getTdbAu().getTdbPublisher()));
      tdbPublisherPopulated = true;
    }

    return super.getTdbPublisher();
  }

  @Override
  public Boolean getDown() {
    if (!downPopulated) {
      setDown(getTdbAu().isDown());
      downPopulated = true;
    }

    return super.getDown();
  }

  @Override
  public Boolean getActive() {
    if (!activePopulated) {
      String auId = getAuId();

      if (auId == null) {
	setActive(false);
      } else {
	setActive(null != getPluginManager().getAuFromId(auId));
      }

      activePopulated = true;
    }

    return super.getActive();
  }

  @Override
  public Map<String, String> getParams() {
    if (!paramsPopulated) {
      setParams(getTdbAu().getParams());
      paramsPopulated = true;
    }

    return super.getParams();
  }

  @Override
  public Map<String, String> getAttrs() {
    if (!attrsPopulated) {
      setAttrs(getTdbAu().getAttrs());
      attrsPopulated = true;
    }

    return super.getAttrs();
  }

  @Override
  public Map<String, String> getProps() {
    if (!propsPopulated) {
      setProps(getTdbAu().getProperties());
      propsPopulated = true;
    }

    return super.getProps();
  }

  /**
   * Provides the title database archival unit, initializing it if necessary.
   * 
   * @return a TdbAu with the title database archival unit.
   */
  private TdbAu getTdbAu() {
    if (tdbAu == null) {
      tdbAu = tdbAuId.getTdbAu();
    }

    return tdbAu;
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

  /**
   * Provides the plugin manager, initializing it if necessary.
   * 
   * @return a PluginManager with the node manager.
   */
  private PluginManager getPluginManager() {
    if (pluginMgr == null) {
      pluginMgr = getTheDaemon().getPluginManager();
    }

    return pluginMgr;
  }

  /**
   * Provides the plugin, initializing it if necessary.
   * 
   * @return a Plugin with the plugin.
   */
  private Plugin getPlugin() {
    if (plugin == null) {
      plugin = getTdbAu().getPlugin(getPluginManager());
    }

    return plugin;
  }
}
