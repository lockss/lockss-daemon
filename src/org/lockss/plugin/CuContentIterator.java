/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.util.Constants.RegexpContext;
import org.lockss.app.*;
import org.lockss.crawler.*;
import org.lockss.extractor.*;


/**
 * Iterator over a possibly filtered set of CachedUrls.
 */
public class CuContentIterator extends CuIterator {
  static Logger log = Logger.getLogger("CuContentIterator");

  private LockssDaemon daemon;
  private Iterator<CachedUrlSetNode> cusIter;
  private CachedUrl nextElement = null;
  private CrawlManager crawlMgr;
  private int excluded = 0;
  private boolean isV2Compat;

  public CuContentIterator(Iterator<CachedUrlSetNode> cusIter) {
    this.cusIter = cusIter;
    daemon = LockssDaemon.getLockssDaemon();
    isV2Compat = daemon.getPollManager().isV2Compat();
  }

  public void remove() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean hasNext() {
    return findNextElement() != null;
  }

  public CachedUrl next() {
    CachedUrl element = findNextElement();
    nextElement = null;

    if (element != null) {
      return element;
    }
    throw new NoSuchElementException();
  }

  public int getExcludedCount() {
    return excluded;
  }

  private CachedUrl findNextElement() {
    if (nextElement != null) {
      return nextElement;
    }
    while (cusIter.hasNext()) {
      CachedUrlSetNode cusn = cusIter.next();
      if (getOptions().isContentOnly() && !cusn.hasContent()) {
	continue;
      }
      CachedUrl cu = AuUtil.getCu(cusn);
      if (cu != null && isIncluded(cu)) {
	nextElement = cu;
	return cu;
      }
    }
    return null;
  }

  CrawlManager getCrawlManager(CachedUrl cu) {
    if (crawlMgr == null) {
      crawlMgr = daemon.getCrawlManager();
    }
    return crawlMgr;
  }

  protected boolean isIncluded(CachedUrl cu) {
    String url = cu.getUrl();
    if (getOptions().isIncludedOnly() &&
	!cu.getArchivalUnit().shouldBeCached(url)) {
      boolean shouldRelease = !cu.needsRelease();
      try {
        if (isV2Compat) {
          CIProperties props = cu.getProperties();
          String nodeUrl = props.getProperty(CachedUrl.PROPERTY_NODE_URL);
          if (UrlUtil.isDirectoryRedirection(url, nodeUrl) &&
              cu.getArchivalUnit().shouldBeCached(nodeUrl)) {
            log.debug2("Including dirnode: " + nodeUrl);
            return true;
          }
        }
        log.debug("Excluding " + url);
        excluded++;
        return false;
      } finally {
        if (shouldRelease) {
          cu.release();
        }
      }
    }
    if (getCrawlManager(cu) != null &&
	getCrawlManager(cu).isGloballyExcludedUrl(cu.getArchivalUnit(),
						url)) {
      excluded++;
      return false;
    }
    return true;
  }
}
