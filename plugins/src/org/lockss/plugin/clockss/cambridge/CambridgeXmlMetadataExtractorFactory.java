/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.clockss.cambridge;

import java.util.ArrayList;
import java.util.List;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.plugin.clockss.XPathXmlMetadataParser;

/*
 * Cambridge back content comes with the metadata in foo.sgm files
 * Cambridge front content comes with the metadata in JATS based xml files
 * 
 * We handle both - identifying the schema based on the file suffix.
 * If sgml, we filter it to complete non-terminating tags before turning it in 
 * to a parsable Doc. 
 * 
 * 
 */


public class CambridgeXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(CambridgeXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper JatsPublishingHelper = null;
  private static SourceXmlSchemaHelper CambridgeSgmlPublishingHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new CambridgeXmlMetadataExtractor();
  }

  public class CambridgeXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    
    /*
     * (non-Javadoc)
     *Support both XML (JATS) schema and 
     *SGML (proprietary) which has been sanitized into valid XML
     */
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      String url = cu.getUrl();
      if ((url != null) && url.endsWith(".sgm")) {
        // Once you have it, just keep returning the same one. It won't change.
        if (CambridgeSgmlPublishingHelper == null) {
          CambridgeSgmlPublishingHelper = new CambridgeSgmlSchemaHelper();
        }
        return CambridgeSgmlPublishingHelper;
      } else {
      // Once you have it, just keep returning the same one. It won't change.
      if (JatsPublishingHelper == null) {
        JatsPublishingHelper = new JatsPublishingSchemaHelper();
      }
      return JatsPublishingHelper;
    }
    }
    
    @Override
    protected XPathXmlMetadataParser createXpathXmlMetadataParser() {
      // the doXmlFiltering is false, so this doesn't need an argument...
      return new CambridgeXPathXmlMetadataParser();
    }

    /* 
     * metadata filename is <article_number>h.xml or <article_number>h.sgm
     * PDF filename is <article_number>a.pdf
     * in the same directory
     * we have excluded the <article_number>w.xml files from the iterator
     * we don't care about the <article_number>a_hi.pdf which are hi-resolution pdf files
     * So far in the sample there is a 1:1 corresondence from h.xml files and a.pdf files
     * 
     * There is a special case for run-on articles (eg a column of book reviews or letters)
     * where there is one pdf for multiple sgm files.  We aren't handling this properly for now.
     * Identifying the PDF is more complicated, and in many cases we seem to be missing the pdf.
     * I need to do more investigation, but these aren't the research articles...
     * 
     */
    /* In this case, the filename is the same as the xml filename
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      String url_string = cu.getUrl();
      //remove "h.xml" and replace with "a.pdf" if there is an "h" before the .xml
      //just swap the ".xml" for "a.pdf" if there is no h
      int remove_count = 4;
      if (url_string.endsWith("h.xml")) {
    	  remove_count = 5;
      }
      String pdfName = url_string.substring(0,url_string.length() - remove_count) + "a.pdf";
      log.debug3("pdfName is " + pdfName);
      List<String> returnList = new ArrayList<String>();
      returnList.add(pdfName);
      return returnList;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {
      
      if (schemaHelper == JatsPublishingHelper) {
        log.debug3("in Cambridge postCookProcess for an XML file");
        //If we didn't get a valid date value, use the copyright year if it's there
        if (thisAM.get(MetadataField.FIELD_DATE) == null) {
          if (thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date) != null) {
            thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date));
          } else {// last chance
            thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_edate));
          }
        }
      }
    }

  }
}

