/*
 * $Id: GenericFileCachedUrlSet.java,v 1.13 2002-12-19 01:28:40 aalto Exp $
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
import java.security.MessageDigest;
import java.net.MalformedURLException;
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.repository.*;
import org.lockss.util.Logger;
import org.lockss.poller.PollManager;
import org.lockss.util.TimeBase;

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
    repository = LockssRepositoryImpl.repositoryFactory(owner);
  }

  public Iterator flatSetIterator() {
    List nodes = spec.getPrefixList();
    if (nodes.size()>1) {
      // currently does not support more than one prefix
      logger.error("More than one prefix found in CachedUrlSetSpec.");
      throw new UnsupportedOperationException("More than one prefix found in CachedUrlSetSpec.");
    }
    TreeSet setTree = new TreeSet(new UrlComparator());
    if (nodes.size()==1) {
      String prefix = (String)nodes.get(0);
      try {
        RepositoryNode intNode = repository.getNode(prefix);
        Iterator children = intNode.listNodes(spec);
        while (children.hasNext()) {
          RepositoryNode child = (RepositoryNode)children.next();
          CachedUrlSetSpec rSpec =
              new RECachedUrlSetSpec(child.getNodeUrl(), (String)null);
          CachedUrlSet newSet = ((BaseArchivalUnit)au).makeCachedUrlSet(rSpec);
          setTree.add(newSet);
        }
      } catch (MalformedURLException mue) {
        logger.error("Bad url in spec: "+prefix);
      } catch (Exception e) {
        // this shouldn't occur
        logger.error(e.getMessage());
      }
    }
    return setTree.iterator();
  }

  public Iterator leafIterator() {
    List nodes = spec.getPrefixList();
    if (nodes.size()>1) {
      // currently does not support more than one prefix
      logger.error("More than one prefix found in CachedUrlSetSpec.");
      throw new UnsupportedOperationException("More than one prefix found in CachedUrlSetSpec.");
    }
    contentNodeCount = 0;
    totalNodeSize = 0;
    TreeSet leafSet = new TreeSet(new UrlComparator());
    if (nodes.size()==1) {
      String prefix = (String)nodes.get(0);
      try {
        RepositoryNode intNode = repository.getNode(prefix);
        Iterator children = intNode.listNodes(spec);
        while (children.hasNext()) {
          RepositoryNode child = (RepositoryNode)children.next();
          if (child.hasContent()) {
            CachedUrl newUrl = ((BaseArchivalUnit)au).cachedUrlFactory(this,
                child.getNodeUrl());
            leafSet.add(newUrl);
            contentNodeCount++;
            totalNodeSize += child.getContentSize();
          }
          // internal nodes could have content, so always recurse
          recurseLeafFetch(child, leafSet);
        }
      } catch (MalformedURLException mue) {
        logger.error("Bad url in spec: "+prefix);
      } catch (Exception e) {
        // this shouldn't occur
        logger.error(e.getMessage());
      }
    }
    return leafSet.iterator();
  }

  private void recurseLeafFetch(RepositoryNode node, TreeSet set) {
    Iterator children = node.listNodes(null);
    while (children.hasNext()) {
      RepositoryNode child = (RepositoryNode)children.next();
      if (child.hasContent()) {
        CachedUrl newUrl = ((BaseArchivalUnit)au).cachedUrlFactory(this,
            child.getNodeUrl());
        set.add(newUrl);
        contentNodeCount++;
        totalNodeSize += child.getContentSize();
      }
      // internal nodes could have content, so always recurse
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
      MessageDigest hasher = PollManager.getPollManager().getHasher();
      CachedUrlSetHasher cush = contentHasherFactory(this, hasher);
      long bytesPerMs = 0;
      SystemMetrics metrics = SystemMetrics.getSystemMetrics();
      try {
        bytesPerMs = metrics.getBytesPerMsHashEstimate(cush, hasher);
      } catch (IOException ie) {
        logger.error("Couldn't finish estimating hash time: "+ie);
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
      leafIterator();
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
      if ((o1 instanceof CachedUrlSet) && (o2 instanceof CachedUrlSet)) {
        String prefix = (String)((CachedUrlSet)o1).getSpec().getPrefixList().get(0);
        String prefix2 = (String)((CachedUrlSet)o2).getSpec().getPrefixList().get(0);
        if (prefix.equals(prefix2)) {
          throw new UnsupportedOperationException("Comparing equal CachedUrlSet prefixes: "+prefix);
        }
        return prefix.compareTo(prefix2);
      } else if ((o1 instanceof CachedUrl) && (o2 instanceof CachedUrl)) {
        return ((CachedUrl)o1).getUrl().compareTo(((CachedUrl)o2).getUrl());
      } else {
        throw new IllegalStateException("Bad object in iterator: " +
                                        o1.getClass() + "," +
                                        o2.getClass());
      }
    }
  }
}
