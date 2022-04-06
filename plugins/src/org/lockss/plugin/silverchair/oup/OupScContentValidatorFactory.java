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
