/*
 * $Id: GenericFileCachedUrlSet.java,v 1.35 2003-04-23 00:55:52 aalto Exp $
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
import org.lockss.state.*;

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
  private NodeManager nodeManager;
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
    if (spec instanceof SingleNodeCachedUrlSetSpec) {
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
    //average with current estimate to minimize effect of extreme results
    if (lastDuration > 0) {
      lastDuration = (lastDuration + elapsed) / 2;
    } else {
      lastDuration = elapsed;
    }
    nodeManager.hashFinished(this, lastDuration);

    lastException = err;
  }

  public long estimatedHashDuration() {
    if (lastDuration>0) return lastDuration;
    else {
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
      SystemMetrics metrics = SystemMetrics.getSystemMetrics();
      try {
        bytesPerMs = metrics.getBytesPerMsHashEstimate(cush, hasher);
      } catch (IOException ie) {
        logger.error("Couldn't finish estimating hash time: " + ie);
        return totalNodeSize * BYTES_PER_MS_DEFAULT;
      }
      if (bytesPerMs == 0) {
        logger.warning("Couldn't estimate hash time: getting 0");
        return totalNodeSize * BYTES_PER_MS_DEFAULT;
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
  /**
   * Iterator over all the elements in a CachedUrlSet
   */
  private class CUSIterator implements Iterator {
    //Stack of flatSetIterators at each tree level
    LinkedList stack = new LinkedList();

    //if null, we have to look for nextElement
    private CachedUrlSetNode nextElement = null;

    public CUSIterator() {
      if (!((spec instanceof RangeCachedUrlSetSpec) &&
	    (((RangeCachedUrlSetSpec)spec).getLowerBound()!=null))) {
	nextElement = GenericFileCachedUrlSet.this;
      }
      stack.addFirst(flatSetIterator());
    }

    public void remove() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public boolean hasNext() {
      return findNextElement() != null;
    }

    public Object next() {
      Object foo = findNextElement();
      nextElement = null;

      if (foo != null) {
	return foo;
      }
      throw new NoSuchElementException();
    }

    /**
     * Does a pre-order traversal of the CachedUrlSet tree
     * @return a {@link CachedUrlSetNode}
     */
    private CachedUrlSetNode findNextElement() {
      if (nextElement != null) {
	return nextElement;
      }
      while (true) {
	if (stack.isEmpty()) {
	  return null;
	}
	Iterator it = (Iterator)stack.getFirst();
	if (!it.hasNext()) {
	  //this iterator is exhausted, pop from stack
	  stack.removeFirst();
	} else {
	  CachedUrlSetNode curNode = (CachedUrlSetNode)it.next();

	  if (!curNode.isLeaf()) {
	    CachedUrlSet cus = (CachedUrlSet)curNode;
	    //push the iterator of this child node onto the stack
	    stack.addFirst(cus.flatSetIterator());
	  }
	  nextElement = curNode;
	  return nextElement;
	}
      }
    }
  }
}
