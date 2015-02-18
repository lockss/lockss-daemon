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
package org.lockss.ws.entities;

import java.util.Map;

/**
 * Container for the information related to a plugin that is the result of a
 * query.
 */
public class PluginWsResult {
  private String pluginId;
  private String name;
  private String version;
  private String type;
  private Map<String, String> definition;
  private String registry;
  private String url;
  private Integer auCount;
  private String publishingPlatform;

  /**
   * Provides the plugin identifier.
   * 
   * @return A String with the identifier.
   */
  public String getPluginId() {
    return pluginId;
  }
  public void setPluginId(String pluginId) {
    this.pluginId = pluginId;
  }

  /**
   * Provides the plugin name.
   * 
   * @return A String with the name.
   */
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Provides the plugin version.
   * 
   * @return A String with the version.
   */
  public String getVersion() {
    return version;
  }
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Provides the plugin type.
   * 
   * @return A String with the type.
   */
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Provides the plugin definition properties.
   * 
   * @return A Map<String, String> with the properties.
   */
  public Map<String, String> getDefinition() {
    return definition;
  }
  public void setDefinition(Map<String, String> definition) {
    this.definition = definition;
  }

  /**
   * Provides the plugin registry name.
   * 
   * @return A String with the registry name.
   */
  public String getRegistry() {
    return registry;
  }
  public void setRegistry(String registry) {
    this.registry = registry;
  }

  /**
   * Provides the plugin URL.
   * 
   * @return A String with the URL.
   */
  public String getUrl() {
    return url;
  }
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Provides the count of Archival Units configured with this plugin.
   * 
   * @return An Integer with the count.
   */
  public Integer getAuCount() {
    return auCount;
  }
  public void setAuCount(Integer auCount) {
    this.auCount = auCount;
  }

  /**
   * Provides the plugin publishing platform name.
   * 
   * @return a String with the publishing platform name.
   */
  public String getPublishingPlatform() {
    return publishingPlatform;
  }
  public void setPublishingPlatform(String publishingPlatform) {
    this.publishingPlatform = publishingPlatform;
  }

  @Override
  public String toString() {
    return "PluginWsResult [pluginId=" + pluginId + ", name=" + name
	+ ", version=" + version + ", type=" + type + ", definition="
	+ definition + ", registry=" + registry + ", url=" + url + ", auCount="
	+ auCount + ", publishingPlatform=" + publishingPlatform + "]";
  }
}
