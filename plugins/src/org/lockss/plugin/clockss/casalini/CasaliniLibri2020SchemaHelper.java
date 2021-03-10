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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.io.FilenameUtils;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import java.io.InputStream;
import java.io.FileInputStream;

import org.lockss.plugin.clockss.MetadataStringHelperUtilities;
import org.lockss.util.UrlUtil;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.ControlField;
import java.util.List;

import org.lockss.util.Logger;

/**
 *  Marc4J library https://github.com/marc4j/marc4j
 */
public class CasaliniLibri2020SchemaHelper implements FileMetadataExtractor {

  private static final Logger log = Logger.getLogger(CasaliniLibri2020SchemaHelper.class);

  /**
  The following is an example of a "mrc" record with some modification to avoid Comment Illegal Unicode Sequences
s
  =LDR  01276nam a2200373 i 4500
  =001  2249531
  =003  ItFiC
  =005  20180726030539.0
  =007  ### can not use original content here, for illegal unicode error
  =008  060828s2004\\\\it\\\\\\s\\\\\000\0\ita\d
  =020  \\$a8884760313
  =040  \\$aItFiC$beng$cItFiC
  =043  \\$ae-gr
  =045  \\$ad4i-
  =050  \4$aDF77$b.G35 2004
  =082  14$a880$214
  =082  14$a306$214
  =082  14$a184$214
  =082  14$a938$214
  =100  1\$aGallo, Italo.
  =245  10$aRiflessioni e divagazioni sulla grecità /$cItalo Gallo.
  =260  \\$aRoma :$bEdizioni dell'Ateneo,$c2004.
  =300  \\$a91 p.
  =490  0\$aFilologia e critica / Università degli studi di Urbino ;$v92
  =504  \\$aIncludes bibliographical references.
  =500  \\$aCollection of already publ. writings, now slightly rev.
  =500  \\$aHalf title: Centro internazionale di studi sulla grecità ...
  =500  \\$aThe ISBN on back cover, 88-8476-016-X, is incorrect.
  =650  \0$aGreek literature$xHistory and criticism.
  =650  \0$aPhilosophy, Ancient.
  =651  \0$aGreece$xCivilization.
  =856  40$uhttp://digital.casalini.it/8884760313
  =900  \\$a(c) Casalini Libri, 50014 Fiesole (Italy) - www.casalini.it
  =910  \\$aBibliographic data$eTorrossa Fulltext Resource$gCasalini Libri
   */

  private static final String TITLE = "title";
  private static final String AUTHOR = "author";
  private static final String AUTHOR2 = "author_alt";
  private static final String ISBN = "isbn";
  private static final String PUBLISHER = "publisher";
  private static final String ENDPAGE = "endpage";

  private static final String PUBLISHER_NAME = "Casalini";
  private static final String PUBLISHER_NAME_APPENDIX = " - " + PUBLISHER_NAME ;


  @Override
  public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {

    InputStream input = cu.getUnfilteredInputStream();

    MarcReader reader = new MarcStreamReader(input);
    int recordCount = 0;

    while (reader.hasNext()) {

      ArticleMetadata am = new ArticleMetadata();

      Record record = reader.next();
      recordCount++;

      String MARC_isbn = getMARCData(record, "020", 'a');
      String MARC_title = getMARCData(record, "245", 'a');
      String MARC_pub_date =  getMARCData(record, "260", 'c');
      String MARC_pub_date_alt =  getMARCData(record, "264", 'c');
      String MARC_publisher = getMARCData(record, "260", 'b');
      String MARC_total_page = getMARCData(record, "300", 'a');
      String MARC_author =   getMARCData(record, "100", 'a');
      String MARC_author_alt =   getMARCData(record, "700", 'a');
      String MARC_pdf =  getMARCControlFieldData(record, "001");

      // Only count metadata when there is a PDF file
      if (MARC_pdf != null) {
        am.put(MetadataField.FIELD_ISBN,  MARC_isbn);
        am.put(MetadataField.FIELD_PUBLICATION_TITLE,  MARC_title);
        if (MARC_pub_date != null) {
          am.put(MetadataField.FIELD_DATE, MetadataStringHelperUtilities.cleanupPubDate(MARC_pub_date));
        } else {
          am.put(MetadataField.FIELD_DATE, MetadataStringHelperUtilities.cleanupPubDate(MARC_pub_date_alt));
        }
        if (MARC_author == null) {
          am.put(MetadataField.FIELD_AUTHOR, MARC_author_alt);
        } else {
          am.put(MetadataField.FIELD_AUTHOR, MARC_author);
        }
        // They did not provide start page, just total number of page,
        // start page and end page will cause it thinks it is chapter, instead of whole book
        //am.put(MetadataField.FIELD_END_PAGE, MARC_total_page);

        String cuBase = FilenameUtils.getFullPath(cu.getUrl());
        String fullPathFile = UrlUtil.minimallyEncodeUrl(cuBase + MARC_pdf + ".pdf");
        am.put(MetadataField.FIELD_ACCESS_URL, fullPathFile);

        // Prepare raw metadata
        am.putRaw(TITLE,  MARC_title);
        am.putRaw(AUTHOR, MARC_author);
        am.putRaw(AUTHOR2, MARC_author_alt);
        am.putRaw(ISBN, MARC_isbn);

        if (MARC_publisher == null) {
          am.put(MetadataField.FIELD_PUBLISHER, PUBLISHER_NAME);
        }  else {
          if (!MARC_publisher.equalsIgnoreCase(PUBLISHER_NAME)) {
            am.put(MetadataField.FIELD_PUBLISHER, MARC_publisher.replace(",", "") + PUBLISHER_NAME_APPENDIX);
          } else {
            am.put(MetadataField.FIELD_PUBLISHER, MARC_publisher);
          }
        }

        am.putRaw(PUBLISHER, MARC_publisher);
        am.putRaw(ENDPAGE, MARC_total_page);
      }
      
      emitter.emitMetadata(cu, am);
    }
    log.debug3(String.format("Metadata file source: %s, recordCount: %d", cu.getUrl(), recordCount));
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
      return data;
    } else {
        log.debug3("Mrc Record getMARCControlFieldData: " + dataFieldCode + " return null");
        return null;
    }
  }
  
}