/*
 * $Id: RepositoryNode.java,v 1.2 2002-11-15 02:48:20 aalto Exp $
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
   * any data.
   */
  public void makeNewVersion();

  /**
   * Closes the new version to any further writing.  Should be called when done
   * storing data.
   */
  public void sealNewVersion();

  /**
   * Discards the currently open new version without writing.
   */
  public void abandonNewVersion();

  /**
   * Returns the current version.  This is the open version when writing,
   * and the one accessed by the <code>getInputStream()</code> and
   * <code>getProperties()</code>.
   * @return the current version
   */
  public int getCurrentVersion();

  /**
   * Return an <code>InputStream</code> object which accesses the
   * content in the cache.
   * @return an <code>InputStream</code> object from which the contents of
   *         the cache can be read.
   */
  public InputStream getInputStream();

  /**
   * Return a <code>Properties</code> object containing the headers of
   * the object in the cache.
   * @return a <code>Properties</code> object containing the headers of
   *         the original object being cached.
   */
  public Properties getProperties();

  /**
   * Return an <code>OutputStream</code> object which writes to a new version
   * in the cache.  <code>makeNewVersion()</code> must be called first.
   * @return an <code>OutputStream</code> object to which the new contents can be
   * written.
   * @see LeafNode#makeNewVersion()
   */
  public OutputStream getNewOutputStream();

  /**
   * Stores the properties for a new version of the cache.  <code>makeNewVersion()</code>
   * must be called first.
   * @param newProps a <code>Properties</code> object containing the headers of
   *         the new version being cached.
   * @see LeafNode#makeNewVersion()
   */
  public void setNewProperties(Properties newProps);}
