package org.lockss.plugin.silverchair;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.HtmlUtil;
import org.lockss.util.IOUtil;
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


  protected static final MultiMap cookMap = new MultiValueMap(); // see static initializer
  static {
    cookMap.put("citation_title", MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put("citation_author", MetadataField.FIELD_AUTHOR);
    cookMap.put("citation_date", MetadataField.FIELD_DATE);
    cookMap.put("citation_doi",MetadataField.FIELD_DOI);
    cookMap.put("citation_isbn",MetadataField.FIELD_ISBN);
    cookMap.put("citation_eisbn",MetadataField.FIELD_EISBN);
    cookMap.put("citation_abstract",MetadataField.FIELD_ABSTRACT);
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
        InputStream in = null;
        try {
          // try-finally block to ensure closure of input stream
          in = cu.getUnfilteredInputStream();
          Document doc = Jsoup.parse(in, null, cu.getUrl());
          if(log.isDebug3()) log.debug3("begin parse...");
          // add the missing items
          String value;
          value = extractSelector(doc, TITLE_SELECTOR,null);
          if(!StringUtil.isNullString(value)) am.putRaw("citation_title", value);
          // authors
          value = extractSelector(doc,AUTHORS_SELECTOR,"Author(s):");
          if(!StringUtil.isNullString(value)) {

            Vector<String> authors = StringUtil.breakAt(value,";",0,true,true);
            for(String author : authors) {
              am.putRaw("citation_author", author);
              if(log.isDebug3()) log.debug3("added author: " + author);
            }
          }
          // publication_date
          value = extractSelector(doc,DATE_SELECTOR,"Published:");
          if(!StringUtil.isNullString(value)) {
            am.putRaw("citation_date",value);
            if(log.isDebug3()) log.debug3("added date: " + value);
          }
          // doi
          value = extractSelector(doc, DOI_SELECTOR,"DOI:");
          if(!StringUtil.isNullString(value)) {
            am.putRaw("citation_doi",value);
            if(log.isDebug3()) log.debug3("added doi:" + value);
          }
          // citations
          value = extractSelector(doc, CITATION_SELECTOR, null);
          Vector<String> cits = StringUtil.breakAt(value,"|",0,false,true);
          for(String cit_value: cits) {
            int idx;
            // eISBN
            if (cit_value.contains("eISBN:")) {
              idx = cit_value.indexOf("eISBN:");
              if (idx >= 0) {
                value = cit_value.substring(idx + "eISBN:".length()).trim();
                if (!StringUtil.isNullString(value)) {
                  am.putRaw("citation_eisbn", value);
                  if(log.isDebug3()) log.debug3("added eisbn: "+value);
                }
              }
            }
            else if (cit_value.contains("ISBN13:")) {
              // ISBN = cits[1]
              idx = cit_value.indexOf("ISBN13:");
              if (idx >= 0) {
                value = cit_value.substring(idx + "ISBN13:".length()).trim();
                if (!StringUtil.isNullString(value)) {
                  am.putRaw("citation_isbn", value);
                  if(log.isDebug3()) log.debug3("added isbn: "+value);
                }
              }
            }
          }
          // description
          value = extractSelector(doc,DESCRIPTION_SELECTOR,null);
          if(!StringUtil.isNullString(value)) {
            am.putRaw("citation_abstract", value);
            if(log.isDebug3()) log.debug3("added abstract: "+ value);
          }
        }

        finally {
          IOUtil.safeClose(in);
        }
        //foo
      }
      am.cook(cookMap);

      TdbAu tdbau = cu.getArchivalUnit().getTdbAu(); // returns null if titleConfig is null
      if (tdbau != null) {
        if (am.get(MetadataField.FIELD_PUBLISHER) == null) {
          // get the publishger from the tdb file.  This would be the most accurate
          String publisher = tdbau.getPublisherName();
          if (publisher != null) {
            am.put(MetadataField.FIELD_PUBLISHER, publisher);
            if(log.isDebug3()) log.debug3("added publisher: "+ publisher);
          }
        }
        if (am.get(MetadataField.FIELD_PUBLICATION_TITLE) == null) {
          String book_title = tdbau.getPublicationTitle();
          if (book_title != null) {
            am.put(MetadataField.FIELD_PUBLICATION_TITLE, book_title);
            if(log.isDebug3()) log.debug3("added title: "+ book_title);
          }
        }
        if (am.get(MetadataField.FIELD_EISBN) == null) {
          String eisbn = tdbau.getEisbn();
          if (eisbn != null) {
            am.put(MetadataField.FIELD_EISBN, eisbn);
            if(log.isDebug3()) log.debug3("added eisbn: "+ eisbn);

          }
        }
        if (am.get(MetadataField.FIELD_ISBN) == null) {
          String isbn = tdbau.getIsbn();
          if (isbn != null) {
            am.put(MetadataField.FIELD_ISBN, isbn);
            if(log.isDebug3()) log.debug3("added isbn: "+ isbn);
          }
        }

      }
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
            if(pos != -1) val = val.substring(pos + startAfter.length());
          }
          if(log.isDebug3()) log.debug3("Found "+ selector + ": " +val);
          return val.trim();
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
      String ret_value = value;
      ret_value = ret_value.replace("\u00A0","");
      ret_value = HtmlUtil.stripHtmlTags(ret_value);
      // remove character entities from content
      ret_value = StringEscapeUtils.unescapeHtml4(ret_value);
      // normalize multiple whitespaces to a single space character
      ret_value = ret_value.replaceAll("\\s+", " ").trim();
      return ret_value;
    }
  }

}
