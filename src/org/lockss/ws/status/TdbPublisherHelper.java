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
import org.lockss.config.TdbPublisher;
import org.lockss.config.TdbUtil;
import org.lockss.util.Logger;
import org.lockss.ws.entities.TdbPublisherWsResult;

/**
 * Helper of the DaemonStatus web service implementation of title database
 * publisher queries.
 */
public class TdbPublisherHelper {
  /**
   * The fully-qualified name of the class of the objects used as source in a
   * query.
   */
  static String SOURCE_FQCN = TdbPublisherWsSource.class.getCanonicalName();

  /**
   * The fully-qualified name of the class of the objects returned by the query.
   */
  static String RESULT_FQCN = TdbPublisherWsResult.class.getCanonicalName();

  //
  // Property names used in title database publisher queries.
  //
  static String NAME = "name";

  /**
   * All the property names used in title database publisher queries.
   */
  @SuppressWarnings("serial")
  static final Set<String> PROPERTY_NAMES = new HashSet<String>() {
    {
      add(NAME);
    }
  };

  private static Logger log = Logger.getLogger(TdbPublisherHelper.class);

  /**
   * Provides the universe of title database publisher-related query objects
   * used as the source for a query.
   * 
   * @return a List<TdbPublisherWsSource> with the universe.
   */
  List<TdbPublisherWsSource> createUniverse() {
    final String DEBUG_HEADER = "createUniverse(): ";

    // Get all the title database publishers.
    Collection<TdbPublisher> allTdbPublishers =
	TdbUtil.getTdb().getAllTdbPublishers().values();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "allTdbPublishers.size() = "
	+ allTdbPublishers.size());

    // Initialize the universe.
    List<TdbPublisherWsSource> universe =
	new ArrayList<TdbPublisherWsSource>(allTdbPublishers.size());

    // Loop through all the title database publishers.
    for (TdbPublisher tdbPublisher : allTdbPublishers) {
      // Add the object initialized with this title database publisher to the
      // universe of objects.
      universe.add(new TdbPublisherWsSource(tdbPublisher));
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "universe.size() = " + universe.size());
    return universe;
  }

  /**
   * Provides a printable copy of a collection of title database
   * publisher-related query results.
   * 
   * @param results
   *          A Collection<TdbPublisherWsResult> with the query results.
   * @return a String with the requested printable copy.
   */
  String nonDefaultToString(Collection<TdbPublisherWsResult> results) {
    StringBuilder builder = new StringBuilder("[");
    boolean isFirst = true;

    // Loop through through all the results in the collection.
    for (TdbPublisherWsResult result : results) {
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
   * Provides a printable copy of a title database publisher-related query
   * result.
   * 
   * @param result
   *          A TdbPublisherWsResult with the query result.
   * @return a String with the requested printable copy.
   */
  private String nonDefaultToString(TdbPublisherWsResult result) {
    StringBuilder builder = new StringBuilder("TdbPublisherWsResult [");

    if (result.getName() != null) {
      builder.append("name=").append(result.getName());
    }

    return builder.append("]").toString();
  }
}
