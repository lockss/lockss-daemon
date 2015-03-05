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

package org.lockss.plugin.springer.api;

import java.io.IOException;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.base.SimpleUrlConsumer;

public class SpringerApiUrlConsumer extends SimpleUrlConsumer {

  protected Pattern origPdfPat;
  
  protected Pattern destPdfPat;
  
  public SpringerApiUrlConsumer(CrawlerFacade facade,
                                FetchedUrlData fud) {
    super(facade, fud);
    origPdfPat = Pattern.compile(String.format("^%scontent/pdf/.*\\.pdf$",
                                               facade.getAu().getConfiguration().get(ConfigParamDescr.BASE_URL.getKey())),
                                 Pattern.CASE_INSENSITIVE);
    destPdfPat = Pattern.compile(String.format("^%sstatic/pdf/.*\\.pdf?auth[^=]*=[^&]*&ext=.pdf$",
                                               SpringerApiCrawlSeed.CDN_URL),
                                 Pattern.CASE_INSENSITIVE);
  }

  @Override
  public void consume() throws IOException {
    if (storeRedirectsAtOrigUrl()) {
      fud.fetchUrl = null;
      fud.redirectUrls = null;
    }
    super.consume();
  }

  protected boolean storeRedirectsAtOrigUrl() {
    return fud.redirectUrls != null
        && fud.redirectUrls.size() == 1
        && fud.redirectUrls.get(0).equals(fud.fetchUrl)
        && destPdfPat.matcher(fud.fetchUrl).find()
        && origPdfPat.matcher(fud.origUrl).find();
  }
  
}
