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
 * Helper of the DaemonStatus web service implementation of plugin queries.
 */
package org.lockss.ws.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lockss.app.LockssDaemon;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.util.Logger;
import org.lockss.ws.entities.PluginWsResult;

public class PluginHelper {
  /**
   * The fully-qualified name of the class of the objects used as source in a
   * query.
   */
  static String SOURCE_FQCN = PluginWsSource.class.getCanonicalName();

  /**
   * The fully-qualified name of the class of the objects returned by the query.
   */
  static String RESULT_FQCN = PluginWsResult.class.getCanonicalName();

  //
  // Property names used in plugin queries.
  //
  static String PLUGIN_ID = "pluginId";
  static String NAME = "name";
  static String VERSION = "version";
  static String TYPE = "type";
  static String DEFINITION = "definition";
  static String REGISTRY = "registry";
  static String URL = "url";
  static String AU_COUNT = "auCount";
  static String PUBLISHING_PLATFORM = "publishingPlatform";

  /**
   * All the property names used in plugin queries.
   */
  @SuppressWarnings("serial")
  static final Set<String> PROPERTY_NAMES = new HashSet<String>() {
    {
      add(PLUGIN_ID);
      add(NAME);
      add(VERSION);
      add(TYPE);
      add(DEFINITION);
      add(REGISTRY);
      add(URL);
      add(AU_COUNT);
      add(PUBLISHING_PLATFORM);
    }
  };

  private static Logger log = Logger.getLogger(PluginHelper.class);

  /**
   * Provides the universe of plugin-related objects used as the source for a
   * query.
   * 
   * @return a List<PluginWsProxy> with the universe.
   */
  List<PluginWsSource> createUniverse() {
    final String DEBUG_HEADER = "createUniverse(): ";

    // Get all the plugins.
    Collection<Plugin> allPlugins =
	((PluginManager) LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER))
	.getRegisteredPlugins();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "allPlugins.size() = " + allPlugins.size());

    // Initialize the universe.
    List<PluginWsSource> universe =
	new ArrayList<PluginWsSource>(allPlugins.size());

    // Loop through all the plugins.
    for (Plugin plugin : allPlugins) {
      // Add the object initialized with this plugin to the universe of objects.
      universe.add(new PluginWsSource(plugin));
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "universe.size() = " + universe.size());
    return universe;
  }

  /**
   * Provides a printable copy of a collection of plugin-related query results.
   * 
   * @param results
   *          A Collection<PluginWsResult> with the query results.
   * @return a String with the requested printable copy.
   */
  String nonDefaultToString(Collection<PluginWsResult> results) {
    StringBuilder builder = new StringBuilder("[");
    boolean isFirst = true;

    // Loop through through all the results in the collection.
    for (PluginWsResult result : results) {
      // Handle the first result differently.
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append(nonDefaultToString(result));
    }

    // Add this result to the printable copy.
    return builder.append("]").toString();
  }

  /**
   * Provides a printable copy of a plugin-related query result.
   * 
   * @param result
   *          A PluginWsResult with the query result.
   * @return a String with the requested printable copy.
   */
  private String nonDefaultToString(PluginWsResult result) {
    StringBuilder builder = new StringBuilder("PluginWsResult [");
    boolean isFirst = true;

    if (result.getPluginId() != null) {
      builder.append("pluginId=").append(result.getPluginId());
      isFirst = false;
    }

    if (result.getName() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("name=").append(result.getName());
    }

    if (result.getVersion() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("version=").append(result.getVersion());
    }

    if (result.getType() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("type=").append(result.getType());
    }

    if (result.getDefinition() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("definition=").append(result.getDefinition());
    }

    if (result.getRegistry() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("registry=").append(result.getRegistry());
    }

    if (result.getUrl() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("url=").append(result.getUrl());
    }

    if (result.getAuCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("auCount=").append(result.getAuCount());
    }

    if (result.getPublishingPlatform() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("publishingPlatform=")
      .append(result.getPublishingPlatform());
    }

    return builder.append("]").toString();
  }
}
