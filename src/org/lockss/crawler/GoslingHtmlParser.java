/*
 * $Id: GoslingHtmlParser.java,v 1.46 2006-11-15 21:18:38 troberts Exp $
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

/*
 * Some portions of this code are:
 * Copyright (c) 2000-2003 Sun Microsystems. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduct the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems or the names of contributors may
 * be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 *   This software is provided "AS IS," without a warranty of any
 *   kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 *   WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 *   EXCLUDED. SUN MICROSYSTEMS AND ITS LICENSORS SHALL NOT BE LIABLE
 *   FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 *   MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 *   EVENT WILL SUN MICROSYSTEMS OR ITS LICENSORS BE LIABLE FOR ANY
 *   LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 *   CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 *   REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 *   OR INABILITY TO USE SOFTWARE, EVEN IF SUN MICROSYSTEMS
 *   HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that Software is not designed, licensed or intended for
 * use in the design, construction, operation or maintenance of any
 * nuclear facility.
 */

package org.lockss.crawler;

import java.util.*;
import java.net.*;
import java.io.*;

import org.htmlparser.util.*;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.lockss.config.*;


public class GoslingHtmlParser implements ContentParser {

  public static final int DEFAULT_BUFFER_CAPACITY = 4096;
  public static final String PARAM_BUFFER_CAPACITY =
    Configuration.PREFIX + "crawler.buffer_capacity";

  public static final boolean DEFAULT_PARSE_JS = false;
  public static final String PARAM_PARSE_JS =
    Configuration.PREFIX + "crawler.parse_js";

  protected static final String ATAG = "a";
  protected static final String APPLETTAG = "applet";
  protected static final String AREATAG = "area";
  protected static final String BASETAG = "base";
  protected static final String BODYTAG = "body";
  protected static final String BACKGROUNDSRC = "background";
  protected static final String CODE = "code";
  protected static final String CODEBASE = "codebase";
  protected static final String EMBEDTAG = "embed";
  protected static final String FRAMETAG = "frame";
  protected static final String IMGTAG = "img";
  protected static final String JSCRIPTTAG = "javascript";
  protected static final String HREF = "href";
  protected static final String LINKTAG = "link";
  protected static final String METATAG = "meta";
  protected static final String OBJECTTAG = "object";
  protected static final String OPTIONTAG = "option";
  protected static final String SCRIPTTAG = "script";
  protected static final String SRC = "src";
  protected static final String TABLETAG = "table";
  protected static final String TDTAG = "tc";
  protected static final String VALUETAG = "value";

  protected static final String SCRIPTTAGEND = "/script>";


  protected static final String REFRESH = "refresh";
  protected static final String HTTP_EQUIV = "http-equiv";
  protected static final String HTTP_EQUIV_CONTENT = "content";
  protected static final String HTTP_EQUIV_URL = "url";

  protected static final char NEWLINE_CHAR = '\n';
  protected static final char CARRIAGE_RETURN_CHAR = '\r';

  //smallest size any of the tags we're interested in can be; <a href=
  private static final int MIN_TAG_LENGTH = 5;

  // initial tag string buffer size
  private static int EST_TAG_LENGTH = 60;

  // initial url string buffer size
  private static int EST_URL_LENGTH = 40;

  private static Logger logger = Logger.getLogger("GoslingHtmlParser");

  protected String srcUrl = null;
  protected URL baseUrl = null;
  private Reader reader;
  private boolean readerEof;

  private CharRing ring;
  private int ringCapacity;
  private boolean shouldParseJavaScript;
  private boolean isTrace = logger.isDebug2();

  private boolean malformedBaseUrl = false;

  private boolean lastTagWasScript = false;

  public GoslingHtmlParser() {
    ringCapacity = CurrentConfig.getIntParam(PARAM_BUFFER_CAPACITY,
					     DEFAULT_BUFFER_CAPACITY);
    shouldParseJavaScript =
      CurrentConfig.getBooleanParam(PARAM_PARSE_JS, DEFAULT_PARSE_JS);
  }

//  public GoslingHtmlParser(int ringCapacity) {
//    this.ringCapacity = ringCapacity;
//  }

