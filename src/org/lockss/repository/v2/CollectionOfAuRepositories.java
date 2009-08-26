/*
 * $Id: CollectionOfAuRepositories.java,v 1.1.2.3 2009-08-26 22:47:24 edwardsb1 Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.Map;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.repository.LockssRepositoryException;
import org.lockss.util.PlatformUtil.*;

/**
 * @author edwardsb
 *
 */
public interface CollectionOfAuRepositories {
  /**
   * A CollectionOfAuRepositories implementation reads all directories under the
   * given directory, looking for AUs of its type (either Unix repositories or 
   * JCR repositories.)  It gathers them and all information needed to generate 
   * them, and provides this map to the RepositoryManagerManager.
   * 
   * Each implementation knows how to find its space within the pool (“cache” or 
   * “v2cache” subdir.  Within that space (the collection) there will only be AUs 
   * of its type.
   *
   * @param dirSource -- the parent directory for all directories underneath.
   * @return Map<String, Object>:
   *    Given the name of an AU repository, it returns the directory where this repository
   *    is found.
   */
  public Map<String, File> listAuRepositories() throws IOException;
  
  /**
   * This method creates a new AU Repository in a given directory.
   * 
   * @param dirSource
   * @throws IOException
   */
  public void generateAuRepository(File dirSource) throws IOException;
  
  
  /**
   * This method opens an AU repository in a given directory.
   */
  public LockssAuRepository openAuRepository(ArchivalUnit au, File dirLocation) throws IOException, LockssRepositoryException;
  
  
  /**
   * Return the space used by this collection.
   * @throws UnsupportedException 
   */
  public DF getDF() throws UnsupportedException; 
}
