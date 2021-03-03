package org.lockss.plugin.clockss.casalini;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

import java.io.*;
import java.util.*;

import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.ControlField;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.Leader;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class MarcXmlToMarcBinarySchemaHelper implements FileMetadataExtractor {

    private static final Logger log = Logger.getLogger(MarcXmlToMarcBinarySchemaHelper.class);

    private static final String COLLECTION_NAME = "Monographs";
    private static final String TITLE = "title";
    private static final String AUTHOR = "author";
    private static final String AUTHOR2 = "author_alt";
    private static final String ISBN = "isbn";
    private static final String PUBLISHER = "publisher";
    private static final String ENDPAGE = "endpage";

    private static final String PUBLISHER_NAME = "Casalini";
    private static final String PUBLISHER_NAME_APPENDIX = " - " + PUBLISHER_NAME ;

    private static final Map<String,String> PublisherNameShortcutMap = new HashMap<String,String>();
    static {
        PublisherNameShortcutMap.put("Edizioni dell'Ateneo".toLowerCase(), "ATENEO");
        PublisherNameShortcutMap.put("Cadmo".toLowerCase(), "CADMO");
        PublisherNameShortcutMap.put("Centro per la filosofia italiana".toLowerCase(), "CADMO");
        PublisherNameShortcutMap.put("The Wolfsonian Foundation".toLowerCase(), "CADMO");
        PublisherNameShortcutMap.put("Cadmo".toLowerCase(), "CADMO");
        PublisherNameShortcutMap.put("Amalthea".toLowerCase(), "CADMO");
        PublisherNameShortcutMap.put("Casalini libri".toLowerCase(), "CASA");
        PublisherNameShortcutMap.put("Casalini Libri".toLowerCase(), "CASA");
        PublisherNameShortcutMap.put("CLUEB".toLowerCase(), "CLUEB");
        PublisherNameShortcutMap.put("Jaca book".toLowerCase(), "CLUEB");
        PublisherNameShortcutMap.put("Dipartimento di filosofia Università di Bologna".toLowerCase(), "CLUEB");
        PublisherNameShortcutMap.put("Petite plaisance".toLowerCase(), "CLUEB");
        PublisherNameShortcutMap.put("Eum".toLowerCase(), "CLUEB");
        PublisherNameShortcutMap.put("[s.n.]".toLowerCase(),"CLUEB");
        PublisherNameShortcutMap.put("Regione Emilia-Romagna".toLowerCase(), "CLUEB");
        PublisherNameShortcutMap.put("Ministero per i beni e le attività culturali Direzione generale per gli archivi".toLowerCase(), "CLUEB");
        PublisherNameShortcutMap.put("Faenza editrice".toLowerCase(), "CLUEB");
        PublisherNameShortcutMap.put("Università La Sapienza".toLowerCase(), "CLUEB");
        PublisherNameShortcutMap.put("Uranoscopo".toLowerCase(), "CLUEB");
        PublisherNameShortcutMap.put("Giardini editori e stampatori".toLowerCase(), "GIARDI");
        PublisherNameShortcutMap.put("Gruppo editoriale internazionale".toLowerCase(), "GEI");
        PublisherNameShortcutMap.put("Giardini".toLowerCase(), "GIARDI");
        PublisherNameShortcutMap.put("Giardini editori e stampatori".toLowerCase(), "GIARDI");
        PublisherNameShortcutMap.put("Istituti editoriali e poligrafici internazionali".toLowerCase(), "IEPI");
        PublisherNameShortcutMap.put("Università degli studi di Macerata".toLowerCase(), "IEPI");
        PublisherNameShortcutMap.put("Antenore".toLowerCase(), "IEPI");
    };


    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter) throws IOException, PluginException {
        
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
            String MARC_issn = getMARCData(record, "022", 'a');
            String MARC_title = getMARCData(record, "245", 'a');
            String MARC_pub_date =  getMARCData(record, "260", 'c');
            String MARC_publisher = getMARCData(record, "260", 'b');
            String MARC_publisher_alt = getMARCData(record, "264", 'b');
            String MARC_total_page = getMARCData(record, "300", 'a');
            String MARC_author =   getMARCData(record, "100", 'a');
            String MARC_author_alt =   getMARCData(record, "700", 'a');

            /*
            http://clockss-ingest.lockss.org/sourcefiles/casalini2012-released/2016/Monographs/CLUEB/2448043
            http://clockss-ingest.lockss.org/sourcefiles/casalini2012-released/2016/Monographs/CLUEB/2448043/2448043.pdf
            http://clockss-ingest.lockss.org/sourcefiles/casalini2012-released/2016/Monographs/CLUEB/2448043/2448043.pdf.md5sum
            http://clockss-ingest.lockss.org/sourcefiles/casalini2012-released/2016/Monographs/CLUEB/2448043/2449010.pdf
            http://clockss-ingest.lockss.org/sourcefiles/casalini2012-released/2016/Monographs/CLUEB/2448043/2449010.pdf.md5sum
            http://clockss-ingest.lockss.org/sourcefiles/casalini2012-released/2016/Monographs/CLUEB/2448043/2449011.pdf
            http://clockss-ingest.lockss.org/sourcefiles/casalini2012-released/2016/Monographs/CLUEB/2448043/2449011.pdf.md5sum
            http://clockss-ingest.lockss.org/sourcefiles/casalini2012-released/2016/Monographs/CLUEB/2448043/2449012.pdf
            http://clockss-ingest.lockss.org/sourcefiles/casalini2012-released/2016/Monographs/CLUEB/2448043/2449012.pdf.md5sum
            http://clockss-ingest.lockss.org/sourcefiles/casalini2012-released/2016/Monographs/CLUEB/2448043/2449013.pdf
            */
            //Casalini 2016 do not use this one
            //String MARC_pdf =  getMARCControlFieldData(record, "001");

            String MARC_bookid =   getMARCData(record, "097", 'a');
            String MARC_chapterid =   getMARCData(record, "097", 'c');


            String publisherCleanName = MARC_publisher.
                    trim().
                    replace(",", "").
                    replace(";", "").
                    replace(":", "")
                    .trim();
            String publisherShortCut = PublisherNameShortcutMap.get(publisherCleanName.toLowerCase());

            if (publisherShortCut == null) {
                log.debug(String.format("publisherShortCut is null: MARC_publisher: %s | publisherCleanName: %s",
                        MARC_publisher, publisherCleanName, publisherShortCut));
            }

            if (MARC_isbn != null) {
                am.put(MetadataField.FIELD_ISBN, MARC_isbn);
            }

            if (MARC_issn != null) {
                am.put(MetadataField.FIELD_ISSN, MARC_issn);
            }

            if (MARC_title != null) {
                am.put(MetadataField.FIELD_PUBLICATION_TITLE, MARC_title);
            }

            if ( MARC_pub_date != null) {
                am.put(MetadataField.FIELD_DATE, MARC_pub_date.replace(".", ""));
            }

            if (MARC_author == null) {
                am.put(MetadataField.FIELD_AUTHOR, MARC_author_alt);
            } else {
                am.put(MetadataField.FIELD_AUTHOR, MARC_author);
            }
            // They did not provide start page, just total number of page,
            if (MARC_total_page != null) {
                am.put(MetadataField.FIELD_END_PAGE, MARC_total_page);
            }

            if (MARC_publisher == null) {
                am.put(MetadataField.FIELD_PUBLISHER, MARC_publisher_alt.replace(",", ""));
            } else {
                am.put(MetadataField.FIELD_PUBLISHER, MARC_publisher.replace(",", ""));
            }

            // This part handle Casanili special request for publisher name
            if (MARC_publisher == null) {
                am.put(MetadataField.FIELD_PUBLISHER, PUBLISHER_NAME);
            }  else {
                if (!MARC_publisher.equalsIgnoreCase(PUBLISHER_NAME)) {
                    am.put(MetadataField.FIELD_PUBLISHER, MARC_publisher.replace(",", "") + PUBLISHER_NAME_APPENDIX);
                } else {
                    am.put(MetadataField.FIELD_PUBLISHER, MARC_publisher);
                }
            }

            // Read type from "leader" record and set it accordingly
            /*
            Leader byte 07 “a” = Book (monographic component part)
            Leader byte 07 “m” = Book
            Leader byte 07 “s” = Journal
            Leader byte 07 “b” = Journal (serial component part)

             */
            Leader leader = record.getLeader();
            // return <code>char[]</code>- implementation defined values, it return chars at 07, 08 position
            char publication_type = leader.getImplDefined1()[0];

            if (publication_type == 'm' || publication_type == 'a') {
                am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_BOOKVOLUME);
                am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_BOOK);
            }

            if (publication_type == 's' || publication_type == 'b') {
                am.put(MetadataField.FIELD_ARTICLE_TYPE, MetadataField.ARTICLE_TYPE_JOURNALARTICLE);
                am.put(MetadataField.FIELD_PUBLICATION_TYPE, MetadataField.PUBLICATION_TYPE_JOURNAL);
            }

            String MARC_pdf =  String.format("%s/%s/%s/%s", COLLECTION_NAME, publisherShortCut, MARC_bookid, MARC_bookid);

            log.debug3("MARC_pdf: " + MARC_pdf);

            if (MARC_bookid != null && MARC_chapterid != null ) {
                log.debug3(String.format("Emit chapter: MARC_bookid %s | MARC_chapterid: %s ",
                        MARC_bookid, MARC_chapterid));
            } else if (MARC_chapterid == null) {
                log.debug3(String.format("Do not emit chapter: MARC_bookid %s ", MARC_bookid));
            }

            if (MARC_pdf != null) {
                String cuBase = FilenameUtils.getFullPath(cu.getUrl());
                String fullPathFile = UrlUtil.minimallyEncodeUrl(cuBase + MARC_pdf + ".pdf");
                log.debug3("MARC_pdf: " + MARC_pdf + ", fullPathFile = " + fullPathFile);
                am.put(MetadataField.FIELD_ACCESS_URL, fullPathFile);

                // Only emit the books metadata

                if (!bookIDs.contains(MARC_bookid)) {
                    bookIDs.add(MARC_bookid);
                    emitter.emitMetadata(cu, am);
                } else {
                    log.debug3("bookID already exist: " + MARC_bookid);
                }

            } else {
                log.debug3("MARC_pdf field is not used");
            }

            if (MARC_bookid != null && MARC_chapterid != null ) {
                log.debug3(String.format("Emit chapter: MARC_bookid %s | MARC_chapterid: %s ",
                        MARC_bookid, MARC_chapterid));
            } else if (MARC_chapterid == null) {
                log.debug3(String.format("Do not emit chapter: MARC_bookid %s ", MARC_bookid));
            }
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
