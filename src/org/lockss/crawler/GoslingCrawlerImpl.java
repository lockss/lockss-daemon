/*
 * $Id: GoslingCrawlerImpl.java,v 1.4 2002-12-17 20:59:04 troberts Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
 * Copyright (c) 2002 Sun Microsystems. All Rights Reserved.
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

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import org.lockss.daemon.*;
import org.lockss.util.*;

/**
 * The crawler.
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */


public class GoslingCrawlerImpl implements Crawler {
  /**
   * TODO
   * 1) write state to harddrive using whatever system we come up for for the
   * rest of LOCKSS
   * 2) check deadline and die if we run too long
   */


  private static final String IMGTAG = "img";
  private static final String ATAG = "a";
  private static final String FRAMETAG = "frame";
  private static final String LINKTAG = "link";
  private static final String SCRIPTTAG = "script";
  private static final String SCRIPTTAGEND = "/script";
  private static final String BODYTAG = "body";
  private static final String TABLETAG = "table";
  private static final String TDTAG = "tc";

  private static final String ASRC = "href";
  private static final String SRC = "src";
  private static final String BACKGROUNDSRC = "background";

  private static Logger logger = Logger.getLogger("GoslingCrawlerImpl");


  /**
   * Main method of the crawler; it loops through crawling and caching
   * urls.
   *
   * @param au ArchivalUnit which this crawl is within 
   * @param spec CachedUrlSet encapsulating the starting point of the
   * crawl and the rules for which urls to cache
   *
   */
  public void doCrawl(ArchivalUnit au, List urls, boolean followLinks,
		       boolean overWrite, Deadline deadline) {
    if (au == null) {
      throw new IllegalArgumentException("Called with a null ArchivalUnit");
    } else if (urls == null) {
      throw new IllegalArgumentException("Called with a null CrawlSpec");
    } else if (deadline == null) {
      throw new IllegalArgumentException("Called with a null Deadline");
    }



    List list = createInitialList(urls);
    CachedUrlSet cus = au.getAUCachedUrlSet();
    while (!list.isEmpty() && !deadline.expired()) {
      String url = (String)list.remove(0);
      logger.debug("Dequeued url from list: "+url);
      UrlCacher uc = cus.makeUrlCacher(url);
      if (uc.shouldBeCached()) {
 	if (overWrite || !cus.isCached(url)) {
	  try {
	    logger.info("caching "+uc);
	    uc.cache(); //IOException if there is a caching problem
	  } catch (IOException ioe) {
	    //XXX handle this better.  Requeue?
	    logger.error("Problem caching "+uc+". Ignoring", ioe);
	  }
	  au.pause(); //XXX make sure we throw InterruptedExceptions
	}
 	else {
 	  logger.info(uc+" exists, not caching");
 	}
	try{
	  if (followLinks) {
	    CachedUrl cu = uc.getCachedUrl();
	    addUrlsToList(cu, list);//IOException if the CU can't be read
	  }
	} catch (IOException ioe) {
	  //XXX handle this better.  Requeue?
	  logger.error("Problem parsing "+uc+". Ignoring", ioe);
	}
      }
      logger.debug("Removing from list: "+uc.getUrl());
    }
  }

  /**
   * Method to generate a List of urls to start a crawl at from a
   * CachedUrlSet.
   *
   * @param urls List of urls to start the crawl at
   * @returns A modifiable list of the urls (in string form)
   */
  protected static List createInitialList (List urls) {
    return new LinkedList(urls);
  }

  /**
   * Method which will parse the html file represented by cu and add all
   * urls on it to list
   *
   *@param cu object representing a html file in the local file system
   *@param list list to which all the urs in cu should be added
   */
  protected static void addUrlsToList(CachedUrl cu, List list)
      throws IOException {
    if (shouldExtractLinksFromCachedUrl(cu)) {
      String cuStr = cu.getUrl();
      if (cuStr == null) {
	logger.error("CachedUrl has null getUrl() value: "+cu);
	return;
      }

      InputStream is = cu.openForReading();
      Reader reader = new InputStreamReader(is); //should do this elsewhere
      URL srcUrl = new URL(cuStr);
      logger.debug("Extracting urls from srcUrl");
      String nextUrl = null;
      while ((nextUrl = ExtractNextLink(reader, srcUrl)) != null) {
	logger.debug("Extracted "+nextUrl);

	//should check if this is something we should cache first
	if (!list.contains(nextUrl)) { 
	  list.add(nextUrl);
	}
      }
    }
  }

