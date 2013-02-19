/*
 * $Id: BioOneAtyponUrlNormalizer.java,v 1.4 2013-02-19 18:53:32 janicecheng Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bioone;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;


public class BioOneAtyponUrlNormalizer implements UrlNormalizer {

  protected static final String[] endings = new String[] {
    "?cookieSet=1",
    "?prevSearch=",
  };
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    int ind;
    
    // Normalize ending
    ind = url.indexOf('?');
    if (ind >= 0) {
      for (String ending : endings) {
        url = StringUtils.chomp(url, ending);
      }
    }

    // Normalize ending
    // example: http://www.bioone.org/toc/brvo/512?seq=512
    ind = url.indexOf("?seq=");
    if (ind >= 0) {
    	url = url.substring(0, ind);
    }

    // Normalize double-slash
    ind = url.indexOf("://");
    if (ind >= 0) {
      ind = url.indexOf("//", ind + 3);
      if (ind >= 0) {
        url = url.substring(0, ind) + url.substring(ind + 1);
      }
    }
    
    return url;
  }

}
