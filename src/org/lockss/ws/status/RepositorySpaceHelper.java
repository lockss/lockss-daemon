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
 * Helper of the DaemonStatus web service implementation of repository space
 * queries.
 */
package org.lockss.ws.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lockss.app.LockssDaemon;
import org.lockss.remote.RemoteApi;
import org.lockss.util.Logger;
import org.lockss.ws.entities.RepositorySpaceWsResult;

public class RepositorySpaceHelper {
  /**
   * The fully-qualified name of the class of the objects used as source in a
   * query.
   */
  static String SOURCE_FQCN = RepositorySpaceWsSource.class.getCanonicalName();

  /**
   * The fully-qualified name of the class of the objects returned by the query.
   */
  static String RESULT_FQCN = RepositorySpaceWsResult.class.getCanonicalName();

  //
  // Property names used in repository space queries.
  //
  static String REPOSITORY_SPACE_ID = "repositorySpaceId";
  static String SIZE = "size";
  static String USED = "used";
  static String FREE = "free";
  static String PERCENTAGE_FULL = "percentageFull";
  static String ACTIVE_COUNT = "activeCount";
  static String INACTIVE_COUNT = "inactiveCount";
  static String DELETED_COUNT = "deletedCount";
  static String ORPHANED_COUNT = "orphanedCount";

  /**
   * All the property names used in repository space queries.
   */
  @SuppressWarnings("serial")
  static final Set<String> PROPERTY_NAMES = new HashSet<String>() {
    {
      add(REPOSITORY_SPACE_ID);
      add(SIZE);
      add(USED);
      add(FREE);
      add(PERCENTAGE_FULL);
      add(ACTIVE_COUNT);
      add(INACTIVE_COUNT);
      add(DELETED_COUNT);
      add(ORPHANED_COUNT);
    }
  };

  private static Logger log = Logger.getLogger(RepositorySpaceHelper.class);

  /**
   * Provides the universe of repository space-related objects used as the
   * source for a query.
   * 
   * @return a List<RepositorySpaceWsProxy> with the universe.
   */
  List<RepositorySpaceWsSource> createUniverse() {
    final String DEBUG_HEADER = "createUniverse(): ";
    
    // Get the remote API manager.
    RemoteApi remoteApi =
	(RemoteApi)LockssDaemon.getManager(LockssDaemon.REMOTE_API);

    // Get all the repository space names.
    List<String> allRepositorySpacesNames =
	(List<String>)remoteApi.getRepositoryList();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "allRepositorySpacesNames.size() = "
	+ allRepositorySpacesNames.size());
    // Initialize the universe.
    List<RepositorySpaceWsSource> universe =
	new ArrayList<RepositorySpaceWsSource>(allRepositorySpacesNames.size());

    // Loop through all the repository space names.
    for (String repositorySpaceName : allRepositorySpacesNames) {
      // Add the object initialized with this repository space to the universe
      // of objects.
      universe.add(new RepositorySpaceWsSource(repositorySpaceName,
	  remoteApi.getRepositoryDF(repositorySpaceName)));
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "universe.size() = " + universe.size());
    return universe;
  }

  /**
   * Provides a printable copy of a collection of repository space-related query
   * results.
   * 
   * @param results
   *          A Collection<RepositorySpaceWsResult> with the query results.
   * @return a String with the requested printable copy.
   */
  String nonDefaultToString(Collection<RepositorySpaceWsResult> results) {
    StringBuilder builder = new StringBuilder("[");
    boolean isFirst = true;

    // Loop through through all the results in the collection.
    for (RepositorySpaceWsResult result : results) {
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
   * Provides a printable copy of a repository space-related query result.
   * 
   * @param result
   *          A RepositorySpaceWsResult with the query result.
   * @return a String with the requested printable copy.
   */
  private String nonDefaultToString(RepositorySpaceWsResult result) {
    StringBuilder builder = new StringBuilder("RepositorySpaceWsResult [");
    boolean isFirst = true;

    if (result.getRepositorySpaceId() != null) {
      builder.append("repositorySpaceId=")
      .append(result.getRepositorySpaceId());
      isFirst = false;
    }

    if (result.getSize() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("size=").append(result.getSize());
    }

    if (result.getUsed() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("used=").append(result.getUsed());
    }

    if (result.getFree() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("free=").append(result.getFree());
    }

    if (result.getPercentageFull() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("percentageFull=").append(result.getPercentageFull());
    }

    if (result.getActiveCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("activeCount=").append(result.getActiveCount());
    }

    if (result.getInactiveCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("inactiveCount=").append(result.getInactiveCount());
    }

    if (result.getDeletedCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("deletedCount=").append(result.getDeletedCount());
    }

    if (result.getOrphanedCount() != null) {
      if (!isFirst) {
	builder.append(", ");
      } else {
	isFirst = false;
      }

      builder.append("orphanedCount=").append(result.getOrphanedCount());
    }

    return builder.append("]").toString();
  }
}
