/*
 * $Id: TitleConfig.java,v 1.14 2006-04-07 21:44:35 thib_gc Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.util.*;

import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.plugin.*;

/**
 * An entry in the title database, specifying a title name, plugin and list
 * of {@link ConfigParamAssignment}s.
 */
public class TitleConfig {
  private String displayName;
  private String journalTitle;
  private String pluginName;
  private String pluginVersion = null;
  private long estSize = 0;
  private List params = null;
  private Map attrs = null;

  /**
   * Create a TitleConfig associating a title with a plugin.
   * @param displayName the title string
   * @param plugin the plugin that handles the title.
   */
  public TitleConfig(String displayName, Plugin plugin) {
    this(displayName, plugin.getPluginId());
  }

  /**
   * Create a TitleConfig associating a title with a plugin name.
   * @param displayName the title string
   * @param pluginName the name of the plugin that handles the title
   */
  public TitleConfig(String displayName, String pluginName) {
    this.pluginName = pluginName;
    this.displayName = displayName;
  }

  /**
   * Set the required plugin version
   * @param pluginVersion the plugin version
   */
  public void setPluginVersion(String pluginVersion) {
    this.pluginVersion = pluginVersion;
  }

  /**
   * Set the journal title string.  This must be the same across all
   * instances (volumes) of the same journal.
   * @param journalTitle the journal title
   */
  public void setJournalTitle(String journalTitle) {
    this.journalTitle = journalTitle;
  }

  /**
   * Set the parameter value list
   * @param params List of {@link ConfigParamAssignment}s
   */
  public void setParams(List params) {
    this.params = params;
  }

  /**
   * @return the parameter assignments
   */
  public List getParams() {
    return params;
  }

  /**
   * Set the attributes
   */
  public void setAttributes(Map attrs) {
    this.attrs = attrs;
  }

  /**
   * @return the attributes
   */
  public Map getAttributes() {
    return attrs;
  }

  /**
   * Return the title string
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Return the journal title string.  This must be the same across all
   * instances (volumes) of the same journal
   */
  public String getJournalTitle() {
    return journalTitle;
  }

  /**
   * Return the plugin name
   */
  public String getPluginName() {
    return pluginName;
  }

  /**
   * Return the minimum plugin version
   */
  public String getPluginVersion() {
    return pluginVersion;
  }

  private String auid = null;

  public String getAuId(PluginManager pluginMgr) {
    if (auid != null) {
      return auid;
    }
    Plugin plugin =
      pluginMgr.getPlugin(PluginManager.pluginKeyFromId(getPluginName()));
    return getAuId(pluginMgr, plugin);
  }

  public String getAuId(PluginManager pluginMgr, Plugin plugin) {
    if (auid == null) {
      if (plugin == null) {
	throw new RuntimeException("No such plugin " + getPluginName());
      }
      auid = PluginManager.generateAuId(plugin, getConfig());
    }
    return auid;
  }

  /**
   * Set the estimated size
   * @param size estimated size in bytes
   */
  public void setEstimatedSize(long size) {
    this.estSize = size;
  }

  /**
   * @return the estimated size
   */
  public long getEstimatedSize() {
    return estSize;
  }

  /** Temporary until clients fixed */
  public Configuration getConfig() {
    if (params == null) {
      return ConfigManager.EMPTY_CONFIGURATION;
    }
    Configuration config = ConfigManager.newConfiguration();
    for (Iterator iter = params.iterator(); iter.hasNext(); ) {
      ConfigParamAssignment cpa = (ConfigParamAssignment)iter.next();
      ConfigParamDescr cpd = cpa.getParamDescr();
      if (!cpd.isDefaultOnly()) {
	config.put(cpd.getKey(), cpa.getValue());
      }
    }
    return config;
  }

  // AuConfig hasn't been fully converted to use TitleConfig.  These
  // methods provide the info it needs to do things mostly the old way

  public Collection getUnEditableKeys() {
    if (params == null) {
      return Collections.EMPTY_LIST;
    }
    List res = new ArrayList();
    for (Iterator iter = params.iterator(); iter.hasNext(); ) {
      ConfigParamAssignment cpa = (ConfigParamAssignment)iter.next();
      ConfigParamDescr cpd = cpa.getParamDescr();
      if (!cpa.isEditable()) {
	res.add(cpd.getKey());
      }
    }
    return res;
  }

