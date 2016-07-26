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

package org.lockss.plugin.clockss.phildoc;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


public class PhilDocXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(PhilDocXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper PhilDocHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new PhilDocXmlMetadataExtractor();
  }

  public class PhilDocXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (PhilDocHelper == null) {
        PhilDocHelper = new PhilDocSchemaHelper();
      }
      return PhilDocHelper;
    }


    
    // The sample used pdf subdir but the later deposit had the pdf in the same
    // directory as the XML file - try both
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, 
        CachedUrl cu,
        ArticleMetadata oneAM) {
      
      String filenameValue = oneAM.getRaw(helper.getFilenameXPathKey());
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      ArrayList<String> returnList = new ArrayList<String>();
      log.debug3("looking for filename of: " + cuBase + "pdf/" + filenameValue + ".pdf");
      returnList.add(cuBase + filenameValue + ".pdf");
      returnList.add(cuBase + "pdf/" + filenameValue + ".pdf");
      return returnList;
    }    
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
    
      thisAM.put(MetadataField.FIELD_PUBLISHER,"Philosophy Documentation Center");
      // Now build up a full title if that is necessary
      String subT = thisAM.getRaw(PhilDocSchemaHelper.art_subtitle);  
      if (subT != null) {
        StringBuilder title_br = new StringBuilder(thisAM.get(MetadataField.FIELD_ARTICLE_TITLE));
        title_br.append(": ");
        title_br.append(subT);
        thisAM.replace(MetadataField.FIELD_ARTICLE_TITLE,  title_br.toString());
      }
      log.debug3("in PhilDoc postCookProcess");
    }

  }
}

