/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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
import de.schlichtherle.truezip.file.*;
// import de.schlichtherle.truezip.fs.*;

import org.lockss.plugin.*;
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.scheduler.*;
import org.lockss.repository.*;
import org.lockss.util.*;
import org.lockss.state.*;
import org.lockss.truezip.*;

/**
 * Base class for CachedUrlSets.  Utilizes the LockssRepository.
 * Plugins may extend this to get some common CachedUrlSet functionality.
 */
public class BaseCachedUrlSet implements CachedUrlSet {
  private static final int BYTES_PER_MS_DEFAULT = 100;
  static final double TIMEOUT_INCREASE = 1.5;

  private LockssDaemon theDaemon;
  private LockssRepository repository;
  private NodeManager nodeManager;
  private TrueZipManager trueZipManager;
  protected static Logger logger = Logger.getLogger("CachedUrlSet");

 // int contentNodeCount = 0;
  long totalNodeSize = 0;
  protected ArchivalUnit au;
  protected Plugin plugin;
  protected CachedUrlSetSpec spec;
  protected long excludeFilesUnchangedAfter = 0;

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
  public Iterator<CachedUrlSetNode> contentHashIterator() {
    return new CusIterator();
  }

  public CuIterator getCuIterator() {
    return CuIterator.forCus(this);
  }

  public CuIterable getCuIterable() {
    return new CuIterable() {
      @Override
      protected CuIterator makeIterator() {
	return getCuIterator();
      }};
  }

