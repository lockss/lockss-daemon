/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.BitSet;
import java.util.List;

import org.lockss.util.*;


/**
 *Structure to hold all the information about a fetched url
 *to pass between UrlFetcher and UrlConsumer
 */
public class FetchedUrlData {
  static Logger log = Logger.getLogger(FetchedUrlData.class);

  public InputStream input;
  public CIProperties headers;
  public String fetchUrl;
  public String origUrl;
  public List<String> redirectUrls;
  private boolean storeRedirects;
  private BitSet fetchFlags;
  private UrlFetcher fetcher;
  
  public FetchedUrlData(String origUrl, String fetchUrl, InputStream input, CIProperties headers,
        List<String> redirectUrls, UrlFetcher fetcher) {
    if(input == null || headers == null || fetchUrl == null) {
      //XXX: this exeption needs a msg
      throw new IllegalArgumentException();
    }
    this.input = input;
    this.headers = headers;
    this.fetchUrl = fetchUrl;
    this.origUrl = origUrl;
    if(redirectUrls != null && !redirectUrls.isEmpty()) {
      this.redirectUrls = redirectUrls;
      this.storeRedirects = true;
    }
    this.fetcher = fetcher;
  }
  
  public void setStoreRedirects(boolean storeRedirects) {
    this.storeRedirects = storeRedirects;
  }

  public boolean storeRedirects() {
    return redirectUrls != null && storeRedirects == true;
  }
  
  public void setFetchFlags(BitSet fetchFlags) {
    this.fetchFlags = fetchFlags;
  }
  
  public BitSet getFetchFlags() {
    if(fetchFlags == null) {
      fetchFlags = new BitSet();
    }
    return fetchFlags;
  }
  
  public InputStream getInputStream() throws IOException {
    return input;
  }

  public boolean resetInputStream() throws IOException {
    if(fetcher != null) {
      input = fetcher.resetInputStream(input, null);
      return true;
    }
    return false;
  }
  
  public UrlData getUrlData() {
    return new UrlData(input, headers, origUrl);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[fud: ");
    sb.append(origUrl);
    if(redirectUrls != null && !redirectUrls.isEmpty()) {
      sb.append(", red: ");
      sb.append(redirectUrls);
    }
    sb.append(", hdrs: ");
    sb.append(headers);
    sb.append("]");
    return sb.toString();
  }

}
