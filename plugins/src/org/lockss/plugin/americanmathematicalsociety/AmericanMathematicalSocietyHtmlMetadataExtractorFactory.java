/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.americanmathematicalsociety;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.extractor.MetadataField.Cardinality;
import org.lockss.extractor.MetadataField.Validator;
import org.lockss.plugin.*;


/*
 Metadata on page http://www.ams.org/journals/jams/2013-26-01/S0894-0347-2012-00742-5/ in the form of:
<meta content="Journal of the American Mathematical Society" name="citation_journal_title">
<meta content="J. Amer. Math. Soc." name="citation_journal_abbrev">
<meta content="http://www.ams.org/jams/2013-26-01/S0894-0347-2012-00742-5/" name="citation_abstract_html_url">
<meta content="http://www.ams.org/jams/2013-26-01/S0894-0347-2012-00742-5/S0894-0347-2012-00742-5.pdf" name="citation_pdf_url">
<meta content="0894-0347" name="citation_issn">
<meta content="1088-6834" name="citation_issn">
<meta content="Masur, Howard" name="citation_author">
<meta content="Department of Mathematics, University of Chicago, Chicago, Illinois 60637" name="citation_author_institution">
<meta content="masur@math.uic.edu" name="citation_author_email">
<meta content="Schleimer, Saul" name="citation_author">
<meta content="Mathematics Institute, University of Warwick, Coventry, CV4 7AL, United Kingdom" name="citation_author_institution">
<meta content="s.schleimer@warwick.ac.uk" name="citation_author_email">
<meta content="The geometry of the disk complex" name="citation_title">
<meta content="2012/08/22" name="citation_online_date">
<meta content="2013" name="citation_publication_date">
<meta content="26" name="citation_volume">
<meta content="1" name="citation_issue">
<meta content="1" name="citation_firstpage">
<meta content="62" name="citation_lastpage">
<meta content="10.1090/S0894-0347-2012-00742-5" name="citation_doi">

 Metadata on page http://www.ams.org/books/surv/032
<meta name="citation_series_title" content="Mathematical Surveys and Monographs">
<meta name="citation_abstract_html_url" content="http://www.ams.org/surv/032">
<meta name="citation_pdf_url" content="http://www.ams.org/surv/032/surv032.pdf">
<meta name="citation_issn" content="0076-5376">
<meta name="citation_issn" content="2331-7159">
<meta name="citation_isbn" content="978-0-8218-1533-5">
<meta name="citation_isbn" content="978-1-4704-1259-3">
<meta name="citation_publisher" content="American Mathematical Society">
<meta name="citation_author" content="Jacobowitz, Howard">
<meta name="citation_title" content="An Introduction to CR Structures">
<meta name="citation_publication_date" content="1990/08/07">
<meta name="citation_volume" content="32">
<meta name="citation_doi" content="http://dx.doi.org/10.1090/surv/032">

 */

public class AmericanMathematicalSocietyHtmlMetadataExtractorFactory
    implements FileMetadataExtractorFactory {
  static Logger log = Logger.getLogger(
      AmericanMathematicalSocietyHtmlMetadataExtractorFactory.class);
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
        String contentType)
      throws PluginException {
    return new AmericanMathematicalSocietyHtmlMetadataExtractor();
  }

  public static class AmericanMathematicalSocietyHtmlMetadataExtractor
    extends SimpleHtmlMetaTagMetadataExtractor {
    
    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      // tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER); // XXX
      tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put("citation_doi", MetadataField.FIELD_DOI);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_isbn", MetadataField.FIELD_ISBN);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_author",
          new MetadataField(MetadataField.FIELD_AUTHOR,
                            MetadataField.splitAt(",")));
      tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);
      tagMap.put("citation_series_title", MetadataField.FIELD_SERIES_TITLE);
      tagMap.put("citation_keywords", 
          new MetadataField(MetadataField.FIELD_KEYWORDS, 
                            MetadataField.splitAt(",")));
    }
    
    
    /* 
     * Trying to pull metadata from Book Reviews using HTML. 
     * Example: https://www.ams.org/journals/bull/2022-59-01/S0273-0979-2021-01733-2/
     * <section id="additionalinformation">
     *    <details>
     *        <ul>
     *            <li></li>
     *            <li>
     *              "DOI: https://doi.org/10.1090/bull/1733"
     *            </li>
     *            <li></li>
     *            <li></li>
     *        </ul>
     *    </details>
     * </section>
     * 
    */
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
        throws IOException {

      ArticleMetadata am = super.extract(target, cu);
      
      //check if doi is pulled from metadata
      String doi = am.getRaw("citation_doi");
      if(doi != null){
        log.debug3("The doi is " + doi);
      }
      //if not doi is found, add doi from html page
      if (doi == null) {
        Document doc = Jsoup.parse(cu.getUnfilteredInputStream(), cu.getEncoding() ,cu.getUrl());
        Elements listItems = doc.select("section#additionalinformation > details > ul > li");
        for(Element listItem : listItems){
          if(listItem.text().contains("DOI: ")){
            putDoiValue(am, "citation_doi", listItem.text());
          }
        }
        
      }
      am.cook(tagMap);
      am.putIfBetter(MetadataField.FIELD_PUBLISHER, "American Mathematical Society");
      return am;
    }
    
    protected void putDoiValue(ArticleMetadata am, String name, String content) {
      // filter raw HTML tags embedded within content -- publishers get sloppy
      content = HtmlUtil.stripHtmlTags(content);
      // remove character entities from content
      content = StringEscapeUtils.unescapeHtml4(content);
      String finalDoi = content.substring(content.indexOf("https://doi.org/")+16);
      if (log.isDebug3()) log.debug3("Add: " + name + " = " + finalDoi);
      am.putRaw(name, finalDoi);
    }
  }
  
}