  /** Return true if the supplied config is consistent with this title,
   * and there are no editable definitional params.
   * @param config an AU config tree
   */
  public boolean matchesConfig(Configuration config) {
    if (params == null || config == null) {
      return false;
    }
    for (Iterator iter = params.iterator(); iter.hasNext(); ) {
      ConfigParamAssignment cpa = (ConfigParamAssignment)iter.next();
      ConfigParamDescr cpd = cpa.getParamDescr();
      if (cpd.isDefinitional()) {
	if (cpa.isEditable() ||
	    !StringUtil.equalStrings(cpa.getValue(),
				     config.get(cpd.getKey()))) {
	  return false;
	}
      }
    }
    return true;
  }

  /** Return true if this titleConfig completely defines an Au (<i>ie</i>,
   * all definitional parameters have non editable values).
   * @param plugin
   */
  public boolean isSingleAu(Plugin plugin) {
    if (params == null) {
      return false;
    }
    for (Iterator iter = plugin.getAuConfigDescrs().iterator();
	 iter.hasNext(); ) {
      ConfigParamDescr reqd = (ConfigParamDescr)iter.next();
      if (reqd.isDefinitional()) {
	if (!assignsDescr(reqd)) {
	  return false;
	}
      }
    }
    return true;
  }

  public ConfigParamAssignment findCpa(ConfigParamDescr descr) {
    for (Iterator iter = params.iterator(); iter.hasNext(); ) {
      ConfigParamAssignment cpa = (ConfigParamAssignment)iter.next();
      ConfigParamDescr cpd = cpa.getParamDescr();
      if (descr.equals(cpd)) {
	return cpa;
      }
    }
    return null;
  }

  private boolean assignsDescr(ConfigParamDescr descr) {
    ConfigParamAssignment cpa = findCpa(descr);
    if (cpa != null) {
      return !cpa.isEditable();
    }
    return false;
  }

  /** Generate Properties that will result in this TitleConfig when loaded
   * by BasePlugin */
  public Properties toProperties(String propAbbr) {
    String pre = "org.lockss.title." + propAbbr + ".";
    Properties p = new OrderedProperties();
    p.put(pre+"title", getDisplayName());
    p.put(pre+"plugin", getPluginName());
    if (pluginVersion != null) {
      p.put(pre+"pluginVersion", getPluginVersion());
    }
    if (journalTitle != null) {
      p.put(pre+"journalTitle", getJournalTitle());
    }
    if (params != null) {
      for (int ix = 0; ix < params.size(); ix++) {
	ConfigParamAssignment cpa = (ConfigParamAssignment)params.get(ix);
	String ppre = pre + "param." + (ix+1) + ".";
	p.put(ppre + "key", cpa.getParamDescr().getKey());
	p.put(ppre + "value", cpa.getValue());
      }
    }
    return p;
  }

  public boolean equals(Object obj) {
    if (! (obj instanceof TitleConfig)) {
      return false;
    }
    TitleConfig o = (TitleConfig)obj;
    return
      StringUtil.equalStrings(displayName, o.getDisplayName()) &&
      StringUtil.equalStrings(journalTitle, o.getJournalTitle()) &&
      StringUtil.equalStrings(pluginName, o.getPluginName()) &&
      StringUtil.equalStrings(pluginVersion, o.getPluginVersion()) &&
      estSize == o.getEstimatedSize() &&
      // params is order-independent, can't call List.equals()
      (params == null ?
       o.getParams() == null : (params.size() == o.getParams().size() &&
				params.containsAll(o.getParams())));
  }

  public int hashCode() {
    int hash = 0x6620704;
    if (displayName != null) hash += displayName.hashCode();
    if (journalTitle != null) hash += journalTitle.hashCode();
    if (pluginName != null) hash += pluginName.hashCode();
    if (pluginVersion != null) hash += pluginVersion.hashCode();
    hash += getEstimatedSize();
    // params is order-independent, can't call List.hashCode()
    if (params != null) {
      for (Iterator iter = params.iterator(); iter.hasNext(); ) {
	Object obj = iter.next();
	if (obj != null)
	  hash += obj.hashCode();
      }
    }
    return hash;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(40);
    sb.append("[Title: ");
    sb.append(displayName);
    sb.append(", journal: ");
    sb.append(journalTitle);
    sb.append(", plugin: ");
    sb.append(pluginName);
    sb.append(", params: ");
    sb.append(params);
    if (attrs != null) {
      sb.append(", attrs: ");
      sb.append(attrs);
    }
    sb.append("]");
    return sb.toString();
  }


}
