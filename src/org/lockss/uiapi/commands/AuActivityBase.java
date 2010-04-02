/*
 * $Id: AuActivityBase.java,v 1.6 2010-04-02 23:30:20 pgust Exp $
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

package org.lockss.uiapi.commands;

import java.util.*;

import org.w3c.dom.*;

import org.lockss.util.*;
import org.lockss.remote.*;
import org.lockss.daemon.*;
import org.lockss.config.*;
import org.lockss.uiapi.util.*;

/**
 * Common code for Archival Unit management (create/update/remove/restore)
 */
public class AuActivityBase extends StatusActivityBase {

  private static String NAME  = "AuActivityBase";
  private static Logger log   = Logger.getLogger(NAME);

  private PluginProxy   _pluginProxy;
  private TitleConfig   _titleConfig;
  private List          _auConfigParams;
  private Collection    _definingKeys;
  private Collection    _noEditKeys;
  private List          _editKeys;


  public AuActivityBase() {
    super();
  }

  /*
   * Required by ApiActivityBase
   *
   * Return success
   */
  public boolean doCommand() throws Exception {
    return true;
  }

  /*
   * Utilities
   *
   * Several of these are taken in spirit or verbatim from AuConfig.java
   */

  /**
   * Get the active Plugin
   * @return Active Plugin
   */
  protected PluginProxy getPlugin() {
    return _pluginProxy;
  }

  /**
   * Set the Plugin for this command
   * @param plugin The Plugin
   */
  protected void setPlugin(PluginProxy plugin) {
    _pluginProxy = plugin;
  }

  /**
   * Set the Plugin for this command
   * @param au The active Archival Unit
   */
  protected void setPlugin(AuProxy au) {
    _pluginProxy = au.getPlugin();
  }

  /**
   * Set the Plugin TitleConfig object for this command
   * @param titleConfig The TitleConfig
   */
  protected void setTitleConfig(TitleConfig titleConfig) {
    if (titleConfig == null) {
      throw new IllegalStateException( "Null title configuration saved");
    }
    _titleConfig = titleConfig;
  }

  /**
   * Get the Plugin TitleConfig object
   * @return TitleConfig
   */
  protected TitleConfig getTitleConfig() {
    if (_titleConfig == null) {
      throw new IllegalStateException("Null title configuration referenced");
    }
    return _titleConfig;
  }

  /**
   * Set property values based on the Archival Unit "defining keys"
   * @param auConfig AU Configuration
   * @param properties The Properties object to populate
   */
  protected void getAuProperties(Configuration auConfig,
                                 Properties properties) {

    Iterator  iterator  = getDefiningKeys().iterator();

    while (iterator.hasNext()) {
      String key = (String) iterator.next();

      if (auConfig.get(key) != null) {
        properties.put(key, auConfig.get(key));
      }
    }
  }

  /**
   * Get Archival Unit property values from the request document
   * @param properties Properties object (values established here)
   * @param metadataName The name of the "metadata" element in the request
   * document - dynamic fields are identified via this element
   */
  protected void getAuPropertiesFromForm(Properties properties,
                                         String metadataName) {
    KeyedList metadata;
    int       size;

    metadata = ParseUtils.getDynamicFields(getXmlUtils(),
                                           getRequestDocument(),
                                           metadataName);
    size = metadata.size();

    for (int i = 0; i < size; i++) {
      String value = (String) metadata.getValue(i);

      if (value != null) {
        properties.put(metadata.getKey(i), value);
      }
    }
  }

  /**
   * Get the Configuration for this AU (from values in the request document)
   * @return The AU Configuration
   */
  protected Configuration getAuConfigFromForm() {
    Properties  properties = new Properties();

    getAuPropertiesFromForm(properties, AP_MD_AUDEFINING);
    getAuPropertiesFromForm(properties, AP_MD_AUEDIT);

    return ConfigManager.fromPropertiesUnsealed(properties);
  }

  /**
   * Get the AU Configuration - return defining keys from the current AU
   * Configuration, "edit" keys from the request document
   * @param auConfig Current AU Configuration
   * @return AU Configuration
   */
  protected Configuration getAuConfigFromForm(Configuration auConfig) {
    Properties  properties    = new Properties();

    getAuProperties(auConfig, properties);
    getAuPropertiesFromForm(properties, AP_MD_AUEDIT);

    return ConfigManager.fromProperties(properties);
  }

  /**
   * True if both values are null, empty strings, or equal strings
   */
  private boolean isEqual(String newVal, String oldVal) {

    return (StringUtil.isNullString(newVal))  ? StringUtil.isNullString(oldVal)
                                              : newVal.equals(oldVal);
  }

