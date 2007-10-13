/*
 * $Id: Exploder.java,v 1.4 2007-10-13 03:16:57 tlipkis Exp $
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

import java.io.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.exploded.*;
import org.lockss.crawler.BaseCrawler;
import org.lockss.config.*;
import org.lockss.app.LockssDaemon;
import org.lockss.repository.*;
import org.lockss.state.HistoryRepository;
import org.lockss.filter.StringFilter;

/**
 * The abstract base class for exploders, which handle extracting files from
 * archives.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

public abstract class Exploder {
  private static Logger logger = Logger.getLogger("Exploder");

  public static final String PARAM_EXPLODED_PLUGIN_NAME =
    Configuration.PREFIX + "crawler.exploder.explodedPluginName";
  public static final String DEFAULT_EXPLODED_PLUGIN_NAME =
    ExplodedPlugin.class.getName();

  protected UrlCacher urlCacher;
  protected int maxRetries;
  protected CrawlSpec crawlSpec;
  protected BaseCrawler crawler;
  protected boolean explodeFiles;
  protected boolean storeArchive;
  protected String archiveUrl = null;
  protected Hashtable addTextTo = null;
  protected LockssDaemon daemon = null;
  protected PluginManager pluginMgr = null;
  protected Hashtable touchedAus = new Hashtable();

  protected static final String indexTag = "<!-- Next Entry Goes Here -->\n";
  protected static final String manifestPageTag = "</body>\n";
  protected static final String manifestPageAdd = "<ul>\n" + indexTag +
    "</ul>\n" + manifestPageTag;

  /**
   * Constructor
   * @param uc UrlCacher for the archive
   * @param maxRetries
   * @param crawlSpec the CrawlSpec for the crawl that found the archive
   * @param crawler the crawler that found the archive
   * @param explode true to explode the archives
   * @param store true to store the archive as well
   */
  protected Exploder(UrlCacher uc, int maxRetries, CrawlSpec crawlSpec,
		     BaseCrawler crawler, boolean explode, boolean store) {
    urlCacher = uc;
    this.maxRetries = maxRetries;
    this.crawlSpec = crawlSpec;
    this.crawler = crawler;
    archiveUrl = uc.getUrl();
    explodeFiles = explode;
    storeArchive = store;
    daemon = crawler.getDaemon();
    pluginMgr = daemon.getPluginManager();
  }

  /**
   * Explode the archive into its constituent elements
   */
  protected abstract void explodeUrl();

  protected void storeEntry(ArchiveEntry ae) throws IOException {
    // We assume that all exploded content is organized into
    // AUs which each contain only URLs starting with the AUs
    // base_url.  This allows ExplodedArchivalUnit to maintain
    // a map from a baseUrl to one of its AUs.
    ArchivalUnit au = null;
    String baseUrl = ae.getBaseUrl();
    String restOfUrl = ae.getRestOfUrl();
    CachedUrl cu = pluginMgr.findCachedUrl(baseUrl + restOfUrl, false);
    if (cu != null) {
      au = cu.getArchivalUnit();
      logger.debug(baseUrl + restOfUrl + " old au " + au.getAuId());
      cu.release();
    }
    if (au == null) {
      // There's no AU for this baseUrl,  so create one
      au = createAu(ae, baseUrl);
    }
    if (!(au instanceof ExplodedArchivalUnit)) {
      logger.error("New AU not ExplodedArchivalUnit " + au.toString(),
		   new Throwable());
    }
    touchedAus.put(baseUrl, au);
    String newUrl = baseUrl + restOfUrl;
    // Create a new UrlCacher from the ArchivalUnit and store the
    // element using it.
    BaseUrlCacher newUc = (BaseUrlCacher)au.makeUrlCacher(newUrl);
    BitSet flags = newUc.getFetchFlags();
    flags.set(UrlCacher.DONT_CLOSE_INPUT_STREAM_FLAG);
    newUc.setFetchFlags(flags);
    // XXX either fetch or storeContent synthesizes some properties
    // XXX for the URL - check and move the place to storeContent
    logger.debug3("Storing " + newUrl + " in " + au.toString());
    newUc.storeContentIn(newUrl, ae.getInputStream(), ae.getHeaderFields());
    crawler.getCrawlerStatus().signalUrlFetched(newUrl);
    // crawler.getCrawlerStatus().signalMimeTypeOfUrl(newUrl, mimeType);
  }

  protected ArchivalUnit createAu(ArchiveEntry ae, String baseUrl)
      throws IOException {
    CIProperties props = ae.getAuProps();
    if (props == null) {
      props = new CIProperties();
      props.put(ConfigParamDescr.BASE_URL.getKey(), baseUrl);
    }
    props.put(ConfigParamDescr.PUB_NEVER.getKey(), "true");
    addRepoProp(props);
    String pluginName = CurrentConfig.getParam(PARAM_EXPLODED_PLUGIN_NAME,
					       DEFAULT_EXPLODED_PLUGIN_NAME);
    String key = PluginManager.pluginKeyFromName(pluginName);
    logger.debug3(pluginName + " has key: " + key);
    Plugin plugin = pluginMgr.getPlugin(key);
    if (plugin == null) {
      logger.error(pluginName + " key " + key + " not found");
      throw new IOException(pluginName + " not found");
    }
    if (logger.isDebug3()) {
      logger.debug3("Create AU, plugin: " + pluginName +
		    ": " + plugin.toString());
      logger.debug3("props: " + props);
    }
    try {
      ArchivalUnit au = pluginMgr.createAndSaveAuConfiguration(plugin, props);
      if (au == null) {
	logger.error("Failed to create new AU", new Throwable());
	throw new IOException("Can't create au for " + baseUrl);
      }
      return au;
    } catch (ArchivalUnit.ConfigurationException ex) {
      logger.error("createAndSaveAuConfiguration() threw " + ex.toString());
      throw new IOException(pluginName + " not initialized for " + baseUrl);
    }
  }

  protected void addRepoProp(Properties props) {
    RepositoryManager repoMgr = daemon.getRepositoryManager();
    String bestRepo = repoMgr.findLeastFullRepository();
    if (bestRepo != null) {
      props.put(PluginManager.AU_PARAM_REPOSITORY, bestRepo);
    }
  }

  protected void handleAddText(ArchiveEntry ae) {
    Hashtable addText = ae.getAddText();
    String addBaseUrl = ae.getBaseUrl();
    if (addText != null) {
      if (addTextTo == null) {
	addTextTo = new Hashtable();
      }
      for (Enumeration en = addText.keys(); en.hasMoreElements(); ) {
	String addToUrl = (String)en.nextElement();
	String oldText = "";
	if (addTextTo.containsKey(addToUrl)) {
	  oldText = (String)addTextTo.get(addToUrl);
	}
	String addition = (String)addText.get(addToUrl);
	if (oldText.indexOf(addition) < 0) {
	  oldText += addition;
	  addTextTo.put(addToUrl, oldText);
	  logger.debug3("addText for " + addToUrl +
			" now " + oldText);
	} else {
	  logger.debug3(oldText + " contains " + addition);
	}
      }
    }
  }

  protected void addText() {
    if (addTextTo != null) {
      for (Enumeration en = addTextTo.keys(); en.hasMoreElements(); ) {
	String url = (String) en.nextElement();
	String text = (String) addTextTo.get(url);

	CachedUrl cu = pluginMgr.findCachedUrl(url, false);
	if (cu == null) {
	  logger.error("Trying to update page outside AU" +
		       url);
	} else try {
	  addTextToPage(cu, url, text);
	} catch (IOException e) {
	  logger.error("addTextToPage(" + url + ") threw " + e.toString());
	} finally {
	  cu.release();
	}	  
      }
    }
  }

  protected void addTextToPage(CachedUrl indexCu, String url, String newText)
      throws IOException {
    ArchivalUnit au = indexCu.getArchivalUnit();
    Reader oldPage = null;
    CIProperties props = new CIProperties();
    if (!indexCu.hasContent()) {
      // Create a new page by copying the manifest page and adding the
      // tag that locates the added text.
      logger.debug3("Create new page " + url + " in " + au.getAuId());
      List manifestPages = crawlSpec.getPermissionPages();
      if (manifestPages.isEmpty()) {
	throw new IOException("Permission page list empty for " + url);
      }
      CachedUrl manifestCu =
	pluginMgr.findCachedUrl((String)manifestPages.get(0));
      if (manifestCu == null) {
	throw new IOException("Can't get CachedUrl for " +
			      (String)manifestPages.get(0));
      }
      oldPage = new StringFilter(manifestCu.openForReading(),
				 manifestPageTag, manifestPageAdd);
      props = manifestCu.getProperties();
      props.setProperty("x-lockss-node-url", UrlUtil.minimallyEncodeUrl(url));
    } else {
      logger.debug3("Adding text " + newText + " to page " + url +
		    " in " + au.getAuId());
      oldPage = indexCu.openForReading();
      props = indexCu.getProperties();
    }
    BaseUrlCacher indexUc = (BaseUrlCacher)au.makeUrlCacher(url);
    if (indexUc != null) {
      Reader rdr = new StringFilter(oldPage, indexTag, newText + indexTag);
      indexUc.storeContent(new ReaderInputStream(rdr), props);
      logger.debug3("stored filtered text at " + url);
    } else {
      logger.error("No URL cacher for " + url);
    }
  }

  protected static final String[] extension = {
    ".html",
    ".htm",
    ".txt",
    ".xml",
    ".pdf",
    ".raw",
    ".sgm",
    ".gif",
    ".jpg",
    ".toc",
    ".fil",
    ".sml",
    ".tiff",
    ".doc",
  };
  protected static final String[] contentType = {
    "text/html",
    "text/html",
    "text/plain",
    "application/xml",
    "application/pdf",
    "text/plain",
    "application/sgml",
    "image/gif",
    "image/jpeg",
    "text/plain", // XXX check
    "text/plain", // XXX check
    "application/sgml",
    "image/tiff",
    "application/msword",
  };
  private static HashMap mimeMap = null;

  /**
   * Return a CIProperties object containing a set of header fields
   * and values that seem appropriate for the URL in question.
   */
  public static CIProperties syntheticHeaders(String url, long size) {
    CIProperties ret = new CIProperties();

    if (mimeMap == null) {
      mimeMap = new HashMap();
      for (int i = 0; i < extension.length; i++) {
	mimeMap.put(extension[i], contentType[i]);
      }
    }

    String mimeType = "text/plain";
    int ix = url.lastIndexOf(".");
    if (ix > 0) {
      String mt = (String)mimeMap.get(url.substring(ix));
      if (mt !=null) {
	mimeType = mt;
      }
    }
    logger.debug3(url + " mime-type " + mimeType);
    ret.setProperty("Content-Type", mimeType);
    ret.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, mimeType);

    ret.setProperty(CachedUrl.PROPERTY_NODE_URL, url);
    if (size >= 0) {
      ret.setProperty("Content-Length", Long.toString(size));
    }
    return ret;
  }

  protected class DefaultExploderHelper implements ExploderHelper {
    private UrlCacher uc = null;
    private CrawlSpec crawlSpec = null;
    private Crawler crawler = null;
    private LockssDaemon theDaemon;

    DefaultExploderHelper(UrlCacher uc, CrawlSpec crawlSpec,
			  BaseCrawler crawler) {
      this.uc = uc;
      this.crawlSpec = crawlSpec;
      this.crawler = crawler;
    }

    public void process(ArchiveEntry ae) {
      // By default the files have to go in the crawler's AU
      ArchivalUnit au = crawler.getAu();
      // By default the path should start at the AU's base url.
      Configuration config = au.getConfiguration();
      String url = config.get(ConfigParamDescr.BASE_URL.getKey());
      ae.setBaseUrl(url);
      ae.setRestOfUrl(ae.getName());
      CIProperties cip = new CIProperties();
      // XXX do something here to invent header fields
      ae.setHeaderFields(cip);
    }

    public void setDaemon(LockssDaemon daemon) {
      theDaemon = daemon;
    }
  }
}