  /**
   * Method which will parse the html file represented by reader and call
   * cb.foundUrl() for each url found
   *
   * @throws IOException
   */
  public void parseForUrls(Reader reader, String srcUrl,
			   ArchivalUnit au, ContentParser.FoundUrlCallback cb)
      throws IOException {
    if (reader == null) {
      throw new IllegalArgumentException("Called with null reader");
    } else if (srcUrl == null) {
      throw new IllegalArgumentException("Called with null srcUrl");
    } else if (cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }
    this.srcUrl = srcUrl;
    this.reader = reader;

    try {
      baseUrl = null;

      readerEof = false;
      ring = new CharRing(ringCapacity);

      if (isTrace) logger.debug2("Extracting urls from " + srcUrl);
      String nextUrl = null;
      while ((nextUrl = extractNextLink(ring, au)) != null) {
	if (isTrace) {
	  logger.debug2("Extracted "+nextUrl);
	}
	cb.foundUrl(nextUrl);
      }
    } finally {
      // Let go of large objects
      ring = null;
      if (reader != null) {
	try {
	  reader.close();
	} catch (IOException ignore) {}
	reader = null;
      }
    }
  }


  /**
   * Keep calling skipLeadingWhiteSpace and refilling the char ring until there
   * is no more leading white space
   * @param ring CharRing to strip whitespace from
   * @param minKars minimum number of characters needed in the CharRing
   * @throws IOException
   */
  private void skipWhiteSpace(CharRing ring, int minKars) throws IOException {
    do {
      if (!refill(minKars)) {
	return;
      }
    } while (ring.skipLeadingWhiteSpace());
  }

  /**
   * Read through the reader stream, extract and return the next url found
   * (after decoding any html entities in it)
   *
   * @param ring CharRing from which to read characters
   * @return String representing the next url in reader
   * @throws IOException
   * @throws MalformedURLException
   */
  protected String extractNextLink(CharRing ring, ArchivalUnit au)
      throws IOException, MalformedURLException {
    while (refill(MIN_TAG_LENGTH)) {
      //skip to the next tag
      int idx = ring.indexOf("<", -1, false);
      if (idx < 0) {
	ring.clear();
	continue;
      } else {
	// 	if (isTrace) logger.debug3("Found < at " + idx);
	ring.skip(idx + 1);
	skipWhiteSpace(ring, MIN_TAG_LENGTH);
	if (!refill(MIN_TAG_LENGTH)) return null;
	if (ring.get(0) == '!' && ring.get(1) == '-' && ring.get(2) == '-') {
	  // html comment, skip
	  ring.skip(3);
	  if (isTrace) logger.debug3("Searching for end of comment");
	  while (true) {
	    if (!refill(MIN_TAG_LENGTH)) return null;
	    idx = ring.indexOf(">", -1, false);
	    if (idx >= 2 &&
		((ring.get(idx-1) == '-' && ring.get(idx-2) == '-') ||
		(ring.get(idx-1) == '!' && ring.get(idx-2) == '-'
		 && ring.get(idx-3) == '-'))) {
	      if (isTrace) logger.debug3("Found end of comment");
	      //skip over all of the comment
	      ring.skip(idx);
	      break;
	    }
	    if (idx >= 0) {
	      // found a > that doesn't close the comment.  Skip past it.
	      ring.skip(idx + 1);
	    } else {
	      // No > in ring.
	      // Leave last three chars in case they're "--" or "!--"
	      ring.skip(ring.size() - 3);
	    }
	  }
	} else if (ringStartsWithIgnoreCase(ring, "script>")) {
	  readThroughTag(SCRIPTTAGEND);
	} else {
	  // html tag, read into StringBuffer (created lazily if needed)
	  StringBuffer tagBuf = null;
	  while (true) {
	    if (!refill(MIN_TAG_LENGTH)) break;
	    idx = ring.indexOf(">", -1, false);
	    if (idx >= 0) {
// 	      if (isTrace) logger.debug3("Found > at " + idx);
	      if (tagBuf == null) {
		// If this is first chunk of tag, and it's too short, no
		// need to call parseLink, so avoid creating StringBuffer.
		if (idx < MIN_TAG_LENGTH) {
		  ring.skip(idx + 1);
		  break;
		} else {
		  tagBuf = new StringBuffer(EST_TAG_LENGTH);
		}
	      }
	      ring.remove(tagBuf, idx);
	      ring.skip(1);
	      break;
	    } else {
	      if (tagBuf == null) {
		tagBuf = new StringBuffer(EST_TAG_LENGTH);
	      }
	      ring.remove(tagBuf, ring.size());
	    }
	  }

	  if (tagBuf != null && tagBuf.length() >= MIN_TAG_LENGTH) {
	    String nextLink = parseLink(tagBuf, au);
	    if (lastTagWasScript) {
	      readThroughTag(SCRIPTTAGEND);
	      lastTagWasScript = false;
	    }
	    if (nextLink != null) {
	      return nextLink;
	    }
	  }
	}
      }
    }
    return null;
  }

