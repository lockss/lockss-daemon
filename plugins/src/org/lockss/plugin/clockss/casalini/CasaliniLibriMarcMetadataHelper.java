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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.clockss.MetadataStringHelperUtilities;

import static org.lockss.plugin.clockss.MetadataStringHelperUtilities.cleanupPublisherName;
import static org.lockss.plugin.clockss.casalini.CasaliniLibriPublisherNameStringHelperUtilities.*;
import org.lockss.util.UrlUtil;

import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.ControlField;
import org.marc4j.MarcXmlReader;

import org.lockss.util.Logger;

/**
 *  Marc4J library https://github.com/marc4j/marc4j
 */
public class CasaliniLibriMarcMetadataHelper implements FileMetadataExtractor {

  private static final String unmappedPublisherName = "CasaliniUnmappedPublisherName";
  private static final Logger log = Logger.getLogger(CasaliniLibriMarcMetadataHelper.class);
  private static final Logger pubnamelog = Logger.getLogger(unmappedPublisherName);

  private static final String COLLECTION_NAME = "Monographs";
  private static final String PUBLISHER_NAME = "Casalini";
  private static final String PUBLISHER_NAME_APPENDIX = " - " + PUBLISHER_NAME ;

  private static Pattern DOI_PAT = Pattern.compile("10[.][0-9a-z]{4,6}/.*");


