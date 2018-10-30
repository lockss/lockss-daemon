/*
 * $Id$
 */

/*

Copyright (c) 2000-2018   s Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.iwap;

import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.silverchair.BaseScUrlConsumerFactory;
import org.lockss.util.Logger;
/**
 * @since 1.67.5 
 */
public class IwapUrlConsumerFactory extends BaseScUrlConsumerFactory {
  
  private static final Logger log = Logger.getLogger(IwapUrlConsumerFactory.class);

  //https://iwaponline.com/hr/issue-pdf/x/y/z/q
  //or
  //https://iwaoponline.com/hr/article-pdf/x/y/z/6650409/afw017.pdf
  // it doesn't really matter if it ends in PDF - if it redirects from a canonical to a temporary, we ought to consume

  // TODO: have not yet seen this occur. We go through watermark. Leave in place because it seems likely 
  public static final String DEL_URL = "/(issue|article)-pdf/";
  // will redirect to: 
  // https://iwa.silverchair-cdn.com/iwa/backfile/content_public/journal/hr/....
  public static final String DOC_URL = "/content_public/journal/[^?]+";
  public static final String DOC_ARGS = "\\?Expires=[^&]+&Signature=[^&]+&Key-Pair-Id=.+$";
  // or if through watermarking to:
  // https://watermark.silverchair.com/front_matter.pdf?token=AQECAHi208
  public static final String WMARK_URL = "watermark[.]silverchair[.]com/[^?]+";

  public static final String ORIG_FULLTEXT_STRING = DEL_URL;
  public static final String DEST_FULLTEXT_STRING = DOC_URL +  DOC_ARGS;

  protected final static Pattern origFullTextPat = Pattern.compile(ORIG_FULLTEXT_STRING, Pattern.CASE_INSENSITIVE);
  protected final static Pattern destFullTextPat = Pattern.compile(DEST_FULLTEXT_STRING, Pattern.CASE_INSENSITIVE);
  protected final static Pattern wMarkFullTextPat = Pattern.compile(WMARK_URL, Pattern.CASE_INSENSITIVE);

  public static Pattern getOrigPdfPattern() {
    return origFullTextPat;
  }

  public static Pattern getDestPdfPattern() {
    return destFullTextPat;
  }

  public static Pattern getWaterMarkPattern() {
    return wMarkFullTextPat;
  }

  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    return new IwapUrlConsumer(facade, fud);
  }

  /**
   * <p>
   * A custom URL consumer that identifies specific redirect chains and stores the
   * content at the origin of the chain (e.g. to support collecting and repairing
   * redirect chains that begin with fixed URLs but end with one-time URLs).
   * 
   * </p>
   * 
   */
  public class IwapUrlConsumer extends ScUrlConsumer {

    public IwapUrlConsumer(CrawlerFacade facade,
        FetchedUrlData fud) {
      super(facade, fud);
    }
  }

}
