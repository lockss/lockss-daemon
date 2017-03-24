/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;

public class BaseAtyponContentValidator {
  
  private static final Logger log = Logger.getLogger(BaseAtyponContentValidator.class);
  
  protected static final String PDF_STRING = "/doi/pdf(plus)?/";
  protected static final Pattern PDF_PAT = Pattern.compile(PDF_STRING, Pattern.CASE_INSENSITIVE);
  
  public static class TextTypeValidator implements ContentValidator {
    
    public void validate(CachedUrl cu)
        throws ContentValidationException, PluginException, IOException {
      // validate based on pattern (ie http://<base_url>/doi/pdf(plus)/<doi_value with '/'>)
      String url = cu.getUrl();
      Matcher mat = PDF_PAT.matcher(url);
      if (mat.find()) {
        log.warning("URL MIME type mismatch: " + url);
        throw new ContentValidationException("URL MIME type mismatch");
      }
    }
  }
  
  public static class Factory implements ContentValidatorFactory {
    public ContentValidator createContentValidator(ArchivalUnit au, String contentType) {
      switch (HeaderUtil.getMimeTypeFromContentType(contentType)) {
      case "text/html":
      case "text/*":
        return new TextTypeValidator();
      default:
        return null;
      }
    }
  }
  
}

