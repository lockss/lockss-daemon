/*
 * $Id: BaseCachedUrlSet.java,v 1.9 2003-09-26 23:52:16 eaalto Exp $
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

package org.lockss.plugin.base;

import java.io.*;
import java.util.*;
import java.security.*;
import java.net.MalformedURLException;
import org.lockss.plugin.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.repository.*;
import org.lockss.util.*;
import org.lockss.protocol.*;
import org.lockss.plugin.base.*;
import org.lockss.state.*;
import org.lockss.poller.PollManager;

/**
 * Base class for CachedUrlSets.  Utilizes the LockssRepository.
 * Plugins may extend this to get some common CachedUrlSet functionality.
 */
public class BaseCachedUrlSet implements CachedUrlSet {
  private static final int BYTES_PER_MS_DEFAULT = 100;
  static final double TIMEOUT_INCREASE = 1.5;

  private Exception lastException = null;
  private LockssDaemon theDaemon;
  private LockssRepository repository;
  private NodeManager nodeManager;
  private HashService hashService;
  protected static Logger logger = Logger.getLogger("CachedUrlSet");

 // int contentNodeCount = 0;
  long totalNodeSize = 0;
  protected ArchivalUnit au;
  protected CachedUrlSetSpec spec;

  /**
   * Must invoke this constructor in plugin subclass.
   * @param owner the AU to which it belongs
   * @param spec the CachedUrlSet's spec
   */
  public BaseCachedUrlSet(ArchivalUnit owner, CachedUrlSetSpec spec) {
    this.spec = spec;
    this.au = owner;
    theDaemon = owner.getPlugin().getDaemon();
    repository = theDaemon.getLockssRepository(owner);
    nodeManager = theDaemon.getNodeManager(owner);
    hashService = theDaemon.getHashService();
  }

  /**
   * Return the CachedUrlSetSpec
   * @return the spec
   */
  public CachedUrlSetSpec getSpec() {
    return spec;
  }

  /**
   * Return the enclosing ArchivalUnit
   * @return the AU
   */
  public ArchivalUnit getArchivalUnit() {
    return au;
  }

  public boolean hasContent() {
    CachedUrl cu = au.getPlugin().makeCachedUrl(this, getUrl());
    return (cu == null ? false : cu.hasContent());
  }

  /**
   * Return true if the url falls within the scope of this CachedUrlSet,
   * whether it is present in the cache or not
   * @param url the url to test
   * @return true if is within the scope
   */
  public boolean containsUrl(String url) {
    return spec.matches(url);
  }

  /**
   * Overridden to return the toString() method of the CachedUrlSetSpec.
   * @return the spec string
   */
  public String toString() {
    return "[BCUS: "+spec+"]";
  }

  public String getUrl() {
    return spec.getUrl();
  }

  public int getType() {
    return CachedUrlSetNode.TYPE_CACHED_URL_SET;
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
          Plugin plugin = au.getPlugin();
          CachedUrl newUrl = plugin.makeCachedUrl(this, child.getNodeUrl());
          flatSet.add(newUrl);
        } else {
          CachedUrlSetSpec rSpec =
            new RangeCachedUrlSetSpec(child.getNodeUrl());
          Plugin plugin = au.getPlugin();
          CachedUrlSet newSet = plugin.makeCachedUrlSet(au, rSpec);
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
    return new CusIterator();
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
        SystemMetrics metrics = theDaemon.getSystemMetrics();
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
      SystemMetrics metrics = theDaemon.getSystemMetrics();
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

  /**
   * Overrides Object.hashCode().
   * Returns the hashcode of the spec.
   * @return the hashcode
   */
  public int hashCode() {
    return spec.hashCode();
  }

  /**
   * Overrides Object.equals().
   * Returns the equals() of the specs.
   * @param obj the object to compare to
   * @return true if the specs are equal
   */
  public boolean equals(Object obj) {
    if (obj instanceof CachedUrlSet) {
      CachedUrlSet cus = (CachedUrlSet)obj;
      return spec.equals(cus.getSpec());
    } else {
      return false;
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
protected class CusIterator implements Iterator {
  //Stack of flatSetIterators at each tree level
  LinkedList stack = new LinkedList();

  //if null, we have to look for nextElement
  private CachedUrlSetNode nextElement = null;

  public CusIterator() {
    if (!spec.isRangeRestricted()) {
      nextElement = BaseCachedUrlSet.this;
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
    Object element = findNextElement();
    nextElement = null;

    if (element != null) {
      return element;
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