  private boolean ringStartsWithIgnoreCase(CharRing ring, String str) throws IOException {
    skipWhiteSpace(ring, str.length());
    return ring.startsWithIgnoreCase(str);
  }

  private void readThroughTag(String tag) throws IOException {
    if (isTrace) logger.debug3("Searching for "+tag);
    tag = tag.toLowerCase();
    int tagLength = tag.length();
    while (true) {
      if (!refill(tagLength + 1)) return;
      int idx = ring.indexOf(">", -1, false);
      if (idx >= tagLength-1) {
	if (ring.startsWithIgnoreCase(tag, idx-(tagLength-1))) {
	  if (isTrace) logger.debug3("Found "+tag);
	  ring.skip(idx+1);
	  return;
	}
      }
      if (idx >= 0) {
	// found a > that doesn't close the comment.  Skip past it.
	ring.skip(idx + 1);
      } else {
	// No > in ring.
	// Leave last chars in case they're the end of the tag
	ring.skip(ring.size() - tagLength);
      }
    }
  }

  /** Ensure sufficient chars in ring for shortest tag we're interested in.
   * @param minSize minimum number of chars that must be int he ring
   * @return true if at least minSize chars in ring, false if EOF
   * reached and fewer then minSize chars
   */
  private boolean refill(int minSize) throws IOException {
    if (ring.size() >= minSize) return true;
    while (!readerEof) {
      readerEof = ring.refillBuffer(reader);
      if (isTrace) logger.debug3("refilled: " + ring.toString());
      if (ring.size() >= minSize) {
	return true;
      }
    }
    return false;
  }

  protected boolean beginsWithTag(StringBuffer sb, String tag) {
    return beginsWithTag(sb.toString(), tag);
  }

  protected boolean beginsWithTag(String s1, String tag) {
    if (StringUtil.startsWithIgnoreCase(s1, tag)) {
      int len = tag.length();
      if (s1.length() > len && Character.isWhitespace(s1.charAt(len))) {
        return true;
      }
    }
    return false;
  }


