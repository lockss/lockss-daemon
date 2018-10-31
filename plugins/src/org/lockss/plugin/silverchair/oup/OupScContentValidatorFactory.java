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

package org.lockss.plugin.silverchair.oup;


import org.lockss.plugin.ContentValidator;
import org.lockss.plugin.silverchair.BaseScContentValidatorFactory;

public class OupScContentValidatorFactory extends BaseScContentValidatorFactory {

  public static class OupTextTypeValidator extends ScTextTypeValidator {

    // files like https://academic.oup.com/view-large/figure/112575428/aww280f5.png
    // are html wrapped around an image <img class="content-image" src="https://oup.silverchair-cdn.com/....

    private static final String VIEW_LARGE_FIG_STRING = "/view-large/figure/";

    @Override
    public boolean invalidFileExt(String url) {
      if (url.contains(VIEW_LARGE_FIG_STRING)) {
        return false;
      }
      return super.invalidFileExt(url);
    }

    private static final String MAINTENANCE_STRING = "Sorry for the inconvenience, we are performing some maintenance at the moment. We will be back online shortly";
    //    private static final String RESTRICTED_ACCESS_STRING = "article-top-info-user-restricted-options";
    //    private static final String EXPIRES_PAT_STRING = "[?]Expires=(2147483647)";

    @Override
    public String getMaintenanceString() {
      return MAINTENANCE_STRING;
    }
  }

  public ContentValidator getTextTypeValidator() {
    return new OupTextTypeValidator();
  }

}
