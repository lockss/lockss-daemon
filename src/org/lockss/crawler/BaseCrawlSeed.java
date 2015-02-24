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

import java.util.Collection;

import org.lockss.daemon.PluginException;
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

  /**
   * 
   * @param au
   * @param spec
   */
  public BaseCrawlSeed(ArchivalUnit au){
    this.au = au;
  }

  public Collection<String> getStartUrls() throws ConfigurationException, PluginException{
    Collection<String> startUrls = au.getStartUrls();
    if (startUrls == null || startUrls.isEmpty()) {
      throw new PluginException.InvalidDefinition(
          "CrawlSeed expects the Plugin to define a non-null start URL list");
    }
    return startUrls;
  }

  public Collection<String> getPermissionUrls() throws ConfigurationException, PluginException{
    Collection<String> permUrls = au.getPermissionUrls();
    if (permUrls == null || permUrls.isEmpty()) {
      throw new PluginException.InvalidDefinition(
          "CrawlSeed expects the Plugin to define a non-null permission URL list");
    }
    return permUrls; 
  }

  public boolean isFailOnStartUrlError() {
    return true;
  }

}
