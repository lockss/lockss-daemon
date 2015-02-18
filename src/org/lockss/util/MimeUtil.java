/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.*;

/**
 * MIME utilities
 */

// XXX Will eventually need to allow plugins to override/add mappings.
// XXX Should be initialized from mime.types resource
public class MimeUtil {
  static Logger log = Logger.getLogger("MimeUtil");

  static Map<String,String> mimeExtensionMap = new HashMap<String,String>();
  static Map<String,String> extensionMimeMap = new HashMap<String,String>();
  static {
    initMaps();
  }

  static void addMapping(String mimeType, String... extensions) {
    if (extensions == null) {
      return;
    }
    mimeExtensionMap.put(mimeType, extensions[0]);
    for (String ext : extensions) {
      if (!extensionMimeMap.containsKey(ext)) {
	extensionMimeMap.put(ext, mimeType);
      }
    }
  }

  static void initMaps() {
    addMapping("text/html", ".html", ".htm");
    addMapping("text/plain", ".txt", ".raw", ".toc", ".fil");
    addMapping("text/csv", ".csv");

    addMapping("image/gif", ".gif");
    addMapping("image/jpeg", ".jpeg", ".jpg");
    addMapping("image/tiff", ".tiff", ".tif");

    // XXX ".meta" for Springer - make plugin-specifiable
    addMapping("application/xml", ".xml", ".meta");
    addMapping("application/pdf", ".pdf");
    addMapping("application/postscript", ".ps", ".eps");

    addMapping("application/sgml", ".sgm", ".sml");
    addMapping("application/msword", ".doc");

    addMapping("application/zip", ".zip");
    addMapping("application/x-tar", ".tar");
    addMapping("application/x-gtar", ".gtar", ".tgz");
  }

  public static String getExtensionFromMimeType(String mimeType) {
    return mimeExtensionMap.get(mimeType.toLowerCase());
  }

  public static String getExtensionFromContentType(String contentType) {
    return getExtensionFromMimeType(HeaderUtil
				    .getMimeTypeFromContentType(contentType));
  }

  public static String getMimeTypeFromExtension(String extension) {
    if (!extension.startsWith(".")) {
      extension = "." + extension;
    }
    return extensionMimeMap.get(extension.toLowerCase());
  }

}
