/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.extractor;

import org.apache.commons.io.IOUtils;
import org.htmlparser.util.Translate;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.CharRing;
import org.lockss.util.DataUri;
import org.lockss.util.HtmlUtil;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;
import org.lockss.util.StringUtil;
import org.lockss.util.TypedEntryMap;
import org.lockss.util.UrlUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

public class GoslingHtmlLinkExtractor implements LinkExtractor {

  static final String PREFIX = Configuration.PREFIX + "extractor.gosling.";

  public static final String PARAM_BUFFER_CAPACITY =
    PREFIX + "buffer_capacity";
  public static final int DEFAULT_BUFFER_CAPACITY = 4096;

  public static final String PARAM_PARSE_JS = PREFIX + "parse_js";
  public static final boolean DEFAULT_PARSE_JS = false;
  
  public static final String PARAM_PARSE_CSS = PREFIX + "parse_css";
  public static final boolean DEFAULT_PARSE_CSS = true;

  protected static final String ATAG = "a";
  protected static final String APPLETTAG = "applet";
  protected static final String AREATAG = "area";
  protected static final String BASETAG = "base";
  protected static final String BODYTAG = "body";
  protected static final String EMBEDTAG = "embed";
  /** @since 1.67.5 */
  protected static final String FORMTAG = "form";
  protected static final String FRAMETAG = "frame";
  protected static final String IFRAMETAG = "iframe";
  protected static final String IMGTAG = "img";
  protected static final String JSCRIPTTAG = "javascript";
  protected static final String LINKTAG = "link";
  protected static final String METATAG = "meta";
  protected static final String OBJECTTAG = "object";
  protected static final String OPTIONTAG = "option";
  protected static final String SCRIPTTAG = "script";
  protected static final String STYLETAG = "style";
  protected static final String TABLETAG = "table";
  protected static final String TDTAG = "td";
  protected static final String THTAG = "th";
  protected static final String VALUETAG = "value";
  
  protected static final String SCRIPTTAGEND = "/script>";

  protected static final String BACKGROUNDSRC = "background";
  protected static final String CODE = "code";
  protected static final String CODEBASE = "codebase";
  protected static final String HREF = "href";
  protected static final String SRC = "src";
  protected static final String DATA = "data";

  /*
          Two Different charset specifications
   4.01: <meta http-equiv="content-type" content="text/html; charset=UTF-8">
      5: <meta charset="UTF-8">
   */
  protected static final String CHARSET = "charset";
  protected static final String REFRESH = "refresh";
  protected static final String HTTP_EQUIV = "http-equiv";
  protected static final String HTTP_EQUIV_CONTENT = "content";
  
  protected static final char NEWLINE_CHAR = '\n';
  protected static final char CARRIAGE_RETURN_CHAR = '\r';

  //smallest size any of the tags we're interested in can be; <a href=
  private static final int MIN_TAG_LENGTH = 5;

  // initial tag string buffer size
  private static int EST_TAG_LENGTH = 60;

  // initial url string buffer size
  private static int EST_URL_LENGTH = 40;

  private static Logger logger = Logger.getLogger("GoslingHtmlLinkExtractor");

  protected String srcUrl = null;
  protected URL baseUrl = null;
  private String encoding;
  private Reader reader;
  private boolean readerEof;

  private CharRing ring;
  private int ringCapacity;
  private boolean shouldParseJavaScript;
  private boolean isTrace = logger.isDebug2();

  private boolean malformedBaseUrl = false;

  private boolean lastTagWasScript = false;
  private boolean hasBaseBeenSet = false;

  public GoslingHtmlLinkExtractor() {
    ringCapacity = CurrentConfig.getIntParam(PARAM_BUFFER_CAPACITY,
					     DEFAULT_BUFFER_CAPACITY);
    shouldParseJavaScript =
      CurrentConfig.getBooleanParam(PARAM_PARSE_JS, DEFAULT_PARSE_JS);
  }

  private void init() {
    lastTagWasScript = false;
    hasBaseBeenSet = false;
    baseUrl = null;
    readerEof = false;
    ring = new CharRing(ringCapacity);
    try {
      baseUrl = new URL(srcUrl);
      malformedBaseUrl = false;
    } catch (MalformedURLException e) {
      malformedBaseUrl = true;
      logger.warning("Malformed srcUrl; shouldn't happen: " + srcUrl, e);
    }
  }

  /**
   * Method which will parse the html file represented by reader and call
   * cb.foundUrl() for each url found
   *
   * @throws IOException
   */
  public void extractUrls(ArchivalUnit au, InputStream in,
			  String encoding, String srcUrl,
			  LinkExtractor.Callback cb)
      throws IOException {
    if (in == null) {
      throw new IllegalArgumentException("Called with null InputStream");
    } else if (srcUrl == null) {
      throw new IllegalArgumentException("Called with null srcUrl");
    } else if (cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }
    reader = StringUtil.getLineReader(in, encoding);
    this.srcUrl = srcUrl;
    this.encoding = encoding;

    try {
      init();
      if (isTrace) logger.debug2("Extracting URLs from " + srcUrl);

      do {
        // keep calling extractNextLink
      } while (extractNextLink(ring, au, cb));
    } finally {
      // Let go of large objects
      ring = null;
      IOUtils.closeQuietly(reader);
      reader = null;
    }
  }

