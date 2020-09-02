/*
 * $Id:$
 */

/*

 Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.numeriquepremium;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.plugin.clockss.aiaa.AtyponBooksXmlSchemaHelper;
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

public class NumeriquePremiumBooksMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(NumeriquePremiumBooksMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper BookHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new NumeriquePremiumSourceXmlMetadataExtractor();
  }

  public class NumeriquePremiumSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      if (BookHelper == null) {
        BookHelper = new NumeriquePremiumBooksXmlSchemaHelper();
      }
      return BookHelper;
    }

    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                            ArticleMetadata thisAM) {

      String url_string = cu.getUrl();
      List<String> returnList = new ArrayList<String>();
      if (thisAM.getRaw(NumeriquePremiumBooksXmlSchemaHelper.book_isbn) != null) {
        String pdfName = url_string.substring(0,url_string.lastIndexOf("/") + 1)
                + thisAM.getRaw(NumeriquePremiumBooksXmlSchemaHelper.book_isbn)
                + ".pdf";
        returnList.add(pdfName); // must have the pdf
      }
      return returnList;
    }

    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {

      if (thisAM.getRaw(NumeriquePremiumBooksXmlSchemaHelper.book_isbn) != null) {
        String book_isbn = thisAM.getRaw(NumeriquePremiumBooksXmlSchemaHelper.book_isbn);
        thisAM.put(MetadataField.FIELD_ACCESS_URL, thisAM.getRaw(NumeriquePremiumBooksXmlSchemaHelper.book_isbn) + ".pdf");
      }
    }
  }
}
