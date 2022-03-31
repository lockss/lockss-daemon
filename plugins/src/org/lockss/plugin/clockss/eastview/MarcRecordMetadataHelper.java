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

import org.apache.commons.io.FilenameUtils;
import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.MetadataStringHelperUtilities;
import org.lockss.util.Logger;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Marc4J library https://github.com/marc4j/marc4j
 */
public class MarcRecordMetadataHelper implements FileMetadataExtractor {

  private static final Logger log = Logger.getLogger(MarcRecordMetadataHelper.class);

  private static Pattern DOI_PAT = Pattern.compile("10[.][0-9a-z]{4,6}/.*");

  public static String zippedFolderName;


  @Override
  public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {

    try {
      
      //Ignore the ".txt" file from metadata
      if (!cu.getUrl().contains(".txt")) {
        log.debug(String.format("Url: %s will be avoid ", cu.getUrl()));


        InputStream input = cu.getUnfilteredInputStream();

        MarcReader reader = null;

        if (cu.getUrl().contains(".xml")) {
          reader = new MarcXmlReader(input, "UTF-8");
        }

        if (cu.getUrl().contains(".mrc")) {
          reader = new MarcStreamReader(input, "UTF-8");
        }

        int recordCount = 0;
        List<String> bookIDs = new ArrayList<String>();

        while (reader.hasNext()) {

          ArticleMetadata am = new ArticleMetadata();

          Record record = reader.next();
          recordCount++;

          // Get all the raw metadata and put it to raw
          List<DataField> fields = record.getDataFields();
          if (fields != null) {
            for (DataField field : fields) {
              String tag = field.getTag();
              List<Subfield> subfields = field.getSubfields();
              if (subfields != null) {
                for (Subfield subfield : subfields) {
                  char subtag = subfield.getCode();
                  am.putRaw(String.format("%s_%c", tag, subtag), subfield.getData());
                }
              }
            }
          }

          String MARC_isbn = getMARCData(record, "020", 'a');
          // This is only used in 2016 mrc record
          String MARC_isbn_alt = getMARCData(record, "773", 'z');
          String MARC_issn = getMARCData(record, "022", 'a');
          // MARC_Title will be different for journal vs books
          String MARC_title = getMARCData(record, "245", 'a');
          String MARC_pub_date = getMARCData(record, "260", 'c');
          String MARC_pub_date_alt = getMARCData(record, "264", 'c');
          String MARC_publisher = getMARCData(record, "260", 'b');
          String MARC_author = getMARCData(record, "100", 'a');
          String MARC_author_alt = getMARCData(record, "700", 'a');
          String MARC_doi = getMARCData(record, "024", 'a');
          String MARC_doi_alt = getMARCData(record, "856", 'u');
          String MARC_pdf = getMARCControlFieldData(record, "001");
          // Add it to raw metadata
          am.putRaw("mrc_controlfield_001", MARC_pdf);

          //Set DOI
          if (MARC_doi != null && isDoi(MARC_doi)) {
            log.debug3("MARC_doi:" + MARC_doi);
            am.put(MetadataField.FIELD_DOI, MARC_doi);
          } else if (MARC_doi_alt != null) {
            if (isDoi(MARC_doi_alt)) {
              log.debug3("MARC_doi_alt:" + MARC_doi_alt);
              am.put(MetadataField.FIELD_DOI, MARC_doi_alt);
            }
          }

          // Set ISBN
          if (MARC_isbn != null) {
            log.debug3("MARC_isbn:" + MARC_isbn);
            am.put(MetadataField.FIELD_ISBN, MARC_isbn);
          } else if (MARC_isbn_alt != null) {
            log.debug3("MARC_isbn_alt:" + MARC_isbn_alt);
            am.put(MetadataField.FIELD_ISBN, MARC_isbn_alt);
          }

          // Set publiation date
          if (MARC_pub_date != null) {
            log.debug3("MARC_pub_date:" + MARC_pub_date);
            am.put(MetadataField.FIELD_DATE, MetadataStringHelperUtilities.cleanupPubDate(MARC_pub_date));
          } else if (MARC_pub_date_alt != null) {
            log.debug3("MARC_pub_date_alt:" + MARC_pub_date_alt);
            am.put(MetadataField.FIELD_DATE, MetadataStringHelperUtilities.cleanupPubDate(MARC_pub_date_alt));
          }

          // Set author
          if (MARC_author_alt != null) {
            log.debug3("MARC_author_alt:" + MARC_author_alt);
            am.put(MetadataField.FIELD_AUTHOR, MARC_author_alt.replace(".", ""));
          } else if (MARC_author != null) {
            log.debug3("MARC_author:" + MARC_author);
            am.put(MetadataField.FIELD_AUTHOR, MARC_author.replace(".", ""));
          }

          if (MARC_publisher != null) {
            am.put(MetadataField.FIELD_PUBLISHER, MARC_publisher);
          }

          // Set provider name
          String publisherName = "East View Information Services";

          TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
          if (tdbau != null) {
            publisherName = tdbau.getPublisherName();
          }

          am.put(MetadataField.FIELD_PROVIDER, publisherName);

          // Setup PDF
          String zippedFolderName = getZippedFolderName();

          String fileNum = MARC_pdf;
          String cuBase = FilenameUtils.getFullPath(cu.getUrl());
          String pdfFilePath = "";

          if (zippedFolderName != null) {
            pdfFilePath = cuBase + zippedFolderName + ".zip!/" + fileNum + ".pdf";
            log.debug3("with zipped folder name, pdfFilePath" + pdfFilePath);
          } else {
            pdfFilePath = cuBase.substring(0,cuBase.lastIndexOf("/")) + ".zip!/" + fileNum + ".pdf";
            log.debug3("without zipped folder name, pdfFilePath" + pdfFilePath);
          }
          am.put(MetadataField.FIELD_ACCESS_URL, pdfFilePath);

          /*
          Leader byte 07 “a” = Book (monographic component part)
          Leader byte 07 “m” = Book
          Leader byte 07 “s” = Journal
          Leader byte 07 “b” = Journal (serial component part)
          Leader leader = record.getLeader();

          Since the raw data sent wrong publication type, use the pre defined one
          */

          am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKVOLUME);
          am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_BOOK);

          // Set publication title
          if (MARC_title != null) {
            String cleanedArticleTitle = MARC_title.replace(":", "").
                    replace("/", "").
                    replace("=", "").
                    replace("\"", "").
                    replace("...", "");
            log.debug3(String.format("original artitle title = %s, cleaned title = %s", MARC_title, cleanedArticleTitle));
            am.put(MetadataField.FIELD_PUBLICATION_TITLE, MARC_title);
          }

          emitter.emitMetadata(cu, am);
        }
      }

    } catch (NullPointerException exception) {
      log.error("MarcRecordMetadataHelper NullPointerException:", exception);
    } catch (Exception exception) {
      log.error("MarcRecordMetadataHelper Exception: ", exception);
    }
  }

  /**
   * Get MARC21 data value by dataFieldCode and subFieldCode
   * @param record
   * @param dataFieldCode
   * @param subFieldCode
   * @return String value of MARC21 data field
   */
  public static String getMARCData(Record record, String dataFieldCode, char subFieldCode) {

    try {
      DataField field = (DataField) record.getVariableField(dataFieldCode);

      // It is not guaranteed each record has the same information
      if (field != null) {

        String tag = field.getTag();
        char ind1 = field.getIndicator1();
        char ind2 = field.getIndicator2();

        List subfields = field.getSubfields();
        Iterator i = subfields.iterator();

        while (i.hasNext()) {
          Subfield subfield = (Subfield) i.next();
          char code = subfield.getCode();
          String data = subfield.getData();

          if (code == subFieldCode) {
            // clean up data before return
            if (dataFieldCode.equals("700")) {
              return data.replace("edited by", "");
            } else if (dataFieldCode.equals("300")) {
              return data.replace("p.", "").replace(":", "");
            } else if (dataFieldCode.equals("245") && subFieldCode == 'a') {
              return data.replace("/", "").replace(":", "");
            } else {
              return data;
            }
          }
        }
      }
    } catch (NullPointerException e) {
      log.debug("Mrc Record DataFieldCode: " + dataFieldCode + " SubFieldCode: " + subFieldCode + " has error");
    }
    return null;
  }

  /**
   * Get MARC21 control field data by dataFieldCode
   * @param record
   * @param dataFieldCode
   * @return String value of control field
   */
  private String getMARCControlFieldData(Record record, String dataFieldCode) {

    ControlField field = (ControlField) record.getVariableField(dataFieldCode);

    if (field != null) {
      String data = field.getData();
      log.debug3("Mrc Record getMARCControlFieldData: " + data);
      return data;
    } else {
        log.debug3("Mrc Record getMARCControlFieldData: " + dataFieldCode + " return null");
        return null;
    }
  }

  private boolean isDoi(String doi) {

    if (doi == null) {
      return false;
    }
    Matcher m = DOI_PAT.matcher(doi);

    if(!m.matches()){
      return false;
    }
    return true;
  }

  public static String getZippedFolderName() {
    return zippedFolderName;
  }

  public static void setZippedFolderName(String fname) {
    zippedFolderName = fname;
  }

}