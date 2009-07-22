/*
 * $Id: RepositoryNode.java,v 1.21.34.2 2009-07-22 00:25:06 edwardsb1 Exp $
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

package org.lockss.repository;

import java.io.*;
import java.util.*;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.protocol.PeerIdentity;
import org.lockss.repository.v2.RepositoryFile;

/**
 * RepositoryNode is used to store the contents and
 * meta-information of urls being cached.
 */
public interface RepositoryNode extends RepositoryNodeVersion, RepositoryFile, 
  org.lockss.repository.v2.RepositoryNode {
  /**
   * Returns the url represented by this node.
   * @return the node's url
   */
  public String getNodeUrl();

  /**
   * Determines if the node has current stored content.
   * @return true if the node has content
   */
  public boolean hasContent();

  /**
   * Determines if the node is currently inactive.  Inactive nodes often
   * have old content but no current content, so hasContent() will return false.
   * The node is still counted as part of the AU, and its children are present.
   * @return true if the node is inactive
   */
  public boolean isContentInactive();

  /**
   * Determines if the node is deleted.  Deleted nodes may have old content or
   * children, but will not appear in lists of nodes.  Their children will not
   * be included in the AU.
   * @return true if the node is deleted
   */
  public boolean isDeleted();

  /**
   * Returns the size of the current version of stored cache.  Throws an
   * exception if called on a content-less node.
   * @return size
   * @throws UnsupportedOperationException
   */
  public long getContentSize();

  /**
   * Returns the size of the content tree under (and including) this cache, in
   * bytes.
   * @param filter a spec to determine which urls to return
   * @param calcIfUnknown if true, size will calculated if unknown (time
   * consumeing)
   * @return size, or -1 if currently unknown
   */
  public long getTreeContentSize(CachedUrlSetSpec filter,
				 boolean calcIfUnknown);

  /**
   * Returns true if node has no children.
   * @return true if no children
   */
  public boolean isLeaf();

  /**
   * Returns the number of children at this node.
   * @return the child count
   */
  public int getChildCount();

  /**
   * Returns the immediate children of the entry, possibly filtered (null
   * indicates no filtering).  Includes leaf and internal nodes, but not
   * inactive nodes unless 'includeInactive' is true.
   * @param filter a spec to determine which urls to return
   * @param includeInactive true to include inactive nodes
   * @return an <code>Iterator</code> of RepositoryNode objects
   */
  public Iterator listChildren(CachedUrlSetSpec filter, boolean includeInactive);

  /**
   * Prepares the node to write to a new version.  Should be called before storing
   * any data.  Throws an exception if called while a new version is already open.
   * @throws UnsupportedOperationException
   */
  public void makeNewVersion();

  /**
   * Closes the new version to any further writing.  Should be called when done
   * storing data.  Throws an exception if called before
   * <code>makeNewVersion()</code> or if either <code>getNewOutputStream()</code>
   * or <code>setNewProperties()</code> has not been called.
   * @throws UnsupportedOperationException
   */
  public void sealNewVersion();

  /**
   * Discards the currently open new version without writing.
   * Throws an exception if called before <code>makeNewVersion()</code>
   * @throws UnsupportedOperationException
   */
  public void abandonNewVersion();

  /**
   * Deactivates the node.  This marks its content as inactive, but still allows
   * it to be present if it has children.  Throws if called while a version is
   * open.  To reactivate, call <code>restoreLastVersion()</code> or make a new
   * version.
   * @throws UnsupportedOperationException
   */
  public void deactivateContent();

  /**
   * Marks the node as deleted.  This also calls <code>deactivateContent()</code>.
   * To reactivate, call <code>restoreLastVersion()</code>
   * or make a new version (which gives it content), or call <code>markAsNotDeleted()</code>.
   */
  public void markAsDeleted();

  /**
   * Marks the node as no longer deleted.  This reactivates any content also.
   */
  public void markAsNotDeleted();

  /**
   * Returns the current version.  This is the open version when writing,
   * and the one accessed by <code>getNodeInfo()</code>.  If inactive,
   * returns -1. Throws an exception if called on a content-less node.
   * @return the current version
   * @throws UnsupportedOperationException
   */
  public int getCurrentVersion();

  /**
   * Reverts to the last version.  Throws if called on a node without content
   * or only one version (and active).  Can be used to reactivate inactive nodes
   * or deleted nodes.
   * @throws UnsupportedOperationException
   */
  public void restoreLastVersion();

  /**
   * Return an array of RepositoryNodeVersion for all versions of
   * content/props at this node.  The result is sorted from most to least
   * recent; the RepositoryNodeVersion for current version is the first
   * element in the array.
   * @throws UnsupportedOperationException if node has no versions
   * @return array of {@link RepositoryNodeVersion}
   */
  public RepositoryNodeVersion[] getNodeVersions();

  /**
   * Return an array of RepositoryNodeVersion for the most recent
   * <code>maxVersions</code> versions of content/props at this node.  The
   * result is sorted from most to least recent; the RepositoryNodeVersion
   * for current version is the first element in the array.
   * @throws UnsupportedOperationException if node has no versions
   * @return array of {@link RepositoryNodeVersion}
   */
  public RepositoryNodeVersion[] getNodeVersions(int maxVersions);

  /**
   * Return a RepositoryNodeVersion for the specified content version
   * @throws UnsupportedOperationException if node has no versions
   * @return a {@link RepositoryNodeVersion} bound to the specified version
   */
  public RepositoryNodeVersion getNodeVersion(int version);

  /**
   * Return a <code>RepositoryNodeContents</code> object which accesses the
   * content in the cache and its properties.  Throws if called on a content-less
   * or inactive node.
   * @return an {@link RepositoryNodeContents} object from which the contents of
   *         the cache can be read.
   * @throws UnsupportedOperationException
   */
  public RepositoryNodeContents getNodeContents();

  /**
   * Return an <code>OutputStream</code> object which writes to a new version
   * in the cache.
   * Throws an exception if called before <code>makeNewVersion()</code> or
   * called twice.
   * @return an <code>OutputStream</code> object to which the new contents can be
   * written.
   * @throws UnsupportedOperationException
   * @see RepositoryNode#makeNewVersion()
   */
  public OutputStream getNewOutputStream();

  /**
   * Stores the properties for a new version of the cache.  Throws an exception
   * if called before <code>makeNewVersion()</code> or called twice.
   * @param newProps a <code>Properties</code> object containing the headers of
   *         the new version being cached.
   * @throws UnsupportedOperationException
   * @see RepositoryNode#makeNewVersion()
   */
  public void setNewProperties(Properties newProps);
  
  /**
   * Signal that the given peers have agreed with this node, creating an
   * agreement history file if necessary.
   * @param peers A collection of peers for which to signal agreement.
   */
  public void signalAgreement(Collection peers);
  
  /**
   * Returns true if the PeerIdentity has ever agreed with this node during
   * a poll.
   * @param peer The peer to check for agreement with this node.
   */
  public boolean hasAgreement(PeerIdentity peer);

  /**
   * RepositoryNodeContents is used to obtain Properties and InputStream.
   * This is intended to ensure props and stream reflect a consistent view
   * of a single version, but see the comments in
   * RepositoryNodeImpl.RepositoryNodeContentsImpl.
   * @see RepositoryNodeImpl.RepositoryNodeContentsImpl
   */
  public interface RepositoryNodeContents {
    /** Return an InputStream */
    public InputStream getInputStream();

    /** Return the properties */
    public Properties getProperties();

    /** Release resources, close any cached open stream. */
    public void release();
  }
}
