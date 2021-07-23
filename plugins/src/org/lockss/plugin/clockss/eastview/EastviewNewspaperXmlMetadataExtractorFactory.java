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

package org.lockss.plugin.clockss.eastview;

import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EastviewNewspaperXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(EastviewNewspaperXmlMetadataExtractorFactory.class);
  public static Map<String, String> issnMap = new HashMap<>();

  static {
    try {
      issnMap = new EastviewNewspaperTitleISSNMappingHelper().getISSNList();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static SourceXmlSchemaHelper EastviewHelper = null;

  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new EastviewXmlMetadataExtractor();
  }

  public class EastviewXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // Once you have it, just keep returning the same one. It won't change.
      if (EastviewHelper == null) {
        EastviewHelper = new EastviewNewspaperMetadataHelper();
      }
      return EastviewHelper;
    }


    /* 
     * a PDF file may or may not exist, but assume the XML is full text
     * when it does not
     */
    /*
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      // filename is just the same a the XML filename but with .pdf 
      // instead of .xml
      String url_string = cu.getUrl();
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      String rawPDFPath = oneAM.getRaw(EastviewNewspaperMetadataHelper.PAGE_PDF_PATH);
      String fullPathFile = cuBase + rawPDFPath;
      log.debug3("Eastview Newspaper: getFilenamesAssociatedWithRecord pdfName is " + fullPathFile);
      List<String> returnList = new ArrayList<String>();
      returnList.add(fullPathFile);
      return returnList;
    }
     */

    /*
     * a PDF file may or may not exist, but assume the XML is full text
     * when it does not
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                            ArticleMetadata oneAM) {

      // filename is just the same a the XML filename but with .pdf
      // instead of .xml
      String url_string = cu.getUrl();
      String pdfName = url_string.substring(0,url_string.length() - 3) + "pdf";
      log.debug3("pdfName is " + pdfName);
      List<String> returnList = new ArrayList<String>();
      returnList.add(pdfName);
      returnList.add(url_string); // xml file
      return returnList;
    }

    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      String raw_title = thisAM.getRaw(EastviewSchemaHelper.ART_RAW_TITLE);
      log.debug3(String.format("Eastview Newspaper: metadata raw title parsed: %s", raw_title));

      Pattern pattern =  Pattern.compile("\\d\\d-\\d\\d-\\d\\d\\d\\d\\(([^)]+)-([^(]+)\\)\\s+(.*)");

      Matcher m = pattern.matcher(raw_title);

      String publisher_shortcut = null;
      String publisher_mapped = null;
      String volume = null;
      String title = null;

      if(m.matches()){
        publisher_shortcut = m.group(1).trim();
        publisher_mapped = EastViewPublisherNameMappingHelper.canonical.get(publisher_shortcut);
        volume = m.group(2);
        title = m.group(2);
      }

      log.debug3(String.format("Eastview Newspaper: metadata raw title parsed = %s | " +
                      "publisher_shortcut = %s | publisher_mapped = %s | volume = %s | title = %s",
              raw_title,
              publisher_shortcut,
              publisher_mapped,
              volume,
              title));

      if (publisher_mapped != null) {
        thisAM.put(MetadataField.FIELD_PUBLISHER, publisher_mapped);
      }  else {
        log.debug3(String.format("Eastview Newspaper: metadata raw title parsed = %s | " +
                        "publisher_shortcut = %s | Null publisher_mapped = %s | volume = %s | title = %s",
                raw_title,
                publisher_shortcut,
                publisher_mapped,
                volume,
                title));
      }

      /*
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());
      String rawPDFPath = thisAM.getRaw(EastviewNewspaperMetadataHelper.PAGE_PDF_PATH);
      String fullPathFile = cuBase + rawPDFPath;
      log.debug3("Eastview Newspaper: rawPDFPath = " + rawPDFPath + ", fullPathFile = " + fullPathFile);
      thisAM.put(MetadataField.FIELD_ACCESS_URL, fullPathFile);
       */

      String publicationTitle = thisAM.getRaw(EastviewNewspaperMetadataHelper.PUBLICATION_TITLE_PATH);
      log.debug3("Eastview Newspaper: publicationTitle = " + publicationTitle);

      if (publicationTitle != null) {
        String issn = issnMap.get(publicationTitle);
        log.debug3("Eastview Newspaper: publicationTitle = " + publicationTitle + ", issn = " + issn);
        thisAM.put(MetadataField.FIELD_ISSN, issn);
      } else {
        log.debug3("Eastview Newspaper: publicationTitle is null");
      }
      
      thisAM.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
      thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);
    }
  }
}
