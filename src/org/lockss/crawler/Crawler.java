/*
 * $Id: Crawler.java,v 1.7 2002-10-16 04:54:45 tal Exp $
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


public class Crawler{
  /**
   * TODO
   * 1) write state to harddrive using whatever system we come up for for the 
   * rest of LOCKSS
   * 2) check deadline and die if we run too long
   */


  private static final String imgtag = "img";
  private static final String frametag = "frame";
  private static final String linktag = "link";
  private static final String scripttag = "script";
  private static final String scripttagend = "/script";
  private static final String imgsrc = "src";
  private static final String atag = "a";
  private static final String asrc = "href";
  private static final String bodytag = "body";
  private static final String tabletag = "table";
  private static final String tdtag = "tc";
  private static final String backgroundsrc = "background";

  private static Logger logger = Logger.getLogger("Crawler");

  //  private static final int MAX_TAG_LENGTH = 2048;

  /**
   * Main method of the crawler; it loops through crawling and caching 
   * urls.
   *
   * @param rootUrls CachedUrlSet encapsulating the starting point of the
   * crawl and the rules for which urls to cache
   * 
   */
  public static void doCrawl(ArchivalUnit au, CrawlSpec spec){
    List list = createInitialList(spec);
    CachedUrlSet cus = au.getAUCachedUrlSet();
    while (!list.isEmpty()){
      String url = (String)list.get(0);
      UrlCacher uc = cus.makeUrlCacher(url);
      doOneCrawlCycle(uc, list);
    }
  }

  public static void pause(){
    try{
      Thread thread = Thread.currentThread();
      thread.sleep(2500);
    }
    catch (InterruptedException ie){
    }
  }

  /**
   * Do one cycle of the crawl: check if cu should be downloaded, download it,
   * add the urls in it to the list
   *
   * @param uc UrlCacher representing the url to be crawled
   * @param list List object to add new urls to
   */
  protected static void doOneCrawlCycle(UrlCacher uc, List list)
  {
    if (uc.shouldBeCached()){
    if (!uc.getCachedUrl().exists()) {
	try{
	  pause(); //XXX should get from plugin
	  logger.info("caching "+uc);
	  uc.cache();
	}catch (Exception e){
	  e.printStackTrace();
	  //FIXME handle errors
	  //make sure uc doesn't exist
	  //if (premanent error)
	  //  remove from list
	}
	try{
	  CachedUrl cu = uc.getCachedUrl();
	  addUrlsToList(cu, list);
	}catch (IOException ioe){
	  ioe.printStackTrace();
	  //XXX handle this (probably not too major)
	  //couldn't open the local file
	}
      }
      else{
	logger.info(uc+" exists, not fetching");
      }
    }
    list.remove(uc.getUrl());
  }
  /**
   * Method to generate a List of urls to start a crawl at from a 
   * CachedUrlSet.
   *
   * @param spec CrawlSpec from which the starting points with be fetched.
   * @returns A modifiable list of the urls (in string form)
   */
  protected static List createInitialList (CrawlSpec spec){
    if (spec == null) {
      return Collections.EMPTY_LIST;
    } else {
      return new LinkedList(spec.getStartingUrls());
    }
  }

  /**
   * Method which will parse the html file represented by cu and add all 
   * urls on it to list
   *
   *@param cu object representing a html file in the local file system
   *@param list list to which all the urs in cu should be added
   */
  protected static void addUrlsToList(CachedUrl cu, List list)
      throws IOException{
    if (cu == null) {
      return;
    }
    if (cu != null && shouldExtractLinksFromCachedUrl(cu)){
      String cuStr = cu.getUrl();
      if (cuStr == null){
	return;
      }
      //check to see if we should even try to parse the file
//       if (cuStr.indexOf("html") < 0 && 
// 	  cuStr.indexOf("htm") < 0){ //XXX hack for now
// 	return;
//       }
      
      InputStream is = cu.openForReading();
      if (is != null){
	Reader reader = new InputStreamReader(cu.openForReading());
	URL srcUrl = new URL(cuStr);
	String nextUrl = ExtractNextLink(reader, srcUrl);
	while (nextUrl != null){
	  if (!list.contains(nextUrl)){
	    list.add(nextUrl);
	  }
	  nextUrl = ExtractNextLink(reader, srcUrl);
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
  protected static boolean shouldExtractLinksFromCachedUrl(CachedUrl cu){
    Properties props = cu.getProperties();
    if (props == null){
      return false;
    }
    String contentType = props.getProperty("content-type");
    return (contentType != null && contentType.equalsIgnoreCase("text/html"));
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
      throws IOException, MalformedURLException{
    if (reader == null){
      return null;
    }

    boolean inscript = false; //FIXME or I will break when we look at scripts
    String nextLink = null;
    int c = 0;
    StringBuffer lineBuf = new StringBuffer();

    while(nextLink == null && c >=0){
      //skip to the next tag
      do{
	c = reader.read();
      }while (c >= 0 && c != '<');
      
      if (c == '<') {
	int pos = 0;
	c = reader.read();
	while (c >= 0 && c != '>') {
	  if(pos==2 && c=='-' && lineBuf.charAt(0)=='!' 
	     && lineBuf.charAt(1)=='-') {
	    // comment
	    pos = 0;
	    int lc1 = 0;
	    int lc2 = 0;
	    while((c = reader.read()) >= 0 && (c != '>' || lc1 != '-' || lc2 != '-')) {
	      lc1 = lc2;
	      lc2 = c;
	    }
	    break;
	  }
	  lineBuf.append((char)c);
	  pos++;
	  c = reader.read();
	}
	if(inscript) {
	  //FIXME when you deal with the script problems
	  //	  if(lookingAt(lineBuf, 0, pos, scripttagend)) {
	  inscript = false;
	  //}
	} else if (lineBuf.length() >= 5){ //see if the lineBuf has a link tag
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
  throws MalformedURLException
  {
    String returnStr = null;
    String key = null;
    String tag = null;
    switch (link.charAt(0)) {
    case 'a': //<a href=http://www.yahoo.com>
    case 'A':
      key = asrc;
      break;
    case 'f': //<frame src=frame1.html>
    case 'F':
      key = imgsrc;
      break;
    case 'i': //<img src=image.gif>
    case 'I':
      key = imgsrc;
      break;
    case 'l': //<link href=blah.css>
    case 'L':
      key = asrc;
      break;
    case 'b': //<body backgroung=background.gif>
    case 'B':
      key = backgroundsrc;
      break;
    case 's': //<script src=blah.js>
    case 'S':
      key = imgsrc;
      break;
    case 't': //<tc background=back.gif> or <table background=back.gif>
    case 'T':
      key = backgroundsrc;
      break;
    default:
      return null;
    }
    int linkIdx = StringUtil.getIndexIgnoringCase(link.toString(), key);
    if (linkIdx < 0){
      return null;
    }
    int idx = linkIdx + key.length(); 
    while (idx < link.length() &&
	   (link.charAt(idx) == '"' ||
	    link.charAt(idx) == ' ' ||
	    link.charAt(idx) == '=')){
      idx++;
    }
    if (idx >= link.length()){ //bad key, has no value
      return null;
    }

    returnStr = link.substring(idx);
    returnStr = StringUtil.trimAfterChars(returnStr, " #\"");
    URL retUrl = new URL(srcUrl, returnStr);
    return retUrl.toString();
  } 

}
