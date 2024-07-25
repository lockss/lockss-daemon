/*
 * $Id:$
 */

/*

 Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.pages;

import java.util.ArrayList;
import java.util.List;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;

import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class PagesJatsXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(PagesJatsXmlMetadataExtractorFactory.class);

  private static SourceXmlSchemaHelper JatsPublishingHelper = null;
  private static SourceXmlSchemaHelper IssueHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new PagesPublishingSourceXmlMetadataExtractor();
  }

  public class PagesPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {
    
    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
    }

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document doc) {

      if (doc == null) return null;
      Element root = doc.getDocumentElement();
      if (root == null) return null;
      String rootName = root.getNodeName();

      log.debug3("rootName : " + rootName);
      if ("issue-xml".equals(rootName)) {
        log.debug3("getting issue helper");
        // Once you have it, just keep returning the same one. It won't change.
        if (IssueHelper == null) {
          IssueHelper = new IssueSchemaHelper();
        }
        return IssueHelper;
      } else {
        log.debug3("getting Jats helper");
        // Once you have it, just keep returning the same one. It won't change.
        if (JatsPublishingHelper == null) {
          JatsPublishingHelper = new JatsPublishingSchemaHelper();
        }
        return JatsPublishingHelper;
      }
    }


    /* 
     * filename is the same as the xml, just change the suffix 
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      // filename is just the same a the XML filename but with .pdf 
      // instead of .xml
      // TODO - the issue_xml might use a different correlation
      String url_string = cu.getUrl();
      String pdfName = url_string.substring(0,url_string.length() - 3) + "pdf";
      log.debug3("pdfName is " + pdfName);
      List<String> returnList = new ArrayList<String>();
      returnList.add(pdfName);
      return returnList;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("in Pages postCookProcess");
      if (schemaHelper == IssueHelper) {
        // This is a whole issue in one PDF file 
        //publication title - use publisher id
        // but there is an issue title so leave the title as an "article title"
        if (thisAM.get(MetadataField.FIELD_PUBLICATION_TITLE) == null) {
          if (thisAM.getRaw(IssueSchemaHelper.IX_jid_publisher) != null) {
            thisAM.put(MetadataField.FIELD_PUBLICATION_TITLE, thisAM.getRaw(IssueSchemaHelper.IX_jid_publisher));
          }
        }
      } else {
        //publication title - use publisher id
        if (thisAM.get(MetadataField.FIELD_PUBLICATION_TITLE) == null) {
          if (thisAM.getRaw(JatsPublishingSchemaHelper.JATS_jid_publisher) != null) {
            thisAM.put(MetadataField.FIELD_PUBLICATION_TITLE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_jid_publisher));
          }
        }
        //If we didn't get a valid date value, use the copyright year if it's there
        if (thisAM.get(MetadataField.FIELD_DATE) == null) {
          if (thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date) != null) {
            thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date));
          } else {// last chance
            thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_edate));
          }
        }

        log.debug3("Set article and journal type");

        thisAM.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
        thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);
      }
    }
  }
}
