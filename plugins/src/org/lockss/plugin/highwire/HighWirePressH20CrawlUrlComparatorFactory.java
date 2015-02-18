/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.plugin.highwire.HighWirePressH20CrawlUrlComparatorFactory.HighWirePressH20UrlFactory.*;

/**
 * <p>A crawl URL comparator factory for the HighWire Press H20
 * platform.</p>
 * @author Thib Guicherd-Callin
 */
public class HighWirePressH20CrawlUrlComparatorFactory implements CrawlUrlComparatorFactory {

  /**
   * <p>A factory for {@link HighWirePressH20Url} objects.</p> 
   * @author Thib Guicherd-Callin
   * @see HighWirePressH20Url
   */
  protected static class HighWirePressH20UrlFactory {
    
    /**
     * <p>A HighWire H20 URL, possibly broken down into volume, issue,
     * page and figure components.</p>
     * <p>Each URL is tagged with a level ({@link #priority}). If the
     * priority is {@link HighWirePressH20UrlPriority#HIGHEST}, the
     * {@link #volume}, {@link #issue}, {@link #page} and
     * {@link #figure} components are null. If it is
     * {@link HighWirePressH20UrlPriority#VOLUME}, the {@link #volume}
     * component is normalized and set but the {@link #issue},
     * {@link #page} and {@link #figure} components are null.
     * Increasingly many components are set for
     * {@link HighWirePressH20UrlPriority#ISSUE},
     * {@link HighWirePressH20UrlPriority#PAGE} and
     * {@link HighWirePressH20UrlPriority#FIGURE}.</p>
     * <p>"Normalized" means lower case and including the
     * trailing period followed by digits if that suffix is
     * present.</p>
     * <p>Note that this is a non-static nested class; instances exist
     * within the context of a {@link HighWirePressH20UrlFactory}.</p>
     * @author Thib Guicherd-Callin
     * @see HighWirePressH20UrlFactory
     */
    protected class HighWirePressH20Url {

      /**
       * <p>The URL's priority level.</p>
       */
      protected final HighWirePressH20UrlPriority priority;
      
      /**
       * <p>The URL's volume component if its priority level is
       * {@link HighWirePressH20UrlPriority#VOLUME},
       * {@link HighWirePressH20UrlPriority#ISSUE},
       * {@link HighWirePressH20UrlPriority#PAGE} or
       * {@link HighWirePressH20UrlPriority#FIGURE},
       * null otherwise.</p>
       */
      protected final String volume;
      
      /**
       * <p>The URL's issue component if its priority level is
       * {@link HighWirePressH20UrlPriority#ISSUE},
       * {@link HighWirePressH20UrlPriority#PAGE} or
       * {@link HighWirePressH20UrlPriority#FIGURE},
       * null otherwise.</p>
       */
      protected final String issue;
      
      /**
       * <p>The URL's page component if its priority level is
       * {@link HighWirePressH20UrlPriority#PAGE} or
       * {@link HighWirePressH20UrlPriority#FIGURE},
       * null otherwise.</p>
       */
      protected final String page;
      
      /**
       * <p>The URL's figure component if its priority level is
       * {@link HighWirePressH20UrlPriority#FIGURE},
       * null otherwise.</p>
       */
      protected final String figure;
      
