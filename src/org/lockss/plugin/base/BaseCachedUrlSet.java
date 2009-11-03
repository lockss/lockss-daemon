/*
 * $Id: BaseCachedUrlSet.java,v 1.25.8.2 2009-11-03 23:44:52 edwardsb1 Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.security.*;
import java.net.MalformedURLException;

import org.lockss.plugin.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.scheduler.*;
import org.lockss.repository.*;
import org.lockss.util.*;
import org.lockss.state.*;

/**
 * Base class for CachedUrlSets.  Utilizes the LockssRootRepository.
 * Plugins may extend this to get some common CachedUrlSet functionality.
 */
public class BaseCachedUrlSet implements CachedUrlSet {
  private static final int BYTES_PER_MS_DEFAULT = 100;
  static final double TIMEOUT_INCREASE = 1.5;

  private LockssDaemon theDaemon;
  private LockssRepository repository;
  private NodeManager nodeManager;
  private HashService hashService;
  protected static Logger logger = Logger.getLogger("CachedUrlSet");

 // int contentNodeCount = 0;
  long totalNodeSize = 0;
  protected ArchivalUnit au;
  protected Plugin plugin;
  protected CachedUrlSetSpec spec;

  /**
   * Must invoke this constructor in plugin subclass.
   * @param owner the AU to which it belongs
   * @param spec the CachedUrlSet's spec
   */
  public BaseCachedUrlSet(ArchivalUnit owner, CachedUrlSetSpec spec) {
    this.spec = spec;
    this.au = owner;
    plugin = owner.getPlugin();
    theDaemon = plugin.getDaemon();
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
    try {
      RepositoryNode node = repository.getNode(getUrl());
      if (node == null) {
	// avoid creating node just to answer no.
	return false;
      }
    } catch (MalformedURLException e) {
      return false;
    }
    CachedUrl cu = au.makeCachedUrl(getUrl());
    try {
    return (cu == null ? false : cu.hasContent());
    } finally {
      AuUtil.safeRelease(cu);
    }	    
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
      RepositoryNode node = repository.getNode(getUrl());
      return (node == null) ? false : node.isLeaf();
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
      if (intNode == null) {
	return CollectionUtil.EMPTY_ITERATOR;
      }
      Iterator children = intNode.listChildren(spec, false);
      while (children.hasNext()) {
        RepositoryNode child = (RepositoryNode)children.next();
        if (child.isLeaf()) {
          CachedUrl newUrl = au.makeCachedUrl(child.getNodeUrl());
          flatSet.add(newUrl);
        } else {
          CachedUrlSetSpec rSpec =
            new RangeCachedUrlSetSpec(child.getNodeUrl());
          CachedUrlSet newSet = au.makeCachedUrlSet(rSpec);
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

  public CachedUrlSetHasher getContentHasher(MessageDigest digest) {
    return contentHasherFactory(this, digest);
  }

  public CachedUrlSetHasher getNameHasher(MessageDigest digest) {
    return nameHasherFactory(this, digest);
  }

  // Hack: the err param is
  public void storeActualHashDuration(long elapsed, Exception err) {
    //only store estimate if it was a full hash (not ranged or single node)
    if (spec.isSingleNode() || spec.isRangeRestricted()) {
      return;
    }
    // don't adjust for exceptions, except time-out exceptions
    long currentEstimate =
      nodeManager.getNodeState(this).getAverageHashDuration();
    long newEst;

    logger.debug("storeActualHashDuration(" +
		 StringUtil.timeIntervalToString(elapsed) +
		 ", " + err + "), cur = " +
		 StringUtil.timeIntervalToString(currentEstimate));
    if (err!=null) {
      if (err instanceof HashService.Timeout
	  || err instanceof SchedService.Timeout) {
        // timed out - guess 50% longer next time
        if (currentEstimate > elapsed) {
          // But if the current estimate is longer than this one took, we
          // must have already adjusted it after this one was scheduled.
          // Don't adjust it again, to avoid it becoming huge due to a series
          // of timeouts
          return;
        }
        newEst = (long)(elapsed * TIMEOUT_INCREASE);
      }
      else if (err instanceof HashService.SetEstimate) {

	currentEstimate = -1;	 // force newEst to be set to elapsed below
	newEst = elapsed;	 // keep compiler from complaining that
				 // newEst might not have been set
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
    logger.debug("newEst = " + StringUtil.timeIntervalToString(newEst));
    if (newEst > 10 * Constants.HOUR) {
      logger.error("Unreasonably long hash estimate", new Throwable());
    }
    nodeManager.hashFinished(this, newEst);
  }

  public long estimatedHashDuration() {
    return hashService.padHashEstimate(makeHashEstimate());
  }

  private long makeHashEstimate() {
    RepositoryNode node;
    try {
      node = repository.getNode(spec.getUrl());
      if (node == null) {
	return 0;
      }
    } catch (MalformedURLException e) {
      return 0;
    }
    // if this is a single node spec, don't use standard estimation
    if (spec.isSingleNode()) {
      long contentSize = 0;
      if (!node.hasContent()) {
	return 0;
      }
      contentSize = node.getContentSize();
      return estimateFromSize(contentSize);
    }
    NodeState state = nodeManager.getNodeState(this);
    long lastDuration;
    if (state!=null) {
      lastDuration = state.getAverageHashDuration();
      if (lastDuration>0) {
	return lastDuration;
      }
    }
    // determine total size
    calculateNodeSize();
    lastDuration = estimateFromSize(totalNodeSize);
    // store hash estimate
    nodeManager.hashFinished(this, lastDuration);
    return lastDuration;
  }

  long estimateFromSize(long size) {
    SystemMetrics metrics = theDaemon.getSystemMetrics();
    long bytesPerMs = 0;
    bytesPerMs = metrics.getBytesPerMsHashEstimate();
    if (bytesPerMs > 0) {
      logger.debug("Estimate from size: " + size + "/" + bytesPerMs + " = " +
		   StringUtil.timeIntervalToString(size / bytesPerMs));
      return (size / bytesPerMs);
    } else {
      logger.warning("Hash speed estimate was 0, using default: " +
		     StringUtil.timeIntervalToString(size /
						     BYTES_PER_MS_DEFAULT));
      return size / BYTES_PER_MS_DEFAULT;
    }
  }

  protected CachedUrlSetHasher contentHasherFactory(CachedUrlSet owner,
                                                    MessageDigest digest) {
    return new GenericContentHasher(owner, digest);
  }
  protected CachedUrlSetHasher nameHasherFactory(CachedUrlSet owner,
                                                 MessageDigest digest) {
    return new GenericNameHasher(owner, digest);
  }

  void calculateNodeSize() {
    if (totalNodeSize==0) {
      try {
	RepositoryNode node = repository.getNode(getUrl());
        totalNodeSize = node.getTreeContentSize(spec, true);
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

  public int cusCompare(CachedUrlSet cus2) {
    // check that they're in the same AU
    if (!this.getArchivalUnit().equals(cus2.getArchivalUnit())) {
      return NO_RELATION;
    }
    CachedUrlSetSpec spec1 = this.getSpec();
    CachedUrlSetSpec spec2 = cus2.getSpec();
    String url1 = this.getUrl();
    String url2 = cus2.getUrl();

    // check for top-level urls
    if (spec1.isAu() || spec2.isAu()) {
      if (spec1.equals(spec2)) {
        return SAME_LEVEL_OVERLAP;
      } else if (spec1.isAu()) {
        return ABOVE;
      } else {
        return BELOW;
      }
    }

    if (!url1.endsWith(UrlUtil.URL_PATH_SEPARATOR)) {
      url1 += UrlUtil.URL_PATH_SEPARATOR;
    }
    if (!url2.endsWith(UrlUtil.URL_PATH_SEPARATOR)) {
      url2 += UrlUtil.URL_PATH_SEPARATOR;
    }
    if (url1.equals(url2)) {
      //the urls are on the same level; check for overlap
      if (spec1.isDisjoint(spec2)) {
        return SAME_LEVEL_NO_OVERLAP;
      } else {
        return SAME_LEVEL_OVERLAP;
      }
    } else if (spec1.subsumes(spec2)) {
      // parent
      return ABOVE;
    } else if (spec2.subsumes(spec1)) {
      // child
      return BELOW;
    } else if (spec2.isSingleNode()) {
      if (url1.startsWith(url2)) {
        return SAME_LEVEL_NO_OVERLAP;
      }
      // else, cus2 probably has a range which excludes url1
    } else if (spec1.isSingleNode()) {
      if (url2.startsWith(url1)) {
        return SAME_LEVEL_NO_OVERLAP;
      }
      // else, cus1 probably has a range which excludes url2
    }
    // no connection between the two urls
    return NO_RELATION;
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
