/*
 * $Id$
 */

/*

 Copyright (c) 2000-2023 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.oxforduniversitypress;

import org.apache.commons.io.FilenameUtils;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.Onix2BooksSchemaHelper;
import org.lockss.plugin.clockss.Onix3BooksSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;


public class OxfordUniversityPressOnixXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(OxfordUniversityPressOnixXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper Onix2Helper = null;
 
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new OxfordUniversityPressOnixXmlMetadataExtractor();
  }

  public static class OxfordUniversityPressOnixXmlMetadataExtractor extends SourceXmlMetadataExtractor {


    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
        if (Onix2Helper == null) {
            log.debug3("in OxfordUniversityPressOnixXmlMetadataExtractorFactory setup schema");
            Onix2Helper = new OxfordUniversityPressOnix2BooksSchemaHelper();
        }

        return Onix2Helper;
    }

    @Override
    protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper,
                                     CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in OxfordUniversityPressOnixXmlMetadataExtractorFactory  preEmitCheck");

      List<String> filesToCheck;

      // If no files get returned in the list, nothing to check
      if ((filesToCheck = getFilenamesAssociatedWithRecord(schemaHelper, cu,thisAM)) == null) {
          return true;
      }
      ArchivalUnit B_au = cu.getArchivalUnit();
      CachedUrl fileCu;
      for (int i=0; i < filesToCheck.size(); i++)
      {
          fileCu = B_au.makeCachedUrl(filesToCheck.get(i));
          log.debug3("Check for existence of " + filesToCheck.get(i));
          if(fileCu != null && (fileCu.hasContent())) {
              // Set a cooked value for an access file. Otherwise it would get set to xml file
              thisAM.put(MetadataField.FIELD_ACCESS_URL, fileCu.getUrl());
              return true;
          }
      }
      log.debug3("No file exists associated with this record: " + cu.getUrl());
      return true; //No files found that match this record
  }


    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
    		CachedUrl cu, ArticleMetadata thisAM) {

    	log.debug3("setting publication type in postcook process");
    	// this is a book volume
    	thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE,MetadataField.PUBLICATION_TYPE_BOOK);
    	thisAM.put(MetadataField.FIELD_ARTICLE_TYPE,MetadataField.ARTICLE_TYPE_BOOKVOLUME);

        TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
        String publisherName = "Oxford University Press";
        String providerName = "Oxford University Press";

        if (tdbau != null) {
            publisherName = tdbau.getPublisherName();
            providerName = tdbau.getProviderName();
        }
        log.debug3("postCookProcess publisherName = " + publisherName);
        log.debug3("postCookProcess providerName in tdbau = " + providerName);

        thisAM.put(MetadataField.FIELD_PUBLISHER, publisherName);
        thisAM.put(MetadataField.FIELD_PROVIDER, providerName);
    }

  }
}
