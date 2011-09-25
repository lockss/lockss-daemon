/*
 * $Id: RateLimiterInfo.java,v 1.1 2011-09-25 04:20:40 tlipkis Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.util.*;

/** Container to gather various pieces of plugin rate limiter info into one
 * place */
public class RateLimiterInfo {

  private String crawlPoolKey;
  private String rate;
  private Map<String,String> urlRates;
  private Map<String,String> mimeRates;
  private Map<String,String> limiterRates;

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

  public String getCrawlPoolKey() {
    return crawlPoolKey;
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

  public String toString(int indent) {
    StringBuilder sb = new StringBuilder();
    sb.append("[rli: ");
    sb.append(rate);
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
