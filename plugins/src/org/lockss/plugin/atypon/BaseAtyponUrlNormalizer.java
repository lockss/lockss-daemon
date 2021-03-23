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

package org.lockss.plugin.atypon;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;


/*
 * Removes cookiset and handles cleanup of the citation download URLs
 * probably sufficient as is for most Atypon children 
 */
//public class BaseAtyponUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
public class BaseAtyponUrlNormalizer implements UrlNormalizer {
  protected static Logger log = 
      Logger.getLogger(BaseAtyponUrlNormalizer.class);
  protected static final String SUFFIX = "?cookieSet=1";
  protected static final String BEAN_SUFFIX = "?queryID=%24%7BresultBean.queryID%7D";
  // on TOC used as anchor...eg http://www.euppublishing.com/toc/ajicl/23/1?widget=aboutthisjournal
  protected static final String WIDGET_SUFFIX = "?widget=";
  // these next two show in variants in the updated T&F skin 9/15/16
  // doi/full/foo?scroll=top&needAccess or doi/pdf/foo?needAccess=true#BIGUGLYHASH
  protected static final String ACCESS_SUFFIX = "?needAccess=";  
  protected static final String SCROLL_SUFFIX = "?scroll=";  
  // and part of the new Volume issue toc navigator
  protected static final String VIEW_SUFFIX = "?view=tocList";  
  protected static final Pattern HASH_ARG_PATTERN = Pattern.compile("(\\.css|js)\\?\\d+$");
  protected static final String  NO_ARG_REPLACEMENT = "";

  /* 
   * CITATION DOWNLOAD URL CLEANUP
   *  for those Atypon children that use form download for citation information, the resulting URLs
   *  will be automatically normalized to match what the article iterator expects
   */
  protected static final String CITATION_DOWNLOAD_URL = "action/downloadCitation"; // how we identify these URLs 
  private static final String DOI_ARG = "doi";
  private static final String FORMAT_ARG = "format";
  private static final String INCLUDE_ARG = "include";

  /*
   * JAVASCRIPT show(full)Image URL CLEANUP
   * For those Atypon children that use a BASE/action/show(Full)Image?id=foo&doi=blah URL to 
   *   display additional image sizes, the arguments can come in any order. We generate
   *   some of these URLs with a link extractor and we don't want to get duplicate URLs
   *   so normalize the order to be one of:
   *    BASE/action/showPopup?citid=citart1&id=F1&doi=10.2466%2F05.08.IT.3.3
   *    BASE/action/showFullPopup?id=i1520-0469-66-1-187-f01&doi=10.1175%2F2008JAS2765.1
   *   
   */
  protected static final String ACTION_SHOWPOP = "/action/showPopup?citid=citart1&";
  protected static final String ACTION_SHOWFULL = "/action/showFullPopup?";
  private static final String CITID_ARG = "citid";
  private static final String ID_ARG = "id";


  public String normalizeUrl(String url, ArchivalUnit au)
      throws PluginException {

    // This originated in T&F, BioOne and BloomsburyQ but useful to avoid malformed URLs
    // Turn any mid-URL double slashes in to single slashes
    int ind = url.indexOf("://");
    if (ind >= 0) {
      ind = url.indexOf("//", ind + 3);
      if (ind >= 0) {
        url = url.substring(0, ind) + url.substring(ind + 1);
      }
    }

    // There are several cases where we need to specify order of arguments
    // This is because we generate the URLS with the link extractor and need to 
    // makes sure we don't get redundant URLs due to "found" links that are
    // the same but for ordering.
    // This happens for citation download and image viewer urls

    // Only do this normalization for correctly formatted form downloaded citation URLs
    int qmark = url.indexOf("?");
    boolean isCit = false;
    boolean isShowIm = false;
    boolean isShowFull = false;
    // Only proceed if this is a case we want - capture "url.contains" results
    if (((isCit = url.contains(CITATION_DOWNLOAD_URL)) || (isShowIm = url.contains(ACTION_SHOWPOP)) || 
        (isShowFull = url.contains(ACTION_SHOWFULL))) && url.contains(DOI_ARG) && (qmark >= 0)) {

      // get a map of key-value pairs for the query args on the url
      Map<String, String> argMap = getQueryArgsFromUrl(url, qmark);
      // limit the map access by using var to hold value
      String theVal = null;

      // Now handle each case appropriately  - first citation download
      if (isCit) {
        StringBuilder new_url = new StringBuilder(url.substring(0,qmark));
        if ( (theVal= argMap.get(DOI_ARG)) != null) {
          //DOI's themselves might have "/" and this must be uri encoded
          new_url.append("?" + DOI_ARG + "=" + StringUtil.replaceString(theVal, "/", "%2F"));
        } 
        // add on format arg
        if ( (theVal = argMap.get(FORMAT_ARG)) == null) {
          // this does happen. make it ris
          log.debug3("no format on this citation download url");
          theVal = "ris";
        }
        new_url.append("&" + FORMAT_ARG + "=" + theVal);
        // add on include arg
        if ( (theVal = argMap.get(INCLUDE_ARG)) == null) {
          // this could happen - make it "cit"
          log.debug3("no include on this citation download url");
          theVal = "cit";
        } 
        new_url.append("&" + INCLUDE_ARG + "=" + theVal);
        log.debug3("normalized citation download url: " + new_url);
        return new_url.toString();
      } else if (isShowIm || isShowFull) {
        StringBuilder new_url = new StringBuilder(url.substring(0,qmark));
        boolean firstarg = true;
        // this only occurs for showImage, not showFullImage
        if ( (theVal = argMap.get(CITID_ARG)) != null ) {
          // this always seems to be the same, but use what was collected in case
          new_url.append("?" + CITID_ARG + "=" + theVal);
          firstarg = false;
        } 
        // this will be the second arg for showImage and first for showFullImage
        if ( (theVal = argMap.get(ID_ARG)) != null) {
          new_url.append((firstarg ? "?" : "&") + ID_ARG + "=" + theVal); 
          firstarg = false;
        } 
        if ( (theVal = argMap.get(DOI_ARG)) != null) {
          //firstarg can only be true here with a bad url, but  test ticked it 
          //DOI's themselves might have "/" and this must be uri encoded
          new_url.append((firstarg ? "?" : "&") + DOI_ARG + "=" + StringUtil.replaceString(theVal, "/", "%2F"));
        } 
        log.debug3("normalized show(Full)Image download url: " + new_url);
        return new_url.toString();
      }      
    }

    // some CSS/JS files have a hash argument that isn't needed
    String returnString = HASH_ARG_PATTERN.matcher(url).replaceFirst("$1");
    if (!returnString.equals(url)) {    
      // if we were a normalized css/js, then we're done - return
      log.debug3("normalized css url: " + returnString);
      return returnString;
    }

    // just remove any undesired suffixes
    // if the suffix doesn't exist the url doesn't change, no sense wasting
    // cycles checking if it exists before trying to remove
    if (qmark >= 0) {
      url = StringUtils.substringBeforeLast(url, BEAN_SUFFIX);
      url = StringUtils.substringBeforeLast(url, SUFFIX);
      url = StringUtils.substringBeforeLast(url, WIDGET_SUFFIX);
      url = StringUtils.substringBeforeLast(url, ACCESS_SUFFIX);
      url = StringUtils.substringBeforeLast(url, SCROLL_SUFFIX);
      url = StringUtils.substringBeforeLast(url, VIEW_SUFFIX);
    }
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