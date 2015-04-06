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

package org.lockss.plugin.clockss.sagebaywood;

import java.util.ArrayList;
import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


/*
 * A helper to handle JATS publishing schema.
 * This provides support for the simplest case - where the pdf substance file is
 * in the same directory as, and has the same filename as the xml file.
 * eg - foo.xml maps to foo.pdf
 */

public class SageBaywoodSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(SageBaywoodSourceXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper SageBaywoodHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new SageBaywoodSourceXmlMetadataExtractor();
  }

  public class SageBaywoodSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    /*
    * This version of the method is abstract and must be implemented but should
    * be deprecated and ultimately removed in favor of the one that takes a 
    * CachedUrl
    */
    @Override
    protected SourceXmlSchemaHelper setUpSchema() {
      return null; // cause a plugin exception to get thrown
    }

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (SageBaywoodHelper == null) {
        log.debug3("setting up SB schema helper");
        SageBaywoodHelper = new SageBaywoodSchemaHelper();
      }
      return SageBaywoodHelper;
    }


    /* In this case, the filename is the same as the xml filename
     */
    @Override
    protected ArrayList<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      String url_string = cu.getUrl();
      String pdfName = url_string.substring(0,url_string.length() - 3) + "pdf";
      log.debug3("pdfName is " + pdfName);
      ArrayList<String> returnList = new ArrayList<String>();
      returnList.add(pdfName);
      return returnList;
    }

  }
}
