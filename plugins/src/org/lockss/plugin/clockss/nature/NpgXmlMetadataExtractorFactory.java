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

package org.lockss.plugin.clockss.nature;

import java.util.ArrayList;
import java.util.List;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


public class NpgXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(NpgXmlMetadataExtractorFactory.class);
  
  private static SourceXmlSchemaHelper NPGHelper = null;
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new NpgXmlMetadataExtractor();
  }

  public class NpgXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (NPGHelper != null) {
        return NPGHelper;
      }
      NPGHelper = new NpgXmlSchemaHelper();
      return NPGHelper;
    }

    
    /*
     * Some articles are abstract only and the XML itself provides the entirety 
     * of the content. So look for a PDF but if that's not there, just use 
     * the XML as proof of existence - so all records will emit
     * 
     * In 2016 all the zip files had the foo.xml at the top level of the archive.
     * If there was a PDF, it also was at the top level of the archive with the same basename.
     * 
     * With the 2017 delivery the file layout became less consistent.
     *   bonekey12345.xml (the pdf will be a sibling file of the same basename)
     *   xml/bonekey12345.xml (none of these have pdfs)
     *   xml_temp/bonekey12345.xml (pdf file will live at pdf_temp/bonekey12345.pdf)
     */
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, 
        CachedUrl cu,
        ArticleMetadata oneAM) {
      
      String url_string = cu.getUrl();
      /* use the same basename */
      String pdfFileName = url_string.substring(0,url_string.length() - 3) + "pdf";
      /* if there was a subdirectory within the archive, replace xml with pdf */
      String pdfUrl = pdfFileName.replace("zip!/xml","zip!/pdf");
      ArrayList<String> returnList = new ArrayList<String>();
      returnList.add(pdfUrl); /* the pdf file in its likely location */
      returnList.add(url_string); /* the xml file itself */
      return returnList;
    }    
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
    
      if (thisAM.get(MetadataField.FIELD_PUBLISHER) == null) {
        thisAM.put(MetadataField.FIELD_PUBLISHER,"Nature Publishing Group");
      }
      if (thisAM.get(MetadataField.FIELD_DATE) == null) {
        String copydate = thisAM.getRaw(NpgXmlSchemaHelper.NPG_copyyear);
        if (copydate != null) {
          thisAM.put(MetadataField.FIELD_DATE,copydate);
        }
      }
    }

  }
}
