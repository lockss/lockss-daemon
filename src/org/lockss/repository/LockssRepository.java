/*
 * $Id: LockssRepository.java,v 1.10 2003-02-24 22:13:42 claire Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.repository;

import java.net.MalformedURLException;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * LockssRepository is used to organize the urls being cached.
 */
public interface LockssRepository {
  /**
   * Creates a LockssRepository for the given {@link ArchivalUnit} at
   * a cache location specific to that archive.
   * @param au ArchivalUnit to be cached
   * @return a repository for the archive
   */
  public LockssRepository repositoryFactory(ArchivalUnit au);

  /**
   * Returns a {@link RepositoryNode} which represents the url in question.
   * This only returns urls which exist in the cache (null otherwise).
   * @param url the desired url
   * @return an entry for the url
   * @throws java.net.MalformedURLException
   */
  public RepositoryNode getNode(String url) throws MalformedURLException;

  /**
   * Creates a new {@link RepositoryNode} for the purposes of writing a new leaf
   * to the cache.  This is used to add new leaves, though it still returns the
   * leaf if it already exists.
   * @param url the url to be cached
   * @return an entry for the url
   * @throws java.net.MalformedURLException
   */
  public RepositoryNode createNewNode(String url) throws MalformedURLException;

  /**
   * Delete the node.
   * @param url the url to be deleted
   * @throws MalformedURLException
   */
  public void deleteNode(String url) throws MalformedURLException;

  /**
   * Traverses the node hierarchy and tests internal state consistency for each
   * node.  Corrects and logs any correctable errors it encounters.
   */
//  public void nodeConsistencyCheck();

/**
 * Compares the two CachedUrlSets and returns an int representing the
 * relationship between the two.  For example, if the first was above the
 * second in the hierarchy, it would return <code>LockssRepository.ABOVE</code>.
 * @param cus1 the first cus
 * @param cus2 the second cus
 * @return an int representing the relationship
 */
  public int cusCompare(CachedUrlSet cus1, CachedUrlSet cus2);

  /**
   * The cus is above the second cus in the hierarchy.
   */
  public static final int ABOVE = 0;
  /**
   * The cus is below the second cus in the hierarchy.
   */
  public static final int BELOW = 1;
  /**
   * The cus is at the same level as the second cus in the hierarchy, with
   * no overlap.
   */
  public static final int SAME_LEVEL_NO_OVERLAP = 2;
  /**
   * The cus is at the same level as the second cus in the hierarchy, with
   * overlap.
   */
  public static final int SAME_LEVEL_OVERLAP = 3;

  /**
   * There is no relation between the two sets in the hierarchy.
   */
  public static final int NO_RELATION = 4;

  /**
   * Thrown when an unexpected error is encountered while caching.
   * Typically this is a file system error.
   */
  public class RepositoryStateException extends RuntimeException {
    public RepositoryStateException(String msg) {
      super(msg);
    }
  }

}