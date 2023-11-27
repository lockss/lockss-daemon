/*
 * $Id$
 */

/*

 Copyright (c) 2000-2023 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.intechopen;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.CrossRefQuerySchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

import java.util.ArrayList;
import java.util.List;


public class IntechOpenJournalsXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(IntechOpenJournalsXmlMetadataExtractorFactory.class);

    private static SourceXmlSchemaHelper CrossRefHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new IntechOpenXmlMetadataExtractor();
  }

  public class IntechOpenXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
        if (CrossRefHelper == null) {
            CrossRefHelper = new IntechOpenCrossRefQuerySchemaHelper();
        }

        log.debug3("Setup CrossRefHelper...");

        return CrossRefHelper;
    }

      @Override
      protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                              ArticleMetadata oneAM) {

          String url_string = cu.getUrl();
          String pdfName = url_string.substring(0,url_string.length() - 3) + "pdf";
          log.debug3("pdfName is " + pdfName);
          List<String> returnList = new ArrayList<String>();
          returnList.add(pdfName);
          return returnList;
      }
  }
}
