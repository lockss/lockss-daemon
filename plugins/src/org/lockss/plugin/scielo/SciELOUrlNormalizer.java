/*
 * $Id: SciELOUrlNormalizer.java 44086 2015-09-15 00:56:52Z etenbrink $
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

package org.lockss.plugin.scielo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


public class SciELOUrlNormalizer implements UrlNormalizer {
  protected static Logger log = Logger.getLogger(SciELOUrlNormalizer.class);
  
  protected static final String TARGET_URL1 = "&pdf_path="; // how we identify these URLs
  protected static final Pattern PDF_PAT = Pattern.compile("(https?://[^/]+/).*&pdf_path=([^&]+)&");
  
  protected static final String TARGET_URL2 = "/scielo.php?"; // how we identify these URLs
  protected static final String SCRIPT_PARAM = "script";
  protected static final String PID_PARAM = "pid";
  protected static final String LANG_PARAM = "lang";
  protected static final String LNG_PARAM = "lng";
  protected static final String NRM_PARAM = "nrm";
  protected static final String TLNG_PARAM = "tlng";
  protected static final String ORIGINALLANG_PARAM = "ORIGINALLANG";
  protected static final String NRM_DEFAULT_VAL = "iso";
  protected static final String TLNG_DEFAULT_VAL = "en";
  
  protected static final String TARGET_URL3 = "scieloOrg/php/articleXML.php?"; // how we identify these URLs
  
  protected static final String[] KNOWN_PARAMS =
    {SCRIPT_PARAM, PID_PARAM, LANG_PARAM, LNG_PARAM, NRM_PARAM, TLNG_PARAM, ORIGINALLANG_PARAM};
  
  // protected static final Pattern HASH_ARG_PATTERN = Pattern.compile("(\\.css|js)\\?\\d+$");
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    int qmark = url.indexOf("?");
    
    // Some pdf URLs redirect to other URLs
    // The URLs look like this:
    // <base_url>/readcube/epdf.php?doi=10.1590/S0102-67202014000100001&pid=S0102-67202014000100001
    //      &pdf_path=abcd/v27n1/0102-6720-abcd-27-01-00001.pdf&lang=en
    // and we make them look like this:
    // <base_url>/pdf/abcd/v27n1/0102-6720-abcd-27-01-00001.pdf
    // Trim any &tlng=en param (and any trailing params)
    if (url.contains(TARGET_URL1)) {
      log.debug(url);
      Matcher pdf_mat = PDF_PAT.matcher(url);
      if (pdf_mat.find()) {
        url = pdf_mat.group(1) + "pdf/" + pdf_mat.group(2);
      }
      log.debug(url);
    }
    else if (url.contains(TARGET_URL2)) {
      log.debug(url);
      // There are cases where we need to specify order of arguments
      // This is because we generate the URLS with the link extractor and need to 
      // makes sure we don't get redundant URLs due to "found" links that are
      // the same but for ordering.
      
      // Only do this normalization for correctly formatted URLs
      // Only proceed if this is a case we want - capture "url.contains" results
      if (qmark >= 0) {
        
        StringBuilder new_url = new StringBuilder(url.substring(0, qmark));
        
        // get a map of key-value pairs for the query args on the url
        Map<String, String> argMap = getQueryArgsFromUrl(url, qmark);
        
        // limit the map access by using var to hold value
        String theVal = null;
        
        if ( (theVal= argMap.get(SCRIPT_PARAM)) != null) {
          new_url.append("?" + SCRIPT_PARAM + "=" + theVal);
        } else {
          return url;
        }
        
        if ( (theVal= argMap.get(PID_PARAM)) != null) {
          new_url.append("&" + PID_PARAM + "=" + theVal);
        }
        
        // add on language param
        if ( (theVal = argMap.get(LNG_PARAM)) == null) {
          // this does happen. make it "en"
          log.debug3("no LNG_PARAM on this url:" + url);
          theVal = "en";
        }
        new_url.append("&" + LNG_PARAM + "=" + theVal);
        
        // add on nrm param, if not =iso
        if ((theVal = argMap.get(NRM_PARAM)) == null) {
          // this does happen. make it "iso"
          log.debug3("no NRM_PARAM on this url:" + url);
          theVal = NRM_DEFAULT_VAL;
        }
        if (!theVal.equals(NRM_DEFAULT_VAL)) {
          new_url.append("&" + LNG_PARAM + "=" + theVal);
        }
        
        // add on tlng param, if not =en
        if ((theVal = argMap.get(TLNG_PARAM)) == null) {
          // this does happen. make it "en"
          log.debug3("no TLNG_PARAM on this url:" + url);
          theVal = TLNG_DEFAULT_VAL;
        }
        if (!theVal.equals(TLNG_DEFAULT_VAL)) {
          new_url.append("&" + TLNG_PARAM + "=" + theVal);
        }
        
        url = new_url.toString();
      }
      log.debug(url);
    } else if (url.contains(TARGET_URL3)) {
      log.debug(url);
      if (qmark >= 0) {
        
        StringBuilder new_url = new StringBuilder(url.substring(0, qmark));
        
        // get a map of key-value pairs for the query args on the url
        Map<String, String> argMap = getQueryArgsFromUrl(url, qmark);
        
        String theVal = null;
        
        if ( (theVal= argMap.get(PID_PARAM)) != null) {
          new_url.append("?" + PID_PARAM + "=" + theVal);
        } else {
          throw new PluginException(PID_PARAM + " param is null");
        }
        // force LANG_PARAM =en
        new_url.append("&" + LANG_PARAM + "=en");
        url = new_url.toString();
      }
      log.debug(url);
    }
    else {
      log.debug("unnormalized url: " + url);
    }
    return url;
  }
  
  /*
   * This will ultimately become a util function
   * Given the String url and the index position of the qmark return 
   * a map of the query string key-value pairs
   * this method takes in optional qmark
   */
  private static Map<String, String> getQueryArgsFromUrl(String url, int qmark) {
    //First pick up the args in to a map
    Map<String, String> argMap = new HashMap<String, String>();
    String argKey;
    String argVal;
    int splitIndex;
    
    String query_string = url.substring(qmark+1);
    
    for (String oneArg: query_string.split("&")) {
      log.debug3("argument: " + oneArg);
      splitIndex = oneArg.indexOf("=");
      if (splitIndex > 0) {
        // guard against malformed query - don't create entry
        argKey = oneArg.substring(0, (splitIndex > 0 ? splitIndex : 0));
        argVal = oneArg.substring(splitIndex+1);
        log.debug3("map entry: " + argKey + "  " + argVal);
        if (!Arrays.asList(KNOWN_PARAMS).contains(argKey)) {
          log.warning(oneArg + " not in KNOWN_PARAMS");
        }
        argMap.put(argKey,  argVal);
      } else {
        log.warning(oneArg + " is malformed");
      }
    }
    return argMap;
  }
  
}
