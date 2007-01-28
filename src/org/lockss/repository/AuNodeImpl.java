/*
 * $Id: AuNodeImpl.java,v 1.12 2007-01-28 05:45:06 tlipkis Exp $
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

import java.io.File;
import java.net.*;
import java.util.*;

import org.lockss.config.*;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.util.PlatformUtil;

/**
 * AuNode is used to represent the top-level contents of an ArchivalUnit.
 * It overrides specific functions in RepositoryNodeImpl to list files properly.
 */
public class AuNodeImpl extends RepositoryNodeImpl {
  /** Change in content size that causes disk (du) size to be recalculated */
  static final String PARAM_RECOMPUTE_DU_DELTA = Configuration.PREFIX +
    "repository.recomputeDuDelta";
  static final long DEFAULT_RECOMPUTE_DU_DELTA = 100 * 1024;


  static final String DU_SIZE_PROPERTY = "node.du.size";
  static final String DU_CONTENT_SIZE_PROPERTY = "node.du.contentSize";


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
  public boolean isContentInactive() {
    return false;
  }

  /**
   * Overriden to return false.
   * @return false
   */
  public boolean isLeaf() {
    return false;
  }

  /**
   * The overridden version has special code to properly determine the top-level
   * children of the AU.
   * @param filter the filter, if any
   * @param includeInactive true iff inactive are to be included
   * @return Iterator the children
   */
  protected List getNodeList(CachedUrlSetSpec filter, boolean includeInactive) {
    if (nodeRootFile==null) initNodeRoot();
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
          if (!node.isLeaf() || (!node.isContentInactive()) || includeInactive) {
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
    return childL;
  }

  /** Return the disk space used by the AU (including all overhead), if it
   * is known, else return -1 */
  public long getDiskUsage(boolean calcIfUnknown) {
    // size is cached until content size changes significantly
    ensureCurrentInfoLoaded();
    String cachedDu = nodeProps.getProperty(DU_SIZE_PROPERTY);
    String cachedDuContent = nodeProps.getProperty(DU_CONTENT_SIZE_PROPERTY);
    long content = getTreeContentSize(null, calcIfUnknown);
    if (isPropValid(cachedDu) && isPropValid(cachedDuContent)) {
      long maxDelta =
        CurrentConfig.getCurrentConfig().getSize(PARAM_RECOMPUTE_DU_DELTA,
                                                 DEFAULT_RECOMPUTE_DU_DELTA);
      long duContent =  Long.parseLong(cachedDuContent);
      if (Math.abs(content - duContent) < maxDelta) {
	long du =  Long.parseLong(cachedDu);
	return du;
      }
    }
    if (!calcIfUnknown) {
      repository.queueSizeCalc(this);
      return -1;
    }
    logger.debug("Recomputing du for " + nodeLocation);
    PlatformUtil platInfo = PlatformUtil.getInstance();
    long du = platInfo.getDiskUsage(nodeLocation);
    if (du > 0) {
      nodeProps.setProperty(DU_SIZE_PROPERTY, Long.toString(du));
      nodeProps.setProperty(DU_CONTENT_SIZE_PROPERTY,
			    Long.toString(content));
      writeNodeProperties();
    }
    return du;
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
  public synchronized void deactivateContent() {
    throw new UnsupportedOperationException("Can't deactivate a top-level AuCUS.");
  }
}
