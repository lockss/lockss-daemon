/*
 * $Id$
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

package org.lockss.plugin.americaninstituteofphysics;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


public class AIPJatsSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(AIPJatsSourceXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper AIPJatsHelper = null;


  // these are the subdirs where the xml and pdf files live, respectively
  final static String XMLDIR = "Markup/";
  final static String PDFDIR = "Page_Renditions/";
  final static String PDFFILE = "online.pdf";
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new AIPJatsSourceXmlMetadataExtractor();
  }

  public static class AIPJatsSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      String aipBase = null;
      ArchivalUnit B_au = cu.getArchivalUnit();
      CachedUrl fileCu;


      log.debug3("in AIPJats preEmitCheck");
      // hardcoded to be online.pdf

      // no pre-emit check required if values are all null
      // we know for AIPJats (filenameSuffixList, filenameKey) will be null

      /* AIPJats file structure has the xml at .../Markup/***.xml
       * and the pdf is always named .../Page_Renditions/online.pdf
       * so this pre-emit check has to check to make sure the pdf exists 
       */
      if (cuBase.endsWith(XMLDIR)) {
        int i = cuBase.lastIndexOf(XMLDIR);
        aipBase = cuBase.substring(0, i);
      }
      if (aipBase == null) {
        log.debug3(cuBase + ": non standard location for XML");
        return false;
      }

      //Check in order for at least existing file from among the suffixes
      // just check for the one version using the other items
      fileCu = B_au.makeCachedUrl(aipBase + PDFDIR + PDFFILE);
      if(fileCu != null && (fileCu.hasContent())) {
        // Set a cooked value for an access file. Otherwise it would get set to xml file
        thisAM.put(MetadataField.FIELD_ACCESS_URL, fileCu.getUrl());
        return true;
      } else {
        log.debug3(aipBase+PDFDIR+PDFFILE + " does not exist in this AU");
        return false; //No file found to match this record
      }
    }

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (AIPJatsHelper != null) {
        return AIPJatsHelper;
      }
      AIPJatsHelper = new AIPJatsXmlSchemaHelper();
      return AIPJatsHelper;
    }
  }
}