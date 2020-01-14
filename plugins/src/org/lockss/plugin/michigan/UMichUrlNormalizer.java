/*
 * $Id$
 */

/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.michigan;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class UMichUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  
  protected static final Logger log = Logger.getLogger(UMichUrlNormalizer.class);
  
  protected static final String LOCALE_PARAM = "\\?locale=en";
  protected static final String UTF_PARAM = "\\?utf8=.*";
  /* not used as patterns, no need to escape questionmark and dot*/
  private static final String IMAGE_SERVICE  = "/image-service/";
  private static final String JPEG_ARGUMENT  = ".jpg?";
    
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au)
		  throws PluginException {
//	  // no need to check first 
//	  url = url.replaceFirst(LOCALE_PARAM, "");
//	  url = url.replaceFirst(UTF_PARAM, "");
	  // remove changing argument on end of images
          if (url.matches("/image-service/.*\\.(jpe?g|png|json)\\?")) {
            url = url.replaceFirst("\\?.*", "");
          }
	  return(url);
  }



}
