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
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.lockss.util.MetadataUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EastviewNewspaperXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final String unmappedPublisherName = "EastviewUnmappedPublisherName";
  private static final Logger pubnamelog = Logger.getLogger(unmappedPublisherName);
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
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, CachedUrl cu,
                                                            ArticleMetadata oneAM) {

      // filename is just the same a the XML filename but with .pdf
      // instead of .xml
      String url_string = cu.getUrl();
      String pdfName = url_string.substring(0,url_string.length() - 3) + "pdf";
      log.debug3("Eastview Newspaper: pdfName is " + pdfName);
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

      Pattern pattern =  Pattern.compile("\\d\\d-\\d\\d-\\d{2,4}\\(([^)]+)-([^(]+)\\)\\s+(.*)");

      Matcher m = pattern.matcher(raw_title);

      String publisher_shortcut = null;
      String publisher_mapped = null;
      String volume = null;
      String title = null;

      String directory = null;
      String subdir = null;
      String directory_subsection = null;    // use the part after directory as the last effor, for example, DA-IR use "IR"
      String publisher_mapped_alt = null;
      String publisher_mapped_alt2 = null;
      String publisher_mapped_alt3 = null;

      if(m.matches()){
        publisher_shortcut = m.group(1).trim();
        publisher_mapped = EastViewPublisherNameMappingHelper.canonical.get(publisher_shortcut);
        volume = m.group(2);
        title = m.group(3);
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


        pubnamelog.debug2(String.format("Eastview metadata alternative mapping: metadata raw title parsed = %s | " +
                        "publisher_shortcut = %s | Null publisher_mapped = %s | volume = %s | title = %s",
                raw_title,
                publisher_shortcut,
                publisher_mapped,
                volume,
                title));

        //http://clockss-ingest.lockss.org/sourcefiles/eastview-released/2021_03/Eastview%20Journal%20Content/Digital%20Archives/Ogonek%20(St.%20Petersburg)%20Digital%20Archive%201899-1918/DA-OGN-SP.zip!/DA-OGN-SP/1918/ognsp_1918_17.xml
        //http://clockss-ingest.lockss.org/sourcefiles/eastview-released/2021_01/Eastview%20Journal%20Content/Digital%20Archives/Military%20Thought%20(DA-MLT)%201990-2019/DA-MLT.zip!/DA-MLT/MTH/1990/01/001_01/0010004.xhtml
        String access_url = cu.getUrl();
        Pattern urlPattern = Pattern.compile("(.*)\\.zip!/([^/]+)/([^/]+)/(.*)");

        Matcher urlm = urlPattern.matcher(access_url);

        if (urlm.matches()) {

          directory = urlm.group(2).trim();      //	DA-OGN-SP or DA-MLT
          subdir = urlm.group(3).trim();         //   1918 or  MTH
          directory_subsection = directory.substring(directory.indexOf("-") + 1).replace("-", ""); //"OGNSP" of "DA-OGN-SP"
          publisher_mapped_alt = EastViewPublisherNameMappingHelper.canonical.get(directory);
          publisher_mapped_alt2 = EastViewPublisherNameMappingHelper.canonical.get(subdir);
          publisher_mapped_alt3 = EastViewPublisherNameMappingHelper.canonical.get(directory_subsection);

        }

        if (publisher_mapped_alt != null && publisher_mapped_alt2 == null) {
          thisAM.put(MetadataField.FIELD_PUBLISHER, publisher_mapped_alt);
        } else if (publisher_mapped_alt2 != null && publisher_mapped_alt == null) {
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
      

      String publicationTitle = thisAM.getRaw(EastviewNewspaperMetadataHelper.PUBLICATION_TITLE_PATH);
      log.debug3("Eastview Newspaper: publicationTitle = " + publicationTitle);

      if (publicationTitle != null) {
        String issn = issnMap.get(publicationTitle);
        log.debug3("Eastview Newspaper: publicationTitle = " + publicationTitle + ", issn = " + issn);
        if (MetadataUtil.validateIssn(issn) != null) {
          thisAM.put(MetadataField.FIELD_ISSN, MetadataUtil.validateIssn(issn));
        }
      } else {
        log.debug3("Eastview Newspaper: publicationTitle is null");
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
      thisAM.put(MetadataField.FIELD_ARTICLE_TITLE, thisAM.getRaw(EastviewSchemaHelper.ART_RAW_ATITLE) + " - " + raw_title);

    }
  }
}
