/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.springer;

import java.util.Comparator;

import org.lockss.crawler.CrawlUrl;
import org.lockss.daemon.PluginException.LinkageError;
import org.lockss.plugin.*;

/**
 * <p>A crawl URL comparator factory for the SpringerLink platform powered by
 * MetaPress. Substance-bearing URLs (ending in fulltext.html or
 * fulltext.pdf) are prioritized over all others, except for a few important
 * URLs containing most of the styles and scripts that weren't collected until
 * recently.</p>
 * @author Thib Guicherd-Callin
 */
public class SpringerLinkCrawlUrlComparatorFactory implements CrawlUrlComparatorFactory {

  @Override
  public Comparator<CrawlUrl> createCrawlUrlComparator(ArchivalUnit au) throws LinkageError {
    return new Comparator<CrawlUrl>() {
      private int characterize(String url) {
        if (url.contains("/dynamic-file.axd?")) {          
          return 0;
        }
        else if (url.endsWith("/fulltext.pdf") || url.endsWith("/fulltext.html")) {
          return 1;
        }
        else {
          return 2;
        }
      }
      @Override
      public int compare(CrawlUrl o1, CrawlUrl o2) {
        String url1 = o1.getUrl();
        int prio1 = characterize(url1);
        String url2 = o2.getUrl();
        int prio2 = characterize(url2);
        if (prio1 < prio2) {
          return -1;
        }
        else if (prio1 > prio2) {
          return 1;
        }
        else {
          return url1.compareTo(url2);
        }
      }
    };
  }
  
}
