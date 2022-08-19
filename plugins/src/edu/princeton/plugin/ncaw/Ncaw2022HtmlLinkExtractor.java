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


package edu.princeton.plugin.ncaw;

import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ncaw2022HtmlLinkExtractor extends GoslingHtmlLinkExtractor {

    private static final Logger logger = Logger.getLogger(Ncaw2022HtmlLinkExtractor.class);

    @Override
    protected String extractLinkFromTag(StringBuffer link,
                                        ArchivalUnit au,
                                        Callback cb)
        throws IOException {
  
      char ch = link.charAt(0);
  
      /*
      <meta name="citation_pdf_url" content="http://19thc-artworldwide.org/pdf/python/article_PDFs/NCAW_1074.pdf">
       */
  
      if ((ch == 'm' || ch == 'M') && beginsWithTag(link, METATAG)) {
  
        String meta_name = getAttributeValue("name", link);
        String fullTextUrl =  getAttributeValue("content", link);
        
        if ("citation_pdf_url".equals(meta_name) && (fullTextUrl != null))  {
                 
          cb.foundLink(fullTextUrl);
            
          return fullTextUrl;
        } else {
            logger.debug3("Not a suitable meta tag");
            return super.extractLinkFromTag(link, au, cb);
        }
      } else {
        logger.debug3("No <meta> tag");
        return super.extractLinkFromTag(link, au, cb);
      }  
    }
}
