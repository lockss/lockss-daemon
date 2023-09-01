/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package edu.fcla.plugin.arkivoc;

import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;
import org.w3c.dom.Document;

public class ArkivocXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(ArkivocXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper publishingHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
                                                           String contentType)
      throws PluginException {
    return new ArkivocPublishingSourceXmlMetadataExtractor();
  }

  public static class ArkivocPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    /*
     * This setUpSchema shouldn't be called directly
     * but for safety, just use the CU to figure out which schema to use.
     *
     */
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
    }

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document xmlDoc) {
      if (publishingHelper == null) {
        log.debug3("Setup ArkivocXmlSchemaHelper");
        publishingHelper = new ArkivocXmlSchemaHelper();

      }
      return publishingHelper;
    }

    @Override
    protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {
      log.debug3("ArkivocXmlMetadataExtractor: Do not check PDF");
      return true; //Always return true, since pdf file-name is a random number,
    }

    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {

      String publisherName = "ARKIVOC";

      TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
      if (tdbau != null) {
        publisherName =  tdbau.getPublisherName();
      }

      thisAM.put(MetadataField.FIELD_PUBLISHER, publisherName);
      thisAM.put(MetadataField.FIELD_PROVIDER, publisherName);

      String recordID = thisAM.getRaw(ArkivocXmlSchemaHelper.RECORD_ID);
      String accessUrl = thisAM.get(MetadataField.FIELD_ACCESS_URL);

      log.debug3("access_url: recordID = " + recordID);
      log.debug3("access_url: already_set_access_url = " + accessUrl);

      if (recordID  != null) {
        String updatedAccessUrl = UrlUtil.minimallyEncodeUrl(cu.getUrl() +  "&unique_record_id=" + recordID);
        thisAM.replace(MetadataField.FIELD_ACCESS_URL, updatedAccessUrl);
        log.debug3("access_url: reset updated_access_url = " + updatedAccessUrl);
      }
    }
  }
}
