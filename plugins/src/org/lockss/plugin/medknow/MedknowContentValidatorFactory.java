/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.medknow;

import java.io.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.StringUtil;
import org.lockss.util.HeaderUtil;
import org.lockss.util.IOUtil;

public class MedknowContentValidatorFactory implements ContentValidatorFactory {

  protected static final String VIEWIMAGE = "viewimage.asp?img=";
  protected static final String PDF_EXT = ".pdf";
  protected static final String PNG_EXT = ".png";
  protected static final String JPG_EXT = ".jpg";
  protected static final String JPEG_EXT = ".jpeg";
  protected static final String MAINTENANCE_STRING = "Page you are trying to access is under maintenance";

  public ContentValidator getTextTypeValidator() {
    return new TextTypeValidator();
  }

  public ContentValidator createContentValidator(ArchivalUnit au, String contentType) {
    switch (HeaderUtil.getMimeTypeFromContentType(contentType)) {
    case "text/html":
    case "text/*":
      return getTextTypeValidator();
    default:
      return null;
    }
  }

  public static class TextTypeValidator implements ContentValidator {

    private String maintenanceString;

    public TextTypeValidator() {
      super();
      this.maintenanceString = getMaintenanceString();
    }

    public String getMaintenanceString() {
      return MAINTENANCE_STRING;
    }

    public boolean invalidFileExt(String url) {
      return !url.contains(VIEWIMAGE) &&
          (StringUtil.endsWithIgnoreCase(url, PDF_EXT) ||
           StringUtil.endsWithIgnoreCase(url, PNG_EXT) ||
           StringUtil.endsWithIgnoreCase(url, JPG_EXT) ||
           StringUtil.endsWithIgnoreCase(url, JPEG_EXT));
    }

    public void validate(CachedUrl cu)
        throws ContentValidationException, PluginException, IOException {
      // validate based on extension (ie .pdf or .jpg)
      String url = cu.getUrl();
      if (invalidFileExt(url)) {
        throw new ContentValidationException("URL MIME type mismatch");
      } else {
        Reader rdr = null;
        try {
          rdr = new InputStreamReader(cu.getUnfilteredInputStream(), cu.getEncoding());
          if (!maintenanceString.isEmpty()) {
            if (StringUtil.containsString(rdr, maintenanceString)) {
              throw new ContentValidationException("Found maintenance page: " + maintenanceString);
            }
            IOUtil.safeClose(rdr);
            rdr = new InputStreamReader(cu.getUnfilteredInputStream(), cu.getEncoding());
          }
        } finally {
          IOUtil.safeClose(rdr);
          cu.release();
        }
      }
    }
  }
}

