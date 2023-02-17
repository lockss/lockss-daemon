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

package org.lockss.plugin.clockss.arl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.JatsSetSchemaHelper;
import org.lockss.plugin.clockss.Onix3BooksSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


/*
 * This is to handle XMLS found in the manually generated ZIP file for
 * Association of Research Libraries content
 * The PDFs were downloaded and saved.  Depending on the type their metadata was
 * stored in a JATS set (similar to what we use for WARC files) or an ONIX xml file
 * This extractor will handle both.
 */

public class ARLXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(ARLXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper ArlJatsSetHelper = null;
  private static SourceXmlSchemaHelper ArlOnixHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new ArlXmlSourceMetadataExtractor();
  }

  public class ArlXmlSourceMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      String url = cu.getUrl();
      if ((url!=null) && url.contains("onix3")) {
        if (ArlOnixHelper == null) {
          ArlOnixHelper = new Onix3BooksSchemaHelper();
        }
        return ArlOnixHelper;

      } else { 
        if (ArlJatsSetHelper == null) {
          ArlJatsSetHelper = new JatsSetSchemaHelper();
        }
        return ArlJatsSetHelper;
      }
    }

    
    /*
     * (non-Javadoc)
     * Depends a little bit on which schema
     * If this is onix, use the record_reference
     * If it's JATS, who the heck knows...
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      String filenameValue;
      if (helper == ArlOnixHelper) {
         filenameValue = oneAM.getRaw(Onix3BooksSchemaHelper.ONIX_RR) + ".pdf";
      } else {
        // JatsSet - this has a pdf on it already
        filenameValue = oneAM.getRaw(JatsPublishingSchemaHelper.JATS_self_uri);
      }
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      String fullPathFile = cuBase +filenameValue;
      List<String> returnList = new ArrayList<String>();
      returnList.add(fullPathFile);
      return returnList;
    }
    
    
    /*
     * (non-Javadoc)
     * If there was a chapter title set then this is a book chapter, not a whole book 
     */
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
      
      // for books, if there was an article title (Chapter) then this is a book
      // chapter, not a whole book
      if (schemaHelper == ArlOnixHelper) {
        String chapter_title = thisAM.get(MetadataField.FIELD_ARTICLE_TITLE);
        if (chapter_title != null){
          thisAM.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKCHAPTER);
        } else {
          thisAM.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKVOLUME);
        }
      } else if (schemaHelper == ArlJatsSetHelper) {
        //use alternate date format to fill in the field 
        if (thisAM.get(MetadataField.FIELD_DATE) == null) {
          if (thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date) != null) {
            thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date));
          } else {// last chance
            thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_edate));
          }
        }
      }
    }    

  }
}
