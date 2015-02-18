/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.exploded;

import org.lockss.daemon.Crawler;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ExplodableArchivalUnit;
import org.lockss.plugin.ExploderHelper;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.plugin.definable.DefinableArchivalUnit;

public class ExplodingUrlConsumerFactory implements UrlConsumerFactory {
  public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade crawlFacade,
      FetchedUrlData fud) {
    ArchivalUnit au = crawlFacade.getAu();
    if(au instanceof ExplodableArchivalUnit) {
      ExplodableArchivalUnit eau = (ExplodableArchivalUnit)au;
      ExplodingUrlConsumer euc = new ExplodingUrlConsumer(crawlFacade, fud,
          eau.getExploderPattern());
      ExploderHelper eh = eau.getExploderHelper();
      if(eh != null) {
        euc.setExploderHelper(eh);
      }
      return euc;
    }
    return new SimpleUrlConsumer(crawlFacade, fud);
  }
}
