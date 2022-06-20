/*
 * $Id$
 */

/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.aippublishing;

import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.Onix3BooksSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

import java.util.ArrayList;
import java.util.List;


public class AIPPublishingBooksXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(AIPPublishingBooksXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper SchemaHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new APIPublishingXmlMetadataExtractor();
  }

  public class APIPublishingXmlMetadataExtractor extends SourceXmlMetadataExtractor {
    private static final String SCHEMA_PATH = ".onix.xml";

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      log.debug3("AIP Publishing schema file path: " + cu.getUrl());
      if ((cu.getUrl()).contains(SCHEMA_PATH)) {
        if (SchemaHelper == null) {
          log.debug3("AIP Publishing schema: " + cu.getUrl());
          //SchemaHelper = new Onix3BooksSchemaHelper();
          SchemaHelper = new AIPPublishingOnix3BooksSchemaHelper();
        } else {
          log.debug3("AIP Publishing schema existing: " + cu.getUrl());
        }
      }

      return SchemaHelper;
    }


    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      String url_string = cu.getUrl();
      List<String> returnList = new ArrayList<String>();
      if (helper == SchemaHelper) {
        String filenameValue = oneAM.getRaw(Onix3BooksSchemaHelper.ONIX_RR);
        String cuBase = FilenameUtils.getFullPath(url_string);
        String fullPathFile = null;
        if (filenameValue.contains("onix")) {
          fullPathFile = cuBase + filenameValue.replace("aipp.onix.", "" ) + ".pdf";
        }
        returnList.add(fullPathFile);
      }
      return returnList;        
    }


    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in AIP Publishing postCookProcess");

      String doi = thisAM.getRaw(AIPPublishingOnix3BooksSchemaHelper.ONIX_website_url);

      log.debug3("in AIP Publishing postCookProcess, doi = " + doi);

      if (doi != null) {
        thisAM.put(MetadataField.FIELD_DOI,doi.replaceFirst("https://aip.scitation.org/doi/book/", ""));
    }

      thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE,MetadataField.PUBLICATION_TYPE_BOOK);
      thisAM.put(MetadataField.FIELD_ARTICLE_TYPE,MetadataField.ARTICLE_TYPE_BOOKVOLUME);
    }
  }
}
