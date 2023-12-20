/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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
      Pattern.compile("((action/(download(Table|Figures|PdfFig|Citation)|show(Popup|Cit)))|/releasedAssets/)");

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