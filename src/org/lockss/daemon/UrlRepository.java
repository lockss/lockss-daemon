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

package org.lockss.daemon;
import java.io.*;
import java.util.*;

/**
 * UrlRepository is used to store the contents and
 * meta-information of urls being cached.
 */
public interface UrlRepository {
  /**
   * Identify if the url represents a leaf
   * @param url the url in question
   * @return true if the url represents a leaf
   */
  public boolean isLeaf(String url);

  /**
   * Whether or not the url exists in the cacher.
   * @param url the url in question
   * @return true if the url represents an internal node or a cached leaf node
   */
  public boolean exists(String url);

  /**
   * Returns the state information for the url.
   * @param url the url in question
   * @return state properties of the url XXX not properties, what should this be?
   */
  public Properties getState(String url);

  /**
   * Writes new state information for the url.
   * @param url the url being updated
   * @param newProps the new state information XXX not properties, what should this be?
   */
  public void storeState(String url, Properties newProps);

  /**
   * Returns the immediate children of the url, possibly filtered (null
   * indicates no filtering).  Includes leaf and internal nodes.
   * @param url the parent url
   * @param filter a spec to determine which urls to return
   * @return an Iterator of the filtered children
   */
  public Iterator listUrls(String url, CachedUrlSetSpec filter);

  /**
   * Store a new version of the content from an input stream with associated
   * properties.  Does not overwrite older versions.
   * @param url     a <code>String</code> giving the content's url
   * @param input   an <code>InputStream</code> object from which the
   *                content can be read
   * @param headers a <code>Properties</code> object containing the
   *                relevant HTTP headers
   * @exception java.io.IOException on many possible I/O problems.
   */
  public void makeNewVersion(String url, InputStream input,
			     Properties headers) throws IOException;

  /**
   * Return an <code>InputStream</code> object which accesses the
   * content in the cache.  Returns null if not found or non-leaf.
   * @param url the leaf url being read
   * @return an <code>InputStream</code> object from which the contents of
   *         the cache can be read.
   */
  public InputStream getLeafInputStream(String url);

  /**
   * Return a <code>Properties</code> object containing the headers of
   * the object in the cache.  Returns null if not found or non-leaf.
   * @param url the leaf url being read
   * @return a <code>Properties</code> object containing the headers of
   *         the original object being cached.
   */
  public Properties getLeafProperties(String url);
}
