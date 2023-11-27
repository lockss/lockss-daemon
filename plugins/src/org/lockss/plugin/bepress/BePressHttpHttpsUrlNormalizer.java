/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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