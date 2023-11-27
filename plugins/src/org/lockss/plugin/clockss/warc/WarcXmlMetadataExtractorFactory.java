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

package org.lockss.plugin.clockss.warc;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.Onix3BooksSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;


/*
 * This is to handle gzips of warcfiles with an accompanying .xml file that identifies
 * the contents. The XML file is not in the archive.
 * The XML file accompanying a WARC project can take two forms:
 * 1) The XML file is one file that contains multiple "article" records where the each article
 * conforms to the JATS schema.  The enclosing "<article-set>" is our extension to allow
 * one file to describe multiple articles.
 * 2) The XML file is of the ONIX3 long form
 * We do not need to verify the existence of any content. Just emit what is defined
 * in the article set.
 */

public class WarcXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(WarcXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper WarcJatsPublishingHelper = null;
  private static SourceXmlSchemaHelper WarcOnixPublishingHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new WarcJatsPublishingSourceXmlMetadataExtractor();
  }

  public static class WarcJatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    
    /*
     * This setUpSchema shouldn't be called directly
     * WARCs can use both JATSset and ONIX3 and we need to look in the
     * doc to determine which we're handling
     * 
     */
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
    }


    // WARCs now support both JATSset and ONIX3 for books
    // look in the doc to determine which we're handling in this case
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document doc) {

      Node rootNode = doc.getFirstChild();
      log.debug3("root: " + rootNode.getNodeName());
      if ("ONIXMessage".equals(rootNode.getNodeName())) {
          log.debug3("ONIX xml");
          if (WarcOnixPublishingHelper == null) {
            WarcOnixPublishingHelper = new Onix3BooksSchemaHelper();
          }
          return WarcOnixPublishingHelper;
      } else {
        log.debug3("JATSset xml");
        if (WarcJatsPublishingHelper == null) {
          WarcJatsPublishingHelper = new WarcJatsPublishingSchemaHelper();
        }
        return WarcJatsPublishingHelper;
      }
    }
    
    /* 
     * (non-Javadoc)
     * Do not verify the existence of content. Just emit for each article/book
     * defined in the XML
     */
    @Override
    protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
      return true;
    }
    
    /*
     * (non-Javadoc)
     * WARC XML files are a little non-standard in that they store the actual access.url
     * location in the "self-uri" field for Jats and the proprietary ID field
     * for ONIX
     * set the access_url depending on the schema
     * set the publisher as well. It may get replaced by the TDB value  
     */
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      if (schemaHelper == WarcJatsPublishingHelper) {
        String self_uri = thisAM.getRaw(JatsPublishingSchemaHelper.JATS_self_uri);
        if (self_uri != null) {
          thisAM.replace(MetadataField.FIELD_ACCESS_URL, self_uri);
        }
        String raw_pub = thisAM.getRaw(JatsPublishingSchemaHelper.JATS_pubname);
        if (raw_pub != null) {
          thisAM.replace(MetadataField.FIELD_PUBLISHER, raw_pub);
        }
        if (thisAM.get(MetadataField.FIELD_DATE)== null) {
          String pubdate = thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date);
          if (pubdate != null) {
            thisAM.put(MetadataField.FIELD_DATE,pubdate);
          }
        }        
      } else if (schemaHelper == WarcOnixPublishingHelper) {
        String access = thisAM.getRaw(Onix3BooksSchemaHelper.ONIX_idtype_proprietary);
        if (access != null) {
          thisAM.replace(MetadataField.FIELD_ACCESS_URL, access);
        }
        if (thisAM.get(MetadataField.FIELD_DATE)== null) {
          String copydate = thisAM.getRaw(Onix3BooksSchemaHelper.ONIX_copy_date);
          if (copydate != null) {
            thisAM.put(MetadataField.FIELD_DATE,copydate);
          }
        }
        
      }

    }    
  
  }
  
}
