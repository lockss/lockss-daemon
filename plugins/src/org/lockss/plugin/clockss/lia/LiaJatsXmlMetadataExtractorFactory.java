/*
 * $Id:$
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.lia;

import java.util.ArrayList;
import java.util.List;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


/*
 * If the xml is at foo/blah/Markup/VOR_10.2351_1.4893749.xml
 * then the pdf is at foo/blah/Page_Renditions/online.pdf 
 * which is the Page_Renditions sibling directory with the filename always "online.pdf" 
 */

public class LiaJatsXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(LiaJatsXmlMetadataExtractorFactory.class);
  private static final String PDF_DIR_FILE = "/Page_Renditions/online.pdf";

  private static SourceXmlSchemaHelper JatsPublishingHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new JatsPublishingSourceXmlMetadataExtractor();
  }

  public class JatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (JatsPublishingHelper == null) {
        JatsPublishingHelper = new JatsPublishingSchemaHelper();
      }
      return JatsPublishingHelper;
    }


    /* 
     * filename is always online.pdf and lives in the sibling directory names Page_Renditions
     * The XML file represented by the current cu would be something like:
     * ...76_CLOCKSS_lia_2014-08-22_233000.zip!/v26/i4/014501_1/Markup/VOR_10.2351_1.4893749.xml
     * and the pdf would be
     * ...76_CLOCKSS_lia_2014-08-22_233000.zip!/v26/i4/014501_1/Page_Renditions/online.pdf
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      String pdfPath;
      String url_string = cu.getUrl();
      int markup_dir_start = url_string.lastIndexOf("/Markup/");
      // This will leave the "/", so just add back on the sibling_dir and filename
      if (markup_dir_start < 0) {
        //can't return null because that would make it okay to emit
        // this will fail to emit, as it should - we don't know how to verify the PDF existence
        log.siteWarning("The XML file lives at an unexpected location: " + url_string);
        pdfPath = PDF_DIR_FILE;  
      }  else {
        pdfPath = url_string.substring(0, markup_dir_start) + PDF_DIR_FILE;
      }
      log.debug3("pdfPath is " + pdfPath);
      List<String> returnList = new ArrayList<String>();
      returnList.add(pdfPath);
      return returnList;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in Lia postCookProcess");
      //If we didn't get a valid date value, use the copyright year if it's there
      if (thisAM.get(MetadataField.FIELD_DATE) == null) {
        thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date));
      }
    }

  }
}
