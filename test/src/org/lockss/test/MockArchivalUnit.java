/*
 * $Id: MockArchivalUnit.java,v 1.7 2003-01-25 02:21:11 aalto Exp $
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

/**
 * This is a mock version of <code>ArchivalUnit</code> used for testing
 */

public class MockArchivalUnit implements ArchivalUnit {
  private CrawlSpec spec;
  private String pluginId = "mock";
  private String auId = "none";
  private CachedUrlSet cus = null;
  private MockObjectCallback pauseCallback = null;

  public MockArchivalUnit(){
  }

  public MockArchivalUnit(CrawlSpec spec) {
    this.spec = spec;
  }

  public CrawlSpec getCrawlSpec() {
    return spec;
  }

  public void setCrawlSpec(CrawlSpec spec) {
    this.spec = spec;
  }

  public CachedUrlSet getAUCachedUrlSet() {
    return cus;
  }

  public void setAUCachedUrlSet(CachedUrlSet cus) {
    this.cus = cus;
  }


  /**
   * Make a new MockArchivalUnit object with a list populated with
   * the urls specified in rootUrls (and no reg expressions)
   *
   * @param rootUrls list of string representation of the urls to
   * add to the new MockArchivalUnit's list
   * @return MockArchivalUnit with urls in rootUrls in its list
   */
  public static MockArchivalUnit createFromListOfRootUrls(String[] rootUrls){
    CrawlSpec rootSpec = new CrawlSpec(ListUtil.fromArray(rootUrls), null);
    return new MockArchivalUnit(rootSpec);
  }

  // Methods used by the crawler

  public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
    return null;
  }

  public CachedUrlSet makeCachedUrlSet(String url, String regexp) {
    return null;
  }

  public boolean shouldBeCached(String url) {
    return false;
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

  public String getIdString() {
    return pluginId + ":" + auId;
  }

  public int hashCode() {
    return getIdString().hashCode();
  }

  public void pause() {
    if (pauseCallback != null) {
      pauseCallback.callback();
    }
  }

  public void setPauseCallback(MockObjectCallback callback) {
    this.pauseCallback = callback;
  }
}

