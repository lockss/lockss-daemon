/*
 * $Id$
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

package org.lockss.plugin.europeanmathematicalsociety;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class EuropeanMathematicalSocietyUrlNormalizer implements UrlNormalizer {
  
  protected static final Logger log = Logger.getLogger(EuropeanMathematicalSocietyUrlNormalizer.class);
  protected static final String SEARCH_PARAM = "&srch=series|";
  protected static final String PATTERN_STR = SEARCH_PARAM + ".+$";
  
  // XXX NOTE: may want normalize 
  //  http://www.ems-ph.org/books/show_pdf.php?proj_nr=NN&vol=1 to 
  //  http://www.ems-ph.org/books/show_pdf.php?proj_nr=NN
  // Cannot tell now, will need more experience from crawling AUs
  
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    if (url.contains(SEARCH_PARAM)) {
      url = url.replaceFirst(PATTERN_STR, "");
    }
    return(url);
  }
}
