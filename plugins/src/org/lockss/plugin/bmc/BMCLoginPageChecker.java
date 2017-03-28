/*
 * $Id:$
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

package org.lockss.plugin.bmc;

import java.io.*;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.util.*;

/**
 * <p>Detects login pages in BMC and BioMedCentral journals.
 * The plugin also uses an au_redirect_to_login_url_pattern of <>render/render.asp\.asp\?.*access=denied
 * </p>
 */
public class BMCLoginPageChecker implements LoginPageChecker {

  /**
   * <p>Recognizable string found on login pages.</p>
   * <button id="logon_button" value="Log on" type="submit" class="w62" name="">Log on</button>
   */
  protected static final String LOGIN_PATTERN_STRING = "(<h.>(subscription required|existing subscribers)[^<]*</h.>" +
      "|<button [^>]*id=\"logon_button\" )";
  protected static final Pattern LOGIN_PATTERN = Pattern.compile(LOGIN_PATTERN_STRING, Pattern.CASE_INSENSITIVE);
  
  public boolean isLoginPage(Properties props,
                             Reader reader)
      throws IOException,
             PluginException {
    if ("text/html".equalsIgnoreCase(HeaderUtil.getMimeTypeFromContentType(props.getProperty("Content-Type")))) {
      String theContents = StringUtil.fromReader(reader);
      Matcher loginMat = LOGIN_PATTERN.matcher(theContents);
      
      return loginMat.find();
    }
    return false;
  }

}
