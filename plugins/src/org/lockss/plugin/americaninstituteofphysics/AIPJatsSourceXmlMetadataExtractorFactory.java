/*
 * $Id: AIPJatsSourceXmlMetadataExtractorFactory.java,v 1.1 2013-12-06 17:42:32 aishizaki Exp $
 */

/*

 Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;


public class AIPJatsSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  static Logger log = Logger.getLogger(AIPJatsSourceXmlMetadataExtractorFactory.class);

  // these are the subdirs where the xml and pdf files live, respectively
  final static String XMLDIR = "Markup/";
  final static String PDFDIR = "Page_Renditions/";
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    //log.setLevel("debug3");
    return new AIPJatsSourceXmlMetadataExtractor();
  }

  public static class AIPJatsSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected boolean preEmitCheck(CachedUrl cu, ArticleMetadata thisAM) {
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      String aipBase = null;
      ArchivalUnit B_au = cu.getArchivalUnit();
      CachedUrl fileCu;


      log.debug3("in AIPJats preEmitCheckcheck");
      /* AIPJats file structure has the xml at .../Markup/***.xml
       * and the pdf is always named .../Page_Renditions/online.pdf
       * so this pre-emit check has to check to make sure the pdf exists 
       */
      if (cuBase.endsWith(XMLDIR)) {
        int i = cuBase.lastIndexOf(XMLDIR);
        aipBase = cuBase.substring(0, i);
      }
      String filename = 
        (filenamePrefix != null ? filenamePrefix : "") + 
        (filenameKey != null ? thisAM.getRaw(filenameKey) : "");

      //Check in order for at least existing file from among the suffixes
      if (filenameSuffixList == null) {
        // just check for the one version using the other items
        fileCu = B_au.makeCachedUrl(aipBase + PDFDIR + filename);
        if(fileCu != null && (fileCu.hasContent())) {
          // Set a cooked value for an access file. Otherwise it would get set to xml file
          thisAM.put(MetadataField.FIELD_ACCESS_URL, fileCu.getUrl());
          return true;
        } else {
          log.debug3(filename + " does not exist in this AU");
          return false; //No file found to match this record
        }
      } else {
        log.debug3(filename + " does not exist in this AU");
        return false; //No files found that match this record
      }
    }
  }
}