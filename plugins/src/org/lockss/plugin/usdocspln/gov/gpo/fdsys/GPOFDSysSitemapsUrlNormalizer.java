/*
 * $Id$
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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import org.lockss.daemon.*;
import org.lockss.plugin.*;

public class GPOFDSysSitemapsUrlNormalizer implements UrlNormalizer {

  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    final String prefixPath = "fdsys/search/pagedetails.action?";
    final String packageIdVar = "packageId=";
    final String destination1 = "fdsys/pkg/";
    final String destination2 = "/content-detail.html";

    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    String shortBaseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    if (!url.startsWith(shortBaseUrl)) {
      return url; // No transformation
    }
    
    String prefix1 = baseUrl + prefixPath;
    String prefix2 = shortBaseUrl + ":80/" + prefixPath;  
    if (!(url.startsWith(prefix1) || url.startsWith(prefix2))) {
      return url; // No transformation
    }

    int ix = url.indexOf(packageIdVar, shortBaseUrl.length());
    if (ix < 0) {
      return url; // No transformation
    }

    ix = ix + packageIdVar.length();
    int jx = url.indexOf('&', ix);
    String packageIdVal = (jx < 0 ? url.substring(ix) : url.substring(ix, jx));
    return baseUrl + destination1 + packageIdVal + destination2;
  }

}
