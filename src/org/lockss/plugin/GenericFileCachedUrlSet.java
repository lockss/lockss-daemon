/*
 * $Id: GenericFileCachedUrlSet.java,v 1.46 2003-06-20 22:34:51 claire Exp $
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
import org.lockss.state.*;

/**
 * This is an abstract CachedUrlSet implementation which uses the {@link LockssRepository}.
 *
 * @author  Emil Aalto
 * @version 0.0
 */
public class GenericFileCachedUrlSet extends BaseCachedUrlSet {
  private static final int BYTES_PER_MS_DEFAULT = 100;
  static final double TIMEOUT_INCREASE = 1.5;

  private Exception lastException = null;
  private LockssRepository repository;
  private NodeManager nodeManager;
  private HashService hashService;
  protected static Logger logger = Logger.getLogger("CachedUrlSet");

 // int contentNodeCount = 0;
  long totalNodeSize = 0;

  public GenericFileCachedUrlSet(ArchivalUnit owner, CachedUrlSetSpec spec) {
    super(owner, spec);
    LockssRepositoryService repService = (LockssRepositoryService)
        LockssDaemon.getManager(LockssDaemon.LOCKSS_REPOSITORY_SERVICE);
    repository = repService.getLockssRepository(owner);
    NodeManagerService nodeService = (NodeManagerService)
        LockssDaemon.getManager(LockssDaemon.NODE_MANAGER_SERVICE);
    nodeManager = nodeService.getNodeManager(owner);
    hashService = (HashService)LockssDaemon.getManager(LockssDaemon.HASH_SERVICE);
  }

  public boolean isLeaf() {
    try {
      return repository.getNode(getUrl()).isLeaf();
    } catch (MalformedURLException mue) {
      logger.error("Bad url in spec: " + getUrl());
      throw new LockssRepository.RepositoryStateException("Bad url in spec: "
							  +getUrl());
    }
  }

  public Iterator flatSetIterator() {
    if (spec.isSingleNode()) {
      return CollectionUtil.EMPTY_ITERATOR;
    }
    TreeSet flatSet = new TreeSet(new UrlComparator());
    String prefix = spec.getUrl();
    try {
      RepositoryNode intNode = repository.getNode(prefix);
      Iterator children = intNode.listNodes(spec, false);
      while (children.hasNext()) {
        RepositoryNode child = (RepositoryNode)children.next();
        if (child.isLeaf()) {
          CachedUrl newUrl =
	    ((BaseArchivalUnit)au).cachedUrlFactory(this, child.getNodeUrl());
          flatSet.add(newUrl);
        } else {
	  CachedUrlSetSpec rSpec =
	    new RangeCachedUrlSetSpec(child.getNodeUrl());
          CachedUrlSet newSet = ((BaseArchivalUnit)au).makeCachedUrlSet(rSpec);
          flatSet.add(newSet);
        }
      }
    } catch (MalformedURLException mue) {
      logger.error("Bad url in spec: "+prefix);
      throw new RuntimeException("Bad url in spec: "+prefix);
    }
    return flatSet.iterator();
  }

  /**
   * This returns an iterator over all nodes in the CachedUrlSet.  This
   * includes the node itself if either it's got a RangedCachedUrlSetSpec with
   * no range or a SingleNodeCachedUrlSetSpec
   * @return an {@link Iterator}
   */
  public Iterator contentHashIterator() {
    return new CUSIterator();
  }

  public CachedUrlSetHasher getContentHasher(MessageDigest hasher) {
    return contentHasherFactory(this, hasher);
  }

  public CachedUrlSetHasher getNameHasher(MessageDigest hasher) {
    return nameHasherFactory(this, hasher);
  }

