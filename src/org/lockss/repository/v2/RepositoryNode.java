/**

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.repository.v2;

import java.io.*;
import java.util.*;

import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.repository.v2.*;
import org.lockss.state.*;

/**
 * @author Brent E. Edwards
 * 
 */
public interface RepositoryNode {
  /**
   * Count the number of nodes directly under this node.
   * 
   * Note: This method (intentionally) does not distinguish between deleted 
   * and undeleted files.
   */
  public int getChildCount() throws LockssRepositoryException;

  /**
   * Returns the size of the content tree under (and including) this cache in
   * bytes.
   * 
   * @param filter:
   *                Whether to filter the URLs being searched.
   * @param calcIfUnknown:
   *                If true, then calculate what's not known.
   * @param preferredOnly:
   *                If true, then only consider the preferred version. If
   *                false, then compute for ALL versions.
   */
  public long getTreeContentSize(CachedUrlSetSpec filter,
      boolean calcIfUnknown /*
                             * , boolean preferredOnly = true */)
  throws LockssRepositoryException;

  public long getTreeContentSize(CachedUrlSetSpec filter,
      boolean calcIfUnknown, boolean preferredOnly )
  throws LockssRepositoryException;

  /**
   * Asembles a list of immediate children, possibly filtered. Sorted
   * alphabetically by File.compareTo()
   * 
   * @param filter:
   *                a spec to filter on. Null for no filtering.
   * @param includeInactive:
   *                true iff inactive nodes should be included.
   * @return the list of child RepositoryNodes.
   * 
   * Note from Brent: I do not understand why "listChildren" and "getNodeList"
   * should both exist.
   */
  public List<org.lockss.repository.v2.RepositoryFile> getFileList(CachedUrlSetSpec filter
                                        /* , boolean includeDeleted = false */)
                                        throws LockssRepositoryException;

  public List<org.lockss.repository.v2.RepositoryFile> getFileList(CachedUrlSetSpec filter,
      boolean includeDeleted) throws LockssRepositoryException;

  /**
   * Return the URL represented by this node.
   */
  public String getNodeUrl();

  /**
   * Return an array of RepositoryFile for all versions of content/props at this
   * node. The result is sorted from most to least recent: the current version
   * is the first element in the array.
   */
  public RepositoryFile[] getFiles(/* boolean includeDeleted = false */)
  throws LockssRepositoryException;

  public RepositoryFile[] getFiles(boolean includeDeleted)
  throws LockssRepositoryException;
  
  public RepositoryFile[] getFiles(int maxVersions)
  throws LockssRepositoryException;
  
  public RepositoryFile[] getFiles(int maxVersions, boolean includeDeleted)
  throws LockssRepositoryException;
  
  
  /**
   * Used by LockssAuRepository.loadPollHistories.
   */
  // public List<PollHistory> getPollHistoryList() throws LockssRepositoryException;
  // Removed.

  /**
   * Creates a new file under this node, and returns the associated RepositoryFile.
   */
  public RepositoryFile makeNewRepositoryFile(String name) throws LockssRepositoryException;

  /**
   * Creates a new node under this node, and returns the associated RepositoryNode.
   */
  public RepositoryNode makeNewRepositoryNode(String name) throws LockssRepositoryException;
  

  /**
   * Stores the properties for the full node.
   * 
   * @param newProps
   *                a <code>Properties</code> object containing the headers of
   *                the new version being cached.
   */
  public void setProperties(Properties newProps) throws IOException, LockssRepositoryException;
  
  /**
   * Loads the current node state info, except the histories.  Returns a new
   * NodeState if none found.
   * 
   * @param cus the CachedUrlSet
   * @return a {@link NodeState}
   */
//  public NodeState loadNodeState(CachedUrlSet cus) throws LockssRepositoryException;

  /**
   * Stores the current node state info, except the histories.
   */
//  public void storeNodeState(NodeState nodeState) throws LockssRepositoryException;

  /**
   * Move the location of the .warc files
   */
  public void move(String strNewLocation) throws LockssRepositoryException;

}
