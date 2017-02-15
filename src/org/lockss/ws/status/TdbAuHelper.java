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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbUtil;
import org.lockss.util.Logger;
import org.lockss.ws.entities.TdbAuWsResult;

/**
 * Helper of the DaemonStatus web service implementation of title database
 * archival unit queries.
 */
public class TdbAuHelper {
  /**
   * The fully-qualified name of the class of the objects used as source in a
   * query.
   */
  static String SOURCE_FQCN = TdbAuWsSource.class.getCanonicalName();

  /**
   * The fully-qualified name of the class of the objects returned by the query.
   */
  static String RESULT_FQCN = TdbAuWsResult.class.getCanonicalName();

  //
  // Property names used in title database archival unit queries.
  //
  static String AU_ID = "auId";
  static String NAME = "name";
  static String PLUGIN_NAME = "pluginName";
  static String TDB_TITLE = "tdbTitle";
  static String TDB_PUBLISHER = "tdbPublisher";
  static String DOWN = "down";
  static String ACTIVE = "active";
  static String PARAMS = "params";
  static String ATTRS = "attrs";
  static String PROPS = "props";

  /**
   * All the property names used in title database archival unit queries.
   */
  @SuppressWarnings("serial")
  static final Set<String> PROPERTY_NAMES = new HashSet<String>() {
    {
      add(AU_ID);
      add(NAME);
      add(PLUGIN_NAME);
      add(TDB_TITLE);
      add(TDB_PUBLISHER);
      add(DOWN);
      add(ACTIVE);
      add(PARAMS);
      add(ATTRS);
      add(PROPS);
    }
  };

  private static Logger log = Logger.getLogger(TdbAuHelper.class);

  /**
   * Provides the universe of title database archival unit-related query objects
   * used as the source for a query.
   * 
   * @return a List<TdbAuWsSource> with the universe.
   */
  List<TdbAuWsSource> createUniverse() {
    final String DEBUG_HEADER = "createUniverse(): ";

    // Get all the title database archival units.
    Set<TdbAu.Id> allTdbAuIds = TdbUtil.getTdb().getAllTdbAuIds();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "allTdbAuIds.size() = " + allTdbAuIds.size());

    // Initialize the universe.
    List<TdbAuWsSource> universe =
	new ArrayList<TdbAuWsSource>(allTdbAuIds.size());

    // Loop through all the title database archival units.
    for (TdbAu.Id tdbAuId : allTdbAuIds) {
      // Add the object initialized with this title database archival unit to
      // the universe of objects.
      universe.add(new TdbAuWsSource(tdbAuId));
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "universe.size() = " + universe.size());
    return universe;
  }

  /**
   * Provides a printable copy of a collection of title database archival
   * unit-related query results.
   * 
   * @param results
   *          A Collection<TdbAuWsResult> with the query results.
   * @return a String with the requested printable copy.
   */
  String nonDefaultToString(Collection<TdbAuWsResult> results) {
    StringBuilder builder = new StringBuilder("[");
    boolean isFirst = true;

    // Loop through through all the results in the collection.
    for (TdbAuWsResult result : results) {
      // Handle the first result differently.
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      // Add this result to the printable copy.
      builder.append(nonDefaultToString(result));
    }

    return builder.append("]").toString();
  }

  /**
   * Provides a printable copy of a title database archival unit-related query
   * result.
   * 
   * @param result
   *          A TdbAuWsResult with the query result.
   * @return a String with the requested printable copy.
   */
  private String nonDefaultToString(TdbAuWsResult result) {
    StringBuilder builder = new StringBuilder("TdbAuWsResult [");
    boolean isFirst = true;

    if (result.getAuId() != null) {
      builder.append("auId=").append(result.getAuId());
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

    if (result.getPluginName() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("pluginName=").append(result.getPluginName());
    }

    if (result.getTdbTitle() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("tdbTitle=").append(result.getTdbTitle());
    }

    if (result.getTdbPublisher() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("tdbPublisher=").append(result.getTdbPublisher());
    }

    if (result.getDown() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("down=").append(result.getDown());
    }

    if (result.getActive() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("active=").append(result.getActive());
    }

    if (result.getParams() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("params=").append(result.getParams());
    }

    if (result.getAttrs() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("attrs=").append(result.getAttrs());
    }

    if (result.getProps() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("props=").append(result.getProps());
    }

    return builder.append("]").toString();
  }
}
