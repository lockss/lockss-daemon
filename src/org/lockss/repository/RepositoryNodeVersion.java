/*
 * $Id$
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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
 * RepositoryNodeVersion represents a specific version of content at a
 * node. */
public interface RepositoryNodeVersion {
  /**
   * Returns the version number of this version.
   * @return the current version
   */
  public int getVersion();

  /**
   * Returns true if this version exists.  For the current version, this is
   * equivalent to {@link RepositoryNode#hasContent()} */
  public boolean hasContent();

  /**
   * Returns the content of this content version
   * @return size
   * @throws LockssRepository.RepositoryStateException
   */
  public long getContentSize();

  /**
   * Return a <code>RepositoryNodeContents</code> object which accesses the
   * content in the cache and its properties.  Throws if called on a
   * content-less or inactive node.
   * @return an {@link RepositoryNode.RepositoryNodeContents} object from
   * which the contents of the cache can be read.
   * @throws LockssRepository.RepositoryStateException
   */
  public RepositoryNode.RepositoryNodeContents getNodeContents();

}
