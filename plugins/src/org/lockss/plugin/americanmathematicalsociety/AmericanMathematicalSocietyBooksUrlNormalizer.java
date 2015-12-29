/*
 * $Id: SciELOUrlNormalizer.java 45355 2015-12-22 03:02:12Z etenbrink $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.americanmathematicalsociety;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


public class AmericanMathematicalSocietyBooksUrlNormalizer implements UrlNormalizer {
  
  protected static Logger log = Logger.getLogger(AmericanMathematicalSocietyBooksUrlNormalizer.class);
  
  protected static final String TARGET_URL1 = "www.ams.org/books/"; // how we identify these URLs
  protected static final String TARGET_URL2 = ".pdf";
//http://www.ams.org/books/conm/612/12221/conm612-12221.pdf
  protected static final Pattern PDF_PAT = Pattern.compile(
      "(https?://[^/]+/books/)([^/]+)/([0-9]{2,5})/([0-9]{6})/\\1\\2-\\3\\.pdf");
  
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    
    if (url.contains(TARGET_URL1) &&
        url.endsWith(TARGET_URL2)) {
      log.debug3(url);
      Matcher pdf_mat = PDF_PAT.matcher(url);
      if (pdf_mat.matches()) {
        url = pdf_mat.group(1) + "/" + pdf_mat.group(2) + "/" + pdf_mat.group(3);
      }
      log.debug3(url);
    }
    return url;
  }
  
}
