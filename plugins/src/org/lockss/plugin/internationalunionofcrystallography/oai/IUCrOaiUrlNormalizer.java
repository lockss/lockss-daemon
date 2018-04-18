/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.internationalunionofcrystallography.oai;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;

/*
 * 
 * change  http://
 * to this http://
 * 
 * If and only if the AU is iucrdata
 */

public class IUCrOaiUrlNormalizer implements UrlNormalizer {
  
  private static final String TARGET = "http://journals.iucr.org/";
  private static final String IUCRDATA = "iucrdata";
  
  /*  Note: Normalizes iucrdata urls with journals.iucr.org
   * 
   */
  
  public String normalizeUrl(String url, ArchivalUnit au)
      throws PluginException {
    
    String oaiSet = au.getConfiguration().get(BaseOaiPmhCrawlSeed.KEY_AU_OAI_SET);
    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    
    if (oaiSet.equalsIgnoreCase(IUCRDATA) && url.startsWith(TARGET)) {
      url = url.replace(TARGET, baseUrl);
    }
    return url;
  }

}
