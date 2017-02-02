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
