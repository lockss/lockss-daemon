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

package org.lockss.plugin.michigan.deepblue;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;

import java.io.InputStream;


public class DeepBlueHtmlCrawlFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
          /*
          On page like this: https://deepblue.lib.umich.edu/handle/2027.42/109436
          Need to exclude this part, it will cause overcrawl:
          <ul class="ds-referenceSet-list">
            <!-- External Metadata URL: cocoon://metadata/handle/2027.42/41251/mets.xml-->
            <li>
              <a href="/handle/2027.42/41251">Paleontology, Museum of - Publications</a>
            </li>
          </ul>

          <div class="col-xs-12">
                                       <div class="breadcrumb dropdown visible-xs">
                                          <a data-toggle="dropdown" class="dropdown-toggle" role="button" href="#" id="trail-dropdown-toggle">View Item&nbsp;<b class="caret"></b></a>
                                          <ul aria-labelledby="trail-dropdown-toggle" role="menu" class="dropdown-menu">
                                             <li role="presentation">
                                                <a role="menuitem" href="/documents"><i aria-hidden="true" class="glyphicon glyphicon-home"></i>&nbsp;
                                                Home</a>
                                             </li>
                                             <li role="presentation">
                                                <a role="menuitem" href="/handle/2027.42/13913">Research Collections</a>
                                             </li>
                                             <li role="presentation">
                                                <a role="menuitem" href="/handle/2027.42/41251">Paleontology, Museum of - Publications</a>
                                             </li>
                                             <li role="presentation" class="disabled">
                                                <a href="#" role="menuitem">View Item</a>
                                             </li>
                                          </ul>
                                       </div>
                                       <ul class="breadcrumb hidden-xs" style="max-width: 75%">
                                          <li>
                                             <i aria-hidden="true" class="glyphicon glyphicon-home"></i>&nbsp;
                                             <div style="display: inline; font-size: 18px; font-weight: normal;">
                                                <a href="/documents">Home</a>
                                             </div>
                                          </li>
                                          <li>
                                             <div style="display: inline; font-size: 18px; font-weight: normal;">
                                                <a href="/handle/2027.42/13913">Research Collections</a>
                                             </div>
                                          </li>
                                          <li>
                                             <div style="display: inline; font-size: 18px; font-weight: normal;">
                                                <a href="/handle/2027.42/41251">Paleontology, Museum of - Publications</a>
                                             </div>
                                          </li>
                                          <li class="active">
                                             <div style="display: inline; font-size: 18px; font-weight: bold; font-family: var(--font-base-family);">View Item</div>
                                          </li>
                                       </ul>
                                       <br>
                                    </div>

           */
      throws PluginException {
        NodeFilter[] filters = new NodeFilter[] {
                HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "ds-referenceSet-list"),
                //This is top navigation, will lead overcrawl
                HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "breadcrumb"),
                HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "dropdown-menu"),

    };
    return new 
      HtmlFilterInputStream(in,
                            encoding,
                            HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
