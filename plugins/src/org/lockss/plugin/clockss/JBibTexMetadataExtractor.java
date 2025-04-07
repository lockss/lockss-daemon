package org.lockss.plugin.clockss;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.jbibtex.*;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;


// Publisher Bibtex may not be consistent, also some KEY may not be defined by the Jbibtex library
/*
@article{Sandoval2024Recent,
	journal={healthbook TIMES Oncology Hematology},
	doi={10.36000/HBT.OH.2024.19.137},
	number=1,
	publisher={THE HEALTHBOOK COMPANY LTD.},
	title={Recent Progress in Breast Cancer Treatment},
	volume=19,
	author={Sandoval, Jose Luis},
	pages={36--41},
	date={2024-03-26},
	year=2024,
	month=3,
	day=26,
}

 */

public class JBibTexMetadataExtractor implements FileMetadataExtractor {

    private static final Logger log = Logger.getLogger(JBibTexMetadataExtractor.class);

    private static MultiMap tagMap = new MultiValueMap();
    static {
        tagMap.put("author", MetadataField.FIELD_AUTHOR);
        tagMap.put("date", MetadataField.FIELD_DATE);
        tagMap.put("title", MetadataField.FIELD_ARTICLE_TITLE);
        tagMap.put("journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
        tagMap.put("volume", MetadataField.FIELD_VOLUME);
        tagMap.put("issue", MetadataField.FIELD_ISSUE);
        tagMap.put("firstpage", MetadataField.FIELD_START_PAGE);
        tagMap.put("lastpage", MetadataField.FIELD_END_PAGE);
        tagMap.put("doi", MetadataField.FIELD_DOI);
        tagMap.put("issn", MetadataField.FIELD_ISSN);
        tagMap.put("isbn", MetadataField.FIELD_ISBN);
        tagMap.put("abtract_html_url", MetadataField.FIELD_ACCESS_URL);
    }

    private ArticleMetadata am;

    @Override
    public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
            throws IOException, PluginException {
        ArticleMetadata md = extract(target, cu);
        if (md != null) {
            emitter.emitMetadata(cu, md);
        }
    }

    public final ArticleMetadata extract(MetadataTarget target, CachedUrl cu)
            throws IOException, PluginException {
        if (cu == null) {
            throw new IllegalArgumentException();
        }

        log.debug2("JBibTexMetadataExtractor Parsing: " + cu.getUrl());

        am = new ArticleMetadata();
        Reader reader = null;

        // Parse using JBibTeX
        BibTeXParser parser = null;
        try {
            reader = cu.openForReading();

            parser = new BibTeXParser();
            BibTeXDatabase database = parser.parse(reader);

            // Print out entries
            for (Map.Entry<Key, BibTeXEntry> entry : database.getEntries().entrySet()) {
                BibTeXEntry bibEntry = entry.getValue();

                extractMetadataFromBibEntry(bibEntry, am);
            }
        } catch (ParseException e) {
            log.debug3("Failed to parse BibTeX: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            log.debug3("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.debug3("Failed to close reader: " + e.getMessage());
                }
            }
        }

        am.cook(tagMap);
        return am;
    }

    {}

