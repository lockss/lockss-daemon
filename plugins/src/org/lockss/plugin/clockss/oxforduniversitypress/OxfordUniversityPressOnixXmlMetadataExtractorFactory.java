/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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
import org.lockss.plugin.clockss.mersenne.MersenneIssueMetadataHelper;
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

        if (thisAM.getRaw(OxfordUniversityPressOnix2BooksSchemaHelper.ONIX_doi) != null) {
            thisAM.put(MetadataField.FIELD_DOI, thisAM.getRaw(OxfordUniversityPressOnix2BooksSchemaHelper.ONIX_doi));
        }
    }

  }
}
