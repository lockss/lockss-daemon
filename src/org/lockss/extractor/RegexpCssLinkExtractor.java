/*
 * $Id: RegexpCssLinkExtractor.java,v 1.1 2010-05-27 07:00:47 tlipkis Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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
 */
public class RegexpCssLinkExtractor implements LinkExtractor {
  private static final Logger log = Logger.getLogger("RegexpCssLinkExtractor");
  
  private static final int MAX_URL_LENGTH = 2100;

  // Adapted from Heritrix's ExtractorCSS
  private static final String CSS_URI_EXTRACTOR =    
    "(?i)(?:@import (?:url[(]|)|url[(])\\s*([\\\"\']?)" + // G1
    "(.{0," + MAX_URL_LENGTH + "}?)\\1\\s*[);]"; // G2
    // GROUPS:
    // (G1) optional ' or "
    // (G2) URI

  private static Pattern CSS_URL_PAT = Pattern.compile(CSS_URI_EXTRACTOR);

  // Pattern for character escapes to be removed from URLs
  private static final String CSS_BACKSLASH_ESCAPE = "\\\\([,'\"\\(\\)\\s])";

  private static Pattern CSS_BACKSLASH_PAT =
    Pattern.compile(CSS_BACKSLASH_ESCAPE);

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

    String line;
    BufferedReader rdr =
      new BufferedReader(StringUtil.getLineReader(in, encoding));
    try {
      while ((line = StringUtil.readLineWithContinuation(rdr)) != null) {
	Matcher m1 = CSS_URL_PAT.matcher(line);
	while (m1.find()) {
	  String url = m1.group(2);
	  // Remove backslashes when used as escape character in CSS URL
	  Matcher m2 = CSS_BACKSLASH_PAT.matcher(url);
	  url = m2.replaceAll("$1");
	  if (!StringUtil.isNullString(url)) {
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
      }
    } finally {
      IOUtil.safeClose(rdr);
    }
  }
  
  public static class Factory implements LinkExtractorFactory {
    public LinkExtractor createLinkExtractor(String mimeType) {
      return new RegexpCssLinkExtractor();
    }
  }

}