  protected void emit(LinkExtractor.Callback callback, String url) {
    if (isTrace) {
      logger.debug2("Extracted "+url);
    }
    callback.foundLink(url);
  }


  /**
   * Keep calling skipLeadingWhiteSpace and refilling the char ring until there
   * is no more leading white space
   * @param ring 
   * CharRing to strip whitespace from
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
  protected boolean extractNextLink(CharRing ring, ArchivalUnit au,
				    LinkExtractor.Callback cb)
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
	if (!refill(MIN_TAG_LENGTH)) return false;
	if (ring.get(0) == '!' && ring.get(1) == '-' && ring.get(2) == '-') {
	  // html comment, skip
	  ring.skip(3);
	  if (isTrace) logger.debug3("Searching for end of comment");
	  while (true) {
	    if (!refill(MIN_TAG_LENGTH)) return false;
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
        } else if (ringStartsWithIgnoreCase(ring, "script>")) { // <script> (no attributes)
          readThroughTag(SCRIPTTAGEND);
        } else if (ringStartsWithIgnoreCase(ring, "style>")
                   && CurrentConfig.getBooleanParam(PARAM_PARSE_CSS,
                                                    DEFAULT_PARSE_CSS)) { // <style> (no attributes)
          ring.skip("style>".length());
          parseStyleContentsFromRing(au, cb);
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
	    boolean nextLink = parseLink(tagBuf, au, cb);
            if (lastTagWasScript) {
	      readThroughTag(SCRIPTTAGEND);
	      lastTagWasScript = false;
	    }
	    if (nextLink) {
              return true;
	    }
	  }
	}
      }
    }
    return false;
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
   * Extract all links from tag.
   */
  protected String extractAllLinksFromTag(StringBuffer link, ArchivalUnit au,
					  LinkExtractor.Callback cb)
      throws IOException {
    extractCommonLinksFromTag(link, au, cb);
    return extractLinkFromTag(link, au, cb);
  }

  /**
   * Extract links common to all tags.
   */
  protected void extractCommonLinksFromTag(StringBuffer link,
					   ArchivalUnit au,
					   LinkExtractor.Callback cb)
      throws IOException {
    String style = getAttributeValue("style", link);
    if (!StringUtil.isNullString(style)) {
      processStyleText(au, cb, new StringReader(style), "attribute");
    }
  }

  /**
   * Perform tag-specfic link extraction.
   * Method overridden in many plugin-specific subclasses; change with care
   */
  protected String extractLinkFromTag(StringBuffer link, ArchivalUnit au,
				      LinkExtractor.Callback cb)
      throws IOException {
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
        if (beginsWithTag(link, IFRAMETAG)) {
          return(getAttributeValue(SRC, link));
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
      case 'b': //<body background=background.gif>
      case 'B': //or <base href=http://www.example.com>
        if (beginsWithTag(link, BODYTAG)) {
          return (  getAttributeValue(BACKGROUNDSRC, link) );
        }
        if (beginsWithTag(link, BASETAG)) {
	  processBaseTag(link);
	}
        break;
      case 's': //<script src=blah.js> or <style type="text/css">...CSS...</style>
      case 'S':
        if (beginsWithTag(link, SCRIPTTAG)) {
          lastTagWasScript = true;
          return (  getAttributeValue(SRC, link) );
        }
        if (beginsWithTag(link, STYLETAG)
            && CurrentConfig.getBooleanParam(PARAM_PARSE_CSS,
                                             DEFAULT_PARSE_CSS)) {
          parseStyleContentsFromRing(au, cb);
        }
        break;
      case 'm': //<meta http-equiv="refresh"
      case 'M': //"content="0; url=http://example.com/blah.html">
        if (beginsWithTag(link, METATAG)) {
	  String httpEquiv = getAttributeValue(HTTP_EQUIV, link);
	  if (REFRESH.equalsIgnoreCase(httpEquiv)) {
	    String content = getAttributeValue(HTTP_EQUIV_CONTENT, link);
 	    return HtmlUtil.extractMetaRefreshUrl(content);
	  }
        }
        break;
      case 't': // <table background=back.gif> or <td background=back.gif> or <th background=back.gif> 
      case 'T': // See http://msdn.microsoft.com/workshop/author/dhtml/reference/properties/background_2.asp
        if (beginsWithTag(link, TABLETAG)
            || beginsWithTag(link, TDTAG)
            || beginsWithTag(link, THTAG)) {
          return (  getAttributeValue(BACKGROUNDSRC, link) );
        }
        break;
    }
    return null;
  }

