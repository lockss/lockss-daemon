/**
 * $Id: Sitemap.java,v 1.1 2013-03-19 18:42:23 ldoan Exp $
 */

/**

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

package org.lockss.extractor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.lockss.util.Logger;

/**
 * The Sitemap class represents a Sitemap from the Sitemap Protocol
 * (http://www.sitemaps.org/). This class is modified from 
 * SourceForge open-source by Frank McCown.
 */
public class Sitemap {
  
  protected static Logger log = Logger.getLogger(Sitemap.class);

  /** Sitemap types supported*/
  public enum SitemapType {INDEX, XML, ATOM, RSS, TEXT};
  
  private final SitemapType type;

  /** URLs found in this Sitemap */	
  private Map<String, SitemapUrl> urlMap;
	
  /**
   * Constructors
   */
    
  public Sitemap(SitemapType type) {
    log.debug("in Sitemap constructor");
    this.type = type;
    this.urlMap = new HashMap<String, SitemapUrl>();
  }
	
  /**
   * Setters
   */
  
  void addSitemapUrl(SitemapUrl sitemapUrl) {
   
    SitemapUrl sUrl = urlMap.get(sitemapUrl.getUrl());
    
    if (sUrl == null) { // not yet in hashmap
      urlMap.put(sitemapUrl.getUrl(), sitemapUrl);
    } else {
      /** already in hashmap, compare lastmodified time
        if the latest, then add to hashmap */
      try {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date oldDate = sdf.parse(sUrl.getLastModified());
        Date newDate = sdf.parse(sitemapUrl.getLastModified());

        if (newDate.after(oldDate)) {
          urlMap.put(sitemapUrl.getUrl(), sitemapUrl);
        }
        
      }
      catch (ParseException e) {
        log.error("Date parsing error", e);
      }
    }
  
  } /** end addSitemapUrl */
    
  
  /**
   * Getters
   */
  public SitemapType getType() {
    return type;
  }
  
  public Collection<SitemapUrl>getUrlMap() {
    return urlMap.values();		
  }

  /**
   * Display data
   */
  public String toString() {
    return ("[Sitemap type=\"" + type + ", urlMapSize=" + urlMap.size()) + "]";
  }
  
} /** end class Sitemap */