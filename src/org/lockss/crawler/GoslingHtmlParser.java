/*
 * $Id: GoslingHtmlParser.java,v 1.13 2004-03-09 23:24:52 troberts Exp $
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
import org.lockss.plugin.*;
import org.lockss.util.*;


public class GoslingHtmlParser implements ContentParser {

  private static final String METATAG = "meta";
  private static final String IMGTAG = "img";
  private static final String ATAG = "a";
  private static final String FRAMETAG = "frame";
  private static final String LINKTAG = "link";
  private static final String SCRIPTTAG = "script";
  private static final String SCRIPTTAGEND = "/script";
  private static final String BODYTAG = "body";
  private static final String TABLETAG = "table";
  private static final String TDTAG = "tc";
  private static final String JSCRIPTTAG = "javascript";
  private static final String ASRC = "href";
  private static final String SRC = "src";
  private static final String BACKGROUNDSRC = "background";
  private static final String REFRESH = "refresh";
  private static final String HTTP_EQUIV = "http-equiv";
  private static final String HTTP_EQUIV_CONTENT = "content";
  private static final String HTTP_EQUIV_URL = "url";

  private static final String NEWLINE = "\n";
  private static final String CARRIAGE_RETURN = "\r";

  //smallest size any of the tags we're interested in can be; <a href=
  private static final int MIN_TAG_LENGTH = 5;

  private static Logger logger = Logger.getLogger("GoslingHtmlParser");


  /**
   * Method which will parse the html file represented by cu call
   * cb.foundUrl() for each url found
   *
   * @param cu object representing a html file in the local file system
   * @param cb callback to call each time a url is found
   * @throws IOException
   */
  public void parseForUrls(CachedUrl cu, ContentParser.FoundUrlCallback cb)
      throws IOException {
    if (cu == null) {
      throw new IllegalArgumentException("Called with null cu");
    } else if (cb == null) {
      throw new IllegalArgumentException("Called with null callback");
    }
    String cuStr = cu.getUrl();
    if (cuStr == null) {
      logger.error("CachedUrl has null getUrl() value: "+cu);
      return;
    }

    Reader reader = cu.openForReading();

    String redirectedTo = getRedirectedTo(cu);
    String srcUrl = cuStr;
    if (redirectedTo != null) {
      if (logger.isDebug3()) {
	logger.debug3("redirected-to set to "+redirectedTo
		      +". Using that as base URL");
      }
      srcUrl = redirectedTo;
    } 
    logger.debug3("Extracting urls from srcUrl");
    String nextUrl = null;
    while ((nextUrl = extractNextLink(reader, srcUrl)) != null) {
      if (logger.isDebug3()) {
	logger.debug3("Extracted "+nextUrl);
      }
      cb.foundUrl(nextUrl);
    }
  }

  private static String getRedirectedTo(CachedUrl cu) {
    Properties props = cu.getProperties();
    if (props != null) {
      return (String)props.get(CachedUrl.PROPERTY_REDIRECTED_TO);
    }
    return null;
  }

  /**
   * Read through the reader stream, extract and return the next url found
   *
   * @param reader Reader object to extract the link from
   * @param srcUrl URL object representing the page we are looking at
   * (for resolving relative links)
   * @return String representing the next url in reader
   * @throws IOException
   * @throws MalformedURLException
   */
  protected static String extractNextLink(Reader reader, String srcUrl)
      throws IOException, MalformedURLException {
    String nextLink = null;
    int c = 0;
    StringBuffer lineBuf = new StringBuffer();

    while(nextLink == null && c >=0) {
      //skip to the next tag
      do {
	c = reader.read();
      } while (c >= 0 && c != '<');

      if (c == '<') {
	int pos = 0;
	c = reader.read();
	while (c >= 0 && c != '>') {
	  if(pos==2 && c=='-' && lineBuf.charAt(0)=='!'
	     && lineBuf.charAt(1)=='-') {
	    // we're in a HTML comment
	    pos = 0;
	    int lc1 = 0;
	    int lc2 = 0;
	    while((c = reader.read()) >= 0
		  && (c != '>' || lc1 != '-' || lc2 != '-')) {
	      lc1 = lc2;
	      lc2 = c;
	    }
	    break;
	  }
	  lineBuf.append((char)c);
	  pos++;
	  c = reader.read();
	}
	//optimization. if lineBuf is smaller than this, can't be any link
	if (lineBuf.length() >= MIN_TAG_LENGTH) {
	  nextLink = parseLink(lineBuf, srcUrl);
	}
	lineBuf = new StringBuffer();
      }
    }
    return nextLink;
  }


  private static boolean beginsWithTag(String s1, String tag) {
    if (StringUtil.startsWithIgnoreCase(s1, tag)) {
      int len = tag.length();
      if (s1.length() > len && Character.isWhitespace(s1.charAt(len))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Method to take a link tag, and parse out the URL it points to, returning
   * a string representation of the url (lifted and rewritten from the Gosling
   * crawler)
   *
   * @param link StringBuffer containing the text of a link tag (everything
   * between < and > (ie, "a href=http://www.test.org")
   * @param srcUrl URL object representing the page on which this
   * url was taken from (for resolving relative tags)
   * @return string representation of the url from the link tag
   * @throws MalformedURLException
   */
  protected static String parseLink(StringBuffer link, String srcUrl)
      throws MalformedURLException {
    String returnStr = null;

    switch (link.charAt(0)) {
      case 'a': //<a href=http://www.yahoo.com>
      case 'A':
	//optimization, since we just have to check a single char
	if (Character.isWhitespace(link.charAt(1))) { 
          returnStr = getAttributeValue(ASRC, link);
//           if (returnStr != null && returnStr.startsWith(JSCRIPTTAG)) {
//             returnStr = extractScriptUrl(returnStr);
//           }
        }
        break;
      case 'f': //<frame src=frame1.html>
      case 'F':
        if (beginsWithTag(link.toString(),FRAMETAG)) {
          returnStr = getAttributeValue(SRC, link);
        }
        break;
      case 'i': //<img src=image.gif>
      case 'I':
        if (beginsWithTag(link.toString(),IMGTAG)) {
          returnStr = getAttributeValue(SRC, link);
        }
        break;
      case 'l': //<link href=blah.css>
      case 'L':
        if (beginsWithTag(link.toString(),LINKTAG)) {
          returnStr = getAttributeValue(ASRC, link);
        }
        break;
      case 'b': //<body backgroung=background.gif>
      case 'B':
        if (beginsWithTag(link.toString(),BODYTAG)) {
          returnStr = getAttributeValue(BACKGROUNDSRC, link);
        }
        break;
      case 's': //<script src=blah.js>
      case 'S':
        if (beginsWithTag(link.toString(),SCRIPTTAG)) {
          returnStr = getAttributeValue(SRC, link);
        }
        break;
      case 'm': //<meta http-equiv="refresh"
      case 'M': //"content="0; url=http://example.com/blah.html">
        if (beginsWithTag(link.toString(),METATAG)) {
	  String httpEquiv = getAttributeValue(HTTP_EQUIV, link);
	  if (REFRESH.equalsIgnoreCase(httpEquiv)) {
	    String content = getAttributeValue(HTTP_EQUIV_CONTENT, link);
 	    returnStr = getAttributeValue(HTTP_EQUIV_URL, content);
	  }
        }
        break;
      case 't': //<tc background=back.gif> or <table background=back.gif>
      case 'T':
        if (beginsWithTag(link.toString(),TABLETAG) ||
          beginsWithTag(link.toString(),TDTAG)) {
          returnStr = getAttributeValue(BACKGROUNDSRC, link);
        }
        break;
      default:
        return null;
    }
    if (returnStr != null) {
      returnStr = StringUtil.trimAfterChars(returnStr, "#");
      if (logger.isDebug3()) {
	logger.debug3("Generating url from: " + srcUrl + " and " + returnStr);
      }
      try {
	returnStr = UrlUtil.resolveUri(srcUrl, returnStr);
      } catch (MalformedURLException e) {
	logger.debug("Got a bad URL", e);
	return null;
      }
      if (logger.isDebug3()) {
	logger.debug3("Parsed: " + returnStr);
      }
    }
    return returnStr;
  }

  private static String getAttributeValue(String attribute, StringBuffer sb) {
    return getAttributeValue(attribute, sb.toString());
  }

  private static String getAttributeValue(String attribute, String src) {
    if (logger.isDebug3()) {
      logger.debug3("looking for "+attribute+" in "+src);
    }
    //  we need to allow for all whitespace in our tokenizer;
    StringTokenizer st = new StringTokenizer(src, "\n\t\r '=\"", true);
    String lastToken = null;
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (!token.equals("=")) {
	if (!token.equals(" ") && !token.equals("\"")) {
	  lastToken = token;
	}
      } else {
        if (attribute.equalsIgnoreCase(lastToken)){
	  if (st.hasMoreTokens()) {
	    String firstToken = st.nextToken();
	    if (firstToken.equals("\"")) {
	      return getTokensUntil(st, "\"");
	    } else if (firstToken.equals("'")){
	      return getTokensUntil(st, "'");
	    } else {
	      StringBuffer sb = new StringBuffer(firstToken);
	      sb.append(getTokensUntil(st, " "));
	      return sb.toString();
	    }
	  }
	}
      }
    }
    return null;
  }

    private static String getTokensUntil(StringTokenizer st, String endStr) {
      StringBuffer sb = new StringBuffer();
      while (st.hasMoreTokens()) {
	String token = st.nextToken();
	if (token.equals(endStr)) {
	  break; //we've hit the end of the attribute value
	} else if (!token.equals(NEWLINE) && !token.equals(CARRIAGE_RETURN)) {
	sb.append(token);
	}
      }
      return sb.toString();
    }
  

//   private static String extractScriptUrl(String src) {
//     int begin = src.indexOf("'");
//     int end = src.indexOf("'",begin+1);
//     if(end > begin)
//       return src.substring(begin+1,end);
//     return src;
//   }



}
