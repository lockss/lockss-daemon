/*
 Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.cloudpublish;

import org.lockss.daemon.LoginPageChecker;
import org.lockss.daemon.PluginException;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

public class CloudPublishLoginPageChecker implements LoginPageChecker {

  //protected static Logger log = Logger.getLogger(CloudPublishLoginPageChecker.class);

  protected static final String LOGIN_STRINGS[] = new String[] {
    "<h1>You don’t currently have access to this article</h1>",
    "If you have private access to this content, please log in with your username and password",
    "<label class=\"label\" for=\"access-token-code\">One time Access Token</label>",
    "href=\"/read-this/article/",
  };

  public boolean isLoginPage(Properties props, Reader reader) throws IOException, PluginException {
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
