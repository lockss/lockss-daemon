/*
 * $Id: MockGenericFileArchivalUnit.java,v 1.2 2003-02-11 00:58:16 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * This is a mock version of <code>ArchivalUnit</code> used for testing
 */

public class MockGenericFileArchivalUnit extends BaseArchivalUnit {
  private String pluginId = "mock-file";
  private String auId = "none-file";

  public MockGenericFileArchivalUnit(CrawlSpec spec) {
    super(spec);
  }

  public CachedUrlSet cachedUrlSetFactory(ArchivalUnit owner,
      CachedUrlSetSpec cuss) {
    return new GenericFileCachedUrlSet(owner, cuss);
  }

  public CachedUrl cachedUrlFactory(CachedUrlSet owner, String url) {
    return new GenericFileCachedUrl(owner, url);
  }

  public UrlCacher urlCacherFactory(CachedUrlSet owner, String url) {
    return null;
  }

  public CachedUrlSet getAUCachedUrlSet() {
    try {
      String url = (String)this.getCrawlSpec().getStartingUrls().get(0);
      return new GenericFileCachedUrlSet(this,
                  new RECachedUrlSetSpec(url, ""));
    } catch (Exception e) {
      return null;
    }
  }
  public String getPluginId() {
    return pluginId;
  }

  public String getAUId() {
    return auId;
  }

  public void setPluginId(String newId) {
    pluginId = newId;
  }

  public void setAuId(String newId) {
    auId = newId;
  }

  // Methods used by the crawler

  public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
    return cachedUrlSetFactory(this, cuss);
  }

  public CachedUrlSet makeCachedUrlSet(String url, String regexp) {
    try {
      return cachedUrlSetFactory(this, new RECachedUrlSetSpec(url, regexp));
    } catch (Exception e) {
      return null;
    }
  }
}
