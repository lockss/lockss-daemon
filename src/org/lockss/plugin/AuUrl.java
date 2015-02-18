/*
* $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.*;
import org.lockss.util.*;

/** An AU URL is a URL with a special protocol (LOCKSSAU:), used in
 * top-level poll messages to identify the entire contents of an AU.
 */
public class AuUrl {

  public static final String PROTOCOL = UrlManager.PROTOCOL_AU;
  public static final String PROTOCOL_COLON = PROTOCOL + ":";
  static final int cmp_len = PROTOCOL_COLON.length();

  /** Return true if the supplied URL is an AuUrl.
   * @param url the URL to test.
   * @return true if the protocol in the url is LOCKSS:
   */
  public static boolean isAuUrl(URL url) {
    return PROTOCOL.equalsIgnoreCase(url.getProtocol());
  }

  /** Return true if the supplied URL string is an AuUrl.
   * @param url the string to test.
   * @return true if the protocol in the url is LOCKSS:
   */
  public static boolean isAuUrl(String url) {
    return PROTOCOL_COLON.regionMatches(true, 0, url, 0, cmp_len);
  }

  /** Create an AuUrl from an AU Id.  The Id string may contain any
   * characters: it is URL-encoded before being put in the URL.
   * @param auId the plugin-specific AU Id string.
   * @return a URL with the LOCKSSAU protocol and the supplied AU Id
   * string.
   * @throws MalformedURLException
   */
  public static URL fromAuId(String auId)
      throws MalformedURLException {
    return new URL(PROTOCOL, "", UrlUtil.encodeUrl(auId));
  }

  /** Extract the AU Id from an AuUrl.
   * @param auUrl an AuUrl produced by {@link #fromAuId(String)}
   * @return the AU Id string that was supplied to {@link
   * #fromAuId(String)}
   */
  public static String getAuId(URL auUrl) {
    return UrlUtil.decodeUrl(auUrl.getFile());
  }
}
