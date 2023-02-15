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

package org.lockss.plugin.bioone;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;
import org.lockss.util.Logger;


public class BioOneAtyponUrlNormalizer extends BaseAtyponUrlNormalizer {

  protected static final String[] endings = new String[] {
    "?cookieSet=1",
    "?prevSearch=",
  };
  
  protected static Logger log = 
      Logger.getLogger(BioOneAtyponUrlNormalizer.class);
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    int ind;
    
    // Normalize ending
    ind = url.indexOf('?');
    if (ind >= 0) {
      for (String ending : endings) {
        url = StringUtils.chomp(url, ending);
      }
    }

    // Normalize ending
    // example: http://www.bioone.org/toc/brvo/512?seq=512
    ind = url.indexOf("?seq=");
    if (ind >= 0) {
    	url = url.substring(0, ind);
    }

    // call the parent to handle citation download URLs
    return super.normalizeUrl(url, au);
  }

}
