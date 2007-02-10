/*
 * $Id: HeaderUtil.java,v 1.5 2007-02-10 06:53:58 tlipkis Exp $
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
package org.lockss.util;

import org.apache.commons.collections.map.LRUMap;
import org.mortbay.util.*;


/** Static utilities for dealing with HTTP headers */
public class HeaderUtil {

  // These maps cache the normalized MIME type and charset string extracted
  // from the Content-Type.  The theory is that there are a few standard
  // variants of each in common use, so the string processing can be
  // avoided in most cases without using significant storage.

  static LRUMap charsetMap = new LRUMap(100);
  static LRUMap mimeTypeMap = new LRUMap(100);


  static String extractMimeTypeFromContentType(String contentType) {
    if (contentType == null) {
      return null;
    }
    int idx = contentType.indexOf(";");
    String res;
    if (idx < 0) {
      res = contentType.trim();
    } else {
      res = contentType.substring(0, idx).trim();
    }
    return res.toLowerCase();
  }

  /** Extract the MIME type, if any, from a Content-Type header. The result
   * is cached. */
  public static String getMimeTypeFromContentType(String contentType) {
    String mime = (String)mimeTypeMap.get(contentType);
    if (mime == null) {
      mime = extractMimeTypeFromContentType(contentType);
      mimeTypeMap.put(contentType, mime);
    }
    return mime;
  }

  static String extractCharsetFromContentType(String contentType) {
    int start, end;
    if (contentType != null &&
	(start = contentType.indexOf("charset=")) != -1) {
      String res = contentType.substring(start + 8);
      if ((end = res.indexOf(";")) > -1) {
	res = res.substring(0, end);
      }
      return QuotedStringTokenizer.unquote(res).toLowerCase();
    }
    return null;
  }

  /** Extract the charset, if any, from a Content-Type header,
   * e.g.<code>text/html;charset=utf-8</code> ,
   * <code>text/html;charset="utf-8"</code> .  The result is cached. */
  public static String getCharsetFromContentType(String contentType) {
    String charset = (String)charsetMap.get(contentType);
    if (charset == null) {
      charset = extractCharsetFromContentType(contentType);
      charsetMap.put(contentType, charset);
    }
    return charset;
  }

}