      protected HighWirePressH20Url(String url) {
        // Normalize the URL
        String str = url.toLowerCase();

        // Is it a URL that can be broken down into components?
        boolean components = false;
        for (String prefix : urlPrefixes) {
          if (str.startsWith(prefix)) {
            // Yes; skip over the prefix
            components = true;
            str = str.substring(prefix.length());
            break;
          }
        }
        if (!components) {
          // No; URL of type HIGHEST
          priority = HighWirePressH20UrlPriority.HIGHEST;
          volume = null;
          issue = null;
          page = null;
          figure = null;
          return;
        }
        
        int slash;
        boolean special;
        
        // Find the volume component
        slash = str.indexOf('/');
        if (slash < 0 || slash == str.length() - 1) {
          // No first slash or ends with it: URL of type VOLUME 
          priority = HighWirePressH20UrlPriority.VOLUME;
          volume = extract((slash < 0) ? str : str.substring(0, slash));
          issue = null;
          page = null;
          figure = null;
          return;
        }
        volume = extract(str.substring(0, slash));
        str = str.substring(slash + 1);
        
        // Find the issue component
        slash = str.indexOf('/');
        // If the second slash is followed by "local/", this is still an issue-level URL 
        special = (slash >= 0) && (slash < str.length() - 1) && str.substring(slash + 1).startsWith("local/");
        if (slash < 0 || slash == str.length() - 1 || special) {
          // No second slash, ends with it, or continues with "local/": URL of type ISSUE
          priority = HighWirePressH20UrlPriority.ISSUE;
          issue = extract((slash < 0) ? str : str.substring(0, slash));
          page = null;
          figure = null;
          return;
        }
        issue = extract(str.substring(0, slash));
        str = str.substring(slash + 1);

        // Find the page component
        slash = str.indexOf('/');
        // If the third slash is followed by "suppl/" or "embed/", this is still a page-level URL 
        special = (slash >= 0) && (slash < str.length() - 1) && (str.substring(slash + 1).startsWith("suppl/") || str.substring(slash + 1).startsWith("embed/"));
        if (slash < 0 || slash == str.length() - 1 || special) {
          // No third slash, ends with it, or continues with "suppl/" or "embed/": URL of type PAGE
          priority = HighWirePressH20UrlPriority.PAGE;
          page = extract((slash < 0) ? str : str.substring(0, slash));
          figure = null;
          return;
        }
        page = extract(str.substring(0, slash));
        str = str.substring(slash + 1);

        // URL of type FIGURE
        slash = str.indexOf('/');
        priority = HighWirePressH20UrlPriority.FIGURE;
        figure = extract((slash < 0) ? str : str.substring(0, slash));
        return;
      }
      
      /**
       * <p>Extracts a volume, issue, page or figure component from
       * a HighWire H20 URL component.</p>
       * <p>Such components are an identifier (typically but not
       * necessarily numeric) stripped off of an optional suffix:</p>
<pre>
extract("123") ==> "123"
extract("123.foo") ==> "123"
extract("123.foo+bar") ==> "123"
extract("123.foo-bar") ==> "123"
extract("123.foo.bar") ==> "123"
extract("99b") ==> "99b"
extract("99b.foo") ==> "99b"
</pre>
       * <p>The component may optionally end with a period and a
       * numeric portion. If present, it is also extracted:</p>
<pre>
extract("123.0") ==> "123.0"
extract("123.0.foo") ==> "123.0"
extract("123.0.foo+bar") ==> "123.0"
extract("123.0.foo-bar") ==> "123.0"
extract("123.0.foo.bar") ==> "123.0"
extract("99b.0") ==> "99b.0"
extract("99b.0.foo") ==> "99b.0"
</pre>
       * @param str A substring beginning with a HighWire H20 URL
       *            component.
       * @return The substring's component.
       */
      protected String extract(String str) {
        int dot = str.indexOf('.');
        if (dot < 0) {
          return str;
        }
        if (str.length() > dot + 1 && Character.isDigit(str.charAt(dot + 1))) {
          int dot2 = str.indexOf('.', dot + 1);
          return (dot2 < 0) ? str : str.substring(0, dot2);
        }
        return str.substring(0, dot);
      }
      
    }
    
    /**
     * <p>An enumerated type for the relative priority classes of
     * HighWire H20 URLs.</p>
     * <ul>
     *  <li>{@link #HIGHEST}</li>
     *  <li>{@link #FIGURE}</li>
     *  <li>{@link #PAGE}</li>
     *  <li>{@link #ISSUE}</li>
     *  <li>{@link #VOLUME}</li>
     * </ul>
     * @author Thib Guicherd-Callin
     */
    enum HighWirePressH20UrlPriority {
      HIGHEST,
      FIGURE,
      PAGE,
      ISSUE,
      VOLUME,
    }
    
