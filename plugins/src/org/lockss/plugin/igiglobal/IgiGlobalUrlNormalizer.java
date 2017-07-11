/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.igiglobal;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * Trims   http://www.igi-global.com/gateway/contentowned/articles.aspx?titleid=55606&accesstype=infosci
 * to this http://www.igi-global.com/gateway/contentowned/articles.aspx?titleid=55606 to avoid duplicate pages
 * also works with ?v=.*
 * 
 * This might be temporary, but the change from http to https also causes the addition of a repeat of the 
 * issn & volume args on the start_url
 * https://www.igi-global.com/lockss/journal-issues.aspx?issn=2156-1796&volume=5&issn=2156-1796&volume=5

 * as well as arg patterns on a few others:
 * https://www.igi-global.com/pdf.aspx?tid=179548&ptid=132074&ctid=15&t=Masthead&tid=179548&ptid=132074&ctid=15&t=Masthead
 * became:
 * https://www.igi-global.com/pdf.aspx?tid=179548&ptid=132074&ctid=15&t=Masthead&tid=179548&ptid=132074&ctid=15&t=Masthead
 *  * which breaks support for the transition. Normalize off the extra args when they're a duplicate
 */

public class IgiGlobalUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  
  private static Logger log = Logger.getLogger(IgiGlobalUrlNormalizer.class);

  protected static final String SUFFIX = "&accesstype=";
  protected static final String SUFFIX1 = "?v=";
  //generalize 
  //.aspx?x=foo&x=foo
  //.aspx?x=foo&y=blah&x=foo&y=blah
  //.aspx?x=foo&y=bla&z=baz&x=foo&y=blah&z=baz
  //.aspx?(1st set)&(repeat first set)
  //([^=]+=[^&]+(&[^=]+=[^&]+)*)
  private  static final Pattern REPEAT_ARG_PATTERN =
      Pattern.compile("(\\.aspx\\?([^=]+=[^&]+(?:&[^=]+=[^&]+)*))&\\2", Pattern.CASE_INSENSITIVE);
     // Pattern.compile("(\\.aspx\\?([^=]+=[^&]+)(&volume=[^&]+))(&\\2\\3)", Pattern.CASE_INSENSITIVE);

  @Override
  public String additionalNormalization(String url,
                             ArchivalUnit au)
      throws PluginException {
    
    //moving from htttp to https - duplicate arguments on start_url - remove them
    // this will continue to work even once igi ceases to do this
    String returnString = REPEAT_ARG_PATTERN.matcher(url).replaceFirst("$1");
    if (!returnString.equals(url)) {    
      log.debug3("normalized redirected http start url: " + returnString);      
      return returnString;
    }

    url = StringUtils.substringBeforeLast(url, SUFFIX);
    url = StringUtils.substringBeforeLast(url, SUFFIX1);
    return url;
  }

}
