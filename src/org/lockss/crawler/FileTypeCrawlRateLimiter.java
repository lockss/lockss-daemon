/*
 * $Id$
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

package org.lockss.crawler;

import java.util.*;
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;

/**
 * Manages the rate limiters in use by a crawl: default limiter plus
 * optional MIME-type or URL dependent limiters.
 * @ThreadSafe
 */
public class FileTypeCrawlRateLimiter extends BaseCrawlRateLimiter {
  static Logger log = Logger.getLogger("FileTypeCrawlRateLimiter");

  class PatElem {
    Pattern pat;
    RateLimiter limiter;

    PatElem(Pattern pat, RateLimiter limiter) {
      this.pat = pat;
      this.limiter = limiter;
    }
  }

  public class UrlLimiters extends ArrayList<PatElem>{
    public UrlLimiters(int size) {
      super(size);
    }
  }

  public class MimeLimiters extends HashMap<String,RateLimiter>{}

  RateLimiter defaultLimiter;

  UrlLimiters urlLimiters;
  MimeLimiters mimeLimiterMap;

  /** Create a FileTypeCrawlRateLimiter from a RateLimiterInfo */
  public FileTypeCrawlRateLimiter(RateLimiterInfo rli) {
    if (rli == null) {
      defaultLimiter = newRateLimiter(1, BaseArchivalUnit.DEFAULT_FETCH_DELAY);
    } else {
      defaultLimiter = newRateLimiter(rli.getDefaultRate());
      urlLimiters = makeUrlLimiters(rli.getUrlRates());
      mimeLimiterMap = makeMimeLimiterMap(rli.getMimeRates());
    }
  }

  protected RateLimiter newRateLimiter(String rate) {
    return new RateLimiter(rate);
  }

  protected RateLimiter newRateLimiter(int events, long interval) {
    return new RateLimiter(events, interval);
  }

  /** Convert URL -> rate map to list of Pattern,RateLimiter pairs */
  UrlLimiters makeUrlLimiters(Map<String,String> urlRates) {
    if (urlRates == null) {
      return null;
    }
    UrlLimiters res = new UrlLimiters(urlRates.size());
    for (Map.Entry<String,String> ent : urlRates.entrySet()) {
      Pattern pat = Pattern.compile(ent.getKey());
      res.add(new PatElem(pat, newRateLimiter(ent.getValue())));
    }
    return res;
  }

  /** Convert MIME -> rate map to MIME -> RateLimiter map */
  MimeLimiters makeMimeLimiterMap(Map<String,String> mimeRates) {
    if (mimeRates == null) {
      return null;
    }
    MimeLimiters res = new MimeLimiters();
    for (Map.Entry<String,String> ent : mimeRates.entrySet()) {
      RateLimiter limiter = newRateLimiter(ent.getValue());
      for (String mime :
	     StringUtil.breakAt(ent.getKey(), ",", -1, true, true)) {
	res.put(mime, limiter);
      }
    }
    return res;
  }

  /** Return the RateLimiter on which to wait for the next fetch
   * @param url the url about to be fetched
   * @param previousContentType the MIME type or Content-Type of the
   * previous file fetched
   */
  public RateLimiter getRateLimiterFor(String url, String previousContentType) {
    if (urlLimiters != null && !StringUtil.isNullString(url)) {
      for (PatElem pe : urlLimiters) {
	if (pe.pat.matcher(url).find()) {
	  return pe.limiter;
	}
      }
      return defaultLimiter;
    }
    if (mimeLimiterMap != null &&
	!StringUtil.isNullString(previousContentType)) {
      String mime = HeaderUtil.getMimeTypeFromContentType(previousContentType);
      RateLimiter res = mimeLimiterMap.get(mime);
      if (res == null) {
	res = mimeLimiterMap.get(MimeTypeMap.wildSubType(mime));
      }
      if (res == null) {
	res = mimeLimiterMap.get(MimeTypeMap.WILD_CARD);
      }
      if (res == null) {
	res = defaultLimiter;
      }
      return res;
    }
    return defaultLimiter;
  }
}