    protected String[] urlPrefixes;
    
    public HighWirePressH20UrlFactory(String baseUrl, String jcode) {
      this.urlPrefixes = new String[] {
          baseUrl + "content/" + jcode + "/",
          baseUrl + "content/",
          baseUrl + "powerpoint/",
      };
    }
    
    public HighWirePressH20Url makeUrl(String url) {
      return new HighWirePressH20Url(url);
    }
    
  }

  /**
   * <p>A crawl URL comparator for the HighWire Press H20
   * platform.</p>
   * @author Thib Guicherd-Callin
   */
  protected static class HighWirePressH20CrawlUrlComparator implements Comparator<CrawlUrl> {
    
    protected HighWirePressH20UrlFactory factory;
    
    public HighWirePressH20CrawlUrlComparator(ArchivalUnit au) {
      String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
      String jcode = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());
      factory = new HighWirePressH20UrlFactory(baseUrl, jcode);
    }
    
    @Override
    public int compare(CrawlUrl url1, CrawlUrl url2) {
      // Opportunities for caching
      HighWirePressH20Url hu1 = factory.makeUrl(url1.getUrl());
      HighWirePressH20Url hu2 = factory.makeUrl(url2.getUrl());

      final int hu1p = hu1.priority.ordinal();
      final int hu2p = hu2.priority.ordinal();
      
      // If both are HIGHEST, compare the URL strings
      final int oHighest = HighWirePressH20UrlPriority.HIGHEST.ordinal();
      if (hu1p == oHighest && hu2p == oHighest) { return url1.getUrl().compareTo(url2.getUrl()); }
      
      // If one is HIGHEST, it wins
      if (hu1p == oHighest) { return -1; }
      if (hu2p == oHighest) { return 1; }

      // If they are not in the same volume, one of them wins
      int cmp = hu1.volume.compareTo(hu2.volume);
      if (cmp != 0) { return cmp; }

      // If both are VOLUME, compare the URL strings
      final int oVolume = HighWirePressH20UrlPriority.VOLUME.ordinal();
      if (hu1p == oVolume && hu2p == oVolume) { return url1.getUrl().compareTo(url2.getUrl()); }

      // If one is VOLUME, it loses 
      if (hu1p == oVolume) { return 1; } 
      if (hu2p == oVolume) { return -1; }
      
      // If they are not in the same issue, one of them wins
      cmp = hu1.issue.compareTo(hu2.issue);
      if (cmp != 0) { return cmp; }
      
      // If both are ISSUE, compare the URL strings
      final int oIssue = HighWirePressH20UrlPriority.ISSUE.ordinal();
      if (hu1p == oIssue && hu2p == oIssue) { return url1.getUrl().compareTo(url2.getUrl()); }
      
      // If one is ISSUE, it loses 
      if (hu1p == oIssue) { return 1; } 
      if (hu2p == oIssue) { return -1; }

      // If they are not on the same page, one of them wins
      cmp = hu1.page.compareTo(hu2.page);
      if (cmp != 0) { return cmp; }
      
      // If both are PAGE, compare the URL strings
      final int oPage = HighWirePressH20UrlPriority.PAGE.ordinal();
      if (hu1p == oPage && hu2p == oPage) { return url1.getUrl().compareTo(url2.getUrl()); }
      
      // If one is ISSUE, it loses 
      if (hu1p == oPage) { return 1; } 
      if (hu2p == oPage) { return -1; }

      // If they are not in the same figure, one of them wins
      cmp = hu1.figure.compareTo(hu2.figure);
      if (cmp != 0) { return cmp; }
      
      // Same figure: compare the URL strings
      return url1.getUrl().compareTo(url2.getUrl());
    }
    
  }
  
  @Override
  public Comparator<CrawlUrl> createCrawlUrlComparator(ArchivalUnit au) throws LinkageError {
    return new HighWirePressH20CrawlUrlComparator(au);
  }
  
}
