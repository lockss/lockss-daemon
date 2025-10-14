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

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;

/**
 * Basis needed to start a crawl.
 * Provides start and permission URLs.
 */
public interface CrawlSeed {

  public Collection<String> getStartUrls() 
      throws ConfigurationException, PluginException, IOException;

  public Collection<String> getPermissionUrls() 
      throws ConfigurationException, PluginException, IOException;

  /**
   * Old method to control whether a start URL fetch failure aborts
   * the crawl.  Replaced by {@link #isAllowStartUrlError()} but must
   * be kept until all plugins that implement it have been fixed.
   * @deprecated Implement isAllowStartUrlError() instead
   */
  @Deprecated
  default boolean isFailOnStartUrlError() {
    return true;
  }

  /**
   * If there is an error on fetch of a start Url should we abort?
   * Crawl seeds that provide large lists of start URLs should
   * implement this and return true.
   */
  default boolean isAllowStartUrlError() {
    return !isFailOnStartUrlError();
  }
}


