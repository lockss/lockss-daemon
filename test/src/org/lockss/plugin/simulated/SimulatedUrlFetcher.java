/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.simulated;

import java.io.*;
import java.util.*;
import java.text.*;

import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.test.StringInputStream;

import static org.lockss.util.DateTimeUtil.GMT_DATE_FORMATTER;

/**
 * This is the UrlCacher object for the SimulatedPlugin
 */

public class SimulatedUrlFetcher extends BaseUrlFetcher {

  private static final Logger log = Logger.getLogger(SimulatedUrlFetcher.class);
  
  private String fileRoot;
  private File contentFile = null;
  private CIProperties props = null;
  private SimulatedContentGenerator scgen = null;
  private Properties addProps = null;

  private static final SimpleDateFormat GMT_DATE_PARSER =
    new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
  static {
    GMT_DATE_PARSER.setTimeZone(TimeZoneUtil.getExactTimeZone("GMT"));
  }

  public SimulatedUrlFetcher(Crawler.CrawlerFacade crawlFacade, String url, String contentRoot) {
    super(crawlFacade, url);
    this.fileRoot = contentRoot;
  }

  private File getContentFile() {
    if (contentFile == null) {
      StringBuffer buffer = new StringBuffer(fileRoot);
      if (!fileRoot.endsWith(File.separator)) {
        buffer.append(File.separator);
      }
      buffer.append(mapUrlToContentFileName());
      contentFile = new File(buffer.toString());
    }
    return contentFile;
  }

  SimulatedContentGenerator getScgen(String root) {
    if (scgen == null) {
      log.debug2("Creating SimulatedContentGenerator at: " + contentFile);
      scgen = SimulatedContentGenerator.getInstance(fileRoot);
    }
    return scgen;
  }

  // overrides base behavior to get local file.  BaseUrlFetcher maps
  // exceptions below this method, so we must do it here.
  @Override
  protected InputStream getUncachedInputStreamOnly(String lastModified)
      throws IOException {
    try {
      return getUncachedInputStreamOnly0(lastModified);
    } catch (IOException ex) {
      log.debug2("openConnection: " + ex);
      throw resultMap.mapException(au, getUrl(), ex, null);
    }
  }

  private InputStream getUncachedInputStreamOnly0(String lastModified)
      throws IOException {
    pauseBeforeFetch();
    if (getUrl().indexOf("xxxfail") > 0) {
      throw new CacheException.NoRetryDeadLinkException("Simulated failed fetch");
    }
    if (contentFile == null) {
      contentFile = getContentFile();
    }

    if (contentFile.isDirectory()) {
      if (au.getConfiguration().getBoolean("redirectDirToIndex", false)) {
      	String newUrlString = UrlUtil.resolveUri(fetchUrl, "index.html");
      	fetchUrl = newUrlString;
      	contentFile = null;
      	log.debug2("Following redirect to " + newUrlString);
      	return getUncachedInputStreamOnly(lastModified);
      } else {
      	File dirContentFile =
      	  new File(getScgen(fileRoot).getDirectoryContentFile(contentFile.getPath()));
      	if (dirContentFile.exists()) {
      	  return getDefaultStream(dirContentFile, lastModified);
      	} else {
      	  log.error("Couldn't find file: "+dirContentFile.getAbsolutePath());
      	  return null;
      	}
      }
    } else if (!contentFile.exists() &&
	       contentFile.toString().endsWith("/index.html") &&
	       au.getConfiguration().getBoolean("autoGenIndexHtml", false)) {
      log.debug2("Generating index: " + new File(contentFile.getParent(),
						    "index.html"));
      String indexContent =
        	getScgen(fileRoot).getIndexContent(contentFile.getParentFile(),
					   "index.html", null);
      addProps = PropUtil.fromArgs(CachedUrl.PROPERTY_CONTENT_TYPE,
				   "text/html");
      return new StringInputStream(indexContent);
    } else {
      return getDefaultStream(contentFile, lastModified);
    }
  }

  protected InputStream getDefaultStream(File file, String lastModified)
      throws IOException {
    if (lastModified != null) {
      if (GMT_DATE_FORMATTER.format(file.lastModified()).equals(lastModified)) {
        log.debug3("Last-Modified: " + lastModified + " == " +
                   GMT_DATE_FORMATTER.format(file.lastModified()));
        return null;
      }
    }
    return new SimulatedContentStream(new FileInputStream(file), false);
  }

  // overrides base behavior
  public CIProperties getUncachedProperties() {
    if (props!=null) {
      return props;
    }
    props = new CIProperties();
    String fileName = mapUrlToContentFileName().toLowerCase();
    if (fileName.endsWith(".txt")) {
      props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/plain");
    } else if (fileName.endsWith(".html")) {
      props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    } else if (fileName.endsWith(".xml")) {
      props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    } else if (fileName.endsWith(".xml.meta")) {
      props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    } else if (fileName.endsWith(".pdf")) {
      props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");
    } else if (fileName.endsWith(".jpg")) {
      props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "image/jpeg");
    } else {
      props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/plain");
    }
    props.setProperty(CachedUrl.PROPERTY_ORIG_URL, origUrl);
    // set fetch time as now, since it should be the same system
    props.setProperty(CachedUrl.PROPERTY_FETCH_TIME, ""+TimeBase.nowMs());
    // set last-modified to the file's write date
    Date date = new Date(getContentFile().lastModified());
    props.setProperty(CachedUrl.PROPERTY_LAST_MODIFIED,
		      GMT_DATE_FORMATTER.format(date));
    if (addProps != null) {
      props.putAll(addProps);
      addProps = null;
    }
    return props;
  }

  private String mapUrlToContentFileName() {
    return ((SimulatedArchivalUnit) au).mapUrlToContentFileName(fetchUrl);
  }

  public String toString() {
    return "[SUF: " + contentFile + "]";
  }

}