  /**
   * Determine if this is a CachedUrl that we can parse for new urls; currently
   * this is just done by verifying that the "content-type" property exists
   * and equals "text/html"
   *
   * @param cu CachedUrl representing the web page we may parse
   * @return true if cu has "content-type" set to "text/html", false otherwise
   */
  protected static boolean shouldExtractLinksFromCachedUrl(CachedUrl cu) {
    boolean returnVal = false;
    Properties props = cu.getProperties();
    if (props != null) {
      String contentType = props.getProperty("content-type");
      if (contentType != null) {
	//XXX check if the string starts with this
	returnVal = contentType.toLowerCase().startsWith("text/html");
      }
    }
    if (returnVal) {
      logger.debug("I should try to extract links from "+cu);
    } else {
      logger.debug("I shouldn't try to extract links from "+cu);
    }

    return returnVal;
  }

  /**
   * Read through the reader stream, extract and return the next url found
   *
   * @param reader Reader object to extract the link from
   * @param srcUrl URL object representing the page we are looking at
   * (for resolving relative links)
   * @returns String representing the next url in reader
   */
  protected static String ExtractNextLink(Reader reader, URL srcUrl)
      throws IOException, MalformedURLException {
    if (reader == null) {
      return null;
    }
    boolean inscript = false; //FIXME or I will break when we look at scripts
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
	if (inscript) {
	  //FIXME when you deal with the script problems
	  //	  if(lookingAt(lineBuf, 0, pos, scripttagend)) {
	  inscript = false;
	  //}
	} else if (lineBuf.length() >= 5) { //see if the lineBuf has a link tag
	  nextLink = ParseLink(lineBuf, srcUrl);
	}
	lineBuf = new StringBuffer();
      }
    }
    return nextLink;
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
   * @returns string representation of the url from the link tag
   */
  protected static String ParseLink(StringBuffer link, URL srcUrl)
      throws MalformedURLException {
    String returnStr = null;

    switch (link.charAt(0)) {
    case 'a': //<a href=http://www.yahoo.com>
    case 'A':
      if (StringUtil.getIndexIgnoringCase(link.toString(), 
					  ATAG+" ") == 0) {
	returnStr = getAttributeValue(ASRC, link.toString());
      }
      break;
    case 'f': //<frame src=frame1.html>
    case 'F':
      if (StringUtil.getIndexIgnoringCase(link.toString(), 
					  FRAMETAG+" ") == 0) {
	returnStr = getAttributeValue(SRC, link.toString());
      }
      break;
    case 'i': //<img src=image.gif>
    case 'I':
      if (StringUtil.getIndexIgnoringCase(link.toString(), 
					  IMGTAG+" ") == 0) {
	returnStr = getAttributeValue(SRC, link.toString());
      }
      break;
    case 'l': //<link href=blah.css>
    case 'L':
      if (StringUtil.getIndexIgnoringCase(link.toString(), 
					  LINKTAG+" ") == 0) {
	returnStr = getAttributeValue(ASRC, link.toString());
      }
      break;
    case 'b': //<body backgroung=background.gif>
    case 'B':
      if (StringUtil.getIndexIgnoringCase(link.toString(), 
					  BODYTAG+" ") == 0) {
	returnStr = getAttributeValue(BACKGROUNDSRC, link.toString());
      }
      break;
    case 's': //<script src=blah.js>
    case 'S':
      if (StringUtil.getIndexIgnoringCase(link.toString(), 
					  SCRIPTTAG+" ") == 0) {
	returnStr = getAttributeValue(SRC, link.toString());
      }
      break;
    case 't': //<tc background=back.gif> or <table background=back.gif>
    case 'T':
      if (StringUtil.getIndexIgnoringCase(link.toString(), 
					  TABLETAG+" ") == 0 ||
	  StringUtil.getIndexIgnoringCase(link.toString(), 
					  TDTAG+" ") == 0) {
	  returnStr = getAttributeValue(BACKGROUNDSRC, link.toString());
	}
      break;
    default:
      return null;
    }

    if (returnStr != null) {
      returnStr = StringUtil.trimAfterChars(returnStr, " #\"");
      URL retUrl = new URL(srcUrl, returnStr);
      return retUrl.toString();
    }
    return null;
  }

  private static String getAttributeValue(String attribute, String src) {
    logger.debug("looking for "+attribute+" in "+src);
    StringTokenizer st = new StringTokenizer(src, " =", true);
    String lastToken = null;
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (!token.equals("=")) {
	if (!token.equals(" ")) {
	  lastToken = token;
	}
      } else {
	if (attribute.equalsIgnoreCase(lastToken))
	  while (st.hasMoreTokens()) {
	    token = st.nextToken();
	    if (!token.equals(" ")) {
	      return token;
	    }
	  }
      }
    }
    return null;
  }
}
