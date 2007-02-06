/*
 * $Id: HeaderUtil.java,v 1.3 2007-02-06 00:55:38 tlipkis Exp $
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

  static LRUMap charsetMap = new LRUMap(100);
  static LRUMap mimeTypeMap = new LRUMap(100);


  static String extractMimeTypeFromContentType(String contentType) {
    if (contentType == null) {
      return null;
    }
    int idx = contentType.indexOf(";");
    if (idx < 0) {
      return contentType.trim();
    }
    return contentType.substring(0, idx).trim();
  }

  /** Extract the MIME type, if any, from a Content-Type header. The result
   * is cached. */
  public static String getMimeTypeFromContentType(String contentType) {
    String mime = (String)mimeTypeMap.get(contentType);
    if (mime == null) {
      mime = extractMimeTypeFromContentType(contentType);
      if (mime != null) {
	mime = mime.toLowerCase();
      }
      mimeTypeMap.put(contentType, mime);
    }
    return mime;
  }

  public static String extractCharsetFromContentType(String contentType) {
    if (contentType == null) {
      return null;
    }
    int idx = contentType.indexOf(";");
    if (idx > 0) {
      int i1=contentType.indexOf("charset=",idx);
      if (i1>=0) {
	i1+=8;
	int i2 = contentType.indexOf(' ',i1);
	String charset =
	  i2 > 0 ? contentType.substring(i1, i2) : contentType.substring(i1);
	return QuotedStringTokenizer.unquote(charset);
      }
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
      if (charset != null) {
	charset = charset.toLowerCase();
      }
      charsetMap.put(contentType, charset);
    }
    return charset;
  }

}