  @Override
  public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {

    try {

      // Since 2016 has special case to handle their PDF file path, we need to tell whether it is 2016
      boolean is_year_2016 = false;
      boolean is_books = false;

      String cuBase = FilenameUtils.getFullPath(cu.getUrl());

      //By the time this coded, all the existing Aus are:
      //http://clockss-ingest.lockss.org/sourcefiles/casalini2012-released -- book(it actually has 2016 content)
      //Like this:http://clockss-ingest.lockss.org/sourcefiles/casalini2012-released/2016/Monographs/ATENEO/2249531/2249531.pdf
      //http://clockss-ingest.lockss.org/sourcefiles/casalini-released/, year 2019 -- journal
      //http://clockss-ingest.lockss.org/sourcefiles/casalinibooks-released/, year 2020 -- book
      //http://clockss-ingest.lockss.org/sourcefiles/casalinibooks-released/, directory 2021 -- book
      //http://clockss-ingest.lockss.org/sourcefiles/casalini-released/, directory:2021_01, journal


      if (cuBase.contains("released/2016")) {
        log.debug3("Casalini-Metadata: Year 2016: cuBase = " + cuBase);
        is_year_2016 = true;
        is_books = true;
      } else if (cuBase.contains("casalinibooks-released")) {
        // 2012 was legacy content, it is books, but it does not have "book" in the url
        log.debug3("Casalini-Metadata for books: cuBase = " + cuBase);
        is_books = true;
      } else{
        log.debug3("Casalini-Metadata: Year not 2016, cuBase = " + cuBase);
      }


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
                am.putRaw(String.format("%s_%c", tag, subtag),
                        subfield.getData());
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
        String MARC_title_extended = getMARCData(record, "245", 'b');
        String MARC_publication_title = getMARCData(record, "773", 't');
        String MARC_pub_date = getMARCData(record, "260", 'c');
        String MARC_pub_date_alt = getMARCData(record, "264", 'c');
        String MARC_publisher = getMARCData(record, "260", 'b');
        String MARC_author = getMARCData(record, "100", 'a');
        String MARC_author_alt = getMARCData(record, "700", 'a');
        String MARC_doi =  getMARCData(record, "024", 'a');
        String MARC_doi_alt =  getMARCData(record, "856", 'u');
        String MARC_pdf = getMARCControlFieldData(record, "001");

        //use 097_a as the unique identifier for the article for year 2019
        String MARC_uniq_local_id = getMARCData(record, "097", 'a');
        String MARC_uniq_local_id2 = getMARCData(record, "097", 'b');
        String MARC_uniq_local_id3 = getMARCData(record, "097", 'c');

        // Add it to raw metadata
        am.putRaw("mrc_controlfield_001", MARC_pdf);

        String MARC_bookid = null;
        String MARC_097a = null;   // This is created just for debugging purpose
        String MARC_chapterid = null;

        String publisherCleanName = null;
        String canonicalPublisherName = null;

        //Set DOI
        if (MARC_doi != null && isDoi(MARC_doi)) {
          am.put(MetadataField.FIELD_DOI, MARC_doi);
        } else if (MARC_doi_alt != null )  {
          String clean_doi_alt = MARC_doi_alt.replace("http://digital.casalini.it/", "");
          if (isDoi(clean_doi_alt)) {
            am.put(MetadataField.FIELD_DOI, clean_doi_alt);
          }
        }

        // Set publiation date
        if (MARC_pub_date != null) {
          am.put(MetadataField.FIELD_DATE, MetadataStringHelperUtilities.cleanupPubDate(MARC_pub_date));
        } else if (MARC_pub_date_alt != null) {
          am.put(MetadataField.FIELD_DATE, MetadataStringHelperUtilities.cleanupPubDate(MARC_pub_date_alt));
        } else {
          log.debug3("Casalini-Metadata: MARC_pub_date is null and MARC_pub_date_alt is null");
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

          publisherCleanName = cleanupPublisherName(PUBLISHER_NAME);
          canonicalPublisherName = getCanonicalPublisherName(publisherCleanName);
          am.put(MetadataField.FIELD_PUBLISHER, canonicalPublisherName);

          log.debug3(String.format("Casalini-Metadata: MARC_publisher is null, set it to: %s", canonicalPublisherName));
        } else {
          // Step 1: get the cleaned publisher name
          publisherCleanName = cleanupPublisherName(MARC_publisher);


          log.debug3(String.format("Casalini-Metadata:  getMARC_publisher %s | publisherCleanName: %s", MARC_publisher, publisherCleanName));


          canonicalPublisherName = getCanonicalPublisherName(publisherCleanName);

          // canonicalPublisherName used to be set to 'default' if it not found, but the team
          // agreed to set it as is, since a lot of them are data entried
          if (canonicalPublisherName == null) {
            pubnamelog.debug2(String.format("Casalini-Metadata: missing canonicalPublisherName,  MARC_publisher %s | publisherCleanName: %s | " +
                    "NULL canonicalPublisherName %s ", MARC_publisher, publisherCleanName, canonicalPublisherName));
            canonicalPublisherName = publisherCleanName;

          }

          log.debug3(String.format("Casalini-Metadata:  MARC_publisher %s | publisherCleanName: %s | " +
                          "canonicalPublisherName %s ", MARC_publisher, publisherCleanName, canonicalPublisherName));

          if (!canonicalPublisherName.toLowerCase().contains(PUBLISHER_NAME.toLowerCase())) {
            am.put(MetadataField.FIELD_PUBLISHER, canonicalPublisherName + PUBLISHER_NAME_APPENDIX);
          } else {
            am.put(MetadataField.FIELD_PUBLISHER, canonicalPublisherName);
          }
        }

        if (is_year_2016) {

          //   book should have both

          MARC_bookid = getMARCData(record, "092", 'a');
          MARC_097a = getMARCData(record, "097", 'a');
          MARC_chapterid = getMARCData(record, "097", 'c');


          log.debug3(String.format("Casalini-Metadata: MARC_bookid %s | MARC_097a %s | MARC_chapterid: %s ",
                    MARC_bookid, MARC_097a, MARC_chapterid));


          // Step 3: Get publisher name shortcut
          String publisherShortCut = getPublisherNameShortcut2016(canonicalPublisherName);
          
          if (publisherShortCut == null) {
            log.debug3(String.format("Casalini-Metadata: publisherShortCut NOT found in 2016: publisherCleanName: %s | " +
                            "canonicalPublisherName %s ",
                    publisherCleanName, canonicalPublisherName));
          } else {
            log.debug3(String.format("Casalini-Metadata: publisherShortCut found in 2016: publisherCleanName: %s | " +
                            "canonicalPublisherName %s | publisherShortCut : %s ",
                    publisherCleanName, canonicalPublisherName, publisherShortCut));
          }

          // Handle 2016 PDF goes here
          String MARC_pdf_2016 = String.format("%s/%s/%s/%s", COLLECTION_NAME, publisherShortCut, MARC_bookid, MARC_bookid);

          log.debug3("Casalini-Metadata: 2016 MARC_pdf control field id: " + MARC_pdf);

          if (MARC_pdf_2016 != null) {
            String fullPathFile_2016 = UrlUtil.minimallyEncodeUrl(cuBase + MARC_pdf_2016 + ".pdf");
            log.debug3("Casalini-Metadata: 2016 MARC_pdf: " + MARC_pdf_2016 + ", fullPathFile = " + fullPathFile_2016);
            am.put(MetadataField.FIELD_ACCESS_URL, fullPathFile_2016);

          } else {
            log.debug3("Casalini-Metadata: MARC_pdf field is not used");
          }
          // End handle 2016 PDF
        } else {
          String fullPathFile = UrlUtil.minimallyEncodeUrl(cuBase + MARC_pdf + ".pdf");

          CachedUrl testCuPDF = cu.getArchivalUnit().makeCachedUrl(fullPathFile);

          if (testCuPDF != null && testCuPDF.hasContent()) {
            am.put(MetadataField.FIELD_ACCESS_URL, fullPathFile);
            log.debug3("Casalini-Metadata: PDF testCuPDF has content =  " + testCuPDF);
          } else {
            String fullEpubPathFile = UrlUtil.minimallyEncodeUrl(cuBase + MARC_pdf + ".epub");

            if (fullEpubPathFile != null) { //No need to check hasContent, since it will not work for epub content.
              log.debug3("Casalini-Metadata: MARC_epub " + fullEpubPathFile);
              am.put(MetadataField.FIELD_ACCESS_URL, fullEpubPathFile);
            }
          }
        }

        /*
        This chunk of code was commented out, since publisher may not send Leader field
        Leader byte 07 “a” = Book (monographic component part)
        Leader byte 07 “m” = Book
        Leader byte 07 “s” = Journal
        Leader byte 07 “b” = Journal (serial component part)

        Leader leader = record.getLeader();

        char publication_type = '0';

        publication_type = leader.getImplDefined1()[0];

        if (publication_type != '0') {
          // Setup FIELD_PUBLICATION_TYPE & FIELD_ARTICLE_TYPE
          if (publication_type == 'm' || publication_type == 'a') {
            am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKVOLUME);
            am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_BOOK);

            // Set publication title
            if (MARC_title != null) {
              am.put(MetadataField.FIELD_PUBLICATION_TITLE, MARC_title);
            }
          }

          if (publication_type == 's' || publication_type == 'b') {
            am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
            am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);

            // Set publication title
            if (MARC_title != null) {
              am.put(MetadataField.FIELD_ARTICLE_TITLE, MARC_title);
            }

            // Set ISSN
            if (MARC_issn != null && is_books == false) {
              am.put(MetadataField.FIELD_ISSN, MARC_issn);
            }
          }
        }
        */

        if (is_books == true) {
          am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKVOLUME);
          am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_BOOK);

          // Set publication title
          if (MARC_title != null) {
            am.put(MetadataField.FIELD_PUBLICATION_TITLE, MARC_title);
          }

          // Set ISBN
          if (MARC_isbn != null) {
            am.put(MetadataField.FIELD_ISBN, MARC_isbn);
          } else if (MARC_isbn_alt != null) {
            am.put(MetadataField.FIELD_ISBN, MARC_isbn_alt);
          }
        } else {

          am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
          am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);

