/*
 * $Id: GenericFileCachedUrlSet.java,v 1.25 2003-02-26 19:33:46 aalto Exp $
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

package org.lockss.plugin;

import java.io.*;
import java.util.*;
import java.security.*;
import java.net.MalformedURLException;

import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.repository.*;
import org.lockss.util.*;
import org.lockss.poller.PollManager;
import org.lockss.protocol.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;

/**
 * This is an abstract CachedUrlSet implementation which uses the {@link LockssRepository}.
 *
 * @author  Emil Aalto
 * @version 0.0
 */
public class GenericFileCachedUrlSet extends BaseCachedUrlSet {
  private static final int BYTES_PER_MS_DEFAULT = 100;

  private long lastDuration = 0;
  private Exception lastException = null;
  private LockssRepository repository;
  protected static Logger logger = Logger.getLogger("CachedUrlSet");

  int contentNodeCount = 0;
  long totalNodeSize = 0;

  public GenericFileCachedUrlSet(ArchivalUnit owner, CachedUrlSetSpec spec) {
    super(owner, spec);
    LockssRepository rep = (LockssRepository)LockssDaemon.getManager(
        LockssDaemon.LOCKSS_REPOSITORY);
    repository = rep.repositoryFactory(owner);
  }

  public boolean isLeaf() {
    try {
      return repository.getNode(getUrl()).isLeaf();
    } catch (MalformedURLException mue) {
      logger.error("Bad url in spec: " + getUrl());
      throw new LockssRepository.RepositoryStateException("Bad url in spec: "+getUrl());
    }
  }

  public Iterator flatSetIterator() {
    TreeSet flatSet = new TreeSet(new UrlComparator());
    String prefix = spec.getUrl();
    try {
      RepositoryNode intNode = repository.getNode(prefix);
      Iterator children = intNode.listNodes(spec, false);
      while (children.hasNext()) {
        RepositoryNode child = (RepositoryNode)children.next();
        CachedUrlSetSpec rSpec =
            new RangeCachedUrlSetSpec(child.getNodeUrl());
        if (child.isLeaf()) {
          CachedUrl newUrl = ((BaseArchivalUnit)au).cachedUrlFactory(this,
              child.getNodeUrl());
          flatSet.add(newUrl);
        } else {
          CachedUrlSet newSet = ((BaseArchivalUnit)au).makeCachedUrlSet(
              rSpec);
          flatSet.add(newSet);
        }
      }
    } catch (MalformedURLException mue) {
      logger.error("Bad url in spec: "+prefix);
    } catch (Exception e) {
      // this shouldn't occur
      logger.error(e.getMessage());
    }
    return flatSet.iterator();
  }

  public Iterator treeIterator() {
    contentNodeCount = 0;
    totalNodeSize = 0;
    TreeSet treeSet = new TreeSet(new UrlComparator());

    String prefix = spec.getUrl();
    try {
      RepositoryNode intNode = repository.getNode(prefix);
      Iterator children = intNode.listNodes(spec, false);
      while (children.hasNext()) {
        // add all nodes to tree iterator, regardless of content
        RepositoryNode child = (RepositoryNode)children.next();
        CachedUrlSetSpec rSpec =
            new RangeCachedUrlSetSpec(child.getNodeUrl());
        if (child.isLeaf()) {
          CachedUrl newUrl = ( (BaseArchivalUnit) au).cachedUrlFactory(this,
              child.getNodeUrl());
          treeSet.add(newUrl);
        } else {
          CachedUrlSet newSet = ( (BaseArchivalUnit) au).makeCachedUrlSet(
              rSpec);
          treeSet.add(newSet);
        }
        if (child.hasContent()) {
          contentNodeCount++;
          totalNodeSize += child.getContentSize();
        }
        recurseLeafFetch(child, treeSet);
      }
    }
    catch (MalformedURLException mue) {
      logger.error("Bad url in spec: " + prefix);
    }
    catch (Exception e) {
      // this shouldn't occur
      logger.error(e.getMessage());
    }
    return treeSet.iterator();
  }

