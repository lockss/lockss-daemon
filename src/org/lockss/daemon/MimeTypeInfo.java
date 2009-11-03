/*
 * $Id: MimeTypeInfo.java,v 1.4.16.1 2009-11-03 23:44:51 edwardsb1 Exp $
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
import java.io.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.rewriter.*;
import org.lockss.plugin.*;
import org.lockss.plugin.wrapper.*;

/** Record of MIME type-specific factories (<i>eg</i>, FilterFactory,
 * LinkExtractorFactory).  Primary interface is immutable.
 */
public interface MimeTypeInfo {
  /** An empty MimeTypeInfo */
  public static MimeTypeInfo NULL_INFO = new MimeTypeInfo.Impl();

  public static final String DEFAULT_METADATA_TYPE = "*";

  /** Returns the hash FilterFactory, or null */
  public FilterFactory getHashFilterFactory();
  /** Returns the crarl FilterFactory, or null */
  public FilterFactory getCrawlFilterFactory();
  /** Returns the LinkExtractorFactory, or null */
  public LinkExtractorFactory getLinkExtractorFactory();
  /** Returns the RateLimiter, or null */
  public RateLimiter getFetchRateLimiter();
  /** Returns the UrlRewriterFactory, or null */
  public LinkRewriterFactory getLinkRewriterFactory();
  /** Returns the ArticleIteratorFactory, or null */
  public ArticleIteratorFactory getArticleIteratorFactory();
  /** Returns the default MetadataExtractorFactory, or null */
  public MetadataExtractorFactory getMetadataExtractorFactory();
  /** Returns the MetadataExtractorFactory for the metadata type, or null */
  public MetadataExtractorFactory getMetadataExtractorFactory(String mdType);
  /** Returns the MetadataExtractorFactoryMap, or null */
  public Map getMetadataExtractorFactoryMap();

  /** Sub interface adds setters */
  public interface Mutable extends MimeTypeInfo {
    public Impl setHashFilterFactory(FilterFactory fact);
    public Impl setCrawlFilterFactory(FilterFactory fact);
    public Impl setLinkExtractorFactory(LinkExtractorFactory fact);
    public Impl setFetchRateLimiter(RateLimiter limiter);
    public Impl setLinkRewriterFactory(LinkRewriterFactory fact);
    public Impl setArticleIteratorFactory(ArticleIteratorFactory fact);
    public Impl setMetadataExtractorFactoryMap(Map map);
  }

  class Impl implements Mutable {
    static Logger log = Logger.getLogger("MimeTypeInfo");

    private FilterFactory hashFilterFactory;
    private FilterFactory crawlFilterFactory;
    private LinkExtractorFactory extractorFactory;
    private RateLimiter fetchRateLimiter;
    private LinkRewriterFactory linkFactory;
    private ArticleIteratorFactory articleIteratorFactory;
    private Map metadataExtractorFactoryMap;

    public Impl() {
    }

    public Impl(MimeTypeInfo toClone) {
      if (toClone != null) {
        hashFilterFactory = toClone.getHashFilterFactory();
        crawlFilterFactory = toClone.getCrawlFilterFactory();
        extractorFactory = toClone.getLinkExtractorFactory();
        fetchRateLimiter = toClone.getFetchRateLimiter();
        linkFactory = toClone.getLinkRewriterFactory();
        articleIteratorFactory = toClone.getArticleIteratorFactory();
        metadataExtractorFactoryMap = toClone.getMetadataExtractorFactoryMap();
      }
    }

    public FilterFactory getHashFilterFactory() {
      return hashFilterFactory;
    }

    public Impl setHashFilterFactory(FilterFactory fact) {
      hashFilterFactory = fact;
      return this;
    }

    public FilterFactory getCrawlFilterFactory() {
      return crawlFilterFactory;
    }

    public Impl setCrawlFilterFactory(FilterFactory fact) {
      crawlFilterFactory = fact;
      return this;
    }

    public LinkExtractorFactory getLinkExtractorFactory() {
      return extractorFactory;
    }

    public Impl setLinkExtractorFactory(LinkExtractorFactory fact) {
      extractorFactory = fact;
      return this;
    }

    public RateLimiter getFetchRateLimiter() {
      return fetchRateLimiter;
    }

    public Impl setFetchRateLimiter(RateLimiter limiter) {
      fetchRateLimiter = limiter;
      return this;
    }

    public LinkRewriterFactory getLinkRewriterFactory() {
      return linkFactory;
    }

    public Impl setLinkRewriterFactory(LinkRewriterFactory fact) {
      linkFactory = fact;
      return this;
    }

    public ArticleIteratorFactory getArticleIteratorFactory() {
      return articleIteratorFactory;
    }

    public Impl setArticleIteratorFactory(ArticleIteratorFactory fact) {
      articleIteratorFactory = fact;
      return this;
    }

    public MetadataExtractorFactory getMetadataExtractorFactory() {
      return getMetadataExtractorFactory(DEFAULT_METADATA_TYPE);
    }

    public MetadataExtractorFactory getMetadataExtractorFactory(String mdType) {
      MetadataExtractorFactory ret = null;
      if (metadataExtractorFactoryMap != null) {
        ret = (MetadataExtractorFactory)metadataExtractorFactoryMap.get(mdType);
      }
      return ret;
    }

    public Map getMetadataExtractorFactoryMap() {
      return metadataExtractorFactoryMap;
    }

    public Impl setMetadataExtractorFactoryMap(Map map) {
      metadataExtractorFactoryMap = map;
      return this;
    }

  }

}
