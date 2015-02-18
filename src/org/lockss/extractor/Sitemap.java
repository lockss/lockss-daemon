/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

/*
 * Some portion of this code is Copyright.
 * 
 * SitemapUrl.java - Represents a URL found in a Sitemap 
 *  
 * Copyright 2009 Frank McCown
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.lockss.extractor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.lockss.util.Logger;

/**
 * Represents a Sitemap from the Sitemap Protocol
 * in http://www.sitemaps.org/protocol. Modified from 
 * SourceForge open-source by Frank McCown.
 */
public class Sitemap {
  
  private static Logger log = Logger.getLogger(Sitemap.class);

  /** Sitemap types supported */
  public enum SitemapType {INDEX, XML, ATOM, RSS, TEXT};
  
  /** Type of this Sitemap */
  private final SitemapType type;
  /** URLs found in this Sitemap */	
  private Map<String, SitemapUrl> urlMap;
	
  /**
   * Constructs a Sitemap with a type.
   * 
   * @param type an enum value of SitemapType
   */
  Sitemap(SitemapType type) {
    this.type = type;
    this.urlMap = new HashMap<String, SitemapUrl>();
  }
	
  /**
   * Creates a hashmap with url as key and SitemapUrl as value.
   * 
   * @param sUrlToAdd
   */
  void addSitemapUrl(SitemapUrl sUrlToAdd) {
    if (sUrlToAdd == null) {
      throw
	  new IllegalArgumentException("SitemapUrl can not be null.");
    }
    SitemapUrl sUrl = urlMap.get(sUrlToAdd.getUrl());
    // if not yet in hashmap or if latest, then add
    if ((sUrl == null)
        || (sUrlToAdd.getLastModified() > sUrl.getLastModified())) {
      urlMap.put(sUrlToAdd.getUrl(), sUrlToAdd);
    }
  }
   
  /**
   * Returns the type of the sitemap.
   * 
   * @return type the type of the sitemap
   */
  public SitemapType getType() {
    return type;
  }
  
  /**
   * Returns the collection of SitemapUrl.
   * 
   * @return the collection of SitemapUrl
   */
  public Collection<SitemapUrl> getUrls() {
    return urlMap.values();		
  }

  public String toString() {
    return ("[Sitemap: type=" + type + ", urlMapSize=" + urlMap.size()) + "]";
  }
  
}