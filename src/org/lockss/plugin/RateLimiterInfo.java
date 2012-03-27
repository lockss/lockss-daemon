/*
 * $Id: RateLimiterInfo.java,v 1.2 2012-03-27 20:58:14 tlipkis Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.util.regex.*;
import org.apache.commons.lang.ObjectUtils;

import org.lockss.util.*;
import org.lockss.daemon.*;

/** Container to gather various pieces of plugin rate limiter info into one
 * place */
public class RateLimiterInfo {

  private String crawlPoolKey;
  private String rate;
  private Map<String,String> urlRates;
  private Map<String,String> mimeRates;
  private LinkedHashMap<CrawlWindow,RateLimiterInfo> cond;

  public RateLimiterInfo(String crawlPoolKey, int events, long interval) {
    this.crawlPoolKey = crawlPoolKey;
    this.rate = "1/" + interval;
  }

  public RateLimiterInfo(String crawlPoolKey, String rate) {
    this.crawlPoolKey = crawlPoolKey;
    this.rate = rate;
  }

  public RateLimiterInfo setUrlRates(Map<String,String> urlRates) {
    this.urlRates = urlRates;
    return this;
  }

  public RateLimiterInfo setMimeRates(Map<String,String> mimeRates) {
    this.mimeRates = mimeRates;
    return this;
  }

  public RateLimiterInfo setCond(LinkedHashMap<CrawlWindow,RateLimiterInfo>
				 cond) {
    this.cond = cond;
    return this;
  }

  public Map<CrawlWindow,RateLimiterInfo> getCond() {
    return cond;
  }

  public String getCrawlPoolKey() {
    return crawlPoolKey;
  }

  public RateLimiterInfo setCrawlPoolKey(String crawlPoolKey) {
    this.crawlPoolKey = crawlPoolKey;
    return this;
  }

  public String getDefaultRate() {
    return rate;
  }

  public Map<String,String> getUrlRates() {
    return urlRates;
  }

  public Map<String,String> getMimeRates() {
    return mimeRates;
  }

  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof RateLimiterInfo) {
      RateLimiterInfo o = (RateLimiterInfo)obj;

      return ObjectUtils.equals(crawlPoolKey, o.crawlPoolKey)
	&& ObjectUtils.equals(rate, o.rate)
	&& ObjectUtils.equals(urlRates, o.urlRates)
	&& ObjectUtils.equals(mimeRates, o.mimeRates)
	&& ObjectUtils.equals(cond, o.cond);
    }
    return false;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[rli: ");
    if (crawlPoolKey != null) {
      sb.append("pool: ");
      sb.append(crawlPoolKey);
    }
    if (rate != null) {
      sb.append(rate);
    }
    if (cond != null) {
      sb.append(cond.toString());
    }
    if (urlRates != null) {
      sb.append(", U=");
      sb.append(urlRates.toString());
    }
    if (mimeRates != null) {
      sb.append(", M=");
      sb.append(mimeRates.toString());
    }
    sb.append("]");
    return sb.toString();
  }

}
