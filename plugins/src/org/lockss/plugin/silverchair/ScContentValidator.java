/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair;

import java.io.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.StringUtil;
import org.lockss.util.HeaderUtil;

public class ScContentValidator {
  
  protected static final String PDF_EXT = ".pdf";
  protected static final String PNG_EXT = ".png";
  protected static final String JPG_EXT = ".jpg";
  protected static final String JPEG_EXT = ".jpeg";
  protected static final String MAINTENANCE_STRING = "This site is down for maintenance";
  
  public static class ScTextTypeValidator implements ContentValidator {
    
    private String invalidString;
    private String maintenanceString;
    private String patternString;
    
    public String getInvalidString() {
      return "";
    }
    
    public String getMaintenanceString() {
      return MAINTENANCE_STRING;
    }
    
    public String getPatternString() {
      return "";
    }
    
    public ScTextTypeValidator() {
      super();
      this.invalidString = getInvalidString();
      this.maintenanceString = getMaintenanceString();
      this.patternString = getPatternString();
    }
    
    public void validate(CachedUrl cu)
        throws ContentValidationException, PluginException, IOException {
      // validate based on extension (ie .pdf or .jpg)
      String url = cu.getUrl();
      if (StringUtil.endsWithIgnoreCase(url, PDF_EXT) ||
          StringUtil.endsWithIgnoreCase(url, PNG_EXT) ||
          StringUtil.endsWithIgnoreCase(url, JPG_EXT) ||
          StringUtil.endsWithIgnoreCase(url, JPEG_EXT)) {
        throw new ContentValidationException("URL MIME type mismatch");
      } else {
        try {
          if (!invalidString.isEmpty()) {
            if (StringUtil.containsString(new InputStreamReader(cu.getUnfilteredInputStream(), cu.getEncoding()), invalidString)) {
              throw new ContentValidationException("Found invalid page");
            }
          }
          if (!maintenanceString.isEmpty()) {
            if (StringUtil.containsString(new InputStreamReader(cu.getUnfilteredInputStream(), cu.getEncoding()), maintenanceString)) {
              throw new ContentValidationException("Found maintenance page");
            }
          }
          if (!patternString.isEmpty()) {
            if (containsPattern(new InputStreamReader(cu.getUnfilteredInputStream(), cu.getEncoding()), patternString)) {
              throw new ContentValidationException("Found pattern in page");
            }
          }
        } finally {
          cu.release();
        }
      }
    }
  }
  
  public static class Factory implements ContentValidatorFactory {
    
    public ContentValidator createContentValidator(ArchivalUnit au, String contentType) {
      switch (HeaderUtil.getMimeTypeFromContentType(contentType)) {
      case "text/html":
      case "text/*":
        ScTextTypeValidator sttv = new ScTextTypeValidator();
        return sttv;
      default:
        return null;
      }
    }
  }
  

  /**
   * Scans through the reader looking for the String str; case sensitive
   * @param reader Reader to search; it will be at least partially consumed
   * @return true if the string is found, false if the end of reader is
   * reached without finding the string
   */
  public static boolean containsPattern(Reader reader, String regex)
      throws IOException {
    return containsPattern(reader, regex, false);
  }

  public static boolean containsPattern(Reader reader, String regex,
                       int buffSize)
      throws IOException {
    return containsPattern(reader, regex, false, buffSize);
  }

  public static boolean containsPattern(Reader reader, String regex,
                       boolean ignoreCase)
      throws IOException {
    return containsPattern(reader, regex, ignoreCase, 4096);
  }

  /**
   * Scans through the reader looking for the pattern String regex
   * @param reader Reader to search; it will be at least partially consumed
   * @param ignoreCase whether to ignore case or not
   * @return true if the string is found, false if the end of reader is
   * reached without finding the pattern string
   */
  public static boolean containsPattern(Reader reader, String regex,
                       boolean ignoreCase, int buffSize)
      throws IOException {
    if (reader == null) {
      throw new NullPointerException("Called with a null reader");
    } else if (regex == null) {
      throw new NullPointerException("Called with a null pattern String");
    } else if (regex.length() == 0) {
      throw new IllegalArgumentException("Called with a blank pattern String");
    } else if (buffSize <= 0) {
      throw new IllegalArgumentException("Called with a buffSize < 0");
    }

    int strlen = regex.length();
    // simplify boundary conditions by ensuring buffer always larger than search string
    buffSize = Math.max(buffSize, strlen * 2);
    int shiftSize = buffSize - (strlen - 1);

    int flags = ignoreCase ? java.util.regex.Pattern.CASE_INSENSITIVE : 0;
    java.util.regex.Pattern pat = java.util.regex.Pattern.compile(regex, flags);
    StringBuilder sb = new StringBuilder(buffSize);

    while (StringUtil.fillFromReader(reader, sb, buffSize - sb.length())) {
      java.util.regex.Matcher m1 = pat.matcher(sb);
      if (m1.find()) {
        return true;
      }
      if (sb.length() < buffSize) {
        // avoid unnecessary shift on final iteration
        return false;
      }
      sb.delete(0, shiftSize);
    }
    return false;
  }

}

