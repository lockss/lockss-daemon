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

package org.lockss.crawler;

import java.io.IOException;
import java.util.Collection;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;

/**
 * Pulls start and permission URL lists from the crawl spec.
 * To create a custom CrawlInitializer that only specifies special action
 * for one list extend this and override the one.
 * 
 * @author wkwilson
 */
public class BaseCrawlSeed implements CrawlSeed {
  
  protected ArchivalUnit au;
  boolean isInitialized = false;

  /**
   * @param au
   * @since 1.67
   */
  public BaseCrawlSeed(ArchivalUnit au){
    this.au = au;
  }

  /**
   * @param crawlerFacade
   * @since 1.67.5
   */
  public BaseCrawlSeed(CrawlerFacade crawlerFacade){
    this(crawlerFacade.getAu());
  }
  
  /**
   * Add any initialization here for lazy initialization
   */
  protected void initialize() throws ConfigurationException, PluginException, IOException {
  }
  
  /**
   * Contains lazy initialization logic
   */
  public final Collection<String> getStartUrls() throws ConfigurationException, PluginException, IOException {
    if(!isInitialized) {
      initialize();
      isInitialized = true;
    }
    return doGetStartUrls();
  }
  
  /**
   * Do the work of getting start URLs. By default get them from the AU.
   * Override to provide custom start url generation.
   * 
   * @return startUrls
   * @throws ConfigurationException
   * @throws PluginException
   * @throws IOException
   */
  public Collection<String> doGetStartUrls() throws ConfigurationException, PluginException, IOException {
    Collection<String> startUrls = au.getStartUrls();
    if (startUrls == null || startUrls.isEmpty()) {
      throw new PluginException.InvalidDefinition(
          "CrawlSeed expects the Plugin to define a non-null start URL list");
    }
    return startUrls;
  }
  
  /**
   * Contains lazy initialization logic
   */
  public final Collection<String> getPermissionUrls() throws ConfigurationException, PluginException, IOException{
    if(!isInitialized) {
      initialize();
      isInitialized = true;
    }
    return doGetPermissionUrls();
  }

  /**
   * Do the work of getting permission URLs. By default get them from the AU.
   * Override to provide custom start url generation.
   * @return permUrls
   * @throws ConfigurationException
   * @throws PluginException
   * @throws IOException
   */
  public Collection<String> doGetPermissionUrls() throws ConfigurationException, PluginException, IOException{
    Collection<String> permUrls = au.getPermissionUrls();
    if (permUrls == null || permUrls.isEmpty()) {
      throw new PluginException.InvalidDefinition(
          "CrawlSeed expects the Plugin to define a non-null permission URL list");
    }
    return permUrls; 
  }
  
  /**
   * If there is an error on fetch of a start Url should we abort?
   * Crawl seeds that provide large lists of start URLs might want to
   * override this and return true.
   */
  public boolean isAllowStartUrlError() {
    return au.isAllowStartUrlError();
  }

}
