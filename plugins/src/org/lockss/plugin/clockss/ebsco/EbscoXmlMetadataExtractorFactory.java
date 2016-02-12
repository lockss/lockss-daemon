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

package org.lockss.plugin.clockss.ebsco;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


/*
 * The xml files live at 
 * <base>/<year>/DataFeed/EBSCOhostGKB_20160205_DELTA.zip!/EBSCOhostGKB_20160205_DELTA.xml
 * The corresponding content files live at:
 * <base>/<year>/Content/<ProductID>.(pdf|epub)
 * Note that the substance files are not in an archive.
 * Only the XML files are zipped.
 */

public class EbscoXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(EbscoXmlMetadataExtractorFactory.class);
  private static final String CONTENT_DIR = "/Content/";
  private static SourceXmlSchemaHelper EbscoSchemaHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new EbscoSourceXmlMetadataExtractor();
  }

  public class EbscoSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (EbscoSchemaHelper == null) {
        EbscoSchemaHelper = new EbscoSchemaHelper();
      }
      return EbscoSchemaHelper;
    }


    /* 
     * The filename is the ProducID with either ".pdf" or ".epub" suffix.
     * Tje content files live in a parallel directory 
     *     <base>/<year>/Content/
     * The XML file represented by the current cu would be something like:
     *   <base>/<year>/DataFeed/EBSCOhostGKB_20160205_DELTA.zip!/EBSCOhostGKB_20160205_DELTA.xml
     * and the pdf would be
     *   <base>/<year>/Content/123456.pdf
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      // this has been set to be the "ProductID" value
      String filenameValue = oneAM.getRaw(helper.getFilenameXPathKey());
      
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      int datafeed_dir_start = cuBase.lastIndexOf("/DataFeed/");
      // This will leave the "/", so just add back on the sibling_dir and filename
      String contentPath;
      if (datafeed_dir_start < 0) {
        //can't return null because that would make it okay to emit
        // this will fail to emit, as it should - we don't know how to verify the PDF existence
        log.siteWarning("The XML file lives at an unexpected location: " + cuBase);
        contentPath = CONTENT_DIR; //invalid but will force failure
      } else { 
        contentPath = cuBase.substring(0, datafeed_dir_start) + CONTENT_DIR;
      }
      List<String> returnList = new ArrayList<String>();
      returnList.add(contentPath + filenameValue + ".pdf");
      returnList.add(contentPath + filenameValue + ".epub");
      return returnList;
    }
  }
    
}
