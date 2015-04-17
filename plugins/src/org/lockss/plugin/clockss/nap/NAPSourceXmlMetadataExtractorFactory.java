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

package org.lockss.plugin.clockss.nap;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


public class NAPSourceXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(NAPSourceXmlMetadataExtractorFactory.class);
  
  private static SourceXmlSchemaHelper NAPHelper = null;
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new NAPSourceXmlMetadataExtractor();
  }

  public class NAPSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {
    Logger log = Logger.getLogger(NAPSourceXmlMetadataExtractor.class);
    
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (NAPHelper == null) {
        NAPHelper = new NAPXmlSchemaHelper();
      }
      return NAPHelper;
    }

    
    // TODO - if we get full text XML without a matching pdf we must still emit
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      // filename is just the same a the XML filename but with .stamped.pdf 
      // instead of .xml
      String url_string = cu.getUrl();
      String pdfName = url_string.substring(0,url_string.length() - 3) + "stamped.pdf";
      log.debug3("pdfName is " + pdfName);
      List<String> returnList = new ArrayList<String>();
      returnList.add(pdfName);
      return returnList;
    }
    
    // After cooking the data, check for a date, if none was set, use the
    // alternate information
    // this is defined in the schema as 'NAP_copyyear'
    private static final String COPYRIGHT_KEY = "copyright";
    private static final String FLAT_ISBN_KEY = "flat_isbn";
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in NAPSourceXmlMetadataExtractor postEmitProcess");
      //If we didn't get a valid ISBN13, check the flat_isbn
      if (thisAM.get(MetadataField.FIELD_ISBN) == null) {
        String flat_isbn = thisAM.getRaw(FLAT_ISBN_KEY);
        // null safe - very old content might have invalid ISBN (starts with "N")
        if (!StringUtils.startsWithIgnoreCase(flat_isbn,  "N")) {
          thisAM.put(MetadataField.FIELD_ISBN, flat_isbn);
        }
      }
      //If we didn't get a valid date value, use the copyright year if it's there
      if (thisAM.get(MetadataField.FIELD_DATE) == null) {
        thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(COPYRIGHT_KEY));
      }
    }
  }
}
