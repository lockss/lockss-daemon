/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.eastview;

import org.lockss.config.TdbAu;
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
  private static final String unmappedPublisherName = "EastviewUnmappedPublisherName";
  private static final Logger log = Logger.getLogger(EastviewJournalXmlMetadataExtractorFactory.class);
  private static final Logger pubnamelog = Logger.getLogger(unmappedPublisherName);
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

      String url_string = cu.getUrl();

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
        // return true for all xml/xhtml file, since a lot of older content has no PDF provided
        if(filesToCheck.get(i).contains(".xml") || filesToCheck.get(i).contains(".xhtml")) {
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

      String directory = null;
      String subdir = null;
      String directory_subsection = null;    // use the part after directory as the last effor, for example, DA-IR use "IR"
      String publisher_mapped_alt = null;
      String publisher_mapped_alt2 = null;
      String publisher_mapped_alt3 = null;

      if (cu.getUrl().contains(".xml")) {
        raw_title = thisAM.getRaw(EastviewSchemaHelper.ART_RAW_TITLE);
        log.debug3(String.format("Eastview metadata raw title parsed - xml: %s, cu = %s", raw_title, cu.getUrl()));

      } else if (cu.getUrl().contains(".xhtml")) {
        raw_title = thisAM.getRaw(EastviewJournalMetadataXmlFormatHelper.ART_RAW_TITLE);
        log.debug3(String.format("Eastview metadata raw title parsed - Html: %s, cu = %s, raw_title = %s", raw_title, cu.getUrl(), raw_title));
      }

      if (raw_title != null) {

        //http://clockss-ingest.lockss.org/sourcefiles/eastview-released/2021_03/Eastview%20Journal%20Content/Digital%20Archives/Ogonek%20(St.%20Petersburg)%20Digital%20Archive%201899-1918/DA-OGN-SP.zip!/DA-OGN-SP/1918/ognsp_1918_17.xml
        //http://clockss-ingest.lockss.org/sourcefiles/eastview-released/2021_01/Eastview%20Journal%20Content/Digital%20Archives/Military%20Thought%20(DA-MLT)%201990-2019/DA-MLT.zip!/DA-MLT/MTH/1990/01/001_01/0010004.xhtml
        String access_url = cu.getUrl();
        Pattern urlPattern = Pattern.compile("(.*)\\.zip!/([^/]+)/([^/]+)/(.*)");

        Matcher urlm = urlPattern.matcher(access_url);
        
        if (urlm.matches()) {
          directory = urlm.group(2).trim();      //	DA-OGN-SP or DA-MLT, it is used on mutiple places down
          subdir = urlm.group(3).trim();         //   1918 or  MTH
        }

        // eastview_journal_2021_02/DA-NN/NEW/1993/09/004_30/12375018_new_19930901_000_001.xml: <TITLE>01-09-1993(NEW-No.004 Vol.033) In This Issue</TITLE>
        // eastview_journal_2021_02/DA-VI/ISM/1932/06/003_30/193203im002.xml: <TITLE>30-06-1932(ISM-No. 003(025)) something!</TITLE>
        // eastview_journal_2021_02/DA-VI/ISM/1932/06/003_30/193203im060.xml<TITLE>30-06-1932(ISM-No. 003(025)) something (1874 - 1932) </TITLE>
        Pattern pattern = Pattern.compile("(\\d\\d-\\d\\d-\\d{2,4})\\s*\\(([^-]*)\\s*\\-\\s*((?:\\([^)]*\\)|[^()])*)\\)\\s*(.*)");

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

          log.debug2(String.format("Eastview metadata alternative mapping: metadata raw title parsed = %s | " +
                          "publisher_shortcut = %s | Null publisher_mapped = %s | volume = %s | title = %s",
                  raw_title,
                  publisher_shortcut,
                  publisher_mapped,
                  volume,
                  title));

          pubnamelog.debug2(String.format("Eastview metadata alternative mapping: metadata raw title parsed = %s | " +
                          "publisher_shortcut = %s | Null publisher_mapped = %s | volume = %s | title = %s",
                  raw_title,
                  publisher_shortcut,
                  publisher_mapped,
                  volume,
                  title));

          // map publisher short cut to full name is failed, use partial url to map

          if (urlm.matches()) {

            directory_subsection =  directory.substring(directory.indexOf("-") + 1).replace("-",""); //"OGNSP" of "DA-OGN-SP"
            publisher_mapped_alt = EastViewPublisherNameMappingHelper.canonical.get(directory);
            publisher_mapped_alt2 = EastViewPublisherNameMappingHelper.canonical.get(subdir);
            publisher_mapped_alt3 = EastViewPublisherNameMappingHelper.canonical.get(directory_subsection);

            log.debug2(String.format("Eastview metadata alternative mapping second try, access_url = %s, raw_title = %s, directory  = %s | " +
                            "subdir = %s | directory_subsection = %s | publisher_mapped_alt = %s | publisher_mapped_alt2 = %s | publisher_mapped_alt3 = %s",
                    access_url,
                    raw_title,
                    directory,
                    subdir,
                    directory_subsection,
                    publisher_mapped_alt,
                    publisher_mapped_alt2,
                    publisher_mapped_alt3));

            pubnamelog.debug2(String.format("Eastview metadata alternative mapping second try, access_url = %s, raw_title = %s, directory  = %s | " +
                            "subdir = %s | directory_subsection = %s | publisher_mapped_alt = %s | publisher_mapped_alt2 = %s | publisher_mapped_alt3 = %s",
                    access_url,
                    raw_title,
                    directory,
                    subdir,
                    directory_subsection,
                    publisher_mapped_alt,
                    publisher_mapped_alt2,
                    publisher_mapped_alt3));

            if (publisher_mapped_alt != null && publisher_mapped_alt2 == null) {
              thisAM.put(MetadataField.FIELD_PUBLISHER, publisher_mapped_alt);
            } else if (publisher_mapped_alt2 != null && publisher_mapped_alt == null ) {
              thisAM.put(MetadataField.FIELD_PUBLISHER, publisher_mapped_alt2);
            } else if (publisher_mapped_alt3 != null && publisher_mapped_alt == null && publisher_mapped_alt2 == null) {
              thisAM.put(MetadataField.FIELD_PUBLISHER, publisher_mapped_alt3);
            } else {

              log.debug2(String.format("Eastview metadata alternative mapping failed try, access_url = %s, raw_title = %s, directory  = %s | " + "subdir = %s | directory_subsection = %s | publisher_mapped_alt = %s | publisher_mapped_alt2 = %s | publisher_mapped_alt3 = %s",
                      access_url,
                      raw_title,
                      directory,
                      subdir,
                      directory_subsection,
                      publisher_mapped_alt,
                      publisher_mapped_alt2,
                      publisher_mapped_alt3));

              pubnamelog.debug2(String.format("Eastview metadata alternative mapping failed try, access_url = %s, raw_title = %s, directory  = %s | " + "subdir = %s | directory_subsection = %s | publisher_mapped_alt = %s | publisher_mapped_alt2 = %s | publisher_mapped_alt3 = %s",
                      access_url,
                      raw_title,
                      directory,
                      subdir,
                      directory_subsection,
                      publisher_mapped_alt,
                      publisher_mapped_alt2,
                      publisher_mapped_alt3));


              thisAM.put(MetadataField.FIELD_PUBLISHER, publisher_shortcut);
            }
          }
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
        // Since "<SOURCE/>" and "<SRC/>" is not guaranteed, use part of the access_url as the publication title
        //we will use "DA-OGN-SP" in the example.
        ///eastview-released/2021_03/Eastview Journal Content/Digital Archives/Ogonek (St. Petersburg) Digital Archive 1899-1918/DA-OGN-SP.zip!/DA-OGN-SP/1900/ognsp_1900_01.xml
        thisAM.replace(MetadataField.FIELD_PUBLICATION_TITLE, directory);
        log.debug3("Eastview Journal: publicationTitle is null, set to diretory " + directory);
        pubnamelog.debug3("Eastview Journal: publicationTitle is null, set to diretory " + directory);
      }

      thisAM.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
      thisAM.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);

      String publisherName = "East View Information Services";

      TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
      if (tdbau != null) {
        publisherName =  tdbau.getPublisherName();
      }

      thisAM.put(MetadataField.FIELD_PROVIDER, publisherName);

      // Since raw ATITLE is not guaranteed to be uniqu, it may be called "Page 1, Page 2, etc"
      // Use ATITLE - TITLE as the unique string for reporting purpose
      String new_title =   thisAM.getRaw(EastviewSchemaHelper.ART_RAW_ATITLE) + " - " + raw_title;
      thisAM.put(MetadataField.FIELD_ARTICLE_TITLE, new_title);
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
