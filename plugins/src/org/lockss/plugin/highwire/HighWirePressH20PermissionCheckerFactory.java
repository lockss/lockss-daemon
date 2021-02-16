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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.PermissionHelper;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class HighWirePressH20PermissionCheckerFactory
  implements PermissionCheckerFactory{
  
  public static class H20ProbePermissionChecker extends ProbePermissionChecker {
    
    private static final Logger logger = Logger.getLogger(H20ProbePermissionChecker.class);
    
    @Override
    public boolean checkPermission(PermissionHelper pHelper, Reader inputReader,
        String permissionUrl) {
      
      BufferedReader in = new BufferedReader(inputReader); 
      boolean ret = true;
      try {
        in.mark(102400);
        if (StringUtil.containsString(in, "platform = DRUPAL", true)) {
          logger.siteError(" ");
          logger.siteError("       ===============        ");
          logger.siteError(" ");
          logger.siteError("found DRUPAL flag");
          logger.siteError(" ");
          logger.siteError("       ===============        ");
          logger.siteError(" ");
          ret = false;
        }
        in.reset();
      } catch (IOException e) {
        logger.warning("IOException checking drupal flag", e);
      }
      if (ret) {
        ret = super.checkPermission(pHelper, in, permissionUrl);
      }
      return ret;
    }
    
    public H20ProbePermissionChecker() {
      super();
    }
  }
  
  @Override
  public List<H20ProbePermissionChecker> createPermissionCheckers(ArchivalUnit au) {
    List<H20ProbePermissionChecker> list = new ArrayList<H20ProbePermissionChecker>(1);
    list.add(new H20ProbePermissionChecker());
    return list;
  }
}

