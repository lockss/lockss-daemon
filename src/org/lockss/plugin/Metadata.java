/*
 * $Id: Metadata.java,v 1.10 2010-05-27 18:37:04 pgust Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.io.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.poller.*;
import org.lockss.repository.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.definable.*;

/**
 * Collect and search metadata, supporting metadata-based access to content.
 */
public class Metadata extends Properties {
  private static Logger log = Logger.getLogger("Metadata");

  public static final String PARAM_DOIMAP =
    Configuration.PREFIX + "metadata.doimap";
  public static final String DEFAULT_DOIMAP = "doi";
  public static final String PARAM_DOI_ENABLE =
    Configuration.PREFIX + "metadata.doi_enable";
  public static final Boolean DEFAULT_DOI_ENABLE = false;
  public static final String PARAM_OPENURLMAP =
    Configuration.PREFIX + "metadata.openurlmap";
  public static final String DEFAULT_OPENURLMAP = "openurl";
  public static final String PARAM_OPENURL_ENABLE =
    Configuration.PREFIX + "metadata.openurl_enable";
  public static final Boolean DEFAULT_OPENURL_ENABLE = false;

  // XXX maps should persist across daemon restart
  // XXX should lookup DOI prefix to get map in which to look up suffix
  private static CIProperties doiMap = null;
  // XXX should lookup ISSN to get map in which to look up rest of
  // XXX OpenURL metadata
  private static CIProperties openUrlMap = null;

  private static void initDoiMap() {
    Configuration config = CurrentConfig.getCurrentConfig();
    if (!config.getBoolean(PARAM_DOI_ENABLE, DEFAULT_DOI_ENABLE)) {
      return;
    }
    if (doiMap == null) {
      String doiFileName = config.get(PARAM_DOIMAP, DEFAULT_DOIMAP);
      log.debug("initDoiMap(" + doiFileName + ")");
      File doiFile = new File(doiFileName);
      if (doiFile.exists()) {
	FileInputStream fis = null;
	try {
	  fis = new FileInputStream(doiFile);
	  if (fis != null) {
	    doiMap = new CIProperties();
	    doiMap.load(fis);
	  }
	} catch (IOException ex) {
	  log.error(doiFile + " threw " + ex);
	}
      } else {
	// There isn't a cached DOI map - create one
	// XXX this isn't feasible in production because it
	// XXX would take too long and the map would be way
	// XXX too big, but it is OK for a demo.
	doiMap = createDoiMap();
	if (doiMap != null) {
	  FileOutputStream fos = null;
	  try {
	    fos = new FileOutputStream(new File(doiFileName));
	    if (fos != null) {
	      doiMap.store(fos, "Doi Map");
	    }
	  } catch (IOException ex) {
	    log.error(doiFileName + " threw " + ex);
	  } finally {
	    IOUtil.safeClose(fos);
	  }
	}
      }
    }
  }

  protected static CIProperties createDoiMap() {
    PluginManager pluginMgr = LockssDaemon.getLockssDaemon().getPluginManager();

    CIProperties ret = new CIProperties();
    for (ArchivalUnit au : pluginMgr.getAllAus()) {
      if (pluginMgr.isInternalAu(au)) {
	continue;
      }
      for (Iterator iter = au.getArticleIterator(); iter.hasNext(); ) {
	BaseCachedUrl cu = (BaseCachedUrl)iter.next();
	try {
	  if (cu.hasContent()) {
	    Metadata md = cu.getMetadataExtractor().extract(cu);
	    if (md != null) {
	      String doi = md.getDOI();
	      if (doi != null) {
		ret.put(doi, cu.getUrl());
	      } else {
		log.warning(cu.getUrl() + " has no DOI ");
	      }
	    }      
	  }
	} catch (IOException e) {
	  log.warning("createDoiMap() threw " + e);
	} catch (PluginException e) {
	  log.warning("createDoiMap() threw " + e);
	} finally {
	  AuUtil.safeRelease(cu);
	}
      }
    }
    return ret;
  }

  private static void initOpenUrlMap() {
    Configuration config = CurrentConfig.getCurrentConfig();
    if (!config.getBoolean(PARAM_OPENURL_ENABLE, DEFAULT_OPENURL_ENABLE)) {
      return;
    }
    if (openUrlMap == null) {
      String openUrlFileName = config.get(PARAM_OPENURLMAP, DEFAULT_OPENURLMAP);
      log.debug("initOpenUrlMap(" + openUrlFileName + ")");
      File openUrlFile = new File(openUrlFileName);
      if (openUrlFile.exists()) {
	FileInputStream fis = null;
	try {
	  fis = new FileInputStream(openUrlFile);
	  if (fis != null) {
	    // There is a cached OpenURL map
	    openUrlMap = new CIProperties();
	    openUrlMap.load(fis);
	  }
	} catch (IOException ex) {
	  log.error(openUrlFileName + " threw " + ex);
	} finally {
	  IOUtil.safeClose(fis);
	}
      } else {
	// There isn't a cached OpenURL map - create one
	// XXX this isn't feasible in production because it
	// XXX would take too long and the map would be way
	// XXX too big, but it is OK for a demo.
	openUrlMap = createOpenUrlMap();
	if (openUrlMap != null) {
	  FileOutputStream fos = null;
	  try {
	    fos = new FileOutputStream(new File(openUrlFileName));
	    if (fos != null) {
	      openUrlMap.store(fos, "OpenURL Map");
	    }
	  } catch (IOException ex) {
	    log.error(openUrlFileName + " threw " + ex);
	  } finally {
	    IOUtil.safeClose(fos);
	  }
	}
      }
    }
  }