  public CuIterator archiveMemberIterator() {
    return new ArcMemIterator();
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
    return theDaemon.getHashService().padHashEstimate(makeHashEstimate());
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

  public void setExcludeFilesUnchangedAfter(long date) {
    excludeFilesUnchangedAfter = date;
  }

  public long getExcludeFilesUnchangedAfter() {
    return excludeFilesUnchangedAfter;
  }

  protected boolean isExcludedByDate(CachedUrl cu) {
    if (excludeFilesUnchangedAfter <= 0) {
      return false;
    }
    Properties props = cu.getProperties();
    String fetched = props.getProperty(CachedUrl.PROPERTY_FETCH_TIME);
    if (StringUtil.isNullString(fetched)) {
      return false;
    }
    try {
      long fetchTime = Long.parseLong(fetched);
      return fetchTime <= excludeFilesUnchangedAfter;
    } catch (NumberFormatException ex) {
      logger.warning("Couldn't parse fetch date: " + fetched +
		  ", not excluding " + cu);
      return false;
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
      // This now happens on the first insertion into a TreeMap, and would
      // falsely trigger the prefix error below.
      if (o1 == o2) return 0;
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
      return StringUtil.preOrderCompareTo(prefix, prefix2);
    }
  }

  /**
   * Iterator over all the elements in a CachedUrlSet
   */
  public class CusIterator implements Iterator<CachedUrlSetNode> {
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

    public CachedUrlSetNode next() {
      CachedUrlSetNode element = findNextElement();
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
	  if (!spec.matches(curNode.getUrl())) {
	    continue;
	  }
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
  /**
   * Iterator over all the content files in a CachedUrlSet, including
   * archive members.  Unlike {@link CusIterator}, this returns {@link
   * CachedUrl}s, and only those that have content.  */
  public class ArcMemIterator extends CuIterator {
    private CuIterator cuIter;
    // Stack of archive members in the current archive.
    private LinkedList<TFileIterator> arcIterStack =
	new LinkedList<TFileIterator>();

    // if null, we have to look for nextElement
    private CachedUrl nextCu = null;
    // the CU of the archive we're currently traversing
    private CachedUrl curArcCu = null;

    ArcMemIterator(CuIterator cuIter) {
      this.cuIter = cuIter;
    }

    ArcMemIterator() {
      this(BaseCachedUrlSet.this.getCuIterator());
    }

    public void remove() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public boolean hasNext() {
      return findNextElement() != null;
    }

    public CachedUrl next() {
      CachedUrl cu = findNextElement();
      nextCu = null;

      if (cu != null) {
        return cu;
      }
      throw new NoSuchElementException();
    }

    public int getExcludedCount() {
      return cuIter.getExcludedCount();
    }

    /**
     * Does a pre-order traversal of the CachedUrlSet tree including
     * members of archive files
     * @return the next {@link CachedUrl} in the traversal that has content.
     */
    private CachedUrl findNextElement() {
      if (nextCu != null) {
        return nextCu;
      }
      while (true) {
	if (!arcIterStack.isEmpty()) {
	  Iterator<TFile> arcIter = arcIterStack.getFirst().tFileIterator;
	  if (arcIter.hasNext()) {
	    TFile tf = arcIter.next();
	    ArchiveMemberSpec ams = amsOf(curArcCu, tf);
	    if (!spec.matches(ams.toUrl())) {
	      continue;
	    }
	    if (tf.isFile()) {
	      nextCu = makeCu(curArcCu, tf, ams);
	      break;
	    } else if (tf.isDirectory()) {
	      // Push a new TFile and its iterator and CU onto the stack.
	      arcIterStack.addFirst(new TFileIterator(tf, null));
	      continue;
	    } else {
	      logger.warning("Archive element was neither file nor dir: "
		  	     + tf + ", in " + curArcCu);
	      continue;
	    }
	  } else {
	    // Remove the TFile from the stack and from the cache if it is
	    // there.
	    freeTFile(arcIterStack.removeFirst());

	    continue;
	  }
	}
	curArcCu = null;
	if (cuIter.hasNext()) {
	  CachedUrl cu = cuIter.next();
	  try {
	    if (isExcludedByDate(cu)) {
	      continue;
	    }
	    String arcExt = ArchiveFileTypes.getArchiveExtension(cu);
	    if (arcExt != null) {
	      TFile tf;
	      try {
		CachedUrl tFileCu = au.makeCachedUrl(cu.getUrl());
		tf = getTrueZipManager().getCachedTFile(tFileCu);
		if (!tf.isDirectory()) {
		  logger.error("isDirectory(" + tf +
			       ") = false, including in iterator");
		  nextCu = cu;
		  break;
		}
		if (logger.isDebug3()) {
		  logger.debug3("Found archive: " + tf + " in " + cu);
		}
		// Mark the TFile only as flushable if it is unmounted.
		getTrueZipManager().setFlushAfterUnmountOnly(tFileCu);
		// Push a new TFile and its iterator and CU onto the stack.
		arcIterStack.addFirst(new TFileIterator(tf, tFileCu));
		curArcCu = cu;
		continue;
	      } catch (IOException e) {
		logger.warning("Error opening archive: " + cu, e);
		continue;
	      }
	    } else {
	      nextCu = cu;
	      break;
	    }
	  } finally {
	    AuUtil.safeRelease(cu);
	  }
	} else {	
	  nextCu = null;
	  break;
	}
      }
      return nextCu;
    }

    private ArchiveMemberSpec amsOf(CachedUrl cu, TFile tf) {
      TFile top = tf.getTopLevelArchive();
      if (top == null) {
	String msg = "Shouldn't: TFile.getTopLevelArchive(" + tf + ") = null";
	throw new RuntimeException(msg);
      } else {
	// Find member_path from full_path and topmost_archive_path
	//   Full path = topmost_archive_path + "/" + member_path
	String path = tf.getPath();
	String toppath = top.getPath();
	if (path.startsWith(toppath)) {
	  int pos = toppath.length();
	  if (path.charAt(pos) == '/') {
	    pos++;
	  } else {
	    logger.debug2("no / after archive name in path: " + path);
	  }
	  path = path.substring(pos);

	  return ArchiveMemberSpec.fromCu(cu, path);
	} else {
	  String msg = "Shouldn't: TFile path (" + path
	    + ") doesn't begin with top level archive path (" + toppath + ")";
	  throw new RuntimeException(msg);
	}
      }
    }

    // Create a CU representing an archive member
    private CachedUrl makeCu(CachedUrl cu, TFile tf, ArchiveMemberSpec ams) {
      CachedUrl res = au.makeCachedUrl(cu.getUrl());
      if (false) {
	// experimental - reuse the TFile returned by listFiles() in
	// the member CU
	BaseCachedUrl bcu = (BaseCachedUrl)res;
	return bcu.getArchiveMemberCu(ams, tf);
      } else {
	return res.getArchiveMemberCu(ams);
      }
    }

    /**
     * Unmounts a TFile and removes it from the cache.
     * 
     * @param tfIterator
     *          A TFileIterator with the TFile to be freed.
     */
    private void freeTFile(TFileIterator tfIterator) {
      try {
	getTrueZipManager().freeTFile(tfIterator.tFile,
				      tfIterator.tFileCacheCu);
      } catch (Throwable t) {
	logger.warning("Error freeing TFile: " + tfIterator.tFile, t);
      }
    }

    /**
     * Finalizer.
     */
    @Override
    protected void finalize() throws Throwable {
      // Try to mark as flushable all the TFiles in the stack.
      try {
	while (!arcIterStack.isEmpty()) {
	  markArchiveAsFlushable(arcIterStack.removeFirst());
	}
      } finally {
        super.finalize();
      }
    }

    /**
     * Marks a TFile as flushable.
     * 
     * @param tfIterator
     *          A TFileIterator with the TFile to be marked as flushable.
     */
    private void markArchiveAsFlushable(TFileIterator tfIterator) {
      try {
	getTrueZipManager().markArchiveAsFlushable(tfIterator.tFile,
	    tfIterator.tFileCacheCu);
      } catch (Throwable t) {
	logger.warning("Error freeing TFile: " + tfIterator.tFile, t);
      }
    }

    /**
     * Encapsulation of a TFile that is an archive, an iterator over its members
     * and the cached URL used to locate its entry in the cache.
     */
    private class TFileIterator {
      private final TFile tFile;
      private final Iterator<TFile> tFileIterator;
      private final CachedUrl tFileCacheCu;

      /**
       * Constructor
       * @param tf A TFile with the archive.
       * @param cu A CachedUrl used to locate the TFile entry in the cache.
       */
      public TFileIterator(TFile tf, CachedUrl cu) {
	tFile = tf;
	tFileIterator = (Iterator<TFile>)new ArrayIterator(sortedDir(tFile));
	tFileCacheCu = cu;
      }

      /**
       * Provides the top-level files in an archive, sorted.
       * 
       * @param tf A TFile with the archive.
       * @return a TFile[] with the sorted files.
       */
      private TFile[] sortedDir(TFile tf) {
        TFile[] tfiles = tf.listFiles();
        Arrays.sort(tfiles);
        return tfiles;
      }
    }

    private TrueZipManager getTrueZipManager() {
      if (trueZipManager == null) {
	trueZipManager = theDaemon.getTrueZipManager();
      }
      return trueZipManager;
    }    

  }
}
