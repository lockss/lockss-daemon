/*
 * $Id$
 */

/*

Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.springer.link;

import java.io.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;


public class SpringerLinkJournalsContentValidator {
  
  public static class HtmlContentValidator implements ContentValidator {
    
    private static final String PDF_EXT = ".pdf";
    
    public void validate(CachedUrl cu)
        throws ContentValidationException, PluginException, IOException {
      
      // check the file name for the pdf file
      String url = cu.getUrl();
      if (url.endsWith(PDF_EXT)) {
        throw new ContentValidationException("URL MIME type mismatch");
      }
    }
  }

  /** This factory is used to create validator for all mime types other than pdf. */
  public static class Factory implements ContentValidatorFactory {
    /** 
     * May create a mime-type specific validator here, or a general
     * purpose one which checks the mime-type when invoked on a specific file.
     **/
    public ContentValidator createContentValidator(ArchivalUnit au, String contentType) {
      switch (HeaderUtil.getMimeTypeFromContentType(contentType)) {
      case "text/html":
      case "text/*":
        return new HtmlContentValidator();
      default:
        return null;
      }
    }
  }
  
}

