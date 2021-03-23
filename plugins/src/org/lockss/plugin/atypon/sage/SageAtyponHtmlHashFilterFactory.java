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

package org.lockss.plugin.atypon.sage;

import java.io.InputStream;
import java.util.Vector;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.BulletList;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;
import org.lockss.util.Logger;


//5/9/18 changed to include/exclude
// Keeps contents only (includeNodes), then hashes out unwanted nodes 
//within the content (excludeNodes).
public class SageAtyponHtmlHashFilterFactory 
  extends BaseAtyponHtmlHashFilterFactory {

  private static final Logger log = Logger.getLogger(SageAtyponHtmlHashFilterFactory.class);
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
	  
	NodeFilter[] includeNodes = new NodeFilter[] {
			//HtmlNodeFilters.tag("body"),
		//manifest
	    new NodeFilter() {
		  @Override
		  public boolean accept(Node node) {
		    if (HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/toc/").accept(node)) {
			  Node liParent = node.getParent();
			  if (liParent instanceof Bullet) {
			    Bullet li = (Bullet)liParent;
				Vector liAttr = li.getAttributesEx();
				if (liAttr != null && liAttr.size() == 1) {
				  Node ulParent = li.getParent();
				  if (ulParent instanceof BulletList) {
				    BulletList ul = (BulletList)ulParent;
					Vector ulAttr = ul.getAttributesEx();
					return ulAttr != null && ulAttr.size() == 1;
			      }
				}
			  }
		    } 
			return false;
	      }
	    },
     //toc   <div class="tocContent">
	   HtmlNodeFilters.tagWithAttribute("div", "class","tocContent"),
	 //article - doi/(ref|figure|full|abs...)/ 
     //<div class="widget literatumPublicationContentWidget none articleContent
	   HtmlNodeFilters.tagWithAttributeRegex("div", "class", "literatumPublicationContentWidget"),
     //meeting abstracts seem to use standard TOC, not search argument url
	 //see - http://journals.sagepub.com/toc/faib/38/1_suppl
	 //showCitation - included on article page - not a standalone for this plugin
	 //showPopup&citart <body class="popupBody">
	   HtmlNodeFilters.tagWithAttributeRegex("body","class","popupBody"),
	 /*
	  * 10/2/2018 - updates due to skin change
	  */
	   //TOC still uses tocContent; /full/ and /figure/ still use literatumPublicationContentWidget
	   // article abstract now has <div class="widget accessDenialWidget none articleContent widget-non....>
	   HtmlNodeFilters.tagWithAttributeRegex("div","class"," articleContent "),
	//https://journals.sagepub.com/doi/abs/10.1606/1044-3894.2016.97.18, need the div to get abstracted content
	//https://journals.sagepub.com/doi/abs/10.1606/1044-3894.2016.97.1, it is not guaranteed each article has Abstract content
	// on 05/2020, the skin changed, "div", "hlFld-Abstract" section only displays an image of first page of PDF file.
	HtmlNodeFilters.tagWithAttributeRegex("div","class","hlFld-Abstract"),
	HtmlNodeFilters.tagWithAttributeRegex("div","class","hlFld-Title"),
	HtmlNodeFilters.tagWithAttributeRegex("span","class","publicationContentEpubDate")

	};
    NodeFilter[] excludeNodes = new NodeFilter[] {
        // handled by parent: script, sfxlink, stylesheet
        // literatumAd, style
        // page header: login, register, etc., and journal menu such as
        // subscribe, alerts, ...
        HtmlNodeFilters.tagWithAttribute("header", "class", "page-header"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journalNavContainer"),
        // toc - Right column
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", 
                                              "TOCRightColumn"),
        // article right column
        HtmlNodeFilters.tagWithAttributeRegex("div",  "class", "articleRightColumn"),
        // article pre-footer, removes Access Options (login, purchase)
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "accessDenialDropZone"),
        HtmlNodeFilters.tagWithAttribute("div", "id", "accessDenialWidget"),

        // on full text and references page the ways to linkout to the reference get
        // added to (GoogleScholar, Medline, ISI, abstract, etc)
        // leave the content (NLM_article-title, NLM_year, etc),
        // but remove everything else (links and punctuation between options)
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttribute(
                "table", "class", "references"),
            HtmlNodeFilters.tagWithAttributeRegex(
                "span", "class", "NLM_")),
        //5/10/18 - some additions due to changes; some would repair in time but will slow finishing
        // this is a hack - the references would leave this but at some point Sage stopped using it for
        // reference author names so just remove it from the older versions of content
        HtmlNodeFilters.tagWithAttribute("span","class","NLM_string-name"),
        //keywords have been added to all abs,etc
        HtmlNodeFilters.tagWithAttribute("div","class","hlFld-KeywordText"),
        //change to format of doi information - remove "DOI:" and http://dx.doi.org --> https://doi.org/
        HtmlNodeFilters.tagWithAttribute("a","class","doiWidgetLnk"),
        HtmlNodeFilters.tagWithAttribute("div","class","publicationContentDoi"),
        HtmlNodeFilters.tagWithAttribute("div","id","articleInfo"),

    };
    
    //1. First remove all comments because the use of comments with nested <script> blocks is
    //causing problems for the parser.
    //<!--script>
    //</script><script>
    //</script-->
    InputStream noComment = filterComments(in, encoding);
    return super.createFilteredInputStream(au, noComment, encoding, includeNodes, excludeNodes);

  }

  
  public boolean doTagRemovalFiltering() {
    return true;
  }
   
  @Override
  public boolean doWSFiltering() {
    return true;
  }

}

