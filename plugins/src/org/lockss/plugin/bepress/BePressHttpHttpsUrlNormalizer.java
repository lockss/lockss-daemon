/*
 * $Id$
 */
/*
 Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.bepress;

import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


/*
 * Adds in the http-to-https conversion then removes any https redirect argument
 * ?referer=http://scholarship.law.duke.edu/alr/vol34/iss2&httpsredir=1&
 * It's okay to remove the optional trailing amp because there will be an approriate arg
 * char before the referer - either a "?" or an "&"
 */
public class BePressHttpHttpsUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  protected static Logger log = 
      Logger.getLogger(BePressHttpHttpsUrlNormalizer.class);
  protected static final Pattern REF_ARG_PATTERN = Pattern.compile("referer=[^&]+&httpsredir=1(&)?");


  /*
  *  when redirecting from 
  *   http://scholarship.law.duke.edu/cgi/viewcontent.cgi?article=1531&context=alr
  *   bepress adds in referrer and redirection argumenst
  *  https://scholarship.law.duke.edu/cgi/viewcontent.cgi?referer=&httpsredir=1&article=1523&context=alr
  *  remove the "referrer=<something>&httpsredir=1" and "&" if at end
  */
  
    @Override 
    public String additionalNormalization(String url, ArchivalUnit au)
      throws PluginException {
        //moving from http to https - remove addition of redirection argument within url args  
   	        String returnString = REF_ARG_PATTERN.matcher(url).replaceFirst("");
        if (!returnString.equals(url)) {
          log.debug3("normalized redirected http url: " + returnString);
          return returnString;
        }
        return url;
      }

}