  /**
   * Return true if newConfig is different from oldConfig
   */
  protected boolean isChanged(Configuration oldConfig,
                              Configuration newConfig) {

    Collection dk   = oldConfig.differentKeys(newConfig);
    boolean changed = false;
    for (Iterator iter = dk.iterator(); iter.hasNext(); ) {
      String key    = (String) iter.next();
      String oldVal = oldConfig.get(key);
      String newVal = newConfig.get(key);

      if (!isEqual(oldVal, newVal)) {
	changed = true;
        log.debug("Key " + key + " changed from \"" +
                  oldVal + "\" to \"" + newVal + "\"");
      }
    }
    return changed;
  }

  /**
   * Establish (look up) the defining and editable keys for this AU
   */
  private void prepareConfigParams() {

    _auConfigParams = new ArrayList(_pluginProxy.getAuConfigDescrs());
    _definingKeys   = new ArrayList();
    _editKeys       = new ArrayList();

    for (Iterator iterator = _auConfigParams.iterator(); iterator.hasNext();) {
      ConfigParamDescr descr = (ConfigParamDescr) iterator.next();

      if (descr.isDefinitional()) {
        _definingKeys.add(descr.getKey());
      } else {
	      _editKeys.add(descr.getKey());
      }
    }
  }

  /**
   * Get defining keys
   * @return Defining key Collection
   */
  private Collection getDefiningKeys() {
    if (_definingKeys == null) {
      prepareConfigParams();
    }
    return _definingKeys;
  }

  /**
   * Get editable keys
   * @return Edit key List
   */
  private List getEditKeys() {
    if (_editKeys == null) {
      prepareConfigParams();
    }
    return _editKeys;
  }

  /**
   * Get the list of defining keys that cannot be edited
   */
  protected Collection getNoEditKeys() {

    if (_noEditKeys == null) {
      Collection no = getTitleConfig().getUnEditableKeys();

      _noEditKeys = new ArrayList();

      for (Iterator iterator = no.iterator(); iterator.hasNext(); ) {
        _noEditKeys.add(iterator.next());
      }
    }
    return _noEditKeys;
  }

  /**
   * Get the AU Configuration
   * @return AU Configuration
   */
  private List getAuConfigParams() {
    if (_auConfigParams == null) {
      prepareConfigParams();
    }
    return _auConfigParams;
  }

  /**
   * Is the Plugin for this key loaded?
   * @param key Plugin key
   * @return true If so
   */
  protected boolean pluginLoaded(String key) {

    if (!getLockssDaemon().getPluginManager().ensurePluginLoaded(key)) {
      log.warning("Failed to find plugin: " + key);
      return false;
    }
    return true;
  }

  /**
   * Get an AU Proxy for this AUID
   * @param auid The AUID we need a proxy for
   * @return An appropriate AuProxy object (null if none exists)
   */
  protected AuProxy getAuProxy(String auid) {
    try {
      return getRemoteApi().findAuProxy(auid);
    } catch (Exception exception) {
      return null;
    }
  }

  /**
   * Get an inactive AU Proxy for this AUID
   * @param auid The AUID we need a proxy for
   * @return An appropriate AuProxy object (null if none exists)
   */
  protected AuProxy getInactiveAuProxy(String auid) {
    try {
      return getRemoteApi().findInactiveAuProxy(auid);
    } catch (Exception execption) {
      return null;
    }
  }

  /**
   * Get an AU Proxy (either active or inactive) for this AUID
   * @param auid The AUID we need a proxy for
   * @return An appropriate AuProxy object (null if none exists)
   */
  protected AuProxy getAnyAuProxy(String auid) {
    AuProxy auProxy;

    if ((auProxy = getAuProxy(auid)) == null) {
      auProxy = getInactiveAuProxy(auid);
    }
    return auProxy;
  }

