/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.extractor;

import java.io.*;
import java.net.*;
import java.util.regex.*;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

/**
 * Extracts links from CSS by matching a regular expression.  Processes
 * backslash-escaped URL chars (parentheses, commas, whitespace, single and
 * double quotes) but not numeric escapes.  Illegal URLs are ignored.
 *
 * This implementation may miss pathological cases where huge amounts of
 * whitespace make a single statement larger than the buffer overlap.
 */
public class RegexpCssLinkExtractor implements LinkExtractor {
  private static final Logger log = Logger.getLogger("RegexpCssLinkExtractor");
  
  private static final int MAX_URL_LENGTH = 2100;
  // Amount of CSS input to buffer up for matcher
  private static final int DEFAULT_MAX_BUF = 32 * 1024;
  // Amount at end of buffer to rescan at beginning of next bufferfull
  private static final int DEFAULT_OVERLAP = 2 * 1024;

  // Adapted from Heritrix's ExtractorCSS
  private static final String CSS_URI_EXTRACTOR =    
    "(?i)(?:@import\\s+(?:url[(]|)|url[(])\\s*([\\\"\']?)" + // G1
    "(.{0," + MAX_URL_LENGTH + "}?)\\1\\s*[);]"; // G2
    // GROUPS:
    // (G1) optional ' or "
    // (G2) URI

  private static Pattern CSS_URL_PAT = Pattern.compile(CSS_URI_EXTRACTOR);

  // Pattern for character escapes to be removed from URLs
  private static final String CSS_BACKSLASH_ESCAPE = "\\\\([,'\"\\(\\)\\s])";

  private static Pattern CSS_BACKSLASH_PAT =
    Pattern.compile(CSS_BACKSLASH_ESCAPE);

  private int maxBuf = DEFAULT_MAX_BUF;

  private int overlap = DEFAULT_OVERLAP;

  public RegexpCssLinkExtractor() {
  }

  public RegexpCssLinkExtractor(int maxBuf, int overlap) {
    this.maxBuf = maxBuf;
    this.overlap = overlap;
  }

  /* Inherit documentation */
  public void extractUrls(ArchivalUnit au,
                          InputStream in,
			  String encoding,
                          String srcUrl,
			  LinkExtractor.Callback cb)
      throws IOException {
    log.debug2("Parsing " + srcUrl + ", enc " + encoding);
    if (in == null) {
      throw new IllegalArgumentException("Called with null InputStream");
    }
    if (cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }
    URL baseUrl = new URL(srcUrl);

    // This needs a regexp matcher that can match against a Reader.
    // Interim solution is to loop matching against a rolling fixed-length
    // chunk of input, with overlaps between chunks.  Can miss URLs in
    // pathological situations.

    Reader rdr = new BufferedReader(StringUtil.getLineReader(in, encoding));
    rdr = StringUtil.getLineContinuationReader(rdr);
    StringBuilder sb = new StringBuilder(maxBuf);
    int shift = Math.min(overlap, maxBuf / 2);

    try {
      while (StringUtil.fillFromReader(rdr, sb, maxBuf - sb.length())) {
	Matcher m1 = CSS_URL_PAT.matcher(sb);
	while (m1.find()) {
	  String url = processUrlEscapes(m1.group(2));
	  if (!StringUtil.isNullString(url) && !DataUri.isDataUri(url)) {
	    try {
	      String resolved = UrlUtil.resolveUri(baseUrl, url);
	      if (log.isDebug2()) {
		log.debug2("Found " + url + " which resolves to " + resolved);
	      }
	      cb.foundLink(resolved);
	    } catch (MalformedURLException e) {
	      log.siteError("Resolving " + url + " in CSS at " + baseUrl
			    + ": " + e.toString());
	    }
	  }
	}
	int sblen = sb.length();
	if (sblen < maxBuf) {
	  break;
	}
	// Move the overlap amount to the beginning of the buffer
	sb.delete(0, sblen - shift);
      }
    } finally {
      IOUtil.safeClose(rdr);
    }
  }
  
  // Remove backslashes when used as escape character in CSS URL
  // Should probably also process hex URL encodings
  String processUrlEscapes(String url) {
    Matcher m2 = CSS_BACKSLASH_PAT.matcher(url);
    return m2.replaceAll("$1");
  }

  public static class Factory implements LinkExtractorFactory {
    public LinkExtractor createLinkExtractor(String mimeType) {
      return new RegexpCssLinkExtractor();
    }
  }

}
