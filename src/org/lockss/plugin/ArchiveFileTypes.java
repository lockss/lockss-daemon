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

package org.lockss.plugin;

import java.net.*;
import java.util.*;

import org.lockss.util.*;


/**
 * Describes the archive file types that should have their members exposed
 * as pseudo-CachedUrls.
 */
public class ArchiveFileTypes {
  protected static Logger log = Logger.getLogger("ArchiveFileTypes");


  /** Default mime types and extensions for zip, tar, tgz. */

  private static final Map<String,String> DEFAULT_MAP =
    MapUtil.fromList(ListUtil.list(".zip", ".zip",
				   ".tar", ".tar",
				   ".tgz", ".tgz",
				   ".tar.gz", ".tar.gz",
				   "application/zip", ".zip",
				   "application/x-gtar", ".tar",
				   "application/x-tar", ".tar"));

  public static final ArchiveFileTypes DEFAULT =
    new ArchiveFileTypes(DEFAULT_MAP);

  private Map<String,String> extMimeMap;

  public ArchiveFileTypes() {
  }

  public ArchiveFileTypes(Map<String,String> map) {
    extMimeMap = map;
  }

  public Map<String,String> getExtMimeMap() {
    return extMimeMap;
  }

  /** Return the archive file type corresponding to the CU's MIME type or
   * filename extension, or null if none.
   */
  public String getFromCu(CachedUrl cu) throws MalformedURLException {
    String res = getFromMime(cu.getContentType());
    if (res == null) {
      res = getFromUrl(cu.getUrl());
    }
    return res;
  }

  /** Return the archive file type corresponding to the filename extension
   * in the URL, or null if none.  
   */
  public String getFromUrl(String url) throws MalformedURLException {
    if (StringUtil.endsWithIgnoreCase(url, ".tar.gz")) {
      return getExtMimeMap().get(".tar.gz");
    }
    String ext = UrlUtil.getFileExtension(url).toLowerCase();
    return getExtMimeMap().get("." + ext);
  }

  /** Return the archive file type corresponding to the MIME type, or null
   * if none.
   */
  public String getFromMime(String contentType) {
    String mimeType = HeaderUtil.getMimeTypeFromContentType(contentType);
    if (mimeType == null) {
      return null;
    }
    return getExtMimeMap().get(mimeType.toLowerCase());
  }

  /** Lookup the CU's archive file type in its AU's ArchiveFileTypes
   * @return the file extension (including dot), or null if none found
   */
  public static String getArchiveExtension(CachedUrl cu) {
    ArchiveFileTypes aft = cu.getArchivalUnit().getArchiveFileTypes();
    if (aft == null) {
      return null;
    }
    try {
      return aft.getFromCu(cu);
    } catch (MalformedURLException e) {
      log.warning("isArchive(" + cu + ")", e);
      return null;
    }
  }

}
