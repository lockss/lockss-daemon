/*
 * $Id: MockGenericFileArchivalUnit.java,v 1.16 2003-09-13 00:47:50 troberts Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.util.*;
import java.security.MessageDigest;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.MockCachedUrlSetSpec;
import org.lockss.plugin.*;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.plugin.base.*;

/**
 * This is a mock version of <code>ArchivalUnit</code> used for testing
 */

public class MockGenericFileArchivalUnit extends BaseArchivalUnit {
  private Configuration config;
  private String pluginId = "mock-file";

  public MockGenericFileArchivalUnit() {
    super(null);
  }

  public void setCrawlSpec(CrawlSpec spec) {
    this.crawlSpec = spec;
  }

  public void setConfiguration(Configuration config)
      throws ArchivalUnit.ConfigurationException {
    super.setConfiguration(config);
    this.config = config;
  }

  public Configuration getConfiguration() {
    return config;
  }

  public CachedUrlSet cachedUrlSetFactory(ArchivalUnit owner,
      CachedUrlSetSpec cuss) {
    return new GenericFileCachedUrlSet(owner, cuss);
  }

  public CachedUrl cachedUrlFactory(CachedUrlSet owner, String url) {
    return new GenericFileCachedUrl(owner, url);
  }

  public UrlCacher urlCacherFactory(CachedUrlSet owner, String url) {
    return new MockGenericFileUrlCacher(owner,url);
  }

  public CachedUrlSet getAUCachedUrlSet() {
    try {
      String url = (String)this.getCrawlSpec().getStartingUrls().get(0);
      return new GenericFileCachedUrlSet(this,
                  new RangeCachedUrlSetSpec(url));
    } catch (Exception e) {
      return null;
    }
  }

  public String getPluginId() {
    return pluginId;
  }

  public String getName() {
    return "MockGenericFileAU";
  }

  public void setPluginId(String newId) {
    pluginId = newId;
  }

  public void setPlugin(Plugin plugin) {
    this.plugin = plugin;
  }

  public List getNewContentCrawlUrls() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public Collection getUrlStems() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public String getManifestPage() {
    throw new UnsupportedOperationException("Not implemented.");
  }

  public FilterRule getFilterRule(String mimeType) {
    //no filtering
    return null;
  }
}