  public void storeActualHashDuration(long elapsed, Exception err) {
    //only store estimate if it was a full hash (not ranged or single node)
    if (spec.isSingleNode() || spec.isRangeRestricted()) {
      return;
    }
    // don't adjust for exceptions, except time-out exceptions
    long currentEstimate =
      nodeManager.getNodeState(this).getAverageHashDuration();
    long newEst;

    lastException = err;
    if (err!=null) {
      if (err instanceof HashService.Timeout) {
        // timed out - guess 50% longer next time
	if (currentEstimate > elapsed) {
	  // But if the current estimate is longer than this one took, we
	  // must have already adjusted it after this one was scheduled.
	  // Don't adjust it again, to avoid it becoming huge due to a series
	  // of timeouts
	  return;
	}
	newEst = (long)(elapsed * TIMEOUT_INCREASE);
      } else {
	// other error - don't update estimate
	return;
      }
    } else {
      //average with current estimate to minimize effect of extreme results
      if (currentEstimate > 0) {
        newEst = (currentEstimate + elapsed) / 2;
      }
      else {
        newEst = elapsed;
      }
    }
    nodeManager.hashFinished(this, newEst);
  }

  public long estimatedHashDuration() {
    return hashService.padHashEstimate(makeHashEstimate());
  }

  private long makeHashEstimate() {
    // if this is a single node spec, don't use standard estimation
    if (spec.isSingleNode()) {
      long contentSize = 0;
      try {
        RepositoryNode node = repository.getNode(spec.getUrl());
        if (!node.hasContent()) {
          return 0;
        }
        contentSize = node.getContentSize();
        MessageDigest hasher = LcapMessage.getDefaultHasher();
        CachedUrlSetHasher cush = contentHasherFactory(this, hasher);
        SystemMetrics metrics = SystemMetrics.getSystemMetrics();
        long bytesPerMs = metrics.getBytesPerMsHashEstimate(cush, hasher);
        if (bytesPerMs == 0) {
          logger.warning("Couldn't estimate hash time: getting 0");
          return contentSize / BYTES_PER_MS_DEFAULT;
        }
        return (contentSize / bytesPerMs);
      } catch (Exception e) {
        logger.error("Couldn't finish estimating hash time: " + e);
        return contentSize / BYTES_PER_MS_DEFAULT;
      }
    }
    long lastDuration = nodeManager.getNodeState(this).getAverageHashDuration();
    if (lastDuration>0) {
      return lastDuration;
    } else {
      NodeState state = nodeManager.getNodeState(this);
      if (state!=null) {
        lastDuration = state.getAverageHashDuration();
        if (lastDuration>0) {
          return lastDuration;
        }
      }
      // determine total size
      calculateNodeSize();
      MessageDigest hasher = LcapMessage.getDefaultHasher();
      CachedUrlSetHasher cush = contentHasherFactory(this, hasher);
      long bytesPerMs = 0;
      SystemMetrics metrics = (SystemMetrics)
          LockssDaemon.getManager(LockssDaemon.SYSTEM_METRICS);
      try {
        bytesPerMs = metrics.getBytesPerMsHashEstimate(cush, hasher);
      } catch (IOException ie) {
        logger.error("Couldn't finish estimating hash time: " + ie);
        return totalNodeSize / BYTES_PER_MS_DEFAULT;
      }
      if (bytesPerMs == 0) {
        logger.warning("Couldn't estimate hash time: getting 0");
        return totalNodeSize / BYTES_PER_MS_DEFAULT;
      }
      lastDuration = (long) (totalNodeSize / bytesPerMs);
      // store hash estimate
      nodeManager.hashFinished(this, lastDuration);
      return lastDuration;
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

  void calculateNodeSize() {
    if (totalNodeSize==0) {
      try {
        totalNodeSize = repository.getNode(spec.getUrl()).getTreeContentSize(spec);
      } catch (MalformedURLException mue) {
        // this shouldn't happen
        logger.error("Malformed URL exception on "+spec.getUrl());
      }
    }
  }

  private static class UrlComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      String prefix = null;
      String prefix2 = null;
      if ((o1 instanceof CachedUrlSetNode)
	  && (o2 instanceof CachedUrlSetNode)) {
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