  /**
   * Method overridden in some sub classes, so change with care
   */
  protected String extractLinkFromTag(StringBuffer link, ArchivalUnit au) {
    //String returnStr = null;
    switch (link.charAt(0)) {
      case 'a': //<a href=http://www.yahoo.com>
      case 'A':
	//optimization, since we just have to check a single char
	if (Character.isWhitespace(link.charAt(1))) {
          return ( getAttributeValue(HREF, link) );
        }
        if (beginsWithTag(link, APPLETTAG)) {
          return ( getAttributeValue(CODE, link) );
        }
        if (beginsWithTag(link, AREATAG)) {
          return (  getAttributeValue(HREF, link) );
        }
        break;
      case 'f': //<frame src=frame1.html>
      case 'F':
        if (beginsWithTag(link, FRAMETAG)) {
          return ( getAttributeValue(SRC, link) );
        }
        break;
      case 'o': //<object codebase=blah.java> or <option value="blah.html">
      case 'O':
        if (beginsWithTag(link, OBJECTTAG)) {
          return ( getAttributeValue(CODEBASE, link) );
        }
        if (beginsWithTag(link, OPTIONTAG)) {
          String optionAttribute = getOptionAttribute(au);
          if (optionAttribute != null) {
            return (getAttributeValue(optionAttribute, link));
          }
        }

        break;
      case 'i': //<img src=image.gif>
      case 'I':
        if (beginsWithTag(link, IMGTAG)) {
          return (  getAttributeValue(SRC, link) );
        }
        break;
      case 'e': //<embed src=image.gif>
      case 'E':
        if (beginsWithTag(link, EMBEDTAG)) {
          return (  getAttributeValue(SRC, link) );
        }
        break;
      case 'l': //<link href=blah.css>
      case 'L':
        if (beginsWithTag(link, LINKTAG)) {
          return (  getAttributeValue(HREF, link) );
        }
        break;
      case 'b': //<body backgroung=background.gif>
      case 'B': //or <base href=http://www.example.com>
        if (beginsWithTag(link, BODYTAG)) {
          return (  getAttributeValue(BACKGROUNDSRC, link) );
        }
        if (beginsWithTag(link, BASETAG)) {
	  String newBase = getAttributeValue(HREF, link);
	  if (newBase != null && !"".equals(newBase)) {
	    if (UrlUtil.isMalformedUrl(newBase)) {
	      logger.debug3("base tag found, but has malformed URL: "+newBase);
	      malformedBaseUrl = true;
	    }  else {
	      malformedBaseUrl = false;
	      if (UrlUtil.isAbsoluteUrl(newBase)) {
		logger.debug3("base tag found, setting srcUrl to: " + newBase);
		srcUrl = newBase;
		baseUrl = null;
	      }
	    }
	  }
	}
        break;
      case 's': //<script src=blah.js>
      case 'S':
        if (beginsWithTag(link, SCRIPTTAG)) {
          lastTagWasScript = true;
          return (  getAttributeValue(SRC, link) );
        }
        break;
      case 'm': //<meta http-equiv="refresh"
      case 'M': //"content="0; url=http://example.com/blah.html">
        if (beginsWithTag(link, METATAG)) {
	  String httpEquiv = getAttributeValue(HTTP_EQUIV, link);
	  if (REFRESH.equalsIgnoreCase(httpEquiv)) {
	    String content = getAttributeValue(HTTP_EQUIV_CONTENT, link);
 	    return (  getAttributeValue(HTTP_EQUIV_URL, content) );
	  }
        }
        break;
      case 't': //<tc background=back.gif> or <table background=back.gif>
      case 'T':
        if (beginsWithTag(link, TABLETAG) ||
          beginsWithTag(link, TDTAG)) {
          return (  getAttributeValue(BACKGROUNDSRC, link) );
        }
        break;
    }
    return null;
  }

  private String getOptionAttribute(ArchivalUnit au) {
    if (au != null) {
      TypedEntryMap pMap = au.getProperties();
      if (pMap.containsKey("html-parser-select-attrs")) {
	Collection optionAttributes =
	  pMap.getCollection("html-parser-select-attrs");
	if (optionAttributes != null) {
	  Iterator it = optionAttributes.iterator();
	  return (String)it.next();
	}
      }
    }
    return null;
  }

  /**
   * Method to take a link tag, and parse out the URL it points to, returning
   * a string representation of the url (lifted and rewritten from the Gosling
   * crawler), including the reference tag
   *
   * @param link StringBuffer containing the text of a link tag (everything
   * between < and > (ie, "a href=http://www.test.org")
   * @return string representation of the url from the link tag
   * @throws MalformedURLException
   */
  protected String parseLink(StringBuffer link, ArchivalUnit au)
      throws MalformedURLException {
    String returnStr = extractLinkFromTag(link, au);

    if (returnStr != null) {
      if (isTrace) {
	logger.debug2("Generating url from: " + srcUrl + " and " + returnStr);
      }
      if (malformedBaseUrl) {
	//if we have a malformed base URL, we can't interpret relative urls
	//so we only will return absolute ones
	logger.debug2("Malformed base URL: " + srcUrl +
		      " checking if URL is abolute " + returnStr);
	return UrlUtil.isAbsoluteUrl(returnStr) ? returnStr : null;
      }
      try {
	if (baseUrl == null) {
	  logger.debug3("baseUrl is null, setting to srcUrl: "+srcUrl);
	  baseUrl = new URL(srcUrl);
	}
	returnStr = resolveUri(baseUrl, returnStr);
      } catch (MalformedURLException e) {
	logger.debug("Couldn't resolve URL, base: \"" + srcUrl +
		     "\", link: \"" + returnStr + "\"",
		     e);
	return null;
      }
      if (isTrace) {
	logger.debug2("Parsed: " + returnStr);
      }
    }
    return returnStr;
  }

