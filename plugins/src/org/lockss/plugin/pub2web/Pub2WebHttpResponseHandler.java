/*
 * $Id:$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pub2web;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/*
 * a 500 error gets served when the citation download file isn't available. 
 * don't let this be fatal. Just continue. 
 */
public class Pub2WebHttpResponseHandler implements CacheResultHandler {
    
  // example: http://digital-library.theiet.org/content/journals/10.1049/el_19870726/cite/bibtex
  // child can override through getter to extend or change the pattern
  protected static final Pattern DEFAULT_NON_FATAL_500_PAT = 
      Pattern.compile("/cite/(bibtex|endnote|plaintext|refworks)");
  
  private static final Logger logger = Logger.getLogger(Pub2WebHttpResponseHandler.class);

  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to Pub2WebHttpResponseHandler.init()");
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    logger.debug2(url);
    switch (responseCode) {
      case 500:
        logger.debug2("500 - pattern is " + getNonFatal500Pattern().toString());
        Matcher smat = getNonFatal500Pattern().matcher(url);
        if (smat.find()) {
          return new CacheException.NoRetryDeadLinkException("500 Internal Server Error (non-fatal)");
        } else {
          return new CacheException.RetrySameUrlException("500 Internal Server Error");
        }
      default:
        logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
        throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
    }
  }
  
  public CacheException handleResult(ArchivalUnit au,
		  String url,
		  Exception ex) {
	  logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
	  throw new UnsupportedOperationException();
  }

  // Use a getter so that this can be overridden by a child plugin
  protected Pattern getNonFatal500Pattern() {    
    return DEFAULT_NON_FATAL_500_PAT;   
  }
  
}