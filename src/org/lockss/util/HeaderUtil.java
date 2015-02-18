/*
 * $Id$
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
import org.apache.commons.httpclient.util.DateUtil;
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
    int idx = contentType.indexOf(';');
    String res;
    if (idx < 0) {
      res = contentType.trim();
    } else {
      res = contentType.substring(0, idx).trim();
    }
    return res.toLowerCase();
  }

  /** Extract the MIME type, if any, from a Content-Type header.  The
   * result is always lowercase (so can be compared directly with the
   * MIME-types defined in {@Constants}, e.g., {@link
   * Constants#MIME_TYPE_HTML}).  E.g. given
   * <code>TEXT/HTML;charset=utf-8</code> , returns
   * <code>text/html</code>. */
  public static String getMimeTypeFromContentType(String contentType) {
    synchronized (mimeTypeMap) {
      String mime = (String)mimeTypeMap.get(contentType);
      if (mime == null) {
	mime = extractMimeTypeFromContentType(contentType);
	if (isCachableContentType(contentType)) {
	  mimeTypeMap.put(contentType, mime);
	}
      }
      return mime;
    }
  }

  static String extractCharsetFromContentType(String contentType) {
    int start, end;
    if (contentType != null &&
	(start = StringUtil.indexOfIgnoreCase(contentType, "charset=")) != -1) {
      String res = contentType.substring(start + 8);
      if ((end = res.indexOf(';')) > -1) {
	res = res.substring(0, end);
      }
      return QuotedStringTokenizer.unquote(res).toLowerCase();
    }
    return null;
  }

  /** Extract the charset, if any, from a Content-Type header.  E.g. given
   * <code>text/html;charset=utf-8</code> or
   * <code>text/html;charset="utf-8"</code> , the result is
   * <code>utf-8</code>. */
  public static String getCharsetFromContentType(String contentType) {
    synchronized (charsetMap) {
      String charset = (String)charsetMap.get(contentType);
      if (charset == null) {
	charset = extractCharsetFromContentType(contentType);
	if (isCachableContentType(contentType)) {
	  charsetMap.put(contentType, charset);
	}
      }
      return charset;
    }
  }

  /** Return the charset specified in the Content-Type header, or the
   * default charset.  Never returns null. */
  public static String getCharsetOrDefaultFromContentType(String contentType) {
    if (contentType == null) {
      return Constants.DEFAULT_ENCODING;
    }
    String charset = getCharsetFromContentType(contentType);
    if (StringUtil.isNullString(charset)) {
      return Constants.DEFAULT_ENCODING;
    }
    return charset;
  }

  static final String CHARSET = "charset";
  static final int CLEN = CHARSET.length();

  // Return true iff the contentType contains no attributes other than
  // charset.  Some sites put highly variable strings into attributes (such
  // as the url).  These should not be cached.
  static boolean isCachableContentType(String contentType) {
    if (contentType == null) {
      return false;
    }
    int pos = -1;
    while ((pos = contentType.indexOf('=', pos + 1)) >= 0) {
      if (pos - CLEN < 0 ||
	  !contentType.regionMatches(true, pos-CLEN, "charset", 0, CLEN)) {
	return false;
      }
      if ((pos - CLEN >= 1) &&
	  isTokenChar(contentType.charAt(pos - CLEN - 1))) {
	return false;
      }
    }
    return true;
  }

  static String SEPARATOR_CHARS = "()<>@,;:\\\"/[]?={} \t";

  static boolean TOKEN_CHARS[] = initTokenChars();
  static boolean[] initTokenChars() {
    boolean[] res = new boolean[128];
    for (int ch = 0; ch < 128; ch++) {
      if (ch <= 31 || ch == 127) {
	res[ch] = false;
      } else {
	res[ch] = true;
      }
    }
    for (int ix = SEPARATOR_CHARS.length() - 1; ix >= 0; ix--) {
      res[SEPARATOR_CHARS.charAt(ix)] = false;
    }
    return res;
  }
    

  static boolean isTokenChar(char ch) {
    return TOKEN_CHARS[ch];
  }

  /** Return the earlier of two date strings.  Non-null date is preferred
   * over null date. */
  public static String earlier(String datestr1, String datestr2)
      throws org.apache.commons.httpclient.util.DateParseException {
    return dateMinMax(datestr1, datestr2, true);
  }

  /** Return the later of two date strings.  Non-null date is preferred
   * over null date. */
  public static String later(String datestr1, String datestr2)
      throws org.apache.commons.httpclient.util.DateParseException {
    return dateMinMax(datestr1, datestr2, false);
  }

  private static String dateMinMax(String datestr1, String datestr2,
				   boolean earlier)
      throws org.apache.commons.httpclient.util.DateParseException {
    if (StringUtil.isNullString(datestr1)) datestr1 = null;
    if (StringUtil.isNullString(datestr2)) datestr2 = null;
    if (datestr1 == null) {
      return datestr2;
    }
    if (datestr2 == null) {
      return datestr1;
    }
    if (earlier ^ HeaderUtil.isEarlier(datestr1, datestr2)) {
      return datestr2;
    } else {
      return datestr1;
    }
  }

  /** Return true iff the first date string is strictly earlier than the
   * second */
  public static boolean isEarlier(String datestr1, String datestr2)
      throws org.apache.commons.httpclient.util.DateParseException {
    // common case, no conversion necessary
    if (datestr1.equalsIgnoreCase(datestr2)) return false;
    long d1 = DateUtil.parseDate(datestr1).getTime();
    long d2 = DateUtil.parseDate(datestr2).getTime();
    return d1 < d2;
  }

}