  protected static CIProperties createOpenUrlMap() {
    PluginManager pluginMgr = LockssDaemon.getLockssDaemon().getPluginManager();

    CIProperties ret = new CIProperties();
    for (ArchivalUnit au : pluginMgr.getAllAus()) {
      if (pluginMgr.isInternalAu(au)) {
	continue;
      }
      for (Iterator iter = au.getArticleIterator(); iter.hasNext(); ) {
	BaseCachedUrl cu = (BaseCachedUrl)iter.next();
	try {
	  if (cu.hasContent()) {
	    Metadata md = cu.getMetadataExtractor().extract(cu);
	    if (md != null) {
	      // Key for OpenURL map is
	      // issn + "/" + volume + "/" + issue + "/" + spage
	      String issn = md.getISSN();
	      String volume = md.getVolume();
	      String issue = md.getIssue();
	      String spage = md.getStartPage();
	      if (issn != null && volume != null &&
		  issue != null && spage != null) {
		String key = issn + "/" + volume + "/" + issue + "/" + spage;
		ret.put(key, cu.getUrl());
	      } else {
		log.warning(cu.getUrl() + " has content but bad metadata " +
			    (issn == null ? "null" : issn) + "/" +
			    (volume == null ? "null" : volume) + "/" +
			    (issue == null ? "null" : issue) + "/" +
			    (spage == null ? "null" : spage));
	      }
	    }      
	  }
	} catch (IOException e) {
	  log.warning("createOpenUrlMap() threw " + e);
	} catch (PluginException e) {
	  log.warning("createOpenUrlMap() threw " + e);
	} finally {
	  AuUtil.safeRelease(cu);
	}
      }
    }
    return ret;
  }

  public static String doiToUrl(String doi) {
    String ret = null;
    if (doiMap == null) {
      initDoiMap();
    }
    if (doiMap != null) {
      ret = doiMap.getProperty(doi);
    }
    log.debug2("doiToUrl(" + doi + ") = " + (ret == null ? "null" : ret));
    return ret;
  }

  public static String openUrlToUrl(String openUrl) {
    String ret = null;
    if (openUrlMap == null) {
      initOpenUrlMap();
    }
    if (openUrlMap != null) {
      ret = openUrlMap.getProperty(openUrl);
    }
    return ret;
  }

  protected static void doiForUrl(String doi, String url) {
    if (doiMap == null) {
      initDoiMap();
    }
    if (doiMap != null) {
      doiMap.setProperty(doi, url);
    }
  }

  protected static void openUrlForUrl(String openUrl, String url) {
    if (openUrlMap == null) {
      initOpenUrlMap();
    }
    if (openUrlMap != null) {
      openUrlMap.setProperty(openUrl, url);
    }
  }

  private static String[] doiResolvers = {
    "http://dx.doi.org/",
  };
  private static String[] openUrlResolvers = {
    "http://www.crossref.org/openurl?",
  };
  // If the URL specifies a publisher's DOI or OpenURL resolver,
  // strip the stuff before the ?, reformat the rest and hand it
  // to the Metadata resolver to get the URL for the content in
  // the cache.
  public static String proxyResolver(String url) {
    String ret = null;
    if (StringUtil.isNullString(url)) {
      return ret;
    }
    log.debug2("proxyResolver(" + url + ")");
    boolean found = false;
    // Is it a DOI resolver URL?
    // XXX should use host part to find plugin, then ask plugin if
    // XXX URL specifies resolver, and if so get it to reformat
    // XXX resolver query and feed to Metadata.
    for (int i = 0; i < doiResolvers.length; i++) {
      if (url.startsWith(doiResolvers[i])) {
	String param = url.substring(doiResolvers[i].length());
	log.debug3("doiResolver: " + url + " doi " + param);
	String newUrl =
	  Metadata.doiToUrl(param);
	if (newUrl != null) {
	  ret = newUrl;
	  found = true;
	}
      }
    }
    if (!found) {
      for (int i = 0; i < openUrlResolvers.length; i++) {
	if (url.startsWith(openUrlResolvers[i])) {
	  // issn/volume/issue/spage
	  String query = url.substring(openUrlResolvers[i].length());
	  log.debug3("openUrlResolver: " + url + " openUrl " + query);
	  if (!StringUtil.isNullString(query)) {
	    String[] params = query.split("&");
	    String issn = null;
	    String volume = null;
	    String issue = null;
	    String spage = null;
	    for (int j = 0; j < params.length; j++) {
	      if (params[j].startsWith("issn=")) {
		issn = params[j].substring(5);
	      }
	      if (params[j].startsWith("volume=")) {
		volume = params[j].substring(7);
	      }
	      if (params[j].startsWith("issue=")) {
		issue = params[j].substring(6);
	      }
	      if (params[j].startsWith("spage=")) {
		spage = params[j].substring(6);
	      }
	    }
	    if (issn != null &&
		volume != null &&
		issue != null &&
		spage != null) {
	      String openUrl = issn + "/" + volume + "/" +
		issue + "/" + spage;
	      log.debug3("openUrl: " + openUrl);
	      String newUrl =
		Metadata.openUrlToUrl(openUrl);
	      if (newUrl != null) {
		ret = newUrl;
		found = true;
	      }
	    }
	  }
	}
      }
    }
    log.debug2("proxyResolver returns " + ret);
    return ret;
  }

