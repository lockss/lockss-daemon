/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.asm;

import java.io.IOException;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;
/**
 * @since 1.67.5 
 */
public class ASMscienceUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(ASMscienceUrlConsumerFactory.class);

  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    log.debug3("Creating an ASM UrlConsumer");
    return new ASMscienceUrlConsumer(facade, fud);
  }

  /**
   * <p>
   * A custom URL consumer that identifies specific redirect chains and stores the
   * content at the origin of the chain (e.g. to support collecting and repairing
   * redirect chains that begin with fixed URLs but end with one-time URLs).
   * </p>
   * 
   * BOOKS: 
   * the link on the page is: 
   *   http://www.asmscience.org/deliver/fulltext/10.1128/9781555818357/9781555819101_Chap09.pdf?itemId=/content/book/10.1128/9781555818357.chap9&mimeType=pdf&isFastTrackArticle=
   * it redirects to a one-time expiring URL under docserver
   *   http://www.asmscience.org/docserver/fulltext/10.1128/9781555818357/9781555819101_Chap09.pdf?expires=1426886854&id=id&accname=4398&checksum=B4D5048F55C46BDB558351CDD802FC1D
   * we only want to store the original, stable URL with the contents collected from the one-time URL
   * 
   * similarly to the PDF above, is the xml version, which doesn't (currently?) have a link on the browser page
   * But the html version of the same link just leads to a blank page, so we don't collect it.
   * Still leave "html" in the match in case they ever implement this
   * 
   * JOURNALS - slightly different URL layout, but same function
   * http://www.asmscience.org/deliver/fulltext/microbiolspec/2/6/AID-0022-2014.pdf
   *       ?itemId=/content/journal/microbiolspec/10.1128/microbiolspec.AID-0022-2014&mimeType=pdf&isFastTrackArticle=
   * becomes one-time URL
   * http://www.asmscience.org/docserver/fulltext/microbiolspec/2/6/AID-0022-2014.pdf
   *      ?expires=1427934811&id=id&accname=guest&checksum=0288048D2DD65A740231D10DDF18CE58
   *      
   * @since 1.67.5
   */
  public class ASMscienceUrlConsumer extends SimpleUrlConsumer {

    // Will become a definitional param
    public static final String ASM_BASE = "http://www\\.asmscience\\.org/";
    public static final String DEL_URL = "deliver/fulltext/";
    public static final String DOC_URL = "docserver/fulltext/";
    public static final String DOC_ARGS = "\\?expires=[^&]+&id=id&accname=[^&]+&checksum=.+$";

    public static final String ORIG_FULLTEXT_STRING = "^" + ASM_BASE + DEL_URL + ".*\\.(xml|pdf|html)"; // it has arguments...
    public static final String DEST_FULLTEXT_STRING = "^" + ASM_BASE + DOC_URL + ".*\\.(xml|pdf|html)" + DOC_ARGS;

    protected Pattern origFullTextPat = Pattern.compile(ORIG_FULLTEXT_STRING, Pattern.CASE_INSENSITIVE);
    protected Pattern destFullTextPat = Pattern.compile(DEST_FULLTEXT_STRING, Pattern.CASE_INSENSITIVE);

    public ASMscienceUrlConsumer(CrawlerFacade facade,
        FetchedUrlData fud) {
      super(facade, fud);
    }

    @Override
    public void consume() throws IOException {
      if (shouldStoreRedirectsAtOrigUrl()) {
        // SimpleUrlConsumer stores at fud.origUrl, and processes the redirect
        // chain if (fud.redirectUrls != null && fud.fetchUrl != null)
        log.debug3("swallowing redirection - use orig: " + fud.origUrl + "not fetch: " + fud.fetchUrl);
        fud.redirectUrls = null;
        fud.fetchUrl = null;
        fud.headers.remove(CachedUrl.PROPERTY_REDIRECTED_TO);
      }
      super.consume();
    }

    /**
     * <p>
     * Determines if a particular redirect chain should cause content to be stored
     * only at the origin URL ({@link FetchedUrlData#origUrl}).
     * </p>
     * 
     * @return True if and only if the fetched URL data represents a particular
     *         redirect chain that should cause content to be stored only at the
     *         origin URL.
     * @since 1.67.5
     */
    protected boolean shouldStoreRedirectsAtOrigUrl() {
      boolean should =  fud.redirectUrls != null
          && fud.redirectUrls.size() == 1
          && fud.redirectUrls.get(0).equals(fud.fetchUrl)
          && destFullTextPat.matcher(fud.fetchUrl).find()
          && origFullTextPat.matcher(fud.origUrl).find();
      if (!should) {
        log.debug3("NOT swallowing this redirect");
      }
      return should;
    }

  }

}
