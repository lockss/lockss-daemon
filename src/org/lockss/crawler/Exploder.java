/*
 * $Id$
 */

/*

Copyright (c) 2007-2011 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.plugin.*;
import org.lockss.plugin.PluginManager.CuContentReq;
import org.lockss.plugin.ExploderHelper;
import org.lockss.plugin.base.BaseExploderHelper;
import org.lockss.plugin.exploded.*;
import org.lockss.config.*;
import org.lockss.app.LockssDaemon;
import org.lockss.repository.*;
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
  public static final String PARAM_EXPLODED_AU_YEAR =
    Configuration.PREFIX + "crawler.exploder.explodedAuYear";
  public static final String DEFAULT_EXPLODED_AU_YEAR = "none";
  public static final String PARAM_EXPLODED_AU_COLLECTION =
    Configuration.PREFIX + "crawler.exploder.explodedAuCollection";
  public static final String DEFAULT_EXPLODED_AU_COLLECTION = "none";
  public static final String PARAM_EXPLODER_ENTRIES_PER_PAUSE =
    Configuration.PREFIX + "crawler.exploder.entriesPerPause";
  public static final long DEFAULT_EXPLODER_ENTRIES_PER_PAUSE = 200;
  public static final String PARAM_RETRY_PAUSE =
    Configuration.PREFIX + "crawler.exploder.retryPause";
  public static final long DEFAULT_RETRY_PAUSE = 3*Constants.SECOND;
  public static final String PARAM_EXPLODED_AU_BASE_URL =
    Configuration.PREFIX + "crawler.exploder.explodedAuBaseUrl";
  public static final String DEFAULT_EXPLODED_AU_BASE_URL = "none";
  /** Store archive files in addition to exploding them */
  public static final String PARAM_STORE_ARCHIVES =
      Configuration.PREFIX + "crawler.storeArchives";
  public static final boolean DEFAULT_STORE_ARCHIVES = false;


  protected boolean storeArchive;
  protected String archiveUrl = null;
  protected Hashtable addTextTo = null;
  protected LockssDaemon daemon = null;
  protected PluginManager pluginMgr = null;
  protected Set touchedAus = new HashSet();
  protected long sleepAfter = 1;
  protected boolean multipleStemsPerAu = false;	/* XXX Probably no longer works */
  protected ExplodedArchivalUnit singleAU = null;
  protected String explodedAUBaseUrl = null;
  protected CrawlerFacade crawlFacade;
  protected ArchivalUnit au;
  protected ExploderHelper helper;
  protected String fetchUrl;
  protected String origUrl;
  protected FetchedUrlData archiveData;
  
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
  protected Exploder(FetchedUrlData toExplode, CrawlerFacade crawlFacade,
      ExploderHelper helper) {
    archiveData = toExplode;
    this.crawlFacade = crawlFacade;
    this.helper = helper;
    au = crawlFacade.getAu();
    
    fetchUrl = toExplode.fetchUrl;
    origUrl = toExplode.origUrl;
    if(helper == null) {
      helper = new DefaultExploderHelper(crawlFacade);
    }
    daemon = AuUtil.getDaemon(au);
    pluginMgr = daemon.getPluginManager();
    sleepAfter =
        CurrentConfig.getLongParam(PARAM_EXPLODER_ENTRIES_PER_PAUSE,
           DEFAULT_EXPLODER_ENTRIES_PER_PAUSE);
    storeArchive = CurrentConfig.getBooleanParam(PARAM_STORE_ARCHIVES,
        DEFAULT_STORE_ARCHIVES);
  }

  /**
   * Explode the archive into its constituent elements
   */
  public abstract void explode() throws CacheException;
  
  public String getArchiveUrl() {
    return fetchUrl;
  }

  protected void storeEntry(ArchiveEntry ae) throws IOException {
    // We assume that all exploded content is organized into
    // AUs which each contain only URLs starting with the AUs
    // base_url.  This allows ExplodedArchivalUnit to maintain
    // a map from a baseUrl to one of its AUs.
    ArchivalUnit au = null;
    String baseUrl = ae.getBaseUrl();
    String restOfUrl = ae.getRestOfUrl();
    CachedUrl cu = pluginMgr.findCachedUrl(baseUrl + restOfUrl,
					   CuContentReq.DontCare);
    if (cu != null) {
      au = cu.getArchivalUnit();
      logger.debug(baseUrl + restOfUrl + " old au " + au.getAuId());
      cu.release();
    }
    if (au == null) {
      /*
       * There's no existing AU for this URL, we have to make a new one.
       * All newly created AUs need to be ExplodedArchivalUnit, because
       * there's currently no way to specify the plugin to use to create
       * the AU. Exploding into AUs that were previously created by
       * crawling is OK (e.g. for LuKII).
       */
      if (multipleStemsPerAu) {
      	logger.debug3("New single AU for base url " + baseUrl);
      	if (singleAU == null) {
      	  singleAU = createAu(ae);
      	  singleAU.addUrlStemToAU(ae.getBaseUrl());
      	}
      	// Add this baseUrl to the stem set for the AU we're making
      	singleAU.addUrlStemToAU(ae.getBaseUrl());
      	pluginMgr.addHostAus(singleAU);
      	au = singleAU;
            } else {
      	logger.debug3("New AU for " + ae.getBaseUrl());
      	// There's no AU for this baseUrl,  so create one
      	au = createAu(ae);
      }
      if (au == null) {
        IOException ex = new IOException("No new AU for " + ae.getBaseUrl());
        logger.error(ex.toString() + new Throwable());
        throw ex;
      }
      if (!(au instanceof ExplodedArchivalUnit)) {
        IOException ex = new IOException(au.toString() + " wrong type");
        logger.error("New AU not ExplodedArchivalUnit " + au.toString(), ex);
        throw ex;
      }
    }
    touchedAus.add(au);
    String newUrl = baseUrl + restOfUrl;
    // Create a new UrlCacher from the ArchivalUnit and store the
    // element using it.
    logger.debug3("Storing " + newUrl + " in " + au.toString());
    CIProperties newProps = ae.getHeaderFields();
    String ctype = newProps.getProperty("Content-Type");
    if (!StringUtil.isNullString(ctype)) {
      newProps.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, ctype);
    }
    UrlCacher newUc = au.makeUrlCacher(
            new UrlData(ae.getInputStream(),newProps, newUrl));
    BitSet flags = newUc.getFetchFlags();
    flags.set(UrlCacher.DONT_CLOSE_INPUT_STREAM_FLAG);
    newUc.setFetchFlags(flags);
    newUc.storeContent();
    String mimeType = (String)newProps.get(CachedUrl.PROPERTY_CONTENT_TYPE);
    // XXX other stats to update?
  }

  protected ExplodedArchivalUnit createAu(ArchiveEntry ae)
      throws IOException {
    CIProperties props = ae.getAuProps();
    if (props == null) {
      props = new CIProperties();
      props.put(ConfigParamDescr.BASE_URL.getKey(), ae.getBaseUrl());
    }
    props.put(ConfigParamDescr.PUB_NEVER.getKey(), "true");
    addRepoProp(props);
    if (props.get(ConfigParamDescr.YEAR.getKey()) == null) {
      String year = CurrentConfig.getParam(PARAM_EXPLODED_AU_YEAR,
					   DEFAULT_EXPLODED_AU_YEAR);
      props.put(ConfigParamDescr.YEAR.getKey(), year);
    }
    if (props.get(ConfigParamDescr.COLLECTION.getKey()) == null) {
      String collection = CurrentConfig.getParam(PARAM_EXPLODED_AU_COLLECTION,
					   DEFAULT_EXPLODED_AU_COLLECTION);
      props.put(ConfigParamDescr.COLLECTION.getKey(), collection);
    }

    String pluginName = CurrentConfig.getParam(PARAM_EXPLODED_PLUGIN_NAME,
					       DEFAULT_EXPLODED_PLUGIN_NAME);
    logger.debug2("Creating AU: " + pluginName + " " + props);
    String key = PluginManager.pluginKeyFromName(pluginName);
    logger.debug3(pluginName + " has key: " + key);
    pluginMgr.ensurePluginLoaded(key);
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
      	throw new IOException("Can't create au for " + ae.getBaseUrl());
      }
      return (ExplodedArchivalUnit)au;
    } catch (ArchivalUnit.ConfigurationException ex) {
      logger.error("createAndSaveAuConfiguration() threw " + ex.toString());
      throw new IOException(pluginName + " not initialized for " + ae.getBaseUrl());
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
    Hashtable stringsToAdd = ae.getAddText();
    String addBaseUrl = ae.getBaseUrl();
    if (stringsToAdd != null) {
      if (addTextTo == null) {
      	addTextTo = new Hashtable();
      }
      for (Enumeration en = stringsToAdd.keys(); en.hasMoreElements(); ) {
      	String addToUrl = (String)en.nextElement();
      	String oldText = "";
      	if (addTextTo.containsKey(addToUrl)) {
      	  oldText = (String)addTextTo.get(addToUrl);
      	}
      	String addition = (String)stringsToAdd.get(addToUrl);
      	if (oldText.indexOf(addition) < 0) {
      	  oldText += addition;
      	  addTextTo.put(addToUrl, oldText);
      	  logger.debug3("stringsToAdd for " + addToUrl +
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
      
      	CachedUrl cu = pluginMgr.findCachedUrl(url, CuContentReq.DontCare);
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
      Collection<String> manifestPages = au.getPermissionUrls();
      if (manifestPages.isEmpty()) {
	throw new IOException("Permission page list empty for " + url);
      }
      CachedUrl manifestCu =
          pluginMgr.findCachedUrl((String)manifestPages.iterator().next());
      if (manifestCu == null) {
        throw new IOException("Can't get CachedUrl for " +
			      manifestPages.iterator().next());
      }
      oldPage = new StringFilter(manifestCu.openForReading(),
				 manifestPageTag, manifestPageAdd);
      props = manifestCu.getProperties();
      props.setProperty("x-lockss-node-url", UrlUtil.minimallyEncodeUrl(url));
      manifestCu.release();
    } else {
      logger.debug3("Adding text " + newText + " to page " + url +
		    " in " + au.getAuId());
      oldPage = indexCu.openForReading();
      props = indexCu.getProperties();
    }
    Reader rdr = new StringFilter(oldPage, indexTag, newText + indexTag);
    UrlCacher indexUc = au.makeUrlCacher(
        new UrlData(new ReaderInputStream(rdr), props, url));
    indexUc.storeContent();
    logger.debug3("stored filtered text at " + url);
  }

  /**
   * Return a CIProperties object containing a set of header fields
   * and values that seem appropriate for the URL in question.
   */
  public static CIProperties syntheticHeaders(String url, long size) {
    CIProperties ret = new CIProperties();

    String mimeType = "text/plain";
    int ix = url.lastIndexOf(".");
    if (ix > 0) {
      String mt = MimeUtil.getMimeTypeFromExtension(url.substring(ix));
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

  public class DefaultExploderHelper extends BaseExploderHelper {
    private CrawlerFacade crawlFacade;
    protected LockssWatchdog wdog;

    public DefaultExploderHelper(CrawlerFacade crawlFacade) {
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
      // XXX do something here to invent header fields
      ae.setHeaderFields(cip);
    }
  }
}
