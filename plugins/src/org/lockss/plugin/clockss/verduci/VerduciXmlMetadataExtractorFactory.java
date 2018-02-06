/*
 * $Id:$
 */

/*

 Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.verduci;

import java.util.ArrayList;
import java.util.List;

import org.lockss.util.*;
import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.PubMedSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


/*
 * If the xml is at XML/EurRevMedPharmacolSciv21i22.xml
 * then the pdf is at ../PDF/5153-5159-MiR-155 facilitates lymphoma proliferation.pdf
 */

public class VerduciXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(VerduciXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper PubMedHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new VerduciPubMedXmlMetadataExtractor();
  }

  public class VerduciPubMedXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (PubMedHelper == null) {
        PubMedHelper = new PubMedSchemaHelper();
      }
      return PubMedHelper;
    }


    /* 
     * PDF file lives in sibling directory PDF/xxx.pdf
     * The filename is supposedly startpage-endpage.pdf
     * and if there is no endpage, then startpage-startpage.pdf
     * but the articletitle part is variable relative to the value in 
     * the xml so we are requested that they send it in some other form
     * otherwise we will need to add in an iterator to find the CU.
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      // TODO - placeholder for the prototype - will not work
      // filename is just the same a the XML filename but with .pdf 
      // instead of .xml
    	//TODO - placeholder
      String url_string = cu.getUrl();
      String xmlPath = FilenameUtils.getPath(url_string);
      String spage = oneAM.get(MetadataField.FIELD_START_PAGE);
      String epage = oneAM.get(MetadataField.FIELD_END_PAGE);
      if (epage == null) {
    	  	epage = oneAM.get(MetadataField.FIELD_START_PAGE);
      }
      String pdfName = xmlPath + spage + "-" + epage;
      log.debug3("pdfName is " + pdfName);
      List<String> returnList = new ArrayList<String>();
      returnList.add(pdfName);
      return returnList;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in Verduci postCookProcess");

    }
    
  }
}
