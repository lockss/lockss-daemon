/*
 * $Id: ArcExploder.java,v 1.1.2.1 2007-09-11 19:14:54 dshr Exp $
 */

/*

Copyright (c) 2007 Board of Trustees of Leland Stanford Jr. University,
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
import java.io.*;
import org.archive.io.*;
import org.archive.io.arc.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.state.*;

/**
 * An Exploder that ingest Internet Archive ARC files,  and behaves
 * as if it had ingested each file in the ARC file directly from its
 * original source.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

public class ArcExploder extends Exploder {

  private static Logger logger = Logger.getLogger("ArcExploder");
  private String arcUrl = null;

  /**
   * Constructor
   * @param uc UrlCacher for the archive
   * @param maxRetries
   * @param crawlSpec the CrawlSpec for the crawl that foudn the archive
   * @param crawler the crawler that found the archive
   * @param explode true to explode the archives
   * @param store true to store the archive as well
   */
  protected ArcExploder(UrlCacher uc, int maxRetries, CrawlSpec crawlSpec,
		     BaseCrawler crawler, boolean explode, boolean store) {
    super(uc, maxRetries, crawlSpec, crawler, explode, store);
    arcUrl = uc.getUrl();
  }

  /**
   * Explode the archive into its constituent elements
   */
  protected void explodeUrl() {
    InputStream arcStream = null;
    CachedUrl cachedUrl = null;
    ArchiveReader arcReader = null;
    logger.info((storeArchive ? "Storing" : "Fetching") + " an ARC file: " +
		arcUrl + (explodeFiles ? " will" : " won't") + " explode");
    try {
      if (storeArchive) {
	crawler.cacheWithRetries(urlCacher, maxRetries);
	// Get a stream from which the ARC data can be read
	logger.debug3("About to get ARC stream from " + urlCacher.toString());
	cachedUrl = urlCacher.getCachedUrl();
	arcStream = cachedUrl.getUnfilteredInputStream();
      } else {
	arcStream = urlCacher.getUncachedInputStream();
      }
      // Wrap it in an ArchiveReader
      logger.debug3("About to wrap stream");
      arcReader = wrapStream(urlCacher, arcStream);
      logger.debug3("wrapStream() returns " +
		    (arcReader == null ? "null" : "non-null"));
      // Explode it
      if (arcReader != null) {
	explode(urlCacher.getUrl(), urlCacher.getArchivalUnit(), arcReader);
      }
    } catch (IOException ex) {
      logger.siteError("ArcCrawler.explode() threw", ex);
    } finally {
      if (arcReader != null) try {
	arcReader.close();
	arcReader = null;
      } catch (IOException ex) {
	logger.error(arcUrl + " arcReader.close() threw ", ex);
      }
      if (cachedUrl != null) {
	cachedUrl.release();
      }
      IOUtil.safeClose(arcStream);
    }
  }

  protected CIProperties makeCIProperties(ArchiveRecordHeader elementHeader) {
    CIProperties ret = new CIProperties();
    Set elementHeaderFieldKeys = elementHeader.getHeaderFieldKeys();
    for (Iterator i = elementHeaderFieldKeys.iterator(); i.hasNext(); ) {
      String key = (String) i.next();
      try {
	String value = (String) elementHeader.getHeaderValue(key).toString();
	logger.debug3(key + ": " + value);
	ret.put(key, value);
      } catch (ClassCastException ex) {
	logger.error("makeCIProperties: " + key + " threw ", ex);
      }
    }
    return (ret);
  }

  protected void explode(String arcUrl, ArchivalUnit au, ArchiveReader arcReader) throws IOException {
    int elementCount = 1;
    Set stemSet = new HashSet();
    logger.debug("Exploding " + arcUrl);
    // Iterate through the elements in the ARC file, except the first
    Iterator i = arcReader.iterator();
    // Skip first record
    i.next();
    while (i.hasNext()) {
      // XXX probably not necessary
      if (crawler.wdog != null) {
	crawler.wdog.pokeWDog();
      }
      ArchiveRecord element = (ArchiveRecord)i.next();
      // Each element is a URL to be cached in the AU
      ArchiveRecordHeader elementHeader = element.getHeader();
      String elementUrl = elementHeader.getUrl();
      String elementMimeType = elementHeader.getMimetype();
      logger.debug2("ARC url " + elementUrl + " mime " + elementMimeType + " # " + elementCount);
      if (elementUrl.startsWith("http:")) {
	// Create a new UrlCacher from the ArchivalUnit and store the
	// element using it.
	UrlCacher newUc = au.makeUrlCacher(elementUrl);
	// XXX either fetch or storeContent synthesizes some properties
	// XXX for the URL - check and move the place to storeContent
	newUc.storeContent(element, makeCIProperties(elementHeader));
	stemSet.add(UrlUtil.getUrlPrefix(elementUrl));
	elementCount++;
      }
    }
    // Now adjust the AU's host list and crawl rules if necessary
    logger.debug("Exploding " + arcUrl + " found " + elementCount + " URLs");
    updateAU(stemSet);
  }

  protected ArchiveReader wrapStream(UrlCacher uc, InputStream arcStream) throws IOException {
    ArchiveReader ret = null;
    if (explodeFiles) {
	logger.debug3("Getting an ArchiveReader");
      ret = ArchiveReaderFactory.get(urlCacher.getUrl(), arcStream, true);
      // Just don't ask why the next line is necessary
      ((ARCReader)ret).setParseHttpHeaders(false);
    }
    return (ret);
  }

  private void updateAU(Set stemSet) {
    for (Iterator i = stemSet.iterator(); i.hasNext(); ) {
      String url = (String)i.next();
      //  XXX do something to the AU.  XXX there is a getUrlStem()
      //  XXX on ArchivalUnit.  For ArcCrawler AUs this should
      //  XXX enumerate the second level of the repo to get
      //  XXX the list.
      logger.debug3("stem set includes: " + url);
    }
  }
}
