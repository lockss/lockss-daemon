/*
 * $Id: AJaxCrawlFetcher.java 39864 2015-02-18 09:10:24Z thib_gc $
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.crawler.CrawlUrlData;
import org.lockss.crawljax.AjaxRequestResponse;
import org.lockss.crawljax.AjaxRequestResponse.Header;
import org.lockss.crawljax.AjaxRequestResponse.IndexEntry;
import org.lockss.crawljax.AjaxRequestResponse.Response;
import org.lockss.daemon.Crawler;
import org.lockss.daemon.LockssWatchdog;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.base.BaseUrlFetcher;
import org.lockss.util.CIProperties;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.urlconn.CacheException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AjaxCrawlFetcher  extends BaseUrlFetcher {
  // The Parameters for use by plugins

  public static final String PREFIX =
    Configuration.PREFIX + "extractor.crawljax.";
  public static final String PARAM_DEBUG = PREFIX + "debug";
  public static final String PARAM_OUTPUT_DIR = PREFIX + "outDir";
  public static final String PARAM_CLASSPATH = PREFIX + "classpath";
  public static final String PARAM_CONFIG_FILE = PREFIX + "configFile";
  public static final String LINK_FILE_NAME = "links.txt";

  static final Logger theLogger = Logger.getLogger(AjaxCrawlFetcher.class);


  /**
   * the au for this ajax extractor
   */
  private ArchivalUnit m_au;

  /**
   * the classpath to send when invoking the jar file
   */
  private String m_classpath;

  /**
   * the base command line as configured during construction
   */
  private ArrayList<String> m_baseCommand;

   /* required arguments */
  /**
   * the directory to use for files.
   */
  private String m_outdir;

  /** the configuration file for the crawl */
  private String m_configFile;

 /* optional arguments: all string arguments are set to null, all numbers are -1 */
  /**
   * set true if we should crawl nonvisible links
   */
  private boolean m_debug = false;

  protected LockssWatchdog wdog;

  public AjaxCrawlFetcher(Crawler.CrawlerFacade crawlFacade, String url) {
    super(crawlFacade, url);
    m_classpath = CurrentConfig.getParam(PARAM_CLASSPATH);
    m_outdir = makeCrawlDir(CurrentConfig.getParam(PARAM_OUTPUT_DIR));
    m_configFile = CurrentConfig.getParam(PARAM_CONFIG_FILE);
    m_debug = CurrentConfig.getBooleanParam(PARAM_DEBUG, false);
    m_baseCommand = makeBaseCommand();
  }


  public FetchResult fetch() {
    int exitValue = -1;
    List<String> command = new ArrayList<String>();
    command.addAll(m_baseCommand);
    command.add(fetchUrl);
    command.add(m_outdir);
    try {
      ProcessBuilder pbuilder = new ProcessBuilder(command);
      pbuilder.redirectErrorStream(true);
      Process process = pbuilder.start();
      InputStreamReader isr = new  InputStreamReader(process.getInputStream());
      BufferedReader br = new BufferedReader(isr);

      if(theLogger.isDebug())
        theLogger.debug("Running command:" + pbuilder.command() +"...");
      String line;
      while ((line = br.readLine()) != null) {
        if(theLogger.isDebug())
          theLogger.debug(line);
      }
      exitValue = process.waitFor();
      if(theLogger.isDebug()) {
        theLogger.debug("Ajax Crawl for " + fetchUrl + " exit: " + exitValue);
      }
      // process
      File outDir = new File(m_outdir);
      // process the index file
      File idxFile = new File(outDir,AjaxRequestResponse.INDEX_FILE_NAME);
      processIndexFile(idxFile);
    } catch (IOException ioe) {
      theLogger.warning("Ajax Crawl Failed.", ioe);
    } catch (InterruptedException e) {
      theLogger.warning("Ajax Crawl Interrupted:" + exitValue, e);
    }
    return null;
  }


  /**
   * Get a unique output directory name
   * @return the absolute path to the newly created directory.
   */
  protected String makeCrawlDir(String outdir)
  {
    String fdir = String.valueOf(System.currentTimeMillis());
    File file = new File(outdir, m_au.getAuId() + "_" + fdir);
    file.mkdirs();
    return file.getAbsolutePath();
  }

  /**
   * Process the index file.  This is the most reliable way to process
   * the output. It contains a list of request url to Request-Response output
   * file names.
   * @param indexFile the file containing our json file
   * @throws IOException
   */
  protected void processIndexFile(File indexFile) throws IOException {
    if(indexFile.exists() && indexFile.canRead()) {
      // we load in the json file and read it.
      ObjectMapper mapper = new ObjectMapper();
      List<IndexEntry> entries = mapper.readValue(indexFile,
                                                  new TypeReference<List<IndexEntry>>(){});
      // now we have the urls and their corresponding result files
      String reqUrl;
      String filePath;
      for(IndexEntry entry : entries) {
        reqUrl = entry.getUrl();
        filePath = entry.getFile();
        processOneFile(reqUrl, reqUrl, filePath);
      }
    }
  }

  /**
   * process a AjaxRequestResponse file we fetched
   * @param reqUrl the url that was sent to the server.
   * @param filePath the location of the file returned.
   * @throws IOException
   */
  private FetchResult processOneFile(String reqUrl, String fetchUrl,
                                String filePath) throws IOException{
    File rrFile = new File(filePath);
    ObjectMapper mapper = new ObjectMapper();
    AjaxRequestResponse rr = mapper.readValue(rrFile, AjaxRequestResponse.class);
    Response response = rr.getResponse();
    CIProperties headers = makeCIProperties(response.getHeaders());
    String ctype = headers.getProperty("Content-Type");
    if (!StringUtil.isNullString(ctype)) {
      headers.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, ctype);
    }
    ByteArrayInputStream input = new ByteArrayInputStream(response.getContent());

    FetchedUrlData fud = new FetchedUrlData(reqUrl, fetchUrl,
                                            input, headers,
                                            redirectUrls, this);
    CrawlUrlData cud = new CrawlUrlData(reqUrl, curl.getDepth());

    try {
      fud.setStoreRedirects(redirectScheme
                              .isRedirectOption(RedirectScheme.REDIRECT_OPTION_STORE_ALL));
      fud.setFetchFlags(fetchFlags);
      getUrlConsumerFactory().createUrlConsumer(crawlFacade, fud).consume();
      updateCacheStats(FetchResult.FETCHED, cud );
      crawlFacade.addToParseQueue(cud);
    } catch (CacheException ex) {
      crawlStatus.signalErrorForUrl(reqUrl, ex);
      crawlStatus.setCrawlStatus(Crawler.STATUS_FETCH_ERROR);
      return FetchResult.NOT_FETCHED;
    }
    return FetchResult.FETCHED;

  }

  /**
   * update the cache stats for each fetched url which is not
   * @param res  the result to use for setting the specific crawl result
   * @param curl the CrawlUrlData used to crawl the ajax content
   */
  protected void updateCacheStats(FetchResult res, CrawlUrlData curl) {
    switch (res) {
      case FETCHED:
        crawlStatus.signalUrlFetched(curl.getUrl());
        curl.setFetched(true);
        CachedUrl cu = au.makeCachedUrl(curl.getUrl());

        if (cu.hasContent()) {
          String conType = cu.getContentType();
          if (conType != null) {
            String mimeType = HeaderUtil.getMimeTypeFromContentType(conType);
            crawlStatus.signalMimeTypeOfUrl(mimeType, cu.getUrl());
          }
          crawlStatus.addContentBytesFetched(cu.getContentSize());
          previousContentType = cu.getContentType();
        }
        cu.release();
      break;
      case NOT_FETCHED:
        curl.setFetched(false);
        break;
    }
  }

  /**
   * make the command line based on the values from the context.  This
   * excludes the outdir and url which are added dynamically.
   * "java -jar lockss-crawljax.jar configFile outDir url";
   * @return  the command line as an array
   */
  protected ArrayList<String> makeBaseCommand()
  {
    ArrayList<String> params = new ArrayList<String>();
    params.add("java");
    if(m_classpath != null) {
      params.add("-cp");
      params.add(System.getProperty("java.class.path").concat(";").concat(m_classpath));
    }
    params.add("-jar");
    params.add("lockss-crawljax.jar");
    if(m_configFile != null)
      params.add(m_configFile);
    return params;
  }

  protected CIProperties makeCIProperties(List<Header> headers)
    throws IOException {
    CIProperties ret = new CIProperties();
    for(Header header : headers) {
      String key = header.getName();
      String value = header.getValue();
      if(value == null) {
        theLogger.warning("Ignoring null value for key '" + key + "'.");
      }
      else {
        theLogger.debug3(key + ": " + value);
        ret.put(key, value);
      }
    }
    return (ret);
  }

}