  /**
   * Handle resolving of a URI from a base url and a relative url.
   * Called out separately so we can add exceptions (like javascript) here
   */
  protected String resolveUri(URL base, String relative)
      throws MalformedURLException {
    String baseProto = null;
    if (base != null) {
      baseProto = base.getProtocol();
    }
    if ("javascript".equalsIgnoreCase(baseProto) ||
	relative != null && StringUtil.startsWithIgnoreCase(relative,
							    "javascript:")) {
      return resolveJavascriptUrl(base, relative);
    }
    if ("mailto".equalsIgnoreCase(baseProto) ||
	relative != null && StringUtil.startsWithIgnoreCase(relative,
							    "mailto:")) {
      return null;
    }
    return UrlUtil.resolveUri(baseUrl, relative);
  }

  protected String resolveJavascriptUrl(URL base, String relative)
      throws MalformedURLException {
    if (!shouldParseJavaScript) {
      logger.debug3("Configured to ignore javascript urls, so skipping");
      return null;
    }
    logger.debug("Tried to resolve javascript URI "+base+" "+relative);
    relative = UrlUtil.parseJavascriptUrl(relative);
    return UrlUtil.resolveUri(base, relative);
  }

  /** Return attribute value with any html entities decoded */
  protected String getAttributeValue(String attribute, StringBuffer sb) {
    return getAttributeValue(attribute, sb.toString());
  }

  /** Return attribute value with any html entities decoded */
  protected String getAttributeValue(String attribute, String src) {
    String val = getEncodedAttributeValue(attribute, src);
    return val == null ? null : Translate.decode(val);
  }

  /** Return attribute value as it literally appears in source html */
  protected String getEncodedAttributeValue(String attribute, String src) {
    if (isTrace) {
      logger.debug3("looking for "+attribute+" in "+src);
    }
    //  we need to allow for all whitespace in our tokenizer;
    StringTokenizer st = new StringTokenizer(src, "\n\t\r '=\"", true);
    String prevNonWhite = null;
    // search for "attribute ="
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (isWhitespace(token)) {
	continue;
      }
      if (token.equals("=") && attribute.equalsIgnoreCase(prevNonWhite)) {
	break;
      }
      prevNonWhite = token;
    }
    // extract the attribute value
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (isWhitespace(token)) {
	continue;
      }
      if (token.equals("\"")) {
	return getTokensUntil(st, "\"", null);
      } else if (token.equals("'")){
	return getTokensUntil(st, "'", null);
      } else {
	StringBuffer sb = new StringBuffer(EST_URL_LENGTH);
	sb.append(token);
	return getTokensUntilWhite(st, sb);
      }
    }
    return null;
  }

  String getTokensUntil(StringTokenizer st, String endStr, StringBuffer sb) {
    if (sb == null) {
      sb = new StringBuffer(EST_URL_LENGTH);
    }
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (token.equals(endStr)) {
	break; //we've hit the end of the attribute value
      } else {
	sb.append(token);
      }
    }
    return sb.toString();
  }

  private String getTokensUntilWhite(StringTokenizer st, StringBuffer sb) {
    if (sb == null) {
      sb = new StringBuffer(EST_URL_LENGTH);
    }
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (isWhitespace(token)) {
	break;
      }
      sb.append(token);
    }
    return sb.toString();
  }


  boolean isWhitespace(String token) {
    if (Character.isWhitespace(token.charAt(0))) {
      if (token.length() != 1) {
	logger.warning("Multi-char token begins with white: " + token);
      }
      return true;
    }
    return false;
  }

  boolean isNewline(String token) {
    char ch = token.charAt(0);
    if (ch == NEWLINE_CHAR || ch == CARRIAGE_RETURN_CHAR) {
      if (token.length() != 1) {
	logger.warning("Multi-char token begins with newline: " + token);
      }
      return true;
    }
    return false;
  }


//   private static String extractScriptUrl(String src) {
//     int begin = src.indexOf("'");
//     int end = src.indexOf("'",begin+1);
//     if(end > begin)
//       return src.substring(begin+1,end);
//     return src;
//   }

}
