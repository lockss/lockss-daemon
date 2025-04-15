/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.nper;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;


public class NPERXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(NPERXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper  schemaHelper = null;
  private static final int TITLE_STRING_LEN = 50;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new NPERXmlMetadataExtractor();
  }

  public static class NPERXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
    // Once you have it, just keep returning the same one. It won't change.
      if ( schemaHelper != null) {
        return  schemaHelper;
      }
      schemaHelper = new NPERXmlSchemaHelper();
      return  schemaHelper;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
    		CachedUrl cu, ArticleMetadata thisAM) {


      String customAccessUrl = cu.getUrl();
      String articleTitle = thisAM.get(MetadataField.FIELD_ARTICLE_TITLE);
      String articleEndPage = thisAM.get(MetadataField.FIELD_END_PAGE);

      //in 2021 folder, there are two articles with the same startpage and endpage,
      //so need to append partical article title to make access.url unique
      if (articleTitle != null) {
        if (articleTitle.length() >= TITLE_STRING_LEN) {
          try {
            customAccessUrl = customAccessUrl + "&article_title="
                    + URLEncoder.encode(articleTitle.toLowerCase().substring(0, TITLE_STRING_LEN), "UTF-8");
          } catch (UnsupportedEncodingException e) {
            log.warning("UnsupportedEncodingException", e);
          }
        } else {
          try {
            customAccessUrl = customAccessUrl + "&article_title="
                    + URLEncoder.encode(articleTitle.toLowerCase() , "UTF-8");
          } catch (UnsupportedEncodingException e) {
            log.warning("UnsupportedEncodingException", e);
          }
        }
      }

      if (articleEndPage != null) {
        customAccessUrl = customAccessUrl + "&article_endpage=" + URLEncoder.encode(articleEndPage);
      }

      log.debug3("customAccessUrl  = " + customAccessUrl );

      thisAM.replace(MetadataField.FIELD_ACCESS_URL, customAccessUrl);


      thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);
      thisAM.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
      thisAM.put(MetadataField.FIELD_JOURNAL_TITLE, "Nonpartisan Education Review");
    }
    
  }
}
