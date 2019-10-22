/*
 * $Id$
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

package org.lockss.plugin.clockss.casalini;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;

public class CasaliniLibriMarcXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(CasaliniLibriMarcXmlMetadataExtractorFactory.class);
  
  private static SourceXmlSchemaHelper CasaliniHelper = null;
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new CasaliniMarcXmlMetadataExtractor();
  }

  public class CasaliniMarcXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // They upload two xml source files, and we are only interested in article related xml for metadata
      // psicoterapia_scienze_umane_issues_20191014.xml
      // psicoterapia_scienze_umane_articles_20191014.xml
      if  (CasaliniHelper == null && cu.getUrl().indexOf("articles") > -1) {
        CasaliniHelper = new CasaliniMarcXmlSchemaHelper();
      }
      return CasaliniHelper;
    }

    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, 
        CachedUrl cu,
        ArticleMetadata oneAM) {

      String yearNum = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_YEAR).replace(".", "");
      String volumeNum = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_VOLUME).replace("0", "");
      String fileNum = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_file);

      log.debug3("Building PDF file with  filename - " + fileNum +  ", volume - " + volumeNum + ", year - " + yearNum);

      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      ArrayList<String> returnList = new ArrayList<String>();
      String pdfFilePath = cuBase + yearNum + "_" + volumeNum + "_" + fileNum +  ".pdf";

      returnList.add(pdfFilePath);
      return returnList;
    }
  }
}
