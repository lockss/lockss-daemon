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

package org.lockss.plugin.highwire;

import java.io.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.StringUtil;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;

public class HighWireContentValidator {
  
  private static final Logger log = Logger.getLogger(HighWireContentValidator.class);
  
  protected static final String PDF_EXT = ".pdf";
  protected static final String PNG_EXT = ".png";
  protected static final String JPG_EXT = ".jpg";
  protected static final String JPEG_EXT = ".jpeg";
  protected static final String MOV_EXT = ".mov";
  protected static final String MP4_EXT = ".mp4";
  protected static final String ZIP_EXT = ".zip";
  
  public static class TextTypeValidator implements ContentValidator {
    
    public void validate(CachedUrl cu)
        throws ContentValidationException, PluginException, IOException {
      // validate based on extension (ie .pdf or .jpg)
      String url = cu.getUrl();
      if (StringUtil.endsWithIgnoreCase(url, PDF_EXT) ||
          StringUtil.endsWithIgnoreCase(url, PNG_EXT) ||
          StringUtil.endsWithIgnoreCase(url, JPG_EXT) ||
          StringUtil.endsWithIgnoreCase(url, JPEG_EXT) ||
          StringUtil.endsWithIgnoreCase(url, MOV_EXT) ||
          StringUtil.endsWithIgnoreCase(url, MP4_EXT) ||
          StringUtil.endsWithIgnoreCase(url, ZIP_EXT)) {
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

