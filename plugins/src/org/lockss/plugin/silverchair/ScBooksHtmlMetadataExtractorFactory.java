package org.lockss.plugin.silverchair;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.HtmlUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;


/*
 * Metadata on book page http://ebooks.spiedigitallibrary.org/book.aspx?bookid=1608
 * in the form of:
 * citation_title:
   <span id="scm6MainContent_lblBookTitle">Field Guide to Displacement Measuring
  Interferometry</span>
   <span id="scm6MainContent_lblBookSubTitle"></span></div>
  * citation_authors (split on ;)
   <span id="scm6MainContent_lblAuthors">Author(s):&nbsp;&nbsp;&nbsp;&nbsp;Christoph U. Keller; Ramon Navarro; Bernhard R. Brandl</span>
  * citation_publication_date
   <span id="scm6MainContent_lblDate">Published:&nbsp;&nbsp;&nbsp;2014</span>
  * citation_doi
   <span id="scm6MainContent_lblDOI">DOI:&nbsp;&nbsp;&nbsp;10.1117/3.1002328</span>
  * citation_eisbn, citation_isbn (split on |)
   <span id="scm6MainContent_lblCitation">eISBN:&nbsp;9780819498007&nbsp;&nbsp;|&nbsp;&nbsp;Print&nbsp;ISBN13:&nbsp;9780819497994</span>
   <span id="scm6MainContent_lblDiscription"><p class="para"><p class="para">This <em>Field Guide
    </em>provides a practical treatment .... </p></p></span>
 */

public class ScBooksHtmlMetadataExtractorFactory extends ScHtmlMetadataExtractorFactory {

  static final String TITLE_SELECTOR = "span#scm6MainContent_lblBookTitle";
  static final String AUTHORS_SELECTOR = "span#scm6MainContent_lblAuthors";
  static final String DATE_SELECTOR = "span#scm6MainContent_lblDate";
  static final String DOI_SELECTOR = "span#scm6MainContent_lblDOI";
  static final String CITATION_SELECTOR = "span#scm6MainContent_lblCitation";
  static final String DESCRIPTION_SELECTOR = "span#scm6MainContent_lblDiscription";


  static {
    cookMap.put(TITLE_SELECTOR, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(AUTHORS_SELECTOR, MetadataField.FIELD_AUTHOR);
    cookMap.put(DATE_SELECTOR, MetadataField.FIELD_DATE);
    cookMap.put(DOI_SELECTOR,MetadataField.FIELD_DOI);
    cookMap.put(CITATION_SELECTOR,MetadataField.FIELD_EISBN);
    cookMap.put(DESCRIPTION_SELECTOR,MetadataField.DC_FIELD_DESCRIPTION);
  }

  static Logger log = Logger.getLogger(ScBooksHtmlMetadataExtractorFactory.class);
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,String contentType)
    throws PluginException {
    return new ScBooksHtmlMetadataExtractor();
  }

  public static class ScBooksHtmlMetadataExtractor extends SimpleFileMetadataExtractor {

    @Override
    public ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
      throws IOException, PluginException {
      // extract any metadata tags that might actually be there.
      ArticleMetadata am =
        new SimpleHtmlMetaTagMetadataExtractor().extract(target, cu);
      am.putRaw("extractor.type", "HTML");

      // now we check each of our fields and if they aren't found we extract them using jsoup
      if(cu.getContentSize() > 0) {
        InputStream in = cu.getUnfilteredInputStream();
        Document doc = Jsoup.parse(in, null, cu.getUrl());
        // add the missing items
        String value;
        value = extractSelector(doc, TITLE_SELECTOR,null);
        if(!StringUtil.isNullString(value)) am.putRaw(TITLE_SELECTOR, value);
        // authors
        value = extractSelector(doc,AUTHORS_SELECTOR,"Author(s):");
        if(!StringUtil.isNullString(value)) {
          String authors[] = value.split(";");
          for(String author : authors) {
            am.putRaw(MetadataField.KEY_AUTHOR, author.trim());
          }
        }
        // publication_date
        value = extractSelector(doc,DATE_SELECTOR,"Published:");
        if(!StringUtil.isNullString(value)) am.putRaw(DATE_SELECTOR,value);
        // doi
        value = extractSelector(doc, DOI_SELECTOR,"DOI:");
        if(!StringUtil.isNullString(value)) am.putRaw(DOI_SELECTOR,value);
         // citations
        value = extractSelector(doc, CITATION_SELECTOR, null);
        String[] cits = value.split("|");
        // eISBN = cits[0]
        int index = cits[0].indexOf("eISBN:");
        if(index > 0 ) {
          value = cits[0].substring(index+ "eISBN:".length());
          if(!StringUtil.isNullString(value)) am.putRaw(MetadataField.DC_KEY_IDENTIFIER_EISBN,value);
        }
        // eISBN = cits[0]
        index = cits[0].indexOf("Print ISBN13:");
        if(index > 0 ) {
          value = cits[0].substring(index + "Print ISBN13:".length()) ;
          if(!StringUtil.isNullString(value)) am.putRaw(MetadataField.DC_KEY_IDENTIFIER_ISBN,value);
        }
        // description
        value = extractSelector(doc,DESCRIPTION_SELECTOR,null);
        if(!StringUtil.isNullString(value)) am.putRaw(MetadataField.FIELD_KEY_ABSTRACT,value);
       }

      am.cook(cookMap);
      return am;
    }

    /**
     * extract a single value for a css/jquery selector and return it.
     *
     * @param doc   parsed jsoup doc
     * @param selector a selector to use in selecting the item.
     * @return  a list containing the found items
     */
    String extractSelector(Document doc, String selector, String startAfter)
    {
      String val = null;
      Elements elements = doc.select(selector);
      for(Element element : elements) {
        if(element.hasText()) {
          val = processHtml(element.text());
          if(!StringUtil.isNullString(startAfter)) {
            int pos = val.indexOf(startAfter);
            if(pos != -1) val = val.substring(pos);
          }
          if (log.isDebug3()) log.debug3("Found: "+ selector + " = " +val);
          return val;
        }
      }
      return val;
    }

    /**
     * take the value for a selector from an html page and perform the necessary
     * transformations to regularize it for storing in the article metadata.
     * this will strip embedded html selectors, unescape any escaped html and remove
     * any extra spaces.
     *
     * @param value the value
     * @return the regularized value
     */
    private String processHtml(String value) {
      value = HtmlUtil.stripHtmlTags(value);
      // remove character entities from content
      value = StringEscapeUtils.unescapeHtml4(value);
      // normalize multiple whitespaces to a single space character
      value = value.replaceAll("\\s+", " ");
      return value.trim();
    }
  }

}
