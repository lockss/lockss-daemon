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

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import java.io.InputStream;

import org.lockss.plugin.clockss.MetadataStringHelperUtilities;
import org.lockss.plugin.clockss.MetadataStringHelperUtilities.*;
import static org.lockss.plugin.clockss.casalini.CasaliniLibriPublisherNameStringHelperUtilities.*;
import org.lockss.util.UrlUtil;

import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.ControlField;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.Leader;

import org.lockss.util.Logger;

/**
 *  Marc4J library https://github.com/marc4j/marc4j
 */
public class CasaliniLibriMarcMetadataHelper implements FileMetadataExtractor {

  private static final Logger log = Logger.getLogger(CasaliniLibriMarcMetadataHelper.class);

  private static final String COLLECTION_NAME = "Monographs";
  private static final String PUBLISHER_NAME = "Casalini";
  private static final String PUBLISHER_NAME_APPENDIX = " - " + PUBLISHER_NAME ;


  @Override
  public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {

    try {

      // Since 2016 has special case to handle their PDF file path, we need to tell whether it is 2016
      boolean is_year_2016 = false;

      String cuBase = FilenameUtils.getFullPath(cu.getUrl());

      if (cuBase.contains("released/2016")) {
        log.debug3("Year 2016: cuBase = " + cuBase);
        is_year_2016 = true;
      } else {
        log.debug3("Year not 2016, cuBase = " + cuBase);
      }


      InputStream input = cu.getUnfilteredInputStream();

      MarcReader reader = null;

      if (cu.getUrl().contains(".xml")) {
        reader = new MarcXmlReader(input);
      }

      if (cu.getUrl().contains(".mrc")) {
        reader = new MarcStreamReader(input);
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
                am.putRaw(String.format("%s_%c", tag, subtag),
                        subfield.getData());
              }
            }
          }
        }


        String MARC_isbn = getMARCData(record, "020", 'a');
        // This is only used in 2016 mrc record
        String MARC_isbn_alt = getMARCData(record, "773", 'z');
        String MARC_title = getMARCData(record, "245", 'a');
        String MARC_pub_date = getMARCData(record, "260", 'c');
        String MARC_pub_date_alt = getMARCData(record, "264", 'c');
        String MARC_publisher = getMARCData(record, "260", 'b');
        String MARC_author = getMARCData(record, "100", 'a');
        String MARC_author_alt = getMARCData(record, "700", 'a');
        String MARC_pdf = getMARCControlFieldData(record, "001");

        // Set ISBN
        if (MARC_isbn != null) {
          am.put(MetadataField.FIELD_ISBN, MARC_isbn);
        } else if (MARC_isbn_alt != null) {
          am.put(MetadataField.FIELD_ISBN, MARC_isbn_alt);
        }

        // Set publication title
        if (MARC_title != null) {
          am.put(MetadataField.FIELD_PUBLICATION_TITLE, MARC_title);
        }

        // Set publiation date
        if (MARC_pub_date != null) {
          am.put(MetadataField.FIELD_DATE, MetadataStringHelperUtilities.cleanupPubDate(MARC_pub_date));
        } else if (MARC_pub_date_alt != null) {
          am.put(MetadataField.FIELD_DATE, MetadataStringHelperUtilities.cleanupPubDate(MARC_pub_date_alt));
        } else {
          log.debug3("MARC_pub_date is null and MARC_pub_date_alt is null");
        }

        // Set author
        if (MARC_author == null) {
          am.put(MetadataField.FIELD_AUTHOR, MARC_author_alt);
        } else {
          if (MARC_author != null) {
            am.put(MetadataField.FIELD_AUTHOR, MARC_author);
          }
        }

        // Set publisher name
        if (MARC_publisher == null) {
          am.put(MetadataField.FIELD_PUBLISHER, PUBLISHER_NAME);
        } else {

          String cleanPublisherName = MetadataStringHelperUtilities.cleanupPublisherName(MARC_publisher);

          if (!cleanPublisherName.toLowerCase().contains(PUBLISHER_NAME.toLowerCase())) {
            am.put(MetadataField.FIELD_PUBLISHER, cleanPublisherName + PUBLISHER_NAME_APPENDIX);
          } else {
            am.put(MetadataField.FIELD_PUBLISHER, cleanPublisherName);
          }
        }

        if (is_year_2016) {
          
          String MARC_bookid = getMARCData(record, "097", 'a');
          String MARC_chapterid = getMARCData(record, "097", 'c');

          String publisherCleanName = cleanupKey(MARC_publisher);

          String publisherShortCut = matchPublisherName2016(publisherCleanName.toLowerCase());

          if (publisherShortCut == null) {
            // If it not found in 2016 mapping, it may be in 2019,2020 mapping
            publisherShortCut = matchPublisherName(publisherCleanName.toLowerCase());

            if (publisherShortCut == null) {
              log.debug3(String.format("publisherShortCut is null: MARC_publisher: %s | publisherCleanName: %s",
                      MARC_publisher, publisherCleanName, publisherShortCut));
            }
          }

          // Handle 2016 PDF goes here
          String MARC_pdf_2016 = String.format("%s/%s/%s/%s", COLLECTION_NAME, publisherShortCut, MARC_bookid, MARC_bookid);

          log.debug3("2016 MARC_pdf: " + MARC_pdf);

          if (MARC_bookid != null && MARC_chapterid != null) {
            log.debug3(String.format("Emit chapter: MARC_bookid %s | MARC_chapterid: %s ",
                    MARC_bookid, MARC_chapterid));
          } else if (MARC_chapterid == null) {
            log.debug3(String.format("Do not emit chapter: MARC_bookid %s ", MARC_bookid));
          }

          if (MARC_pdf_2016 != null) {
            String fullPathFile_2016 = UrlUtil.minimallyEncodeUrl(cuBase + MARC_pdf_2016 + ".pdf");
            log.debug3("2016 MARC_pdf: " + MARC_pdf_2016 + ", fullPathFile = " + fullPathFile_2016);
            am.put(MetadataField.FIELD_ACCESS_URL, fullPathFile_2016);

            // Only emit the books metadata
            if (MARC_bookid != null && !bookIDs.contains(MARC_bookid)) {
              bookIDs.add(MARC_bookid);
              emitter.emitMetadata(cu, am);
            }
          } else {
            log.debug3("MARC_pdf field is not used");
          }
          // End handle 2016 PDF
        } else {
          String fullPathFile = UrlUtil.minimallyEncodeUrl(cuBase + MARC_pdf + ".pdf");
          log.debug3("2020 MARC_pdf " + fullPathFile);
          am.put(MetadataField.FIELD_ACCESS_URL, fullPathFile);
        }


        /*
        Leader byte 07 “a” = Book (monographic component part)
        Leader byte 07 “m” = Book
        Leader byte 07 “s” = Journal
        Leader byte 07 “b” = Journal (serial component part)

        */
        Leader leader = record.getLeader();

        char publication_type = '0';

        publication_type = leader.getImplDefined1()[0];

        if (publication_type != '0') {
          // Setup FIELD_PUBLICATION_TYPE & FIELD_ARTICLE_TYPE
          if (publication_type == 'm' || publication_type == 'a') {
            am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKVOLUME);
            am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_BOOK);
          }

          if (publication_type == 's' || publication_type == 'b') {
            am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
            am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);
          }
        }

        emitter.emitMetadata(cu, am);
      }
      log.debug3(String.format("Metadata file source: %s, recordCount: %d", cu.getUrl(), recordCount));
    } catch (NullPointerException exception) {
      log.error("CasaliniLibriMarcMetadataHelper throw NullPointerExceptiop: ", exception);
      exception.printStackTrace();
    } catch (Exception exception) {
      log.error("CasaliniLibriMarcMetadataHelper throw Exceptiop: ", exception);
      exception.printStackTrace();
    }
  }

  /**
   * Get MARC21 data value by dataFieldCode and subFieldCode
   * @param record
   * @param dataFieldCode
   * @param subFieldCode
   * @return String value of MARC21 data field
   */
  private String getMARCData(Record record, String dataFieldCode, char subFieldCode) {

    try {
      DataField field = (DataField) record.getVariableField(dataFieldCode);

      // It is not guaranteed each record has the same information
      if (field != null) {

        String tag = field.getTag();
        char ind1 = field.getIndicator1();
        char ind2 = field.getIndicator2();

        log.debug3("Mrc Record Tag: " + tag + " Indicator 1: " + ind1 + " Indicator 2: " + ind2);

        List subfields = field.getSubfields();
        Iterator i = subfields.iterator();

        while (i.hasNext()) {
          Subfield subfield = (Subfield) i.next();
          char code = subfield.getCode();
          String data = subfield.getData();

          if (code == subFieldCode) {
            log.debug3("Mrc Record Found Tag: " + tag + " Subfield code: " + code + " Data element: " + data);

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
      } else {
        log.debug3("Mrc Record getVariableField: " + dataFieldCode + " return null");
      }
    } catch (NullPointerException e) {
      log.debug3("Mrc Record DataFieldCode: " + dataFieldCode + " SubFieldCode: " + subFieldCode + " has error");
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
  
}