  /**
   * Get a Plugin Proxy for this AUID
   * @param pid The proxy ID we need a proxy for
   * @return An appropriate PluginProxy object (null if none exists)
   */
  protected PluginProxy getPluginProxy(String pid) {
    try {
      return getRemoteApi().findPluginProxy(pid);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Error handler - set error text
   * @return false
   */
  protected boolean error(String text) {
    setResponseStatusMessage(text);
    return false;
  }

  /**
   * Add key details to the response document
   * @param root Root element - key data attached here
   * @param keyRootName Name for key data block (reflects defining or editable)
   * @param keys Collection of keys (defining or editable) to examine
   * @param config Plugin configuration
   */
  protected void addKeys(Element root,
                         String keyRootName,
                         Collection keys,
                         Configuration config) {
    addKeys(root, keyRootName, keys, null, config);
  }

  /**
   * Add key details to the response document
   * @param root Root element - key data attached here
   * @param keyRootName Name for key data block (reflects defining or editable)
   * @param keys {@link Collection} of keys (defining or editable) to examine
   * @param noEditKeys {@link Collection} of keys that should not be edited (null for none)
   * @param config Plugin configuration
   *
   * <code>
   * <key>
   *    <internal>internal name</internal>
   *    <name>key name</name>
   *    <description>footnote text</description>
   *    <value>key value</value>
   *    <edit>true is use can supply a value</edit>
   *    <size>suggested maximum value length</size>
   * </key>
   * </code>
   */
  protected void addKeys(Element root,
                         String keyRootName,
                         Collection keys,
                         Collection noEditKeys,
                         Configuration config) {

    XmlUtils      xmlUtils  = getXmlUtils();
    Element       keyRoot   = xmlUtils.createElement(root, keyRootName);
    Iterator      iterator  = getAuConfigParams().iterator();

    while (iterator.hasNext()) {

      ConfigParamDescr descriptor = (ConfigParamDescr) iterator.next();

      Element auKeyElement;
      Element element;
      String  value;
      boolean edit;

      if (!keys.contains(descriptor.getKey())) {
	      continue;
      }

      auKeyElement = xmlUtils.createElement(keyRoot, AP_E_KEY);

      element = xmlUtils.createElement(auKeyElement, AP_E_INTERNAL);
      XmlUtils.addText(element, descriptor.getKey());

      element = xmlUtils.createElement(auKeyElement, AP_E_NAME);
      XmlUtils.addText(element, descriptor.getDisplayName());

      element = xmlUtils.createElement(auKeyElement, AP_E_DESCRIPTION);
      XmlUtils.addText(element, descriptor.getDescription());

      value = (config == null) ? "" : config.get(descriptor.getKey());

      element = xmlUtils.createElement(auKeyElement, AP_E_VALUE);
      XmlUtils.addText(element, value);

      edit = false;
      if (noEditKeys != null) {
        edit = !noEditKeys.contains(descriptor.getKey());
      }
      element = xmlUtils.createElement(auKeyElement, AP_E_EDIT);
      XmlUtils.addText(element, edit ? COM_TRUE : COM_FALSE);

      element = xmlUtils.createElement(auKeyElement, AP_E_SIZE);
      XmlUtils.addText(element, ("" + descriptor.getSize()));
    }
  }

  /**
   * Add Plugin details to the response document
   * @param root Attach plugin data at this element
   * @param pluginRootName Name for plugin data block
   */
  protected void addPlugin(Element root, String pluginRootName) {

    XmlUtils  xmlUtils    = getXmlUtils();
    Element   pluginRoot  = xmlUtils.createElement(root, pluginRootName);
    Element   element;

    element = xmlUtils.createElement(pluginRoot, AP_E_ID);
    XmlUtils.addText(element, _pluginProxy.getPluginId());

    element = xmlUtils.createElement(pluginRoot, AP_E_NAME);
    XmlUtils.addText(element, _pluginProxy.getPluginName());

    element = xmlUtils.createElement(pluginRoot, AP_E_VERSION);
    XmlUtils.addText(element, _pluginProxy.getVersion());

    element = xmlUtils.createElement(pluginRoot, AP_E_TITLE);
    if (_titleConfig != null) {
      XmlUtils.addText(element, _titleConfig.getJournalTitle());
    }
  }

  /**
   * Generate response XML for command setup data (Plugin, AU keys)
   * @param configuration AU Configuration block
   */
  protected void generateSetupXml(Configuration configuration) {
    generateSetupXml(configuration, null);
  }

  /**
   * Generate response XML for command setup data (Plugin, AU keys)
   * @param configuration AU Configuration block
   * @param noEditKeys A list of defining keys that should no be modified
   */
  protected void generateSetupXml(Configuration configuration,
                                  Collection noEditKeys) {
    Element   element;

    element = getXmlUtils().createElement(getResponseRoot(), AP_E_INFO);
    addPlugin(element, AP_E_PLUGIN);

    addKeys(element, AP_E_AUDEFINING,
                     getDefiningKeys(), noEditKeys, configuration);
    addKeys(element, AP_E_AUEDIT,
                     getEditKeys(), Collections.EMPTY_SET, configuration);

    try {
      log.debug2(XmlUtils.serialize(getResponseDocument()));
    } catch (Exception ignore) { }
  }
}
