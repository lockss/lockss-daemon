/*
 * $Id: RepositoryNode.java,v 1.1 2002-10-31 01:52:41 aalto Exp $
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

/**
 * RepositoryNode is used to store the contents and
 * meta-information of urls being cached.
 */
public interface RepositoryNode {
  /**
   * Returns the url represented by this entry.
   * @return the entry's url
   */
  public String getNodeUrl();

  /**
   * Identify if the entry represents a leaf
   * @return true if the entry represents a leaf
   */
  public boolean isLeaf();

  /**
   * Returns the state information for the entry.
   * @return state properties of the entry
   */
  public Properties getState();

  /**
   * Writes new state information for the entry.
   * @param newProps the new state information
   */
  public void storeState(Properties newProps);
}
