/*
 * $Id: NullCrawler.java,v 1.9 2006-11-14 19:21:28 tlipkis Exp $
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

package org.lockss.test;

import java.util.Collection;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Deadline;

public class NullCrawler implements Crawler {
  public boolean doCrawl() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public ArchivalUnit getAu() {
    throw new UnsupportedOperationException("Not implemented");
  }

//   public long getEndTime() {
//     throw new UnsupportedOperationException("Not implemented");
//   }

//   public long getNumFetched() {
//     throw new UnsupportedOperationException("Not implemented");
//   }
//   public long getNumParsed() {
//     throw new UnsupportedOperationException("Not implemented");
//   }

//   public long getStartTime() {
//     throw new UnsupportedOperationException("Not implemented");
//   }

  public Collection getStartUrls() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public int getType() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean isWholeAU() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public CrawlerStatus getStatus() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void abortCrawl() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void setWatchdog(LockssWatchdog wdog) {
  }
}
