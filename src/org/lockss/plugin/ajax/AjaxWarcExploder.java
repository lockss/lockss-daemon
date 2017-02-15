/*
 * $Id: AjaxWarcExploder.java 39864 2015-02-18 09:10:24Z thib_gc $
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
package org.lockss.plugin.ajax;

import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.util.ArchiveUtils;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.crawler.CrawlUrlData;
import org.lockss.crawler.WarcExploder;
import org.lockss.daemon.ArchiveEntry;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.LockssWatchdog;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseExploderHelper;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;

/**
 * A modified version of the warc exploder which does not assume that the
 * warc contains a complete au and will therefore not terminate the  crawl.
 */
public class AjaxWarcExploder extends WarcExploder {
  private static Logger logger = Logger.getLogger("AjaxWarcExploder");

  /**
   * Constructor
   * @param toExplode  url data of for the archive to explode
   * @param crawlFacade  facade for crawler performing crawl
   * @param helper helper for exploding archive
   */
  public AjaxWarcExploder(final FetchedUrlData toExplode,
                          final CrawlerFacade crawlFacade,
                          final ExploderHelper helper) {
    super(toExplode, crawlFacade, helper);
  }

  /**
   * Explode the archive into its constituent elements
   */
  public void explode() throws CacheException {
    int goodEntries = 0;
    int badEntries = 0;
    int entriesBetweenSleep = 0;
    ArchiveReader arcReader = null;

    logger.debug((storeArchive ? "Storing" : "Fetching") + " WARC file: " +
                origUrl + " will explode");
    try {
      // Wrap it in an ArchiveReader
      logger.debug3("About to wrap stream");
      arcReader = wrapStream(fetchUrl, arcStream);
      logger.debug3("wrapStream() returns " +
                    (arcReader == null ? "null" : "non-null"));
      // Explode it
      if (arcReader == null) {
        throw new CacheException.ExploderException("no WarcReader for " +
                                                   origUrl);
      }
      ArchivalUnit au = crawlFacade.getAu();
      logger.debug("Exploding " + fetchUrl);
      // Iterate through the elements in the WARC file, except the first
      Iterator<ArchiveRecord>  iter = arcReader.iterator();
      // Skip first record
      if(iter.hasNext()) iter.next();
      while(iter.hasNext()) {
        helper.pokeWDog();
        // check need to pause
        handlePause(++entriesBetweenSleep);
        // handle each element in the archive
        ArchiveRecord element = iter.next();
        // Each element is a URL to be cached in our AU
        ArchiveRecordHeader elementHeader = element.getHeader();
        String elementUrl = elementHeader.getUrl();
        String elementMimeType = elementHeader.getMimetype();
        long elementLength = elementHeader.getLength();
        long elementDate;
        try {
          elementDate = ArchiveUtils.parse14DigitDate(elementHeader.getDate())
                                  .getTime();
        }
        catch (ParseException e) {
          elementDate = 0;
        }
        logger.debug2("WARC url " + elementUrl + " mime " + elementMimeType);
        // add check to determine if this is a url which should be cached
        if (au.shouldBeCached(elementUrl) && elementUrl.startsWith("http:")) {
          ArchiveEntry ae =
            new ArchiveEntry(elementUrl,
                             elementLength,
                             elementDate,
                             element, // ArchiveRecord extends InputStream
                             this,
                             fetchUrl);
          ae.setHeaderFields(makeCIProperties(elementHeader));
          long bytesStored = elementLength;
          logger.debug3("ArchiveEntry: " + ae.getName() + " bytes " +
                        bytesStored);
          try {
            helper.process(ae);
          }
          catch (PluginException ex) {
            throw new CacheException.ExploderException("helper.process() threw",
                                                       ex);
          }
          if (ae.getBaseUrl() != null) {
            if (ae.getRestOfUrl() != null &&
                ae.getHeaderFields() != null) {
              storeEntry(ae);
              handleAddText(ae);
              goodEntries++;
              // this needs to use the correct depth ? how
              CrawlUrlData cud = new CrawlUrlData(elementUrl, 0);
              crawlFacade.addToParseQueue(cud);
              crawlFacade.getCrawlerStatus()
                         .addContentBytesFetched(bytesStored);
            }
          }
          else {
            badEntries++;
            logger.debug2("Can't map " + elementUrl + " from " + archiveUrl);
          }
        }
      }
    }
    catch (IOException ex) {
      throw new CacheException.ExploderException(ex);
    }
    finally {
      if (arcReader != null) {
        try {
          arcReader.close();
        }
        catch (IOException ex) {
          throw new CacheException.ExploderException(ex);
        }
      }
      IOUtil.safeClose(arcStream);
    }
    // report failed fetches
    if (badEntries != 0) {
      String msg = archiveUrl + ": " + badEntries + "/" +
                   goodEntries + " bad entries";
      throw new CacheException.UnretryableException(msg);
    }
  }

  public class AjaxExploderHelper extends BaseExploderHelper {
    private CrawlerFacade crawlFacade;
    protected LockssWatchdog wdog;

    public AjaxExploderHelper(CrawlerFacade crawlFacade) {

      this.crawlFacade = crawlFacade;
    }

    public void process(ArchiveEntry ae) {
      // By default the files have to go in the crawler's AU
      ArchivalUnit au = crawlFacade.getAu();
      // By default the path should start at the AU's base url.
      Configuration config = au.getConfiguration();
      String url = config.get(ConfigParamDescr.BASE_URL.getKey());
      ae.setBaseUrl(url);
      ae.setRestOfUrl(ae.getName());
      CIProperties cip = new CIProperties();

      ae.setHeaderFields(cip);
    }
  }

  private void handlePause(int entriesBetweenSleep) {
    if ((entriesBetweenSleep % sleepAfter) == 0) {
      long pauseTime =
        CurrentConfig.getTimeIntervalParam(PARAM_RETRY_PAUSE,
                                           DEFAULT_RETRY_PAUSE);
      Deadline pause = Deadline.in(pauseTime);
      logger.debug3("Sleeping for " +
                    StringUtil.timeIntervalToString(pauseTime));
      while (!pause.expired()) {
        try {
          pause.sleep();
        }
        catch (InterruptedException ie) {
          // no action
        }
      }
    }
  }
}