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

package org.lockss.plugin.springer.link;

import java.io.*;
import java.util.Properties;

import org.lockss.daemon.*;
import org.lockss.util.*;

public class SpringerLinkLoginPageChecker implements LoginPageChecker {

  /**
   * <p>A recognizable string found on login pages.</p>
   */
  protected static final String LOGIN_STRINGS[] = new String[] {
      "Log in to check access",      // found in html for both books, journals
      "Buy eBook",                   // found only on book html
      "Buy article (PDF)",           // found only on journal html
      "Buy article PDF",             // new format, journals. Technically not a login page, but it sends you there.
      "Welcome back. Please log in." // new format, journals. This is on the login page. Only here as a backup if the
                                     // previous format changes.
  };
  
  @Override
  public boolean isLoginPage(Properties props,
                             Reader reader)
      throws IOException,
             PluginException {
    String contentType =
       HeaderUtil.getMimeTypeFromContentType(props.getProperty("Content-Type"));
    if ("text/html".equalsIgnoreCase(contentType)) {
      String fromReader = StringUtil.fromReader(reader);
      for (String loginString : LOGIN_STRINGS) {
        if (fromReader.contains(loginString)) {
          return true;
        }
      }
    }
    return false;
  }

}