    private void extractMetadataFromBibEntry(BibTeXEntry bibEntry, ArticleMetadata am) {

        log.debug3("BibTeX Entry Fields:");
        for (Map.Entry<Key, Value> field : bibEntry.getFields().entrySet()) {
            log.debug3("  Key = " + field.getKey().getValue() + " ; Value = " + field.getValue().toUserString());
            am.putRaw(field.getKey().getValue(),field.getValue().toUserString());
        }


        putField(bibEntry.getField(BibTeXEntry.KEY_TITLE), MetadataField.FIELD_ARTICLE_TITLE, "Title");
        putField(bibEntry.getField(BibTeXEntry.KEY_AUTHOR), MetadataField.FIELD_AUTHOR, "Author");

        Value journal = bibEntry.getField(BibTeXEntry.KEY_JOURNAL);
        if (journal != null) {
            String sjournal = journal.toUserString();
            log.debug3("Publication Title (journal): " + sjournal);
            am.put(MetadataField.FIELD_PUBLICATION_TITLE, sjournal);
        } else {
            Value booktitle = bibEntry.getField(new Key("booktitle"));
            if (booktitle != null) {
                String sbooktitle = booktitle.toUserString();
                log.debug3("Publication Title (book): " + sbooktitle);
                am.put(MetadataField.FIELD_PUBLICATION_TITLE, sbooktitle);
            }
        }

        putField(bibEntry.getField(BibTeXEntry.KEY_VOLUME), MetadataField.FIELD_VOLUME, "Volume");
        putField(bibEntry.getField(BibTeXEntry.KEY_NUMBER), MetadataField.FIELD_ISSUE, "Issue");

        Value pages = bibEntry.getField(BibTeXEntry.KEY_PAGES);
        if (pages != null) {
            String spages = pages.toUserString();
            String[] pageParts = spages.split("--|–|-");
            if (pageParts.length > 0) {
                String sstart = pageParts[0].trim();
                log.debug3("Start Page: " + sstart);
                am.put(MetadataField.FIELD_START_PAGE, sstart);
            }
            if (pageParts.length > 1) {
                String send = pageParts[1].trim();
                log.debug3("End Page: " + send);
                am.put(MetadataField.FIELD_END_PAGE, send);
            }
        }

        putField(bibEntry.getField(new Key("doi")), MetadataField.FIELD_DOI, "DOI");
        putField(bibEntry.getField(new Key("issn")), MetadataField.FIELD_ISSN, "ISSN");
        putField(bibEntry.getField(new Key("isbn")), MetadataField.FIELD_ISBN, "ISBN");
        putField(bibEntry.getField(BibTeXEntry.KEY_URL), MetadataField.FIELD_ACCESS_URL, "Access URL");

        Value dateVal = bibEntry.getField(new Key("date"));
        log.debug3("Raw date value: " + (dateVal != null ? dateVal.toUserString() : "null"));

        if (dateVal != null) {
            String sdate = dateVal.toUserString().trim();
            log.debug3("Raw date value not null, using BibTeX 'date' field: " + sdate);
            am.put(MetadataField.FIELD_DATE, sdate);
        } else {

            Value yearVal = bibEntry.getField(BibTeXEntry.KEY_YEAR);
            Value monthVal = bibEntry.getField(new Key("month"));
            Value dayVal = bibEntry.getField(new Key("day"));

            String year = (yearVal != null) ? yearVal.toUserString().trim() : "";
            String month = (monthVal != null) ? monthVal.toUserString().trim() : "";
            String day = (dayVal != null) ? dayVal.toUserString().trim() : "";

            log.debug3("Raw year value: " + (yearVal != null ? yearVal.toUserString() : "null"));
            log.debug3("Raw month value: " + (monthVal != null ? monthVal.toUserString() : "null"));
            log.debug3("Raw day value: " + (dayVal != null ? dayVal.toUserString() : "null"));


            String pubDate = null;

            try {
                if (!month.isEmpty() && !day.isEmpty() && !year.isEmpty()) {
                    int m = Integer.parseInt(month);
                    int d = Integer.parseInt(day);
                    pubDate = String.format("%02d-%02d-%s", m, d, year);
                } else if (!year.isEmpty()) {
                    pubDate = year;
                }

                if (pubDate != null) {
                    log.debug3("Formatted Publication Date: " + pubDate);
                    am.put(MetadataField.FIELD_DATE, pubDate);
                }
            } catch (NumberFormatException e) {
                log.debug3("Invalid numeric date in BibTeX entry: " + e.getMessage());
            }
        }

    }

    private void putField(Value value, MetadataField field, String label) {
        if (value != null) {
            String sval = value.toUserString();
            log.debug3(label + ": " + sval);
            am.put(field, sval);
        }
    }
}
