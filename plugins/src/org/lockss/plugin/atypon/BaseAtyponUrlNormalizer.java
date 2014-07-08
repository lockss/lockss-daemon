/*
 * $Id: BaseAtyponUrlNormalizer.java,v 1.7 2014-07-08 17:30:28 alexandraohlson Exp $
 */
/*
 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.atypon;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;


/*
 * Removes cookiset and handles cleanup of the citation download URLs
 * probably sufficient as is for most Atypon children 
 */
public class BaseAtyponUrlNormalizer implements UrlNormalizer {
  protected static Logger log = 
      Logger.getLogger("BaseAtyponUrlNormalizer");
  protected static final String SUFFIX = "?cookieSet=1";
  protected static final String BEAN_SUFFIX = "?queryID=%24%7BresultBean.queryID%7D";
  protected static final Pattern HASH_ARG_PATTERN = Pattern.compile("(\\.css|js)\\?\\d+$");
  protected static final String  NO_ARG_REPLACEMENT = "";
  
  /* 
   * CITATION DOWNLOAD URL CLEANUP
   *  for those Atypon children that use form download for citation information, the resulting URLs
   *  will be automatically normalized to match what the article iterator expects
   */
  protected static final String CITATION_DOWNLOAD_URL = "action/downloadCitation"; // how we identify these URLs 
  private final String DOI_ARG = "doi";
  private final String FORMAT_ARG = "format";
  private final String INCLUDE_ARG = "include";

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

    // Only do this normalization for correctly formatted form downloaded citation URLs
    int qmark = url.indexOf("?");
    if (url.contains(CITATION_DOWNLOAD_URL) && (url.contains(DOI_ARG + "=")) && (qmark >= 0)) {
       Map<String, String> argMap = new HashMap<String, String>();
       String oneArg;
       String argKey;
       String argVal;
       int splitIndex;
              
      log.debug3("orig citation download url: " + url);
      String cit_arg_string = url.substring(qmark+1);
      log.debug3("citation url args in: " + cit_arg_string);

      // put all the args in to a map
      String[] cit_arg_list = cit_arg_string.split("&");
      int num_args = cit_arg_list.length;
      for (int i = 0; i < num_args; i++) {
        oneArg = cit_arg_list[i];
        log.debug3("argument: " + oneArg);
        splitIndex = oneArg.indexOf("=");
        argKey = oneArg.substring(0, splitIndex);
        argVal = oneArg.substring(splitIndex+1);
        log.debug3("map entry: " + argKey + "  " + argVal);
        argMap.put(argKey,  argVal);
      }

       StringBuilder new_url = new StringBuilder(url.substring(0,qmark));
      // add on DOI arg - this must be here
      if (argMap.containsKey(DOI_ARG)) {
        //DOI's themselves might have "/" and this must be uri encoded
        new_url.append("?" + DOI_ARG + "=" + StringUtil.replaceString(argMap.get(DOI_ARG), "/", "%2F"));
      } 
      // add on format arg
      if (argMap.containsKey(FORMAT_ARG)) {
        new_url.append("&" + FORMAT_ARG + "=" + argMap.get(FORMAT_ARG));
      } else {
        // this could happen - make it "ris"
        log.debug3("no format on this citation download url");
        new_url.append("&" + FORMAT_ARG + "=ris");
      }
      // add on include arg
      if (argMap.containsKey(INCLUDE_ARG)) {
        new_url.append("&" + INCLUDE_ARG + "=" + argMap.get(INCLUDE_ARG));
      } else {
        // this could happen - make it "cit"
        log.debug3("no include on this citation download url");
        new_url.append("&" + INCLUDE_ARG + "=cit");
      }
      log.debug3("normalized citation download url: " + new_url);
      return new_url.toString();
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
    }
    return url;
  }
}