          // Set publication title
          if (MARC_title != null) {
            if (MARC_title_extended != null) {
              String title = MARC_title + " " + MARC_title_extended;
              am.put(MetadataField.FIELD_ARTICLE_TITLE, title);
            }  else {
              am.put(MetadataField.FIELD_ARTICLE_TITLE, MARC_title);
            }
          }

          if (MARC_publication_title != null) {
            log.debug3("publication_title = " + MARC_publication_title);
            am.put(MetadataField.FIELD_PUBLICATION_TITLE, MARC_publication_title);
          } else {
            log.debug3("publication_title IS NULL = " + MARC_publication_title);
          }

          // Set ISSN
          if (MARC_issn != null) {
            am.put(MetadataField.FIELD_ISSN, MARC_issn);
          }
        }

        // Emit 2016 metadata based on book_id info
        if (is_year_2016 && MARC_097a == null && !bookIDs.contains(MARC_bookid)) {
          bookIDs.add(MARC_bookid);
          emitter.emitMetadata(cu, am);
        }

        // Emit all metadata for 2019 and 2020 anyway
        if (is_year_2016 == false) {
          emitter.emitMetadata(cu, am);
        }

        ////////double check if the fix for 2019

        if (cuBase.contains("2019")) {
          log.debug3("Casalini-Metadata: Year 2019: cuBase = " + cuBase);
          if (MARC_uniq_local_id != null) {
            String local_id = MARC_uniq_local_id;
            if (MARC_uniq_local_id2 != null) {
              local_id = local_id + MARC_uniq_local_id2;
            }
            if (MARC_uniq_local_id3 != null) {
              local_id = local_id + MARC_uniq_local_id3;
            }

            String cuBaseWithLocalId = cuBase.replace("/", "") + "?unique_record_id=" + local_id;
            log.debug3("Casalini-Metadata: Year 2019: cuBaseWithLocalId  = " + cuBaseWithLocalId );

            am.replace(MetadataField.FIELD_ACCESS_URL, cuBaseWithLocalId);
          } else {
            log.debug3("Casalini-Metadata: Year 2019: cuBaseWithLocalId NO CHANGE" );

          }
        }


        /////////////
      }
      log.debug3(String.format("Casalini-Metadata: Metadata file source: %s, recordCount: %d", cu.getUrl(), recordCount));
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
      log.debug3("Casalini-Metadata: Mrc Record DataFieldCode: " + dataFieldCode + " SubFieldCode: " + subFieldCode + " has error");
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
      log.debug3("Casalini-Metadata: Mrc Record getMARCControlFieldData: " + data);
      return data;
    } else {
        log.debug3("Casalini-Metadata: Mrc Record getMARCControlFieldData: " + dataFieldCode + " return null");
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

}