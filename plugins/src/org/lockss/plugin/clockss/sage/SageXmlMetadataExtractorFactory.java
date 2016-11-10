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

package org.lockss.plugin.clockss.sage;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;



public class SageXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(SageXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper SageBooksHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new SageBooksPublishingSourceXmlMetadataExtractor();
  }

  public class SageBooksPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {

      if (SageBooksHelper == null) {
        SageBooksHelper = new SageBooksXmlSchemaHelper();
      }
      return SageBooksHelper;
    }

     


    /* In this case, the filename is the same as the xml filename
     * or, if that's not there could be the print isbn
     * but it might have an _v# after it. 
     * worst case use this very xml file - because it's full text
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      String url_string = cu.getUrl();
      String cuBase = FilenameUtils.getFullPath(url_string);
      String cuFileName = FilenameUtils.getBaseName(url_string);

      String pdf_samename = cuBase + "pdf/" + cuFileName + ".pdf";
      String pdf_isbn = cuBase + "pdf/" + oneAM.getRaw(helper.getFilenameXPathKey());
      List<String> returnList = new ArrayList<String>();
      log.debug3("looking for: " + pdf_samename + "," + pdf_isbn + ".pdf," + pdf_isbn + "_v1.pdf," + url_string);
      returnList.add(pdf_samename);
      returnList.add(pdf_isbn + ".pdf");
      returnList.add(pdf_isbn + "_v1.pdf");
      returnList.add(url_string); // fallback - just use this file
      return returnList;
    }
    

  }
}
