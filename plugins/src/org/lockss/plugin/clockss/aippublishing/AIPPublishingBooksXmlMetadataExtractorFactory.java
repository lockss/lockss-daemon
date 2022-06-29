/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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
