/*
 * $Id: AuNodeImpl.java,v 1.3 2003-04-01 00:08:12 aalto Exp $
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
import java.net.MalformedURLException;
import org.lockss.util.Logger;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.util.Deadline;
import java.net.URL;
import org.lockss.plugin.AuUrl;

/**
 * AuNode is used to represent the top-level contents of an ArchivalUnit.
 * It overrides specific functions in RepositoryNodeImpl to list files properly.
 */
public class AuNodeImpl extends RepositoryNodeImpl {
  AuNodeImpl(String url, String nodeLocation,
                     LockssRepositoryImpl repository) {
    super(url, nodeLocation, repository);
  }

  /**
   * Overriden to return false.
   * @return false
   */
  public boolean hasContent() {
    return false;
  }

  /**
   * Overriden to return false.
   * @return false
   */
  public boolean isInactive() {
    return false;
  }

  /**
   * Overriden to return false.
   * @return false
   */
  public boolean isLeaf() {
    return false;
  }

  public Iterator listNodes(CachedUrlSetSpec filter, boolean includeInactive) {
    if (nodeRootFile==null) loadNodeRoot();
    if (!nodeRootFile.exists()) {
      logger.error("No cache directory located for: "+url);
      throw new LockssRepository.RepositoryStateException("No cache directory located.");
    }
    TreeMap childM = new TreeMap();
    // for all directories beneath Au level
    File[] urlDirs = nodeRootFile.listFiles();
    for (int ii=0; ii<urlDirs.length; ii++) {
      File urlDir = urlDirs[ii];
      if (!urlDir.isDirectory()) continue;
      // get URL name
      String urlStr = urlDir.getName();
      File[] protocolDirs = urlDir.listFiles();
      // for all subdirectores beneath URL
      for (int jj=0; jj<protocolDirs.length; jj++) {
        File protocolDir = protocolDirs[jj];
        if (!protocolDir.isDirectory()) continue;
        // get protocol name
        String protocolStr = protocolDir.getName();
        // use as top-level, with protocol-URL as url
        try {
          String dirName = new URL(protocolStr, urlStr, "").toString();
          RepositoryNode node = repository.getNode(dirName);
          // add all nodes which are internal or active leaves
          if (!node.isLeaf() || (!node.isInactive()) || includeInactive) {
            childM.put(dirName, repository.getNode(dirName));
          }
        } catch (MalformedURLException mue) {
          // this can safely skip bad files because they will
          // eventually be trimmed by the repository integrity checker
          // and the content will be replaced by a poll repair
          logger.error("Malformed url while creating top-level CUS: "+
                       "protocol '"+protocolStr+"', url '"+urlStr+"'");
        }
      }
    }
    ArrayList childL = new ArrayList(childM.size());
    Iterator entriesIt = childM.entrySet().iterator();
    while (entriesIt.hasNext()) {
      Map.Entry entry = (Map.Entry)entriesIt.next();
      childL.add(entry.getValue());
    }
    return childL.iterator();
  }

  /**
   * Overriden to throw an UnsupportedOperationException.
   */
  public synchronized void makeNewVersion() {
    throw new UnsupportedOperationException("Cannot store content for a top-level AuCUS.");
  }

  /**
   * Overriden to throw an UnsupportedOperationException.
   */
  public synchronized void deactivate() {
    throw new UnsupportedOperationException("Can't deactivate a top-level AuCUS.");
  }
}
