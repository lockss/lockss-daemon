/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
