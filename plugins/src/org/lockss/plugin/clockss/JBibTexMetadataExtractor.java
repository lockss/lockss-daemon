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

    private void extractMetadataFromBibEntry(BibTeXEntry bibEntry, ArticleMetadata am) {
        // ARTICLE TITLE
        Value title = bibEntry.getField(BibTeXEntry.KEY_TITLE);
        if (title instanceof StringValue) {
            String stitle = ((StringValue) title).toUserString();
            log.debug3("Title: " + stitle);
            am.put(MetadataField.FIELD_ARTICLE_TITLE, stitle);
        }

        // AUTHOR
        Value author = bibEntry.getField(BibTeXEntry.KEY_AUTHOR);
        if (author instanceof StringValue) {
            String sauthor = ((StringValue) author).toUserString();
            log.debug3("Author: " + sauthor);
            am.put(MetadataField.FIELD_AUTHOR, sauthor);
        }

        // DATE (YEAR)
        Value year = bibEntry.getField(BibTeXEntry.KEY_YEAR);
        if (year instanceof StringValue) {
            String syear = ((StringValue) year).toUserString();
            log.debug3("Date: " + syear);
            am.put(MetadataField.FIELD_DATE, syear);
        }

        // PUBLICATION TITLE
        Value journal = bibEntry.getField(BibTeXEntry.KEY_JOURNAL);
        if (journal instanceof StringValue) {
            String sjournal = ((StringValue) journal).toUserString();
            log.debug3("Publication Title (journal): " + sjournal);
            am.put(MetadataField.FIELD_PUBLICATION_TITLE, sjournal);
        } else {
            Value booktitle = bibEntry.getField(new Key("booktitle"));
            if (booktitle instanceof StringValue) {
                String sbooktitle = ((StringValue) booktitle).toUserString();
                log.debug3("Publication Title (book): " + sbooktitle);
                am.put(MetadataField.FIELD_PUBLICATION_TITLE, sbooktitle);
            }
        }

        // VOLUME
        Value volume = bibEntry.getField(BibTeXEntry.KEY_VOLUME);
        if (volume instanceof StringValue) {
            String svolume = ((StringValue) volume).toUserString();
            log.debug3("Volume: " + svolume);
            am.put(MetadataField.FIELD_VOLUME, svolume);
        }

        // ISSUE
        Value number = bibEntry.getField(BibTeXEntry.KEY_NUMBER);
        if (number instanceof StringValue) {
            String snumber = ((StringValue) number).toUserString();
            log.debug3("Issue: " + snumber);
            am.put(MetadataField.FIELD_ISSUE, snumber);
        }

        // START & END PAGES
        Value pages = bibEntry.getField(BibTeXEntry.KEY_PAGES);
        if (pages instanceof StringValue) {
            String spages = ((StringValue) pages).toUserString();
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

        // DOI
        Value doi = bibEntry.getField(new Key("doi"));
        if (doi instanceof StringValue) {
            String sdoi = ((StringValue) doi).toUserString();
            log.debug3("DOI: " + sdoi);
            am.put(MetadataField.FIELD_DOI, sdoi);
        }

        // ISSN
        Value issn = bibEntry.getField(new Key("issn"));
        if (issn instanceof StringValue) {
            String sissn = ((StringValue) issn).toUserString();
            log.debug3("ISSN: " + sissn);
            am.put(MetadataField.FIELD_ISSN, sissn);
        }

        // ISBN
        Value isbn = bibEntry.getField(new Key("isbn"));
        if (isbn instanceof StringValue) {
            String sisbn = ((StringValue) isbn).toUserString();
            log.debug3("ISBN: " + sisbn);
            am.put(MetadataField.FIELD_ISBN, sisbn);
        }

        // ACCESS URL
        Value url = bibEntry.getField(BibTeXEntry.KEY_URL);
        if (url instanceof StringValue) {
            String surl = ((StringValue) url).toUserString();
            log.debug3("Access URL: " + surl);
            am.put(MetadataField.FIELD_ACCESS_URL, surl);
        }
    }
}
