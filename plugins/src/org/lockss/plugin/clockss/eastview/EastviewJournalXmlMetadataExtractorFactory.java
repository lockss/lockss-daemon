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

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.plugin.clockss.XPathXmlMetadataParser;
import org.lockss.plugin.clockss.XmlFilteringInputStream;
import org.lockss.util.LineRewritingReader;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.lockss.util.MetadataUtil;
import org.lockss.util.ReaderInputStream;


public class EastviewJournalXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(EastviewJournalXmlMetadataExtractorFactory.class);
  private static SourceXmlSchemaHelper EastviewHelper = null;

  public static Map<String, String> issnMap = new HashMap<>();

  static {
    try {
      issnMap = new EastviewNewspaperTitleISSNMappingHelper().getISSNList();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new EastviewXmlMetadataExtractor();
  }

  public class EastviewXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      
      String cuBase = cu.getUrl();

      log.debug3("Eastview Journal All Version: cuBase = " + cuBase);

      if (cuBase.contains("DA-MLT/MTH/2001") || cuBase.contains("DA-MLT/MTH/199")) {
        EastviewHelper = new EastviewJournalMetadataXhtmlFormatHelper();
        log.debug3("Eastview Journal Early Version(199X - 2001): cuBase = " + cuBase);

      } else if (cuBase.contains("DA-MLT/MTH/2002") || cuBase.contains("DA-MLT/MTH/2003")
              || cuBase.contains("DA-MLT/MTH/2004") || cuBase.contains("DA-MLT/MTH/2005")) {
        EastviewHelper = new EastviewJournalMetadataXmlFormatHelper();
        log.debug3("Eastview Journal Early Version(2002 - 2005): cuBase = " + cuBase);

      } else {
        EastviewHelper = new EastviewJournalMetadataHelper();
        log.debug3("Eastview Journal Later Version: cuBase == " + cuBase);
      }

      return EastviewHelper;
    }

    @Override
    protected boolean preEmitCheck(SourceXmlSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {

      log.debug3("Eastview Journal: in SourceXmlMetadataExtractor preEmitCheck");

      String url_string = cu.getUrl();

      log.debug3("Eastview Journal: preEmitCheck url_string = " + url_string);

      List<String> filesToCheck;

      // If no files get returned in the list, nothing to check
      if ((filesToCheck = getFilenamesAssociatedWithRecord(schemaHelper, cu,thisAM)) == null) {
        return true;
      }
      ArchivalUnit B_au = cu.getArchivalUnit();
      CachedUrl fileCu;
      for (int i=0; i < filesToCheck.size(); i++)
      {
        fileCu = B_au.makeCachedUrl(filesToCheck.get(i));
        log.debug3("Eastview Journal: Check for existence of " + filesToCheck.get(i));
        if(filesToCheck.get(i).contains(".pdf") || filesToCheck.get(i).contains(".xml") || filesToCheck.get(i).contains(".xhtml")) {
          // Set a cooked value for an access file. Otherwise it would get set to xml file
          log.debug3("Eastview Journal: set access_url to " + filesToCheck.get(i));
          return true;
        }
      }
      log.debug3("Eastview Journal: No file exists associated with this record");
      return false; //No files found that match this record
    }

    /* 
     * a PDF file may or may not exist, but assume the XML is full text
     * when it does not
     */
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
        ArticleMetadata oneAM) {

      String url_string = cu.getUrl();

      log.debug3("Eastview Journal: getFilenamesAssociatedWithRecord url_string = " + url_string);

      String articlePDFName = url_string.substring(0,url_string.length() - 3) + "pdf";
      log.debug3("Eastview Journal: articlePDFName is " + articlePDFName);

      List<String> returnList = new ArrayList<String>();
      returnList.add(articlePDFName);
      returnList.add(url_string); // xml file
      //returnList.add(manMadePagePDF); // do not add man-made-pagePDF
      return returnList;
    }
    
    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper, 
        CachedUrl cu, ArticleMetadata thisAM) {

      String raw_title = null;
      String publisher_shortcut = null;
      String publisher_mapped = null;
      String volume = null;
      String title = null;
      String pubdate = null;

      if (cu.getUrl().contains(".xml")) {
        raw_title = thisAM.getRaw(EastviewSchemaHelper.ART_RAW_TITLE);
        log.debug3(String.format("Eastview metadata raw title parsed - xml: %s, cu = %s", raw_title, cu.getUrl()));

      } else if (cu.getUrl().contains(".xhtml")) {
        raw_title = thisAM.getRaw(EastviewJournalMetadataXmlFormatHelper.ART_RAW_TITLE);
        log.debug3(String.format("Eastview metadata raw title parsed - Html: %s, cu = %s, raw_title = %s", raw_title, cu.getUrl(), raw_title));
      }

      if (raw_title != null) {

        Pattern pattern = Pattern.compile("(\\d\\d-\\d\\d-\\d{2,4})\\(([^)]+)-([^(]+)\\)\\s+(.*)");

        Matcher m = pattern.matcher(raw_title);

        if (m.matches()) {
          pubdate = m.group(1);
          publisher_shortcut = m.group(2).trim();
          publisher_mapped = EastViewPublisherNameMappingHelper.canonical.get(publisher_shortcut);
          volume = m.group(3);
          title = m.group(4);
        }

        log.debug3(String.format("Eastview metadata raw title parsed = %s | " +
                        "publisher_shortcut = %s | publisher_mapped = %s | volume = %s | title = %s",
                raw_title,
                publisher_shortcut,
                publisher_mapped,
                volume,
                title));

        if (publisher_mapped != null) {
          thisAM.put(MetadataField.FIELD_PUBLISHER, publisher_mapped);
        } else {
          log.debug3(String.format("Eastview metadata raw title parsed = %s | " +
                          "publisher_shortcut = %s | Null publisher_mapped = %s | volume = %s | title = %s",
                  raw_title,
                  publisher_shortcut,
                  publisher_mapped,
                  volume,
                  title));
          thisAM.put(MetadataField.FIELD_PUBLISHER, publisher_shortcut);
        }
      }
      

      if (thisAM.get(MetadataField.FIELD_VOLUME) == null || thisAM.get(MetadataField.FIELD_VOLUME).equals("000") || thisAM.get(MetadataField.FIELD_VOLUME).equals('0')) {
        if (thisAM.get(MetadataField.FIELD_DATE) != null) {
          log.debug3("Eastview Journal: invalid volume, set to " + thisAM.get(MetadataField.FIELD_DATE).substring(0, 4));
          thisAM.replace(MetadataField.FIELD_VOLUME, thisAM.get(MetadataField.FIELD_DATE).substring(0, 4));
        } else if(volume != null) {
          //For early .xhtml data, there is not date field
          thisAM.replace(MetadataField.FIELD_VOLUME, volume.replace("No.", ""));
        }
      }

      if (thisAM.get(MetadataField.FIELD_ISSUE) == null || thisAM.get(MetadataField.FIELD_ISSUE).equals("000") || thisAM.get(MetadataField.FIELD_ISSUE).equals('0')) {
        if (thisAM.get(MetadataField.FIELD_DATE) != null) {
          log.debug3("Eastview Journal: invalid issue, set to " + thisAM.get(MetadataField.FIELD_DATE).replace("-", ""));
          thisAM.replace(MetadataField.FIELD_ISSUE, thisAM.get(MetadataField.FIELD_DATE).replace("-", ""));
        } else {
          thisAM.replace(MetadataField.FIELD_ISSUE, pubdate);
        }
      }
      
      String publicationTitle = thisAM.getRaw(EastviewJournalMetadataHelper.PUBLICATION_TITLE_PATH);
      log.debug3("Eastview Journal: publicationTitle = " + publicationTitle);

      if (publicationTitle != null) {
        String issn = issnMap.get(publicationTitle);
        log.debug3("Eastview Journal: publicationTitle = " + publicationTitle + ", issn = " + issn);
        if (MetadataUtil.validateIssn(issn) != null) {
          thisAM.put(MetadataField.FIELD_ISSN, MetadataUtil.validateIssn(issn));
        }
      } else {
        log.debug3("Eastview Journal: publicationTitle is null");
      }

      if (title != null) {
        thisAM.put(MetadataField.FIELD_ARTICLE_TITLE, title);
      }

      thisAM.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
      thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);
    }

    /**
     * <p>Some IOP XML files contains HTML4 entities, that trip the SAX parser.
     * Work around them with Apache Commons Lang3.</p>
     */
    @Override
    protected XPathXmlMetadataParser createXpathXmlMetadataParser() {
      return new XPathXmlMetadataParser(getDoXmlFiltering()) {
        @Override
        protected InputStream getInputStreamFromCU(CachedUrl cu) {
          if (isDoXmlFiltering()) {
            return new XmlFilteringInputStream(new ReaderInputStream(new LineRewritingReader(new InputStreamReader(cu.getUnfilteredInputStream())) {
              @Override
              public String rewriteLine(String line) {
                //Sample troubled line:
                /*
                <head>
                <title>
                03-31-2003(MTH-No.001) R&D AT THE RUSSIAN DEFENSE MINISTRY: RECOMMENDATIONS ON PLANNING</title>
                </head>
                log.debug3("Eastview Journal line = " + line + ", unescapeHtml4 line = " + StringEscapeUtils.unescapeHtml4(line)
                        + "replaced line = " + line.replace("&", "&amp;"));
                 */
                //return StringEscapeUtils.unescapeHtml4(line);   //this will cause "<xml..." line parsing error.
                return line.replaceAll("&", "&amp;");
              }
            }));
          }
          else {
            return cu.getUnfilteredInputStream();
          }
        }
      };
    }

    /**
     * @see #createXpathXmlMetadataParser()
     */
    @Override
    public boolean getDoXmlFiltering() {
      return true;
    }
  }
}