  protected void processBaseTag(StringBuffer link) {
    if (hasBaseBeenSet) {
      logger.siteWarning("Ignoring 2nd (or later) base tag: " + link);
      return;
    }
    String newBase = getAttributeValue(HREF, link);
    if (newBase == null) {
      logger.debug3("Base tag w/ no href, ignoring: " + link);
      return;
    }
    logger.debug3("Base tag found, setting baseUrl to: " + newBase);
    try {
      baseUrl = new URL(resolveUri(baseUrl, newBase));
      logger.debug3("Base tag found (" + newBase +
		    "), setting baseUrl to: " + baseUrl);
      malformedBaseUrl = false;
      hasBaseBeenSet = true;
    } catch (MalformedURLException e) {
      malformedBaseUrl = true;
      logger.siteWarning("Base tag has malformed URL: "+ newBase, e);
      logger.siteWarning("Base is still: " + baseUrl);
    }
  }

  protected void parseStyleContentsFromRing(ArchivalUnit au,
					    LinkExtractor.Callback cb) {
    Reader cssReader = new Reader() {
      
      boolean closed = false;
      
      public void close() {
        closed = true;
      }
      
      public int read(char[] cbuf, int off, int len) throws IOException {
        int ix = 0;
        while (ix < len) {
          int ret = read();
          if (ret == -1) break; 
          cbuf[off + ix] = (char)ret;
          ++ix;
        }
        return ix == 0 ? -1 : ix;
      }
      
      public int read() throws IOException {
        if (!refill("</style>".length()) && !closed) {
          logger.siteWarning("Unclosed <style> section in " + srcUrl);
        }
        if (ring.size() == 0 || ring.startsWithIgnoreCase("</style>")) {
          return -1;
        }
        return ring.remove();
      }
    };

    processStyleText(au, cb, cssReader, "tag");
  }


  private void processStyleText(ArchivalUnit au,
				LinkExtractor.Callback cb,
				Reader rdr,
				String where) {
    InputStream cssIn = new ReaderInputStream(rdr, encoding);
    try {
      LinkExtractor cssExtractor = au.getLinkExtractor("text/css");
      logger.debug2("CSS extractor: " + cssExtractor + ", " + au);
      if (cssExtractor != null) {
	cssExtractor.extractUrls(au, cssIn, encoding, srcUrl, cb);
      }
    } catch (Exception e) {
      // Important to catch RuntimeExceptions here or a CSS error will
      // abort processing of the rest of the html on the page
      logger.siteError("The CSS parser failed to parse a <style> " + where +
		       " in " + srcUrl, e);
      try {
	readToEof(cssIn);
	IOUtil.safeClose(cssIn);
      }
      catch (IOException ignore) {}
    }
  }

  private void readToEof(InputStream in) throws IOException {
    do {} while (in.read() != -1);
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
  protected boolean parseLink(StringBuffer link, ArchivalUnit au,
			      LinkExtractor.Callback cb)
      throws IOException, MalformedURLException {
    String returnStr = extractAllLinksFromTag(link, au, cb);
    if (returnStr != null) {
      if(DataUri.isDataUri(returnStr)) {
        DataUri.dispatchToLinkExtractor(returnStr,baseUrl.toString(),au,cb);
      }
      else {
        resolveAndEmit(cb, returnStr);
      }
    }
    return true;
  }

  /** Emit the URL after resolving it against the base URL if necessary
   * @param cb the callback
   * @param relOrAbsUrl the possibly-relative url extracted from the html
   * @return true if a URL was omitted.  False if called with a relaitve
   * URL when the last base tag was malformed.
   */
  protected boolean resolveAndEmit(LinkExtractor.Callback cb,
				   String relOrAbsUrl)
      throws IOException, MalformedURLException {

    if (relOrAbsUrl == null) {
      return false;
    }
    if (malformedBaseUrl) {
      if (UrlUtil.isAbsoluteUrl(relOrAbsUrl)) {
	emit(cb, relOrAbsUrl);
	return true;
      } else {
	logger.debug2("Malformed base URL: " + baseUrl +
		      ", not emitting relative link: " + relOrAbsUrl);
	return false;
      }
    }
    if (isTrace) {
      logger.debug2("Generating url from base: " + baseUrl + " and: " +
		    relOrAbsUrl);
    }
    try {
      String absUrl = resolveUri(baseUrl, relOrAbsUrl);
      if (absUrl != null) {
	emit(cb, absUrl);
	return true;
      } else {
	return false;
      }
    } catch (MalformedURLException e) {
      logger.siteWarning("Couldn't resolve URL, base: \"" + baseUrl +
			 "\", link: \"" + relOrAbsUrl + "\": " + e);
      return false;
    }
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
    if (StringUtil.indexOfIgnoreCase(src, attribute) >= 0) {
      String val = getEncodedAttributeValue(attribute, src);
      return val == null ? null : Translate.decode(val);
    }
    return null;
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

  public static class Factory implements LinkExtractorFactory {
    public LinkExtractor createLinkExtractor(String mimeType) {
      return new GoslingHtmlLinkExtractor();
    }
  }
}