  public Metadata() {
  }

  public Metadata(Properties props) {
    super(props);
  }

  /*
   * The canonical representation of a DOI has key "dc.identifier"
   * and starts with doi:
   */
  public static final String KEY_DOI = "LOCKSS.doi";
  public static final String PROTOCOL_DOI = "doi:";
  public String getDOI() {
    String ret = getProperty(KEY_DOI);
    if (ret != null && StringUtil.startsWithIgnoreCase(ret, PROTOCOL_DOI)) {
      return ret.substring(PROTOCOL_DOI.length());
    } else {
      return null;
    }
  }
  public void putDOI(String doi) {
    if (!StringUtil.startsWithIgnoreCase(doi, PROTOCOL_DOI)) {
      doi = PROTOCOL_DOI + doi;
    }
    setProperty(KEY_DOI, doi);
  }

  /*
   * Return the ISSN, if any.
   */
  public static final String KEY_ISBN = "LOCKSS.isbn";
  public String getISBN() {
    String ret = getProperty(KEY_ISBN);
    // XXX
    return ret;
  }
  public void putISBN(String isbn) {
    // XXX protocol?
    setProperty(KEY_ISBN, isbn);
  }

  /*
   * Return the ISSN, if any.
   */
  public static final String KEY_ISSN = "LOCKSS.issn";
  public String getISSN() {
    String ret = getProperty(KEY_ISSN);
    // XXX
    return ret;
  }
  public void putISSN(String issn) {
    // XXX protocol?
    setProperty(KEY_ISSN, issn);
  }

  /*
   * Return the volume, if any.
   */
  public static final String KEY_VOLUME = "LOCKSS.volume";
  public String getVolume() {
    String ret = getProperty(KEY_VOLUME);
    // XXX
    return ret;
  }
  public void putVolume(String volume) {
    // XXX protocol?
    setProperty(KEY_VOLUME, volume);
  }

  /*
   * Return the issue, if any.
   */
  public static final String KEY_ISSUE = "LOCKSS.issue";
  public String getIssue() {
    String ret = getProperty(KEY_ISSUE);
    // XXX
    return ret;
  }
  public void putIssue(String issue) {
    // XXX protocol?
    setProperty(KEY_ISSUE, issue);
  }

  /*
   * Return the start page, if any.
   */
  public static final String KEY_START_PAGE = "LOCKSS.startpage";
  public String getStartPage() {
    String ret = getProperty(KEY_START_PAGE);
    // XXX
    return ret;
  }
  public void putStartPage(String spage) {
    // XXX protocol?
    setProperty(KEY_START_PAGE, spage);
  }

  /*
   * Return the date, if any.  A date can be just a year,
   * a month and year, or a specific issue date.
   */
  public static final String KEY_DATE = "LOCKSS.date";
  public String getDate() {
    String ret = getProperty(KEY_DATE);
    // XXX
    return ret;
  }
  public void putDate(String date) {
    // XXX protocol?
    setProperty(KEY_DATE, date);
  }

  /*
   * Return the title, if any.
   */
  public static final String KEY_TITLE = "LOCKSS.title";
  public String getTitle() {
    String ret = getProperty(KEY_TITLE);
    // XXX
    return ret;
  }
  public void putTitle(String title) {
    // XXX protocol?
    setProperty(KEY_TITLE, title);
  }

  /*
   * Return the author(s), if any. Authors are a 
   * delimited list of one or more authors.
   */
  public static final String KEY_AUTHOR = "LOCKSS.author";
  public String getAuthor() {
    String ret = getProperty(KEY_AUTHOR);
    // XXX
    return ret;
  }
  public void putAuthor(String author) {
    // XXX protocol?
    setProperty(KEY_AUTHOR, author);
  }

  /*
   * Ensure that metadata keys are case-insensitive strings
   * and the values are strings.
   */
  public String getProperty(String key) {
    return super.getProperty(key.toLowerCase());
  }
  public String getProperty(String key, String def) {
    return super.getProperty(key.toLowerCase(), def);
  }
  public Object setProperty(String key, String value) {
    return super.setProperty(key.toLowerCase(), value);
  }
}
