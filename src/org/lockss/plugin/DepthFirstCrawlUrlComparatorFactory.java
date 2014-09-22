/*
 * $Id: DepthFirstCrawlUrlComparatorFactory.java,v 1.1 2014-09-22 20:09:00 thib_gc Exp $
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

import java.util.Comparator;

import org.lockss.crawler.CrawlUrl;
import org.lockss.daemon.PluginException.LinkageError;

/**
 * <p>
 * A factory for a crawl URL comparator that considers URLs as a sequence of
 * directory components separated by <code>/</code> and orders them such that
 * descending into directories would be depth-first.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class DepthFirstCrawlUrlComparatorFactory implements CrawlUrlComparatorFactory {

  private static final char SLASH = '/';
  
  @Override
  public Comparator<CrawlUrl> createCrawlUrlComparator(ArchivalUnit au) throws LinkageError {
    return new Comparator<CrawlUrl>() {
      @Override
      public int compare(CrawlUrl cu1, CrawlUrl cu2) {
        String u1 = cu1.getUrl();
        while (u1.charAt(u1.length() - 1) == SLASH) {
          u1 = u1.substring(0, u1.length() - 1);
        }
        String u2 = cu2.getUrl();
        while (u2.charAt(u2.length() - 1) == SLASH) {
          u2 = u2.substring(0, u2.length() - 1);
        }
        
        int i1 = 0;
        int i2 = 0;
        while (i1 < u1.length() && i2 < u2.length()) {
          // Find next slash in u1
          int j1 = u1.indexOf(SLASH, i1);
          if (j1 == i1) {
            // u1 has consecutive slash: ignore it and loop
            ++i1;
            continue;
          }
          
          // Find next slash in u2
          int j2 = u2.indexOf(SLASH, i2);
          if (j2 == i2) {
            // u1 has consecutive slash: ignore it and loop
            ++i2;
            continue;
          }
          
          if (j1 < 0) {
            if (j2 < 0) {
              // Current components are last of both: compare them
              return u1.substring(i1).compareTo(u2.substring(i2));
            }
            else {
              // Current component is last of u1: if u1 and u2 are in the same
              // subtree, u2 comes first, otherwise compare current components
              int cmp = u1.substring(i1).compareTo(u2.substring(i2, j2));
              return (cmp == 0) ? 1 : cmp;
            }
          }
          else {
            if (j2 < 0) {
              // Current component is last of u2: if u1 and u2 are in the same
              // subtree, u1 comes first, otherwise compare current components
              int cmp = u1.substring(i1, j1).compareTo(u2.substring(i2));
              return (cmp == 0) ? -1 : cmp;
            }
            else {
              // Compare current components
              int cmp = u1.substring(i1, j1).compareTo(u2.substring(i2, j2));
              if (cmp != 0) {
                // Current components are different: compare them
                return cmp;
              }
              // Current components are the same: go to next components and loop
              i1 = j1 + 1;
              i2 = j2 + 1;
            }
          }
        }
        
        // u1 and u2 are equal
        return 0;
      }
    };
  }

}
