/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.europeanmathematicalsociety;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class EuropeanMathematicalSocietyUrlNormalizer implements UrlNormalizer {
  
  protected static final Logger log = Logger.getLogger(EuropeanMathematicalSocietyUrlNormalizer.class);
  protected static final String SEARCH_PARAM = "&srch=series|";
  protected static final String PATTERN_STR = SEARCH_PARAM + ".+$";
  
  // XXX NOTE: may want normalize 
  //  http://www.ems-ph.org/books/show_pdf.php?proj_nr=NN&vol=1 to 
  //  http://www.ems-ph.org/books/show_pdf.php?proj_nr=NN
  // Cannot tell now, will need more experience from crawling AUs
  
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    if (url.contains(SEARCH_PARAM)) {
      url = url.replaceFirst(PATTERN_STR, "");
    }
    return(url);
  }
}
