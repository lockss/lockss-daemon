/*
 * $Id$
 */
/*
 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, destroy, sublicense, and/or sell
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
package org.lockss.plugin.palgrave;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;


/*
 * Removes cookiset and handles cleanup of the citation download URLs
 * probably sufficient as is for most Atypon children 
 */
public class PalgraveBookUrlNormalizer implements UrlNormalizer {
  protected static Logger log = 
      Logger.getLogger(PalgraveBookUrlNormalizer.class);
  /*
   * HTML page viewer cleanup
   * Some palgrave books have multiple (but seemingly identical) html pages
   * that show the chapter within a framed pdf viewer - 
   * examples:
   * BASE_URL/pc/Foo2013/browse/inside/chapter/ISBN#/ISBN#.Chapter#.html?page=1&chapterDoi=ISBN#.Chapter#
   * possible suffixes (after the '?'):
   *    page=X&chapterDoi=ISBN.Chapter#
   *    chapterDoi=ISBN.Chapter#&focus=pdf-viewer
   *    chapterDoi=ISBN.Chapter#        <= Normalize to this
   */
  private static final String QMARK = "?";
  private static final String HTAG = "#";  
  private static final String EQUALS = "=";      

  private static final String CHAPTER_ARG = "chapterDoi";
  private static final String SHOW_PAGE = "page=";
  private static final String HASH_PAGE = "#page=";      
  private static final String FOCUS_PDFVIEWER = "focus=pdf-viewer";

  public String normalizeUrl(String url, ArchivalUnit au)
  throws PluginException {

    // There are several cases where the publisher provides multiple urls
    // that display seemingly identical pages (the arguments past the '?', mostly 
    // image viewer urls). Want to consolidate them to a single, normalized url 

    // Only do this normalization for correctly formatted form html image viewer pages
    // that end in the form "&focus=pdf-viewer"
    int qmark = url.indexOf(QMARK);
    final String base_url = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());

    log.debug3("orig url=" +url);
    if (qmark < 0){    // nothing to normalize
      return url;
    }
    // if not in the base url, don't bother trying to normalize
    if (!url.startsWith(base_url)) {
      return url;
    }
    boolean isShowPage = url.contains(SHOW_PAGE);
    boolean isPdfViewer = url.contains(FOCUS_PDFVIEWER);

    // if the url has '?' and either "page=X" or "focus=pdfviewer"...
    if ((qmark >= 0) && (isShowPage || isPdfViewer)) {

      // get a map of key-value pairs for the query args on the url
      Map<String, String> argMap = getQueryArgsFromUrl(url, qmark);
      // limit the map access by using var to hold value
      String theVal = null;

      // Now handle each case appropriately  - start with "chapterDoi"
      StringBuilder new_url = new StringBuilder(url.substring(0,qmark));
      // remove the "page=X" and "focus=pdf-viewer" part by not putting it back
      if ( (theVal= argMap.get(CHAPTER_ARG)) != null) {
        // but must actively remove "#page=X", which is not a query arg
        if (theVal.contains(HASH_PAGE)){
          int ind = theVal.lastIndexOf(HTAG);
          theVal = theVal.substring(0, ind);
        }
        new_url.append(QMARK + CHAPTER_ARG + EQUALS + theVal);
      } 
      log.debug3("returning normalized palgrave url: " + new_url);

      return new_url.toString();

    }
    log.debug("returning palgrave url: " + url);

    return url;
  }


  /*
   * This will ultimately become a util function
   * Given the String url and the index position of the qmark return 
   * a map of the query string key-value pairs
   * this method take in qmark because I already happen to have it
   */
  private static Map<String, String> getQueryArgsFromUrl(String url, int qmark) {
    //First pick up the args in to a map
    Map<String, String> argMap = new HashMap<String, String>();
    String argKey;
    String argVal;
    int splitIndex;

    log.debug3("orig download url: " + url);
    String query_string = url.substring(qmark+1);
    log.debug3("url args in: " + query_string);

    for (String oneArg: query_string.split("&")) {
      log.debug3("argument: " + oneArg);
      splitIndex = oneArg.indexOf("=");
      if (splitIndex > 0) {
        // guard against malformed query - don't create entry
        argKey = oneArg.substring(0, (splitIndex > 0 ? splitIndex : 0));
        argVal = oneArg.substring(splitIndex+1);
        log.debug3("map entry: " + argKey + "  " + argVal);
        argMap.put(argKey,  argVal);
      }
    }
    return argMap;
  }
}