/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pubfactory;


import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;

import java.io.IOException;

/**
 * UNUSED CURRENTLY
 * Tho the links to the pdf are 
 * blah/downloadpdf/x/y/z.xml
 * which redirect to
 * blah/downloadpdf/x/y/z.pdf
 * after discussion we've decided to leave both versions in - so that if the pdf is ever linked to direclty, we have it
 * 
 */
public class PubFactoryUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(PubFactoryUrlConsumerFactory.class);
  
  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade crawlFacade,
      FetchedUrlData fud) {
    return new PubfactoryUrlConsumer(crawlFacade, fud);
  }
  
  public class PubfactoryUrlConsumer extends SimpleUrlConsumer {
    
    public PubfactoryUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }
    
    @Override
    public void consume() throws IOException {
      if (shouldStoreAtOrigUrl()) {
        storeAtOrigUrl();
      }
      super.consume();
    }
    
    /**
     * https://www.berghahnjournals.com/downloadpdf/journals/boyhood-studies/10/1/bhs100105.pdf
     * https://www.berghahnjournals.com/downloadpdf/journals/boyhood-studies/10/1/bhs100105.xml (consumed in to above)
     */
    public boolean shouldStoreAtOrigUrl() throws IOException {
    	boolean should = false;
/*
    	if (fud.redirectUrls != null && (fud.redirectUrls.size() > 0)) {
    		//the fetched = original but for the terminating ".xml" and ".pdf"
    		if (fud.fetchUrl.endsWith(".pdf") &&
    				fud.origUrl.endsWith(".xml") &&
    				fud.fetchUrl.substring(0,fud.fetchUrl.lastIndexOf(".pdf")).equals(fud.origUrl.substring(0,fud.origUrl.lastIndexOf(".xml")))) {
    			should = true;
    		}
    		log.debug3("Berg redirect: " + fud.redirectUrls.size() + " " + fud.origUrl + " to " + fud.fetchUrl + " : " + should);
    	}
    	*/
    	return should;
    }
  }
}
