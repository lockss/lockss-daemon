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

package org.lockss.plugin.atypon;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/*
 * The newest skin of Atypon has problem where it serves a 500 when it seems to mean 404. 
 * This is particularly a problem for the seemingly programmatically generated links to various
 * formats of tables (action/downloadTable?doi=) but also shows up occasionally for figures
 * (action/downloadFigures? | downloadPdfFig). 
 * In some cases the action/showPopup?citid=citart1 which displays footnote details
 * So far seen in Taylor & Francis, Edocrine and Mark Allen Group and Sage on Atypon
 */
public class BaseAtyponHttpResponseHandler implements CacheResultHandler {
    
  // default for Atypon is 
  //     action/downloadTable
  //     action/downloadFigures
  //     action/downloadPdfFig
  //     action/showPopup
  //
  // child can override through getter to extend or change the pattern
  protected static final Pattern DEFAULT_NON_FATAL_500_PAT = 
      Pattern.compile("action/(download(Table|Figures|PdfFig)|show(Popup|Cit))");

  // ASCE has suppl_data links that are returning 403 but should be 404
  //http://ascelibrary.org/doi/suppl/10.1061/%28ASCE%29IR.1943-4774.0000983/suppl_file/Supplemental_Data_IR.1943-4774.0000983_Guerra1
  protected static final Pattern DEFAULT_NON_FATAL_403_PAT = 
      Pattern.compile("doi/suppl/.*/suppl_file/");
  
  private static final Logger logger = Logger.getLogger(BaseAtyponHttpResponseHandler.class);

  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to BaseAtyponHttpResponseHandler.init()");
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
      case 403: 
        logger.debug2("403 - pattern is " + getNonFatal403Pattern().toString());
        Matcher fmat = getNonFatal403Pattern().matcher(url);
        if (fmat.find()) {
          return new CacheException.NoRetryDeadLinkException("403 Foridden (non-fatal)");
        } else {
          return new CacheException.RetrySameUrlException("403 Forbidden");
        }
      default:
        logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
        throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
    }
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception ex) {
    if (ex instanceof ContentValidationException) {
      logger.debug3("Warning - not storing file " + url);
      // no store cache exception and continue
      return new org.lockss.util.urlconn.CacheException.NoStoreWarningOnly("ContentValidationException" + url);
    }
    
    logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
  }
  
  // Use a getter so that this can be overridden by a child plugin
  protected Pattern getNonFatal500Pattern() {    
    return DEFAULT_NON_FATAL_500_PAT;   
  }
  // Use a getter so that this can be overridden by a child plugin
  protected Pattern getNonFatal403Pattern() {    
    return DEFAULT_NON_FATAL_403_PAT;   
  }
  
}