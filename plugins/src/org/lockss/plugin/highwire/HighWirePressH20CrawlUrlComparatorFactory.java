/*
 * $Id: HighWirePressH20CrawlUrlComparatorFactory.java,v 1.1 2009-08-05 23:59:37 thib_gc Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.util.Comparator;

import org.lockss.crawler.CrawlUrl;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException.LinkageError;
import org.lockss.plugin.*;

public class HighWirePressH20CrawlUrlComparatorFactory implements CrawlUrlComparatorFactory {

  public static class HighWirePressH20CrawlUrlComparator implements Comparator<CrawlUrl> {

    protected Comparator<String> comparator;
    
    public HighWirePressH20CrawlUrlComparator(ArchivalUnit au) {
      this.comparator = new HighWirePressH20StringUrlComparator(au);
    }
    
    public int compare(CrawlUrl url1, CrawlUrl url2) {
      return comparator.compare(url1.getUrl(), url2.getUrl());
    }
    
  }
  
  public static class HighWirePressH20StringUrlComparator implements Comparator<String> {

    protected class HighWireUrl {
      
      protected final boolean tuple;
      
      protected final String issue;
      
      protected final String page;
      
      protected HighWireUrl(String url) {
        boolean init = false;
        String str = url;
        int ind;

        for (String prefix : prefixes) {
          if (url.startsWith(prefix)) {
            init = true;
            str = str.substring(prefix.length());
            break;
          }
        }
        if (!init) {
          // Not an issue-level or article-level URL
          tuple = false;
          issue = null;
          page = null;
          return;
        }

        tuple = true;
        ind = str.indexOf('/');
        if (ind < 0) {
          // Issue-level URL
          if (str.endsWith(".pdf") || str.endsWith(".gif")) {
            str = str.substring(0, str.length() - 4);
          }
          ind = str.lastIndexOf('.');
          issue = ind < 0 ? str : str.substring(0, ind);
          page = "";
          return;
        }
        
        // Article-level URL
        issue = str.substring(0, ind);
        str = str.substring(ind + 1);
        ind = str.indexOf('/');
        if (ind >= 0) {
          str = str.substring(0, ind);
        }
        ind = str.indexOf('.');
        page = ind < 0 ? str : str.substring(0, ind);
      }
      
    }
    
    protected String[] prefixes;

    public HighWirePressH20StringUrlComparator(String baseUrl,
                                               String volumeName) {
      this.prefixes = new String[] {
        baseUrl + "content/" + volumeName + "/",
        baseUrl + "powerpoint/" + volumeName + "/"
      };
    }
    
    public HighWirePressH20StringUrlComparator(ArchivalUnit au) {
      this(au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()),
           au.getConfiguration().get(ConfigParamDescr.VOLUME_NAME.getKey()));
    }
    
    /**
     * <p>Compares two HighWire URLs.</p>
     * <p>Most URLs have greater priority than article-level URLs,
     * which have greater priority than issue-level URLs.</p>
     */
    public int compare(String str1, String str2) {
      // Opportunities for caching here
      HighWireUrl url1 = makeHighWireUrl(str1);
      HighWireUrl url2 = makeHighWireUrl(str2);
      
      if (url1.tuple && url2.tuple) { // Both tuples
        int isscmp = url1.issue.compareTo(url2.issue);
        if (isscmp != 0) { return isscmp; } // In different issues
        int pagcmp = url1.page.compareTo(url2.page);
        if (pagcmp != 0) { return pagcmp; } // On different pages
        // On the same page: default tie break
      }
      else { // At least one not a tuple
        if (url1.tuple) { return 1; } // Second URL not a tuple
        if (url2.tuple) { return -1; } // First URL not a tuple
        // Both not a tuple: default tie break
      }
      return str1.compareTo(str2);
    }
    
    /**
     * <p>For testing purposes.</p>
     */
    public HighWireUrl makeHighWireUrl(String url) {
      return new HighWireUrl(url);
    }
    
  }
  
  public Comparator<CrawlUrl> createCrawlUrlComparator(ArchivalUnit au) throws LinkageError {
    return new HighWirePressH20CrawlUrlComparator(au);
  }

}
