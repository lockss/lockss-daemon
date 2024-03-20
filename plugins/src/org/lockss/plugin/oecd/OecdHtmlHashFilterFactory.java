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

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;

import java.io.InputStream;
import java.io.Reader;

public class OecdHtmlHashFilterFactory implements FilterFactory {

  protected static final NodeFilter[] excludeFilters = new NodeFilter[] {
      /*
       * High risk of turnover: comments, <scripts> and <style> tags.
       */
      HtmlNodeFilters.comment(),
      HtmlNodeFilters.tag("script"),
      HtmlNodeFilters.tag("style"),
      
      /*
       * Structural elements that are subject to evolution:
       */
      /*
       * Cookie bar
       */
      HtmlNodeFilters.tagWithAttribute("div", "id", "cookie-bar"),
      /*
       * General header
       */
      HtmlNodeFilters.tag("header"),
      /*
       * Breadcrumb
       * 
       * Style changed over time, e.g.:
       * 
       * https://www.oecd-ilibrary.org/economics/etudes-economiques-de-l-ocde-malaisie-2019-version-abregee_e544ad44-fr
       * 
       * 14:18:15 04/25/23:

<ol class="breadcrumb">
<li>
<a href="/" 
>Home</a>
</li>
<li>
<a href="/books" 
>Books</a>
</li>
<li>
<a href="/economics/etudes-economiques-de-l-ocde_16843428" 
>Études économiques de l&apos;OCDE</a>
</li>
<li>
Études économiques de l&apos;OCDE : Malaisie 2019 (version abrégée)
</li>
</ol>

       * 15:32:56 06/06/23:

<ol class="breadcrumb">
<li>
<a href="/" 
>Home</a>
</li>
<li>
<a href="/books" 
>Books</a>
</li>
<li>
<a href="/economics/etudes-economiques-de-l-ocde_16843428" 
>Études économiques de l&apos;OCDE</a>
</li>
<li>
<a href="/economics/etudes-economiques-de-l-ocde-malaisie_87a4fada-fr" 
>Études économiques de l‘OCDE : Malaisie</a>
</li>
<li>
2019
</li>
</ol>

       */
      HtmlNodeFilters.tagWithAttribute("ol", "class", "breadcrumb"),
      /*
       * LOCKSS and CLOCKSS permission statements
       * 
       * In an <h1> tag with some styling that could change.
       */
      HtmlNodeFilters.tag("h1"),
      /* 
       * General footer: <footer>
       */
      HtmlNodeFilters.tag("footer"),
      
      /*
       * Links to ancillary CSS and Javascript change versions, e.g.:
       * 
       * https://www.oecd-ilibrary.org/economics/etudes-economiques-de-l-ocde-malaisie-2019-version-abregee_e544ad44-fr
       * 
       * 14:18:15 04/25/23:

<link rel="stylesheet" href="/css/v/9.3.0/instance/site.css" type="text/css" />
<link rel="stylesheet" href="/css/v/9.3.0/instance/jquery.mCustomScrollbar.min.css" />

       * 15:32:56 06/06/23:

<link rel="stylesheet" href="/css/v/10.1.1/instance/site.css" type="text/css" />
<link rel="stylesheet" href="/css/v/10.1.1/instance/jquery.mCustomScrollbar.min.css" />

       * It could also be anticipated that more libraries would be added, or
       * that a used library would gain or lose links in a cluster, so it's just
       * easier to remove all <link> tags.
       */
      HtmlNodeFilters.tag("link"),

      /*
       * The style of the <title> tag changed, e.g.:
       *
       * https://www.oecd-ilibrary.org/economics/etudes-economiques-de-l-ocde-malaisie-2019-version-abregee_e544ad44-fr
       * 
       * 14:18:15 04/25/23:

<title>Études économiques de l&apos;OCDE : Malaisie 2019 (version abrégée) | Études économiques de l&apos;OCDE | OECD iLibrary</title>

       * 15:32:56 06/06/23:

<title>Études économiques de l&apos;OCDE : Malaisie 2019 (version abrégée) | Études économiques de l‘OCDE : Malaisie | OECD iLibrary</title>

       * Simply remove the <title> tag.
       */
      HtmlNodeFilters.tag("title"),

      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "subscription-indicator"),

      HtmlNodeFilters.tagWithAttribute("div", "id", "survicate-fb-box"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "survicate-box"),
      HtmlNodeFilters.tagWithAttribute("li", "class", "boardpaper"),
      // elements with dynamically generated randomness
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "tokenCSRF_HiddenValue"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "timer_id"),
      HtmlNodeFilters.tagWithAttribute("ul", "id", "web_nav"),
      HtmlNodeFilters.tagWithAttributeRegex("span", "class", "cf_email")
  };

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in, String encoding) throws PluginException {

    // Do the initial html filtering
    InputStream filteredStream = new HtmlFilterInputStream(in,
        encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(
            excludeFilters
        )));

    // some tags we remove may only exist in one cached content, so we must remove whitespace
    Reader noWhiteSpace = new WhiteSpaceFilter(
        FilterUtil.getReader(filteredStream, encoding)
    );
    return new ReaderInputStream(noWhiteSpace);
  }

}
