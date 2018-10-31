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

package org.lockss.plugin.silverchair.iwap;


import org.lockss.plugin.*;
import org.lockss.plugin.silverchair.BaseScContentValidatorFactory;

public class IwapContentValidatorFactory extends BaseScContentValidatorFactory {
  
  public static class IwapTextTypeValidator extends ScTextTypeValidator {

    private static final String RESTRICTED_ACCESS_STRING = "article-top-info-user-restricted-options";
    // the pattern is essentially long enough to contain 2 patterns to ensure the match does not give a false positive
    private static final String EXPIRES_PAT_STRING = "[?]Expires=(?!2147483647).......................";

    @Override
    public String getInvalidString() {
      return RESTRICTED_ACCESS_STRING;
    }

    @Override
    public String getPatternString() {
      return EXPIRES_PAT_STRING;
    }
  }

  public ContentValidator getTextTypeValidator() {
    return new IwapTextTypeValidator();
  }
}
