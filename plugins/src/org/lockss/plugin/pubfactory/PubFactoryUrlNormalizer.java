/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pubfactory;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.UrlNormalizer;

public class PubFactoryUrlNormalizer implements UrlNormalizer {

  /**
   *  Clean off some unnecessary one-time arguments from citation urls
   */
  
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {


    /* For AMetSoc (at least), they seem to use .xml for PDFs, in addition to having a .pdf version of the same url.
    * e.g.
    * https://journals.ametsoc.org/downloadpdf/journals/mwre/64/7/1520-0493_1936_64_240b_srodj_2_0_co_2.pdf
    * https://journals.ametsoc.org/downloadpdf/journals/mwre/64/7/1520-0493_1936_64_240b_srodj_2_0_co_2.xml
    * AMetSoc also uses .xml for their html files but it does not appear that they also have the .html version
    *   for now, nothing needs to be done for that.
    */
    if (url.contains("/downloadpdf/") && url.endsWith(".xml")) {
      url = url.replace(".xml",".pdf");
    }

    // remove the one-time argument on citation download
    if (url.contains("cite:exportcitation") && url.contains("t:state:client=")) {
      // from the character before it (probably &) all the way to the end
      url = url.replaceFirst(".t:state:client=.+","");
    }
    return url;
  }

}