  private void recurseLeafFetch(RepositoryNode node, TreeSet set) {
    Iterator children = node.listNodes(null, false);
    while (children.hasNext()) {
      // add all nodes to tree iterator, regardless of content
      RepositoryNode child = (RepositoryNode)children.next();
      CachedUrlSetSpec rSpec =
          new RangeCachedUrlSetSpec(child.getNodeUrl());
      if (child.isLeaf()) {
        CachedUrl newUrl = ((BaseArchivalUnit)au).cachedUrlFactory(this,
            child.getNodeUrl());
        set.add(newUrl);
      } else {
        CachedUrlSet newSet = ((BaseArchivalUnit)au).makeCachedUrlSet(
            rSpec);
        set.add(newSet);
      }
      if (child.hasContent()) {
        contentNodeCount++;
        totalNodeSize += child.getContentSize();
      }
      recurseLeafFetch(child, set);
    }
  }

  public CachedUrlSetHasher getContentHasher(MessageDigest hasher) {
    return contentHasherFactory(this, hasher);
  }

  public CachedUrlSetHasher getNameHasher(MessageDigest hasher) {
    return nameHasherFactory(this, hasher);
  }

  public void storeActualHashDuration(long elapsed, Exception err) {
    //average with current estimate to minimize effect of extreme results
    if (lastDuration > 0) {
      lastDuration = (lastDuration + elapsed) / 2;
    } else {
      lastDuration = elapsed;
    }
    lastException = err;
  }

  public long estimatedHashDuration() {
    if (lastDuration>0) return lastDuration;
    else {
      // determine number of content nodes and total size
      calculateNodeCountAndSize();
      MessageDigest hasher = LcapMessage.getDefaultHasher();
      CachedUrlSetHasher cush = contentHasherFactory(this, hasher);
      long bytesPerMs = 0;
      SystemMetrics metrics = SystemMetrics.getSystemMetrics();
      try {
        bytesPerMs = metrics.getBytesPerMsHashEstimate(cush, hasher);
      } catch (IOException ie) {
        logger.error("Couldn't finish estimating hash time: "+ie);
        return totalNodeSize * BYTES_PER_MS_DEFAULT;
      }
      if (bytesPerMs==0) {
        logger.error("Couldn't estimate hash time: getting 0");
        return totalNodeSize * BYTES_PER_MS_DEFAULT;
      }
      return (long)(totalNodeSize / bytesPerMs);
    }
  }

  public CachedUrl makeCachedUrl(String url) {
    return ((BaseArchivalUnit)au).cachedUrlFactory(this, url);
  }

  public UrlCacher makeUrlCacher(String url) {
    return ((BaseArchivalUnit)au).urlCacherFactory(this, url);
  }

  protected CachedUrlSetHasher contentHasherFactory(CachedUrlSet owner,
						    MessageDigest hasher) {
    return new GenericContentHasher(owner, hasher);
  }
  protected CachedUrlSetHasher nameHasherFactory(CachedUrlSet owner,
						 MessageDigest hasher) {
    return new GenericNameHasher(owner, hasher);
  }

  private void calculateNodeCountAndSize() {
    if ((contentNodeCount==0)||(totalNodeSize==0)) {
      // leafIterator calculates these when running
      treeIterator();
    }
  }

  private File createTempFile(String location, int size) {
    File tempFile = new File(location);
    return null;
  }

  private String getTempFileLocation(String file_name) {
    StringBuffer buffer = new StringBuffer();
    String location = "";
    buffer.append(location);
    if (!location.endsWith(File.separator)) {
      buffer.append(File.separator);
    }
    buffer.append(file_name);
    return buffer.toString();
  }

  private class UrlComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      String prefix = null;
      String prefix2 = null;
      if ((o1 instanceof CachedUrlSetNode) && (o2 instanceof CachedUrlSetNode)) {
        prefix = ((CachedUrlSetNode)o1).getUrl();
        prefix2 = ((CachedUrlSetNode)o2).getUrl();
      } else {
        throw new IllegalStateException("Bad object in iterator: " +
                                        o1.getClass() + "," +
                                        o2.getClass());
      }
      if (prefix.equals(prefix2)) {
        throw new UnsupportedOperationException("Comparing equal prefixes: "+prefix);
      }
      return prefix.compareTo(prefix2);
    }
  }
}
