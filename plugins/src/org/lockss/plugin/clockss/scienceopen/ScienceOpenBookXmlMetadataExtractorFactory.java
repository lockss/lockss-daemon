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

  public static class ScienceOpenBookXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (ScienceOpenBookHelper == null) {
        ScienceOpenBookHelper = new ScienceOpenBookXmlSchemaHelper();
      }

      return ScienceOpenBookHelper;
    }



    protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {
      // check if we are loking at a chapter, if so, ignore it.
      if (thisAM.getRaw(ScienceOpenBookXmlSchemaHelper.chapter_title)!=null) {
        log.debug3("found a book_chapter, not emitting");
        return false;
      }

      return super.preEmitCheck(schemaHelper, cu, thisAM);
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
      // this is definitely a book
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
      ScienceOpenMetadataUtils.fillPublisherAndProviderFromTdb(thisAM, cu);
      ScienceOpenMetadataUtils.normalizePublisher(thisAM);
    }

  }
}
