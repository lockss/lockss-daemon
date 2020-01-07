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

package org.lockss.plugin.clockss.casalini;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;

public class CasaliniLibriMarcXmlMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
  private static final Logger log = Logger.getLogger(CasaliniLibriMarcXmlMetadataExtractorFactory.class);
  
  private static SourceXmlSchemaHelper CasaliniHelper = null;
  
  @Override
  public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
      String contentType)
          throws PluginException {
    return new CasaliniMarcXmlMetadataExtractor();
  }

  public class CasaliniMarcXmlMetadataExtractor extends SourceXmlMetadataExtractor {

    @Override
    protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
      // They upload two xml source files, and we are only interested in article related xml for metadata
      // psicoterapia_scienze_umane_issues_20191014.xml
      // psicoterapia_scienze_umane_articles_20191014.xml
      if  (CasaliniHelper == null && cu.getUrl().indexOf("articles") > -1) {
        CasaliniHelper = new CasaliniMarcXmlSchemaHelper();
      }
      return CasaliniHelper;
    }

    /*
      <datafield ind1="0" ind2=" " tag="773">
        <subfield code="t">Psicoterapia e scienze umane. Fascicolo 4, 2000.</subfield> // we are trying to get "4"
        <subfield code="d">Milano : Franco Angeli, 2000.</subfield>
        <subfield code="w">()2194804</subfield>
      </datafield>
   */
    // It is not clear which one can be used as "volume" of the PDF file, we use the above "4"
    // we also assume it is single digit number between 1-9
    @Override
    protected List<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, 
        CachedUrl cu,
        ArticleMetadata oneAM) {

      String yearNum = "";
      String pdfFilePath = "";
      String volumeNum = "1";
      ArrayList<String> returnList = new ArrayList<String>();
      Boolean volumeNumFound = false;

      if (oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_YEAR) != null) {
        yearNum = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_YEAR).replace(".", "");
      } else {
        log.debug3("yearNum is empty");
      }

      String fileNum = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_file);
      String cuBase = FilenameUtils.getFullPath(cu.getUrl());

      if (oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_VOLUME) != null) {
        String volumeString = oneAM.getRaw(CasaliniMarcXmlSchemaHelper.PDF_FILE_VOLUME);
        int lastComma = volumeString.lastIndexOf(",");
        if (lastComma > -1) {
          volumeNum = volumeString.substring((lastComma - 1), lastComma);
          volumeNumFound = true;
          pdfFilePath = cuBase + yearNum + "_" + volumeNum + "_" + fileNum + ".pdf";
          returnList.add(pdfFilePath);
          log.debug3("Found volume number, building PDF file with  filename - " + fileNum + ", volume - " + volumeNum + ", year - " + yearNum);
        }
      }
      if (!volumeNumFound) {
        ArrayList<Integer> volumes = new ArrayList<>();
        volumes.add(1);
        volumes.add(2);
        volumes.add(3);
        volumes.add(4);

        for (int volume : volumes) {
          pdfFilePath = cuBase + yearNum + "_" + volume + "_" + fileNum + ".pdf";
          returnList.add(pdfFilePath);
          log.debug3("Could not find volume number from xml, building PDF file with filename - " + fileNum + ", with possible guessed volume - " + volumeNum + ", year - " + yearNum);
        }
      }

      return returnList;
    }

    @Override
    protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
                                   CachedUrl cu, ArticleMetadata thisAM) {

      if (thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_start_page) != null) {
        String pages = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_start_page);
        // It might in different formats
        // P. [1-20] [20]
        // 370-370 p.
        String page_pattern = "[^\\d]*?(\\d+)\\s*?\\-\\s*?(\\d+)[^\\d]*?";

        Pattern pattern = Pattern.compile(page_pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(pages);

        String start_page = "0";
        String end_page = "0";

        while (matcher.find()) {
            start_page = matcher.group(1);
            end_page = matcher.group(2);
        }

        thisAM.put(MetadataField.FIELD_START_PAGE, start_page);
        thisAM.put(MetadataField.FIELD_END_PAGE, end_page);
      }

      if (thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_author) != null) {
        String author = thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_author);
        thisAM.put(MetadataField.FIELD_AUTHOR, author.replace(".", ""));
      }

      /*
       Since casalini is using Marc format in a inconsistent way.
       The something_articles_something.xml format changed in the middle of the xml.
       This file list the inconsistence found in real data, so developer will not get trapped

        ========In http://clockss-ingest.lockss.org/sourcefiles/casalini-released/2019/Psicoterapia/psicoterapia_scienze_umane_articles_20191014.xml==
        The following samples list DOI  has not access_url vs DOI with access_url

        ====DOI without access_url===
        <datafield ind1="7" ind2=" " tag="024">
        <subfield code="a">10.3280/PU2015-004011</subfield>
        <subfield code="2">DOI</subfield>
        </datafield>

        ===DOI with access_url===
        <datafield ind1="7" ind2=" " tag="024">
        <subfield code="a">10.3280/PU2015-004011</subfield>
        <subfield code="2">DOI</subfield>
        </datafield>

        <datafield ind1="4" ind2="0" tag="856">
        <subfield code="u">http://digital.casalini.it/10.3280/PU2015-004011</subfield>
        </datafield>
       */
      if (thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_doi) != null) {
        thisAM.put(MetadataField.FIELD_DOI, thisAM.getRaw(CasaliniMarcXmlSchemaHelper.MARC_doi));
      }
    }
  }
}
