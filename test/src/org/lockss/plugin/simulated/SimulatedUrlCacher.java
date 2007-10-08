/*
 * $Id: SimulatedUrlCacher.java,v 1.23 2007-10-08 08:05:05 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * This is the UrlCacher object for the SimulatedPlugin
 *
 * @author  Emil Aalto
 * @version 0.0
 */

public class SimulatedUrlCacher extends BaseUrlCacher {
  private String fileRoot;
  private String contentName = null;
  private File contentFile = null;
  private CIProperties props = null;

  public SimulatedUrlCacher(ArchivalUnit owner, String url, String contentRoot) {
    super(owner, url);
    this.fileRoot = contentRoot;
  }

  private void makeContentName() {
    if (contentName==null) {
      StringBuffer buffer = new StringBuffer(fileRoot);
      if (!fileRoot.endsWith(File.separator)) {
        buffer.append(File.separator);
      }
      buffer.append(mapUrlToContentFileName());
      contentName = buffer.toString();
    }
  }

  // overrides base behavior to get local file
  public InputStream getUncachedInputStreamOnly(String lastModified)
      throws IOException {
    if (getUrl().indexOf("xxxfail") > 0) {
      throw new CacheException.NoRetryDeadLinkException("Simulated failed fetch");
    }
    if (contentFile!=null) {
      return getDefaultStream(contentFile, lastModified);
    }
    makeContentName();
    contentFile = new File(contentName);
    if (contentFile.isDirectory()) {
      File dirContentFile = new File(
          SimulatedContentGenerator.getDirectoryContentFile(contentName));
      if (dirContentFile.exists()) {
        return getDefaultStream(dirContentFile, lastModified);
      } else {
        logger.error("Couldn't find file: "+dirContentFile.getAbsolutePath());
        return null;
      }
    } else {
      return getDefaultStream(contentFile, lastModified);
    }
  }

  protected InputStream getDefaultStream(File file, String lastModified)
      throws IOException {
    if (lastModified != null) {
      try {
	long lastCached = GMT_DATE_FORMAT.parse(lastModified).getTime();
	if ((file.lastModified() <= lastCached) && !toBeDamaged()) {
	  return null;
	}
      } catch (ParseException e) {}
    }
    return new SimulatedContentStream(new FileInputStream(file),toBeDamaged());
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
    props.setProperty(CachedUrl.PROPERTY_LAST_MODIFIED,
		      Long.toString(new File(fileName).lastModified()));
    return props;
  }

  private String mapUrlToContentFileName() {
    return SimulatedArchivalUnit.mapUrlToContentFileName(origUrl);
  }

  private boolean toBeDamaged() {
    try {
      SimulatedArchivalUnit unit = (SimulatedArchivalUnit) au;
      return unit.isUrlToBeDamaged(origUrl);
    } catch (ClassCastException e ) {
      return false;
    }
  }

}

