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

package org.lockss.plugin.sample;

import java.io.*;
import java.util.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/** Demonstration ContentValidator */
public class PdfContentValidator implements ContentValidator {

  public void validate(CachedUrl cu)
      throws ContentValidationException, PluginException, IOException {

    // Check the response headers for an expected value, reject file if not
    // found.
    CIProperties headers = cu.getProperties();
    // Check some 
    String some_val = headers.getProperty("some_prop");
    if (some_val != "some_string") {
      throw new InvalidReponseHeaderException("msg");
    }
    // May also read the content to check type, determine if well-formed,
    // etc.
    if (isTruncatedPdf(cu.getUnfilteredInputStream())) {
      throw new TruncatedPdfException("msg");
    }      

    // If a redirect occurred, the CachedUrl argument reflects the original
    // URL, the response headers are those from the final, non-redirect
    // response, and the list of redirected-to URLs (not including the
    // original URL) in the CachedUrl.PROPERTY_VALIDATOR_REDIRECT_URLS
    // property.  (This is a List, which must be retrieved using Map.get(),
    // not Properties.getProperty()).

    List<String> redirUrls =
      (List<String>)headers.get(CachedUrl.PROPERTY_VALIDATOR_REDIRECT_URLS);
    // ...
  }

  boolean isTruncatedPdf(InputStream ins) {
    return false;
  }

  /** Factory class, name goes in plugin */
  public static class Factory implements ContentValidatorFactory {
    public ContentValidator createContentValidator(ArchivalUnit au,
						   String contentType) {
      // content type available but need not be used.
      return new PdfContentValidator();
    }
  }

  /** Defining an exception specific to this condition facilitates custom
   * action, specified in plugin. */
  public static class InvalidReponseHeaderException
    extends ContentValidationException {

    public InvalidReponseHeaderException(String msg) {
      super(msg);
    }
  }

  public static class TruncatedPdfException
    extends ContentValidationException {

    public TruncatedPdfException(String msg) {
      super(msg);
    }
  }


}
    
