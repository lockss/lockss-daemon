/*
 * $Id: SqlStoredProcedures.java,v 1.2 2011-11-18 19:28:12 mellen22 Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.util;

import org.lockss.app.LockssDaemon;
import org.lockss.config.TdbAu;
import org.lockss.daemon.TitleConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginManager;

/**
 * This utility class contains static methods that enable SQL stored
 * procedures to access LOCKSS functionality.
 *  
 * @author pgust
 *
 */
public class SqlStoredProcedures {
  
  /**
   * Constructor prevents creating instances.
   */
  private SqlStoredProcedures() {
    
  }
  
  /**
   * Return the title from the title title database that corresponds
   * to the URL of an article in that title.
   * 
   * @param articleUrl the URL of the article
   * @return the title for the given URL and null otherwise
   */
  static TdbAu getTdbAuFromArticleUrl(String articleUrl) {
    if (articleUrl == null) {
      throw new IllegalArgumentException("null articleUrl");
    }
    
    // get lockss daemon
    LockssDaemon daemon = LockssDaemon.getLockssDaemon();
    if (daemon == null) {
      throw new IllegalStateException("no LOCKSS daemon");
    }
    
    // get the CachedUrl from the article URL
    PluginManager pluginManager = daemon.getPluginManager();
    CachedUrl cu = pluginManager.findCachedUrl(articleUrl);
    if (cu == null) {
      return null;
    }
    
    // get the AU from the CachedUrl
    ArchivalUnit au = cu.getArchivalUnit();
    
    // return the TdbAu from the AU
    TitleConfig tc = au.getTitleConfig();
    if (tc == null) {
      return null;
    }
    return tc.getTdbAu();
  }

  /**
   * Return the title from the title database that corresponds
   * to the URL of an article in that title.
   * 
   * @param articleUrl the URL of the article
   * @return the title for the given URL
   */
  static public String getTitleFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      throw new IllegalArgumentException(
          "No title for articleUrl " + articleUrl);
    }

    // get the title from the TdbAu
    String title = tdbAu.getJournalTitle();
    
    // return the title
    return title;
  }

  /**
   * Return the publisher from the title database that corresponds
   * to the URL of an article in that publisher.
   * 
   * @param articleUrl the URL of the article
   * @return the publisher for the given URL
   */
  static public String getPublisherFromArticleUrl(String articleUrl) {
    // get the TdbAu from the AU
    TdbAu tdbAu = getTdbAuFromArticleUrl(articleUrl);
    if (tdbAu == null) {
      throw new IllegalArgumentException(
          "No publisher for articleUrl " + articleUrl);
    }
  
    // get the publisher from the TdbAu
    String publisher = tdbAu.getTdbPublisher().getName();
    
    // return the publisher
    return publisher;
  }

}
