/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.util;

import java.util.*;

/**
 * Constants of general use
 */
public interface Constants {

  /** The number of milliseconds in a second */
  public static final long SECOND = 1000;
  /** The number of milliseconds in a minute */
  public static final long MINUTE = 60 * SECOND;
  /** The number of milliseconds in an hour */
  public static final long HOUR = 60 * MINUTE;
  /** The number of milliseconds in a day */
  public static final long DAY = 24 * HOUR;
  /** The number of milliseconds in a week */
  public static final long WEEK = 7 * DAY;
  /** The number of milliseconds in a (non-leap) year */
  public static final long YEAR = 365 * DAY;

  /** List delimiter in strings */
  public static String LIST_DELIM = ";";
  /** List delimiter char in strings */
  public static char LIST_DELIM_CHAR = ';';

  /** The default timezone, GMT */
  public static final TimeZone DEFAULT_TIMEZONE = TimeZoneUtil.getExactTimeZone("GMT");

  /** The line separator string on this system */
  public static String EOL = System.getProperty("line.separator");

  /** Carriage-return linefeed */
  public static final String CRLF = "\r\n";

  /** Separator for polling groups */
  public static String GROUP_SEPARATOR = ";";

  /** The RE string matching the EOL string */
  public static String EOL_RE = StringUtil.escapeNonAlphaNum(EOL);

  /**
   * <p>The US ASCII encoding.</p>
   */
  public static final String ENCODING_US_ASCII = "US-ASCII";
  
  /**
   * <p>The UTF-8 encoding.</p>
   */
  public static final String ENCODING_UTF_8 = "UTF-8";
  
  /**
   * <p>The ISO-8859-1 encoding.</p>
   */
  public static final String ENCODING_ISO_8859_1 = "ISO-8859-1";
  
  /** The default encoding used when none is detected */
  public static String DEFAULT_ENCODING = ENCODING_ISO_8859_1;

  /**
   * <p>The encoding of URLs.</p>
   */
  public static final String URL_ENCODING = ENCODING_US_ASCII;

  /** LOCKSS home page */
  public static String LOCKSS_HOME_URL = "https://www.lockss.org/";

  /** LOCKSS HTTP header, can have multiple values */
  public static String X_LOCKSS = "X-Lockss";

  /** X-LOCKSS value indicating this is a repair request */
  public static String X_LOCKSS_REPAIR = "Repair";

  /** X-LOCKSS value indicating this response comes from the cache */
  public static String X_LOCKSS_FROM_CACHE = "from-cache";

  /** Header used in props fetches to supply version info */
  public static String X_LOCKSS_INFO = "X-Lockss-Info";

  /** Header used with proxy to request content from publisher or cache */
  public static String X_LOCKSS_SOURCE = "X-Lockss-Source";

  /** Value of {@value #X_LOCKSS_SOURCE} header that requests content only
   * from the publisher */
  public static String X_LOCKSS_SOURCE_PUBLISHER = "publisher";

  /** Value of {@value #X_LOCKSS_SOURCE} header that requests content only
   * from the cache */
  public static String X_LOCKSS_SOURCE_CACHE = "cache";

  /** Header used with proxy to request content from a specific AU */
  public static String X_LOCKSS_AUID = "X-Lockss-Auid";

  /** Header in ServeContent response when it serves content from an AU */
  public static String X_LOCKSS_FROM_AUID = "X-Lockss-From-Auid";

  /** Header in ServeContent response when it serves content from the
   * publisher, which has been rewritten because it logically belongs to
   * the AU, but wasn't actually served from the cache (because it's
   * missing or the publisher has more recent content */
  public static String X_LOCKSS_REWRITTEN_FOR_AUID =
    "X-Lockss-Rewritten-For-Auid";

  /** The local address to which to the proxy should bind the socket for
   * outgoing requests */
  public static String X_LOCKSS_LOCAL_ADDRESS = "X-Lockss-Local-Addr";

  /** The real identity of a repairer sending a request to localhost, for
   * testing */
  public static String X_LOCKSS_REAL_ID = "X-Lockss-Id";

  /** The real identity of a repairer sending a request to localhost, for
   * testing */
  public static String HTTP_REFERER = "Referer";

  // Cookie policies.

  /** Cookie policy: ignore cookies */
  public static String COOKIE_POLICY_IGNORE = "ignore";
  /** Cookie policy: Netscape draft policy */
  public static String COOKIE_POLICY_NETSCAPE = "netscape";
  /** Cookie policy: Common browser compatibility */
  public static String COOKIE_POLICY_COMPATIBILITY = "compatibility";
  /** Cookie policy: RFC 2109  */
  public static String COOKIE_POLICY_RFC_2109 = "rfc2109";

  // Colors

  public static final String COLOR_WHITE = "#ffffff";
  public static final String COLOR_BLACK = "#000000";
  public static final String COLOR_RED = "#ff0000";
  public static final String COLOR_ORANGE = "#ff6600";

  // Exit codes

  /** Exit code - normal exit */
  public static int EXIT_CODE_NORMAL = 0;

  /** Exit code - thread hung */
  public static int EXIT_CODE_THREAD_HUNG = 101;

  /** Exit code - thread died */
  public static int EXIT_CODE_THREAD_EXIT = 102;

  /** Exit code - unsupported Java version */
  public static int EXIT_CODE_JAVA_VERSION = 103;

  /** Exit code - exception thrown in main loop */
  public static int EXIT_CODE_EXCEPTION_IN_MAIN = 104;

  /** Exit code - required resource unavailable */
  public static int EXIT_CODE_RESOURCE_UNAVAILABLE = 105;

  /** Exit code - critical keystore missing or not loadable (wrong
   * password, missing password file) */
  public static int EXIT_CODE_KEYSTORE_MISSING = 106;

  /** Exit code - invalid time zone data */
  public static int EXIT_INVALID_TIME_ZONE_DATA = 107;

  /** Regexp contexts.  Depending on the type of string a regexp will be
   * used to match against, printf arguments substituted into a pattern
   * template may need custom escaping.
   * @see org.lockss.plugin.PrintfConverter
   */
  enum RegexpContext {
    /** Regexp wil be used to match against unencoded strings */
    String,
    /** Regexp will be used to match against URL-encoded URLs */
    Url,
  };

  /** The MIME type string for HTML ({@value}). */
  public static final String MIME_TYPE_HTML = "text/html";

  /** The MIME type string for JSON ({@value}) */
  public static final String MIME_TYPE_JSON = "application/json";

  /** The MIME type string for PDF ({@value}). */
  public static final String MIME_TYPE_PDF = "application/pdf";
  
  /** The MIME type string for XML ({@value}). */
   public static final String MIME_TYPE_XML = "text/xml";

  /** The MIME type string for browser PAC files ({@value}). */
  public static final String MIME_TYPE_PAC = "application/x-ns-proxy-autoconfig";

  /** The MIME type string for RealAudio Media ({@value}). */
  public static String MIME_TYPE_RAM = "audio/x-pn-realaudio";
  
  /** The MIME type string for RIS citation files ({@value}). */
  public static String MIME_TYPE_RIS = "application/x-research-info-systems";

  /** The Form Encoding type string for a "get" or "post" as url ({@value}). */
  public static String FORM_ENCODING_URL = "application/x-www-form-urlencoded";

  /** The Form Encoding type string for binary "post" ({@value}). */
  public static String FORM_ENCODING_DATA = "multipart/form-data";

  /** Form Encoding type string for plain "post" ({@value}). */
  public static String FORM_ENCODING_PLAIN = "text/plain";


}
