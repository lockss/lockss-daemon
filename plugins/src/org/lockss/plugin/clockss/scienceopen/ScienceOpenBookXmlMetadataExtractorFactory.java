/*

 Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.scienceopen;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.lockss.config.TdbAu;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Each zip delivers all the articles associated with a particular conference
 * The zip has a "book" level xml file that contains publication information, including ISBN if
 * it's available.  It contains time and title information about each talk (article)
 * Each article is in its own subdirectory by DOI and contains a PDf and XML with the same
 * name. The XML contains article level information and the publication title but not the
 * publication ISBN nor necessarily a publication date.
 *
 * We are going to emit for all items, including the BOOK_VOLUME even though it doesn't have a PDF.
 * The idea is that the book_volume item and the book_chapter items will coalesce under the same
 * publication item and therefore get associated with the isbn when it is available.
 *
 */

public class ScienceOpenBookXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(ScienceOpenBookXmlMetadataExtractorFactory.class);
  //example: .....foo.zip!/miecec13/6.2013-4027/6.2013-4027.pdf
  private static final Pattern DATEPAT = Pattern.compile("/[^/.]+\\.([0-9]{4})-[^/.-]+\\.pdf$", Pattern.CASE_INSENSITIVE);


  // currently only for meeting papers which is book format
  private static SourceXmlSchemaHelper ScienceOpenBookHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new ScienceOpenBookXmlMetadataExtractor();
  }

  public class ScienceOpenBookXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (ScienceOpenBookHelper == null) {
        ScienceOpenBookHelper = new ScienceOpenBookXmlSchemaHelper();
      }
      return ScienceOpenBookHelper;
    }



    /*
     * There are two types of XML files
     * Each meeting has a top level XML that encompasses the entire conference.
     * This is the XML file with the publisher, ISBN and date if either are available - publication level
     * The other XML files are per-article, equivalent of chapter level with author information
     * We process both types but only check for content.pdf file when looking at chapter info
     * In this case, the filename is the same as the xml filename
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                            ArticleMetadata oneAM) {

      String url_string = cu.getUrl();
      List<String> returnList = new ArrayList<String>();
      // hard to really know...could check pattern of url path... for now do this
      if( (oneAM.getRaw(ScienceOpenBookXmlSchemaHelper.chapter_pdf) == null )
          && (oneAM.getRaw(ScienceOpenBookXmlSchemaHelper.chapter_title) == null)){
        returnList.add(url_string); // this xml is sufficient
      } else {
        // look for the pdf
        String pdfName = url_string.substring(0,url_string.length() - 3) + "pdf";
        log.debug3("pdfName is " + pdfName);
        returnList.add(pdfName); // must have the pdf
      }
      return returnList;
    }

    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in AIAA postCookProcess");
      //If we didn't get a valid date value, use the copyright year if it's there
      if (thisAM.get(MetadataField.FIELD_DATE) == null) {
        if (thisAM.getRaw(ScienceOpenBookXmlSchemaHelper.chapter_copyright_year) != null) {
          thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(ScienceOpenBookXmlSchemaHelper.chapter_copyright_year));
        } else {
          // fallback if there was a PDF - extract the year from the pdf name
          Matcher dateMat = DATEPAT.matcher(thisAM.get(MetadataField.FIELD_ACCESS_URL));
          if (dateMat.find()) {
            thisAM.put(MetadataField.FIELD_DATE, dateMat.group(1));
          }
        }
      }
      // this is definnitely a book
      thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE,MetadataField.PUBLICATION_TYPE_BOOK);
      if (thisAM.get(MetadataField.FIELD_ACCESS_URL).endsWith(".pdf")) {
        // but only a chapter if we have content associated with it
        thisAM.put(MetadataField.FIELD_ARTICLE_TYPE,MetadataField.ARTICLE_TYPE_BOOKCHAPTER);
      } else {
        // this is publication level information only
        // only set the book-level doi if we're emitting for the book volume and not a chapter
        // we don't have a PDF but we have an XML listing of the contents we could use in the
        // even of a trigger so that will be the access_url for this
        thisAM.put(MetadataField.FIELD_ARTICLE_TYPE,MetadataField.ARTICLE_TYPE_BOOKVOLUME);
        thisAM.put(MetadataField.FIELD_ARTICLE_TITLE,thisAM.get(MetadataField.FIELD_PUBLICATION_TITLE));
        if (thisAM.getRaw(ScienceOpenBookXmlSchemaHelper.book_doi) != null) {
          thisAM.put(MetadataField.FIELD_DOI,thisAM.getRaw(ScienceOpenBookXmlSchemaHelper.book_doi));
        }
      }
      // Fill in Publisher Name from TDB, with this fall back for safety.
      String publisherName = "Carl Grossman";
      TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
      if (tdbau != null) {
        publisherName =  tdbau.getPublisherName();
      }
      thisAM.put(MetadataField.FIELD_PUBLISHER, publisherName);
    }

  }
}
