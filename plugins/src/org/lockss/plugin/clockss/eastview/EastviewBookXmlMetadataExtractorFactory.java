/*
 * $Id$
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.eastview;

import org.apache.commons.io.FilenameUtils;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class EastviewBookXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(EastviewBookXmlMetadataExtractorFactory.class);
  private static SourceXmlSchemaHelper EastviewBookXmlHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new EastviewBookXmlMarcXmlMetadataExtractor();
  }

  public static class EastviewBookXmlMarcXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
    // Once you have it, just keep returning the same one. It won't change.
      if (EastviewBookXmlHelper == null) {
        EastviewBookXmlHelper = new EastviewMarcXmlSchemaHelper();
      }
      return EastviewBookXmlHelper;
    }
    
    @Override
    protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {
      return true;
    }

    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {


      if (thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_pdf) != null) {

        String zippedFolderName = EastviewMarcXmlSchemaHelper.getZippedFolderName();

        String fileNum = thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_pdf);
        String cuBase = FilenameUtils.getFullPath(cu.getUrl());

        String pdfFilePath = cuBase + zippedFolderName + ".zip!/" + fileNum + ".pdf";
        log.debug3("pdfFilePath" + pdfFilePath );
        thisAM.put(MetadataField.FIELD_ACCESS_URL, pdfFilePath);
      }

      if (thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_author) != null) {
        String author = thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_author);
        thisAM.put(MetadataField.FIELD_AUTHOR, author.replace(".", ""));
      }

      if (thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_publisher) != null) {
        thisAM.put(MetadataField.FIELD_PUBLISHER, thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_publisher));
      }

      if (thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_pub_date) != null) {
        String MARC_pub_date = thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_pub_date);
        thisAM.put(MetadataField.FIELD_DATE, MARC_pub_date.replace(".", ""));
      } else {
        if (thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_pub_date2) != null) {
          String MARC_pub_date = thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_pub_date2);
          thisAM.put(MetadataField.FIELD_DATE, MARC_pub_date.replace(".", ""));
        }
      }

      String publisherName = "EastView";

      TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
      if (tdbau != null) {
        publisherName = tdbau.getPublisherName();
      }

      thisAM.put(MetadataField.FIELD_PUBLISHER, publisherName);

      thisAM.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKVOLUME);
      thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_BOOK);

      String articleTitle = thisAM.getRaw(EastviewMarcXmlSchemaHelper.MARC_title);
      String cleanedArticleTitle = articleTitle.replace(":", "").
              replace("/", "").
              replace("=", "").
              replace("\"", "").
              replace("...", "");
      log.debug(String.format("original artitle title = %s, cleaned title = %s",articleTitle, cleanedArticleTitle));
      thisAM.put(MetadataField.FIELD_ARTICLE_TITLE, cleanedArticleTitle);
    }

  }
}
