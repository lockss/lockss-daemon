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

package org.lockss.plugin.jstor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.config.CurrentConfig;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.HtmlFormExtractor;
import org.lockss.extractor.HtmlFormExtractor.FormElementLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.ClassUtil;
import org.lockss.util.Logger;


/* 
 * JSTOR limited plugin
 * This is a plugin that collects a limited set of content due to redirection.
 * We only pick up TOC and pdf (and in rare cases when it exists a  "full", "select", "media")
 * Based on information on the TOC page, we also engineer the RIS citation page
 * TOC items have with each listing:
 *     <input type="checkbox" name="doi" class="checkBox" value="10.2307/4436967" id="cite4436967" />
 * for each one, generate the url:
 *     <base_url2>action/downloadSingleCitationSec?format=refman&doi=<doi>
 *    
 */

public class JstorHtmlFormExtractor extends HtmlFormExtractor {

  private static Logger log = Logger.getLogger(JstorHtmlFormExtractor.class);
  
  public JstorHtmlFormExtractor(ArchivalUnit au, Callback cb, String encoding,
      Map<String, FormFieldRestrictions> restrictions,
      Map<String, FieldIterator> generators) {
    super(au, cb, encoding, restrictions, generators);
  }
  
  @Override
  public FormElementLinkExtractor newTagsLinkExtractor() {
    return new JstorFormElementLinkExtractor();
  }

  public static class JstorFormElementLinkExtractor extends FormElementLinkExtractor {
    
    private static final String NAME_ATTR = "name";
    private static final String VAL_ATTR = "value";

    protected Pattern DOI_URL_PATTERN = Pattern.compile("([.0-9]+)/(.*)$");
    // baseURL for table of contents when arrived at from manifest page  
    protected Pattern BASE_URL_PATTERN = Pattern.compile("^(http://.*/)(action/showToc\\?)(.*)$");
    private static final String CITATION_URL_START = "action/downloadSingleCitationSec?format=refman&doi=";

    private static Logger log = Logger.getLogger(JstorFormElementLinkExtractor.class);

    /*
     * Extending 
     */
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      log.debug3("custom tagBegin");
      String srcUrl = node.baseUri();

      if (node.hasAttr(NAME_ATTR) && node.hasAttr(VAL_ATTR)) {
        if ("doi".equalsIgnoreCase((node.attr(NAME_ATTR)))) {
          String doitext = node.attr(VAL_ATTR);
          Matcher doiMat = DOI_URL_PATTERN.matcher(doitext);
          Matcher baseMat = BASE_URL_PATTERN.matcher(srcUrl);
          /* Are we on a TOC page? and doi we have a node that identifies a doi? */
          if (doiMat.find() && baseMat.find()) {
            String doi1 = doiMat.group(1);
            String doi2 = doiMat.group(2);
            // ris citation uses https, not http
            String new_base = au.getConfiguration().get(ConfigParamDescr.BASE_URL2.getKey());
            String newUrl = new_base + CITATION_URL_START + doi1 + "/" + doi2;
            log.debug3("Generating a new link: " + newUrl);
            cb.foundLink(newUrl);
          }
        }
      }
      log.debug3("now calling the super tagBegin");
      super.tagBegin(node, au, cb);
    }
  }
}


