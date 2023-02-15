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

package org.lockss.plugin.psychiatryonline;

import java.io.*;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.lockss.daemon.PluginException;
import org.lockss.filter.StringFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class PsychiatryOnlineHtmlFilterFactory implements FilterFactory {
  
  private static Logger logger = Logger.getLogger(PsychiatryOnlineHtmlFilterFactory.class);
  
  private static final String FILTERED_CHARSET_STRING =
      "<META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-16\">";
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    // First remove "<META http-equiv="Content-Type" content="text/html; charset=utf-16">"
    try {
      InputStreamReader unfilteredReader = new InputStreamReader(in, encoding);
      // Create a StringFilter and set ingore case, BufferedReader allows set mark()
      StringFilter ifr = new StringFilter(unfilteredReader, FILTERED_CHARSET_STRING);
      ifr.setIgnoreCase(true);
      BufferedReader filteredReader = new BufferedReader(ifr);
      in = new ReaderInputStream(filteredReader, encoding);
    }
    catch (UnsupportedEncodingException uee) {
      // Leave in unchanged but log a message
      logger.warning("Unknown InputStreamReader encoding: " + encoding, uee);
    }
    
    // Then filter out HTML constructs
    
    NodeFilter[] filters = new NodeFilter[] {
        // Filter out <script>
        new TagNameFilter("script"),
        // Filter out <span id=~"lblSeeAlso">
        HtmlNodeFilters.tagWithAttributeRegex("span", "id", "lblSeeAlso"),
        // Filter out <input type="hidden">
        HtmlNodeFilters.tagWithAttribute("input", "type", "hidden"),
    };
    
    return new HtmlFilterInputStream(in, encoding, HtmlNodeFilterTransform.exclude(
        new OrFilter(filters)));
  }
  
}
