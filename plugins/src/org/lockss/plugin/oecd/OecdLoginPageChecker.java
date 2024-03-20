/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.oecd;

import java.io.*;
import java.util.Properties;

import org.apache.commons.io.LineIterator;
import org.lockss.daemon.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.*;

/**
 * <p>
 * Some journal start URLs have the permission statement, yet say {@code Sorry,
 * you don't have access to the content}.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @see #NO_ACCESS
 */
public class OecdLoginPageChecker implements LoginPageChecker {
  
  Logger log = Logger.getLogger(OecdLoginPageChecker.class);
  
  public static final String NO_ACCESS = "Sorry, you don't have access to the content";
  
  @Override
  public boolean isLoginPage(Properties props,
                             Reader reader)
      throws IOException, PluginException {
    if (Constants.MIME_TYPE_HTML.equals(HeaderUtil.getMimeTypeFromContentType(props.getProperty("Content-Type")))) {
      LineIterator liter = new LineIterator(reader);
      while (liter.hasNext()) {
        String line = liter.nextLine();
        if (line.contains(NO_ACCESS)) {
          return true;
        }
      }
    }
    return false;

  }  
  
}
