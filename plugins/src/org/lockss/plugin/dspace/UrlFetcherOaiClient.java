/*
 * $Id:$
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

package org.lockss.plugin.dspace;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlFetcher;

import com.lyncode.xoai.serviceprovider.client.OAIClient;
import com.lyncode.xoai.serviceprovider.exceptions.HttpException;
import com.lyncode.xoai.serviceprovider.parameters.Parameters;

public class UrlFetcherOaiClient implements OAIClient {
  protected CrawlerFacade facade;
  protected String baseUrl;

  public UrlFetcherOaiClient(String baseUrl, CrawlerFacade cf) {
    this.baseUrl = baseUrl;
    facade = cf;
  }
  
  @Override
  public InputStream execute(Parameters parameters) throws HttpException {
    UrlFetcher uf = facade.makeUrlFetcher(parameters.toUrl(baseUrl));
    BitSet permFetchFlags = uf.getFetchFlags();
    permFetchFlags.set(UrlCacher.REFETCH_FLAG);
    uf.setFetchFlags(permFetchFlags);
    InputStream content;
    try {
      content = uf.getUncachedInputStream();
      if(content == null) {
        throw new HttpException("UrlFetcher returned null imput stream");
      }
    } catch(IOException e) {
      throw new HttpException(e);
    }
    return content;
  }
}
