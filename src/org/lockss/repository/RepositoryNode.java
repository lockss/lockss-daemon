/*
 * $Id: RepositoryNode.java,v 1.6 2002-12-17 01:57:50 aalto Exp $
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

import java.io.*;
import java.util.*;
import org.lockss.daemon.CachedUrlSetSpec;

/**
 * RepositoryNode is used to store the contents and
 * meta-information of urls being cached.
 */
public interface RepositoryNode {
  /**
   * Returns the url represented by this node.
   * @return the node's url
   */
  public String getNodeUrl();

  /**
   * Determines if the node has stored content.
   * @return true if the node has content
   */
  public boolean hasContent();

  /**
   * Returns the size of the current version of stored cache.  Throws an
   * exception if called on a content-less node.
   * @return size
   * @throws UnsupportedOperationException
   */
  public long getContentSize();

  /**
   * Returns the state information for the node.
   * @return state properties of the node
   */
  public Properties getState();

  /**
   * Writes new state information for the node.
   * @param newProps the new state information
   */
  public void storeState(Properties newProps);

  /**
   * Returns the immediate children of the entry, possibly filtered (null
   * indicates no filtering).  Includes leaf and internal nodes.
   * @param filter a spec to determine which urls to return
   * @return an <code>Iterator</code> of RepositoryNode objects
   */
  public Iterator listNodes(CachedUrlSetSpec filter);

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
   * Returns the current version.  This is the open version when writing,
   * and the one accessed by <code>getNodeInfo()</code>.  Throws an exception
   * if called on a content-less node.
   * @return the current version
   * @throws UnsupportedOperationException
   */
  public int getCurrentVersion();

  /**
   * Return a <code>RepositoryNodeContents</code> object which accesses the
   * content in the cache and its properties.
   * @return an {@link RepositoryNodeContents} object from which the contents of
   *         the cache can be read.
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
   * RepositoryNodeContents is a struct containing a matched inputstream
   * and properties.
   */
  public class RepositoryNodeContents {
    public InputStream input;
    public Properties props;

    public RepositoryNodeContents(InputStream input, Properties props) {
      this.input = input;
      this.props = props;
    }
  }
}
