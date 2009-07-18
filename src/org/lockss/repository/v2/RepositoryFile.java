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

import org.lockss.protocol.*;
import org.lockss.repository.*;
import org.lockss.repository.v2.*;
import org.lockss.state.NodeState;

/**
 * @author Brent E. Edwards
 * 
 * This interface maintains all versions within a file.
 */
public interface RepositoryFile extends RepositoryNode {
  /**
   * Stores PollHistories from a NodeState in this repository file.
   */
//  public void setPollHistories(NodeState nodeState) throws LockssRepositoryException;

  /**
   * Does the preferred file have stored content? This method refers to the
   * preferred version of the Repository File; it is a convenience method.
   */
  public boolean hasContent() throws LockssRepositoryException;
  
  /**
   * Returns properties within a RepositoryFile. These properties should affect
   * all versions.
   */
  public Properties getProperties() throws IOException, LockssRepositoryException;
  
  /**
   * Sets the unversioned properties within a RepositoryFile.
   */
  public void setProperties(Properties prop) throws IOException, LockssRepositoryException;
  
  /**
   * Returns the list of peer id sets that agree with a particular file.
   */
  public PersistentPeerIdSet getAgreeingPeerIdSet() throws LockssRepositoryException;

  /**
   * Sets the list of peer id sets that agree with a particular file.
   * 
   * @param sepi
   */
  public void setAgreeingPeerIdSet(PersistentPeerIdSet sepi) throws LockssRepositoryException;
    
    
  /**
   * Create a new version under the current RepositoryFile.
   * 
   * @param isPreferred -- Should we set this code as the preferred version?
   * @return RepositoryFileVersion.
   * @throws LockssRepositoryException
   */
  public RepositoryFileVersion createNewVersion() throws LockssRepositoryException, FileNotFoundException;

  /**
   * Create a new version under the current RepositoryFile, before another version.
   * 
   * @param isPreferred -- Should we set this code as the preferred version?
   * @return RepositoryFileVersion.
   * @throws LockssRepositoryException
   */
  public RepositoryFileVersion createNewVersionBefore(RepositoryFileVersion rfvBefore) throws LockssRepositoryException, FileNotFoundException;

  /**
   * Return the size of the contents of this "file".
   * 
   * @param mostRecentOnly -- If true, return the size of only one version.
   *                           If false, include all versions. 
   * @return
   */
  public long getContentSize(/* boolean mostRecentOnly = true */)
    throws LockssRepositoryException;

  public long getContentSize(boolean mostRecentOnly)
    throws LockssRepositoryException;
  
 /**
   * What versions does this code know about? The parameterized method specifies
   * how many (recent) versions to return. The unparameterized method will
   * return all versions.
   */
  public List<RepositoryFileVersion> listVersions() throws LockssRepositoryException;
  
  public List<RepositoryFileVersion> listVersions(int numVersions) throws LockssRepositoryException;
  
  /**
   * Set and get the preferred version, which should be used by the client.
   */
  public RepositoryFileVersion getPreferredVersion() throws LockssRepositoryException;
  
  public void setPreferredVersion(RepositoryFileVersion rfv) throws LockssRepositoryException;
  
  /**
   * Return the Url set on construction.
   */
  public String getNodeUrl();
  
  /**
   * Clean the database of versions no longer wanted.
   */
  public void cleanDatabase() throws LockssRepositoryException;
  
  /**
   * Move the location of the .warc files
   */
  public void move(String strNewLocation) throws LockssRepositoryException;
}
