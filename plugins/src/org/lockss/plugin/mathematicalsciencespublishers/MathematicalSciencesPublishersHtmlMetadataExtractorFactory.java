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

package org.lockss.plugin.mathematicalsciencespublishers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringEscapeUtils;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.repository.LockssRepository;


/*
 * Metadata on an abstract page http://msp.org/camcos/2012/7-2/p01.xhtml
 * in the form of:
 * <meta name="citation_publisher" content="Mathematical Sciences Publishers"/>
 * <meta name="citation_title" content="Discontinuous Galerkin method with the spectral deferred correction time-integration scheme and a modified moment limiter for adaptive grids"/>
 * <meta name="citation_journal_title" content="Communications in Applied Mathematics and Computational Science"/>
 * <meta name="citation_volume" content="7"/>
 * <meta name="citation_issue" content="2"/>
 * <meta name="citation_firstpage" content="133"/>
 * <meta name="citation_lastpage" content="174"/>
 * <meta name="citation_publication_date" content="2012-10-16"/>
 * <meta name="citation_pdf_url" content="http://msp.org/camcos/2012/7-2/camcos-v7-n2-p01-s.pdf"/>
 * <meta name="citation_doi" content="10.2140/camcos.2012.7.133"/>
 * <meta name="citation_issn" content="2157-5452"/>
 * <meta name="citation_author" content="Gryngarten, Leandro"/>
 * <meta name="citation_author" content="Smith, Andrew"/>
 * <meta name="citation_author" content="Menon, Suresh"/>
 */

public class MathematicalSciencesPublishersHtmlMetadataExtractorFactory 
    extends JsoupTagExtractorFactory {
  static Logger log = Logger.getLogger(
      MathematicalSciencesPublishersHtmlMetadataExtractorFactory.class);
  
  static final String CITATION_DOI = "citation_doi";
  static final String CITATION_TITLE = "citation_title";
  
  static final String SCRAPED_DOI = "scraped_doi";
  static final String SCRAPED_TITLE = "scraped_title";
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
        String contentType)
      throws PluginException {
    return new MathematicalSciencesPublishersHtmlMetadataExtractor(contentType);
  }
  
  public static class MathematicalSciencesPublishersHtmlMetadataExtractor
    extends JsoupTagExtractor {
    
    public MathematicalSciencesPublishersHtmlMetadataExtractor(String contentType) {
      // TODO  XXX  replace text/html with contentType parameter
      // XXX the JsoupTagExtractor only allows "text/html" || "text/xml" || "application/xml"
      // contentType of application/xhtml+xml was causing NPE
      super("text/html");
    }
    // Map HTML meta tag names to cooked metadata fields
    private static MultiMap tagMap = new MultiValueMap();
    static {
      tagMap.put("citation_journal_title", MetadataField.FIELD_PUBLICATION_TITLE);
      tagMap.put("citation_publisher", MetadataField.FIELD_PUBLISHER);
      tagMap.put(CITATION_TITLE, MetadataField.FIELD_ARTICLE_TITLE);
      tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
      tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
      tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
      tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
      tagMap.put(CITATION_DOI, MetadataField.FIELD_DOI);
      tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
      tagMap.put("citation_publication_date", MetadataField.FIELD_DATE);
      tagMap.put("citation_author",
          new MetadataField(MetadataField.FIELD_AUTHOR,
                            MetadataField.splitAt(",")));
      tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);
      tagMap.put(SCRAPED_DOI, MetadataField.FIELD_DOI);
      tagMap.put(SCRAPED_TITLE, MetadataField.FIELD_ARTICLE_TITLE);
    }
    
    private static Pattern whiteSpacePat = Pattern.compile("\\s+");
    private static Pattern patternDoi = Pattern.compile(
        "[ >]DOI:.*?(?:<a href= \"http:.*?\">)?(10.2140/[^ <]*?)[ <]", 
        Pattern.CASE_INSENSITIVE);
    private static Pattern patternTitleAuth = Pattern.compile(
        "<div class=\"title\">(.*?)</div>", 
        Pattern.CASE_INSENSITIVE);
    
    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
        throws IOException, PluginException {
      ArticleMetadata am = super.extract(target, cu);
      
      // attempt to get doi. etc from xhtml file
      if ((am.getRaw(CITATION_DOI) == null) || 
          (am.getRaw(CITATION_TITLE) == null)) {
        
        // TODO investigate using Jsoup to extract the DOI and title
        // rather than read the file
        String colContent="";
        // get the content
        BufferedReader bReader = new BufferedReader(openForReading(cu));
        
        try {
          Matcher matcher;
          // go through the cached URL content line by line
          for (String line = bReader.readLine(); line != null; line = bReader.readLine()) {
            line = line.trim();
            //<td id="content-area" class="content-column">
            if (!((line.startsWith("<td class=\"content-column\"")) ||
                  (line.startsWith("<td id=\"content-area\"")))) {
              continue;
            }
            // save the next block of text in textContent
            while((line != null) && !line.toLowerCase().contains("<h5>abstract</h5>")){
              if(!line.equalsIgnoreCase("")){
                colContent += " " + line.trim();
              }
              line = bReader.readLine();
            }
          }
          
          // regexes to extract DOI, etc. from articles
          if (am.getRaw(CITATION_DOI) == null) {
            // find DOI with optional anchor tag
            matcher = patternDoi.matcher(colContent);
            if (matcher.find()) {
              putValue(am, SCRAPED_DOI, matcher.group(1).trim());
            }
            else {
              log.debug("No DOI found in content");
            }
          }
          if (am.getRaw(CITATION_TITLE) == null) {
            // find article title 
            matcher = patternTitleAuth.matcher(colContent);
            if (matcher.find()) {
              putValue(am, SCRAPED_TITLE, matcher.group(1).trim());
            }
            else {
              log.debug("No ARTICLE_TITLE found in content");
            }
          }
        } catch (Exception e) {
          log.debug(e + " : Malformed Pattern");
        }
        finally {
          IOUtil.safeClose(bReader);
        }
      }
      
      am.cook(tagMap);
      
      return am;
    }
    // XXX do not forget to remove when new method to get charset encoding is released
    // XXX probably in BaseCachedUrl
    private Reader openForReading(CachedUrl cu) {
      try {
        return new InputStreamReader(
            cu.getUnfilteredInputStream(), Constants.ENCODING_UTF_8);
      } catch (IOException e) {
        log.error("Creating InputStreamReader for '" + cu.getUrl() + "'", e);
        throw new LockssRepository.RepositoryStateException(
            "Couldn't create InputStreamReader:" + e.toString());
      }
    }
    
    // taken from SimpleHtmlMetaTagMetadataExtractor.putValue, Jsoup has no putValue method
    protected void putValue(ArticleMetadata ret, String name, String content) {
      // filter raw HTML tags embedded within content -- publishers get sloppy
      content = HtmlUtil.stripHtmlTags(content);
      // remove character entities from content
      content = StringEscapeUtils.unescapeHtml(content);
      // normalize multiple whitespace characters to a single space character
      Matcher m = whiteSpacePat.matcher(content);
      content = m.replaceAll(" ");
      
      if (log.isDebug3()) log.debug3("Add: " + name + " = " + content);
      ret.putRaw(name, content);
    }
  }
  
}
