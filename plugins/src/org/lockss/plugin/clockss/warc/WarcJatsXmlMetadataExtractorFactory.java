/*
 * $Id:$
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

package org.lockss.plugin.clockss.warc;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


/*
 * This is to handle gzips of warcfiles with an accompanying .xml file that identifies
 * the contents. The XML file is not in the archive.
 * The XML file is one file that contains multiple "article" records where the each article
 * conforms to the JATS schema.  The enclosing "<article-set>" is our extension to allow
 * one file to describe multiple articles.
 * We do not need to verify the existence of any content. Just emit what is defined
 * in the article set.
 */

public class WarcJatsXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(WarcJatsXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper WarcJatsPublishingHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new WarcJatsPublishingSourceXmlMetadataExtractor();
  }

  public class WarcJatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (WarcJatsPublishingHelper == null) {
        WarcJatsPublishingHelper = new WarcJatsPublishingSchemaHelper();
      }
      return WarcJatsPublishingHelper;
    }
    
    /* 
     * (non-Javadoc)
     * Do not verify the existence of content. Just emit for each article
     * defined in the article set.
     */
    @Override
    protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
      return true;
    }
    
    /*
     * (non-Javadoc)
     * WARC XML files are a little non-standard in that they store the actual access.url
     * location in the "self-uri" field
     * set the access_url to the self-uri 
     * set the publisher as well. It may get replaced by the TDB value  
     */
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
      
      String self_uri = thisAM.getRaw(JatsPublishingSchemaHelper.JATS_self_uri);
      if (self_uri != null) {
        thisAM.replace(MetadataField.FIELD_ACCESS_URL, self_uri);
      }
      String raw_pub = thisAM.getRaw(JatsPublishingSchemaHelper.JATS_pubname);
      if (raw_pub != null) {
        thisAM.replace(MetadataField.FIELD_PUBLISHER, raw_pub);
      }

    }    
    

  }
}
