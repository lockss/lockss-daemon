/*
 * $Id: MimeTypeInfo.java,v 1.2 2007-02-08 08:56:35 tlipkis Exp $
 */

/*
 Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/** Record of MIME type-specific factories (<i>eg</i>, FilterFactory,
 * LinkExtractorFactory), and static global defaults
 */
public class MimeTypeInfo {
  static Logger log = Logger.getLogger("MimeTypeInfo");

  public static MimeTypeInfo NULL_INFO = new MimeTypeInfo();

  private FilterFactory filterFactory;
  private LinkExtractorFactory extractorFactory;
  private RateLimiter fetchRateLimiter;

  public MimeTypeInfo() {
  }

  public MimeTypeInfo(MimeTypeInfo toClone) {
    if (toClone != null) {
      filterFactory = toClone.getFilterFactory();
      extractorFactory = toClone.getLinkExtractorFactory();
      fetchRateLimiter = toClone.getFetchRateLimiter();
    }
  }

  public FilterFactory getFilterFactory() {
    return filterFactory;
  }

  public void setFilterFactory(FilterFactory fact) {
    filterFactory = fact;
  }

  public LinkExtractorFactory getLinkExtractorFactory() {
    return extractorFactory;
  }

  public void setLinkExtractorFactory(LinkExtractorFactory fact) {
    extractorFactory = fact;
  }

  public RateLimiter getFetchRateLimiter() {
    return fetchRateLimiter;
  }

  public void setFetchRateLimiter(RateLimiter limiter) {
    fetchRateLimiter = limiter;
  }
}
